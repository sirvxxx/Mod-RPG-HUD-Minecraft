package dev.rpghud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Arrays;

/**
 * HUD bergaya RPG (v4, mengikuti sketsa):
 *
 *            [ nama ]
 *        .-------------.
 *       /  bar melengkung \   [ XYZ x y z ]
 *      |   (kepala skin)   |  [ 204 FPS    ]
 *       \  merah/oranye/abu /
 *        '-------------'
 *          [ Lv 20 ]  (hijau, + bar progress EXP)
 *
 * Cincin kotak-kotak (satu kotak = satu langkah sudut 9 derajat), arah isi dari bawah ke atas:
 *   kiri (180 derajat)           = HEALTH, 20 kotak (1 kotak = 1 HP)
 *   kanan-atas (90 derajat)      = HUNGER, 10 kotak
 *   kanan-bawah (90 derajat)     = ARMOR,  10 kotak
 */
public final class HudRenderer {
    // ---- dimensi layout (piksel GUI sebelum scale)
    public static final int CIRCLE_D = 92;
    private static final int CIRCLE_R = CIRCLE_D / 2;
    public static final int NAME_H = 15;
    public static final int INFO_H = 19;
    public static final int FPS_W = 66;
    public static final int LEVEL_H = 15;
    private static final int SIDE_Y = CIRCLE_D / 2 - 21;   // panel samping rata tengah lingkaran
    private static final int SIDE_GAP = 3;
    private static final int PANEL_GAP = 4;

    // cincin bar: radius dalam / luar (tebal 7px: garis luar 1px + isi 5px + garis dalam 1px)
    private static final int BAND_RI = 33;
    private static final int BAND_RO = 40;
    private static final double CELL_DEG = 9.0;     // 40 kotak x 9 derajat = 360 derajat

    // ---- palet logam: emas tua / cokelat, highlight terang di sisi atas
    private static final int OUTLINE         = 0xFF0A0806;
    private static final int METAL_MID       = 0xFF9A7230;
    private static final int METAL_HL_TOP    = 0xFFF6DE8E;
    private static final int METAL_HL_LEFT   = 0xFFD4AA52;
    private static final int METAL_SH_BOTTOM = 0xFF5A3C16;
    private static final int METAL_SH_RIGHT  = 0xFF6C4A1E;
    private static final int BRONZE_LINE     = 0xFF6B4A1E;
    private static final int INNER_TOP       = 0xFF21190F;
    private static final int INNER_BOTTOM    = 0xFF0C0907;
    private static final int GOLD_TEXT       = 0xFFFFD36B;
    private static final int LEVEL_GREEN     = 0xFF9CFF7A;

    private static final int HP_BASE    = 0xFFE02A2A;
    private static final int FOOD_BASE  = 0xFFFF9F1C;
    private static final int ARMOR_BASE = 0xFFC4CAD2;
    private static final int GHOST_BASE = 0xFFEBDDB8;
    private static final int SAT_ACCENT = 0xFFFFF2A0;

    /** true saat layar pengaturan / edit terbuka (layar itu menggambar preview sendiri). */
    public static boolean suppressHud = false;

    private static long fpsWindowStart = System.currentTimeMillis();
    private static int frames;
    private static int fps;

    private static float shownHp = -1f;
    private static long lastNs = System.nanoTime();

    // pixel cincin yang dihitung sekali (posisi, sektor, kotak, posisi di dalam kotak, tingkat shade)
    private static int[] bandX, bandY, bandSec, bandCell, bandShade;
    private static float[] bandPf;
    private static int bandCount;

    private static final float[] HP_C = new float[20];
    private static final float[] GH_C = new float[20];
    private static final float[] FO_C = new float[10];
    private static final float[] SA_C = new float[10];
    private static final float[] AR_C = new float[10];

    private HudRenderer() {}

    // ------------------------------------------------------------------ ukuran

    public static boolean circleVisible(HudConfig c) {
        return c.showFace || c.showHealth || c.showHunger || c.showArmor;
    }

    private static String coordsText(LocalPlayer p) {
        return (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " " + (int) Math.floor(p.getZ());
    }

    private static int coordsWidth() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 112;
        return Math.max(100, mc.font.width("XYZ " + coordsText(mc.player)) + 18);
    }

    public static int width(HudConfig c) {
        boolean circle = circleVisible(c);
        int w = (circle || c.showName) ? CIRCLE_D : 0;
        int side = 0;
        if (c.showCoords) side = Math.max(side, coordsWidth());
        if (c.showFps) side = Math.max(side, FPS_W);
        if (side > 0) w = Math.max(w, (circle ? CIRCLE_D + SIDE_GAP : 0) + side);
        return w;
    }

    public static int height(HudConfig c) {
        boolean circle = circleVisible(c);
        int ctop = c.showName ? NAME_H + 2 : 0;
        int h = c.showName ? NAME_H : 0;
        if (circle) h = ctop + CIRCLE_D + (c.showLevel ? LEVEL_H - 8 : 0);
        int sy = circle ? ctop + SIDE_Y : ctop;
        int bottom = 0;
        if (c.showCoords) { bottom = sy + INFO_H; sy += INFO_H + PANEL_GAP; }
        if (c.showFps) bottom = sy + INFO_H;
        return Math.max(h, bottom);
    }

    // ------------------------------------------------------------------ entry point

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        tickCounters(p);
        HudConfig c = HudConfig.get();
        if (suppressHud || !c.enabled || p == null) return;
        draw(g, mc, p, c);
    }

    public static void preview(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        draw(g, mc, mc.player, HudConfig.get());
    }

    private static void tickCounters(LocalPlayer p) {
        long now = System.currentTimeMillis();
        frames++;
        if (now - fpsWindowStart >= 1000) {
            fps = (int) (frames * 1000L / (now - fpsWindowStart));
            frames = 0;
            fpsWindowStart = now;
        }
        if (p != null) {
            float hp = p.getHealth();
            long ns = System.nanoTime();
            float dt = Math.min(0.25f, (ns - lastNs) / 1_000_000_000f);
            lastNs = ns;
            if (shownHp < 0f || hp >= shownHp) shownHp = hp;
            else shownHp -= (shownHp - hp) * Math.min(1f, dt * 2.5f);
        }
    }

    // ------------------------------------------------------------------ gambar utama

    private static void draw(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c) {
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) c.x, (float) c.y);
        pose.scale(c.scale, c.scale);

        boolean circle = circleVisible(c);
        int ctop = c.showName ? NAME_H + 2 : 0;

        if (c.showName) {
            badge(g, 0, 0, CIRCLE_D, NAME_H);
            String name = fit(mc, p.getName().getString(), CIRCLE_D - 14);
            g.text(mc.font, name, (CIRCLE_D - mc.font.width(name)) / 2, 3, GOLD_TEXT, true);
        }
        if (circle) drawCircle(g, mc, p, c, ctop);

        int sx = circle ? CIRCLE_D + SIDE_GAP : 0;
        int sy = circle ? ctop + SIDE_Y : ctop;
        if (c.showCoords) {
            drawCoords(g, mc, p, sx, sy, coordsWidth());
            sy += INFO_H + PANEL_GAP;
        }
        if (c.showFps) drawFps(g, mc, sx, sy);

        pose.popMatrix();
    }

    // ------------------------------------------------------------------ lingkaran utama

    private static void drawCircle(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c, int top) {
        final int cx = CIRCLE_R, cy = top + CIRCLE_R;

        // bayangan + rim logam (highlight kiri-atas, gelap kanan-bawah)
        disc(g, cx + 1, cy + 2, CIRCLE_R + 1, 0x55000000);
        disc(g, cx, cy, CIRCLE_R, OUTLINE);
        disc(g, cx, cy, CIRCLE_R - 1, METAL_HL_TOP);
        disc(g, cx + 1, cy + 1, CIRCLE_R - 2, METAL_SH_BOTTOM);
        disc(g, cx, cy, CIRCLE_R - 2, METAL_MID);
        disc(g, cx, cy, CIRCLE_R - 3, BRONZE_LINE);                  // garis ukir di rim
        disc(g, cx, cy, CIRCLE_R - 4, METAL_MID);
        disc(g, cx, cy, BAND_RO + 1, 0xFF080605);                    // alas gelap tempat cincin kotak

        // cincin dalam (cekung) + alas kepala
        disc(g, cx, cy, BAND_RI, METAL_HL_LEFT);
        disc(g, cx - 1, cy - 1, BAND_RI - 1, METAL_SH_BOTTOM);
        disc(g, cx, cy, BAND_RI - 2, 0xFF14100B);

        // ---- nilai -> pecahan per kotak
        float max = Math.max(1f, p.getMaxHealth());
        float hp = p.getHealth();
        float ghost = shownHp < 0f ? hp : shownHp;
        int food = p.getFoodData().getFoodLevel();
        float sat = Math.min(p.getFoodData().getSaturationLevel(), food);
        int arm = Math.min(20, p.getArmorValue());
        cells(HP_C, 20, hp, max);
        cells(GH_C, 20, ghost, max);
        cells(FO_C, 10, food, 20f);
        cells(SA_C, 10, sat, 20f);
        cells(AR_C, 10, arm, 20f);

        int hpBase = HP_BASE;
        if (hp / max < 0.3f) hpBase = mul(hpBase, 0.8f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 140.0));

        curvedBars(g, cx, cy, hpBase, c.showHealth, c.showHunger, c.showArmor);

        // ---- kepala skin 40x40 (8x8 diperbesar 5x) + layer topi
        if (c.showFace) {
            Identifier skin = SkinHelper.bodyTexture(p);
            g.blit(RenderPipelines.GUI_TEXTURED, skin, cx - 20, cy - 20, 8f, 8f, 40, 40, 8, 8, 64, 64);
            g.blit(RenderPipelines.GUI_TEXTURED, skin, cx - 20, cy - 20, 40f, 8f, 40, 40, 8, 8, 64, 64);
        }

        // ---- kotak level: hanya teks "Lv N", bingkai logam di tengah bawah lingkaran
        if (c.showLevel) {
            String lv = "Lv " + p.experienceLevel;
            int tw = mc.font.width(lv);
            int bw = Math.max(54, tw + 16);
            int bx = (CIRCLE_D - bw) / 2;
            int by = top + CIRCLE_D - 8;
            badge(g, bx, by, bw, LEVEL_H);
            g.text(mc.font, lv, bx + (bw - tw) / 2, by + 3, LEVEL_GREEN, true);
        }
    }

    private static void cells(float[] out, int n, float value, float max) {
        for (int k = 0; k < n; k++) out[k] = clamp01(value * n / max - k);
    }

    // ------------------------------------------------------------------ cincin kotak-kotak melengkung

    /** Hitung sekali: tiap pixel cincin -> sektor, kotak, posisi dalam kotak, shade. Celah antar kotak dibuang. */
    private static void initBand() {
        if (bandX != null) return;
        final int RO = BAND_RO, RI = BAND_RI;
        int cap = (2 * RO) * (2 * RO);
        int[] xs = new int[cap], ys = new int[cap], secs = new int[cap], cls = new int[cap], shs = new int[cap];
        float[] pfs = new float[cap];
        int n = 0;
        for (int dy = -RO; dy < RO; dy++) {
            double fy = dy + 0.5;
            for (int dx = -RO; dx < RO; dx++) {
                double fx = dx + 0.5;
                double d2 = fx * fx + fy * fy;
                if (d2 < (double) RI * RI || d2 >= (double) RO * RO) continue;
                double r = Math.sqrt(d2);
                double theta = Math.toDegrees(Math.atan2(fx, -fy));      // 0 = atas, searah jarum jam
                if (theta < 0) theta += 360.0;

                int sec, cellsN;
                double l;                                                 // jarak dari ujung bawah sektor
                if (theta >= 180.0)      { sec = 0; cellsN = 20; l = theta - 180.0; }   // kiri: HEALTH
                else if (theta < 90.0)   { sec = 1; cellsN = 10; l = 90.0 - theta; }    // kanan-atas: HUNGER
                else                     { sec = 2; cellsN = 10; l = 180.0 - theta; }   // kanan-bawah: ARMOR

                int k = Math.min(cellsN - 1, (int) Math.floor(l / CELL_DEG));
                double p = l - k * CELL_DEG;
                double edgeDeg = Math.toDegrees(1.0 / r);                 // garis pembatas ~1px
                boolean edge = r < RI + 1.0 || r >= RO - 1.0              // garis dalam & luar
                        || p < edgeDeg                                    // garis di awal kotak
                        || (k == cellsN - 1 && p > CELL_DEG - edgeDeg);   // garis penutup sektor

                xs[n] = dx; ys[n] = dy; secs[n] = sec; cls[n] = k;
                pfs[n] = (float) Math.max(0.0, Math.min(1.0, (p - edgeDeg) / (CELL_DEG - edgeDeg)));
                shs[n] = edge ? 3 : (r < RI + 2.0 ? 0 : (r >= RO - 2.0 ? 2 : 1));
                n++;
            }
        }
        bandX = Arrays.copyOf(xs, n); bandY = Arrays.copyOf(ys, n);
        bandSec = Arrays.copyOf(secs, n); bandCell = Arrays.copyOf(cls, n);
        bandShade = Arrays.copyOf(shs, n); bandPf = Arrays.copyOf(pfs, n);
        bandCount = n;
    }

    private static final float[] SHADE_K = {0.78f, 1.0f, 1.25f, 0.32f};   // indeks 3 = garis tepi
    private static final int[] EMPTY = {0xFF1E1511, 0xFF2C211A, 0xFF3A2D24, 0xFF0B0705};

    private static void curvedBars(GuiGraphicsExtractor g, int cx, int cy, int hpBase,
                                   boolean showHp, boolean showFood, boolean showArmor) {
        initBand();
        int runY = 0, runX0 = 0, runX1 = 0, runCol = 0;
        for (int i = 0; i < bandCount; i++) {
            int sec = bandSec[i];
            if ((sec == 0 && !showHp) || (sec == 1 && !showFood) || (sec == 2 && !showArmor)) continue;
            int k = bandCell[i], sh = bandShade[i];
            float pf = bandPf[i];
            int col;
            int base = 0;
            boolean accent = false;
            switch (sec) {
                case 0 -> {
                    float f = HP_C[k];
                    if (f >= 0.999f || pf < f) base = hpBase;
                    else if (GH_C[k] >= 0.999f || pf < GH_C[k]) base = GHOST_BASE;
                }
                case 1 -> {
                    float f = FO_C[k];
                    if (f >= 0.999f || pf < f) {
                        base = FOOD_BASE;
                        accent = sh == 2 && (SA_C[k] >= 0.999f || pf < SA_C[k]);
                    }
                }
                default -> {
                    float f = AR_C[k];
                    if (f >= 0.999f || pf < f) base = ARMOR_BASE;
                }
            }
            if (base == 0) col = EMPTY[sh];
            else col = accent ? SAT_ACCENT : mul(base, SHADE_K[sh]);

            int y = bandY[i], x = bandX[i];
            if (runCol != 0 && col == runCol && y == runY && x == runX1) {
                runX1 = x + 1;
            } else {
                if (runCol != 0) g.fill(cx + runX0, cy + runY, cx + runX1, cy + runY + 1, runCol);
                runCol = col; runY = y; runX0 = x; runX1 = x + 1;
            }
        }
        if (runCol != 0) g.fill(cx + runX0, cy + runY, cx + runX1, cy + runY + 1, runCol);
    }

    // ------------------------------------------------------------------ panel samping

    private static void drawCoords(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, int x, int y, int w) {
        frame(g, x, y, w, INFO_H);
        String label = "XYZ ";
        g.text(mc.font, label, x + 8, y + 6, GOLD_TEXT, true);
        g.text(mc.font, coordsText(p), x + 8 + mc.font.width(label), y + 6, 0xFFFFFFFF, true);
    }

    private static void drawFps(GuiGraphicsExtractor g, Minecraft mc, int x, int y) {
        frame(g, x, y, FPS_W, INFO_H);
        String num = String.valueOf(fps);
        String label = " FPS";
        int total = mc.font.width(num) + mc.font.width(label);
        int fx = x + (FPS_W - total) / 2;
        int col = fps >= 60 ? 0xFF7CFC7C : (fps >= 30 ? 0xFFFFE066 : 0xFFFF6B6B);
        g.text(mc.font, num, fx, y + 6, col, true);
        g.text(mc.font, label, fx + mc.font.width(num), y + 6, GOLD_TEXT, true);
    }

    // ------------------------------------------------------------------ komponen gambar

    /** Frame logam: bevel 3D (terang di atas/kiri), garis perunggu, sudut terpotong, stud emas, bayangan. */
    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 1, y + 1, x + w + 3, y + h + 3, 0x22000000);
        g.fill(x + 2, y + 3, x + w + 2, y + h + 2, 0x44000000);

        g.fill(x + 1, y, x + w - 1, y + h, OUTLINE);
        g.fill(x, y + 1, x + w, y + h - 1, OUTLINE);

        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, METAL_MID);
        g.fill(x + 1, y + 2, x + w - 1, y + h - 2, METAL_MID);

        g.fill(x + 2, y + 1, x + w - 2, y + 2, METAL_HL_TOP);
        g.fill(x + 1, y + 2, x + 2, y + h - 2, METAL_HL_LEFT);
        g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, METAL_SH_BOTTOM);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, METAL_SH_RIGHT);

        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, BRONZE_LINE);
        vgrad(g, x + 4, y + 4, w - 8, h - 8, INNER_TOP, INNER_BOTTOM);
        g.fill(x + 4, y + 4, x + w - 4, y + 5, 0xFF050403);
        g.fill(x + 4, y + 5, x + 5, y + h - 4, 0xFF050403);
        g.fill(x + 4, y + h - 5, x + w - 4, y + h - 4, 0x40E0BB62);

        int st = 0xFFFFF0B8;
        g.fill(x + 2, y + 2, x + 4, y + 4, st);
        g.fill(x + w - 4, y + 2, x + w - 2, y + 4, st);
        g.fill(x + 2, y + h - 4, x + 4, y + h - 2, st);
        g.fill(x + w - 4, y + h - 4, x + w - 2, y + h - 2, st);
    }

    /** Kapsul logam ramping untuk nama & level. */
    private static void badge(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 1, y + 2, x + w + 1, y + h + 2, 0x55000000);
        g.fill(x + 1, y, x + w - 1, y + h, OUTLINE);
        g.fill(x, y + 1, x + w, y + h - 1, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, METAL_MID);
        g.fill(x + 2, y + 1, x + w - 2, y + 2, METAL_HL_TOP);
        g.fill(x + 1, y + 2, x + 2, y + h - 2, METAL_HL_LEFT);
        g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, METAL_SH_BOTTOM);
        vgrad(g, x + 3, y + 3, w - 6, h - 6, INNER_TOP, INNER_BOTTOM);
    }

    private static void vgrad(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bottom) {
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            g.fill(x, y + i, x + w, y + i + 1, lerp(top, bottom, t));
        }
    }

    /** Lingkaran terisi: satu fill per baris. */
    private static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy < r; dy++) {
            double fy = dy + 0.5;
            int half = (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - fy * fy)));
            if (half > 0) g.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    // ------------------------------------------------------------------ util

    private static String fit(Minecraft mc, String s, int maxW) {
        while (s.length() > 1 && mc.font.width(s) > maxW) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    private static int lerp(int a, int b, float t) {
        t = clamp01(t);
        int aa = (int) (((a >>> 24) & 255) * (1 - t) + ((b >>> 24) & 255) * t);
        int rr = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bb = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (aa << 24) | (rr << 16) | (gg << 8) | bb;
    }

    private static int mul(int c, float k) {
        int r = Math.min(255, (int) (((c >> 16) & 255) * k));
        int g = Math.min(255, (int) (((c >> 8) & 255) * k));
        int b = Math.min(255, (int) ((c & 255) * k));
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
