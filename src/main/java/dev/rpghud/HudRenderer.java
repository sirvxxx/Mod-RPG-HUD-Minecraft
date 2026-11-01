package dev.rpghud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * HUD bergaya RPG (v3):
 *  - Profil: lingkaran logam + ring gauge ala Kingdom Hearts (kiri = HP, kanan = EXP), wajah skin di tengah, badge Lv.
 *  - Panel utama: nama + angka HP, lalu bar Health / Hunger / Armor, masing-masing 10 segmen.
 *  - Panel bawah: koordinat XYZ + FPS berwarna dinamis.
 */
public final class HudRenderer {
    private static final Identifier HEART = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/full.png");
    private static final Identifier FOOD  = Identifier.withDefaultNamespace("textures/gui/sprites/hud/food_full.png");
    private static final Identifier ARMOR = Identifier.withDefaultNamespace("textures/gui/sprites/hud/armor_full.png");

    // ---- dimensi layout (piksel GUI sebelum scale)
    public static final int CIRCLE_D = 64;     // diameter lingkaran profil
    public static final int PROFILE_H = 68;    // lingkaran + badge Lv
    public static final int PANEL_X = 68;      // posisi panel utama bila profil tampil
    public static final int PANEL_W = 150;
    public static final int PANEL_H = 58;
    public static final int GAP = 3;
    public static final int INFO_H = 19;
    private static final int SEGMENTS = 10;

    // ---- palet logam: emas tua / cokelat, highlight terang di sisi atas
    private static final int OUTLINE        = 0xFF0A0806;
    private static final int METAL_MID      = 0xFF9A7230;
    private static final int METAL_HL_TOP   = 0xFFF6DE8E;
    private static final int METAL_HL_LEFT  = 0xFFD4AA52;
    private static final int METAL_SH_BOTTOM = 0xFF5A3C16;
    private static final int METAL_SH_RIGHT = 0xFF6C4A1E;
    private static final int BRONZE_LINE    = 0xFF6B4A1E;
    private static final int INNER_TOP      = 0xFF21190F;
    private static final int INNER_BOTTOM   = 0xFF0C0907;
    private static final int GOLD_TEXT      = 0xFFFFD36B;

    /** true saat layar pengaturan terbuka (layar itu menggambar preview sendiri). */
    public static boolean suppressHud = false;

    private static long fpsWindowStart = System.currentTimeMillis();
    private static int frames;
    private static int fps;

    private static float shownHp = -1f;
    private static long lastNs = System.nanoTime();

    private HudRenderer() {}

    // ------------------------------------------------------------------ ukuran

    public static boolean statusVisible(HudConfig c) {
        return c.showName || c.showHealth || c.showHunger || c.showArmor;
    }

    public static boolean infoVisible(HudConfig c) {
        return c.showCoords || c.showFps;
    }

    private static int topHeight(HudConfig c) {
        if (c.showFace) return c.showLevel ? PROFILE_H : CIRCLE_D;
        return statusVisible(c) ? PANEL_H : 0;
    }

    public static int width(HudConfig c) {
        boolean st = statusVisible(c);
        int w = 0;
        if (c.showFace && st) w = PANEL_X + PANEL_W;
        else if (st) w = PANEL_W;
        else if (c.showFace) w = CIRCLE_D;
        if (infoVisible(c)) w = Math.max(w, (c.showCoords && c.showFps) ? 168 : PANEL_W);
        return w;
    }

    public static int height(HudConfig c) {
        int h = topHeight(c);
        if (infoVisible(c)) h += (h > 0 ? GAP : 0) + INFO_H;
        return h;
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

        boolean status = statusVisible(c);
        int top = topHeight(c);

        if (c.showFace) drawProfile(g, mc, p, c);
        if (status) {
            int px = c.showFace ? PANEL_X : 0;
            int py = c.showFace ? (CIRCLE_D - PANEL_H) / 2 : 0;
            drawStatus(g, mc, p, c, px, py);
        }
        if (infoVisible(c)) {
            drawInfo(g, mc, p, c, top > 0 ? top + GAP : 0, width(c));
        }

        pose.popMatrix();
    }

    // ------------------------------------------------------------------ profil (ring gauge ala Kingdom Hearts)

    private static void drawProfile(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c) {
        final int cx = CIRCLE_D / 2, cy = CIRCLE_D / 2;
        float max = Math.max(1f, p.getMaxHealth());
        float hpF = clamp01(p.getHealth() / max);
        float ghostF = clamp01((shownHp < 0f ? p.getHealth() : shownHp) / max);
        float expF = clamp01(p.experienceProgress);

        // bayangan + rim logam bertingkat (highlight kiri-atas, gelap kanan-bawah)
        disc(g, cx + 1, cy + 2, 32, 0x55000000);
        disc(g, cx, cy, 32, OUTLINE);
        disc(g, cx, cy, 31, METAL_HL_TOP);
        disc(g, cx + 1, cy + 1, 30, METAL_SH_BOTTOM);
        disc(g, cx, cy, 30, METAL_MID);
        disc(g, cx, cy, 29, OUTLINE);
        disc(g, cx, cy, 24, 0xFF050403);
        disc(g, cx, cy, 23, 0xFF14100B);

        // warna HP dinamis: hijau -> kuning -> merah (berdenyut saat kritis)
        int hpCol = hpF >= 0.5f ? lerp(0xFFFFD23C, 0xFF45E060, (hpF - 0.5f) * 2f)
                                : lerp(0xFFFF4A40, 0xFFFFD23C, hpF * 2f);
        if (hpF < 0.3f) hpCol = mul(hpCol, 0.8f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 140.0));

        ringGauge(g, cx, cy, hpF, ghostF, hpCol, expF, 0xFF4AA8FF, c.showExp);

        // wajah skin 32x32 (8x8 diperbesar 4x) + layer topi
        Identifier skin = SkinHelper.bodyTexture(p);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, cx - 16, cy - 16, 8f, 8f, 32, 32, 8, 8, 64, 64);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, cx - 16, cy - 16, 40f, 8f, 32, 32, 8, 8, 64, 64);

        if (c.showLevel) {
            String lv = "Lv " + p.experienceLevel;
            int bw = Math.max(36, mc.font.width(lv) + 12);
            int bx = (CIRCLE_D - bw) / 2;
            int by = CIRCLE_D - 10;
            badge(g, bx, by, bw, 14);
            g.text(mc.font, lv, bx + (bw - mc.font.width(lv)) / 2, by + 3, 0xFF9CFF7A, true);
        }
    }

    /**
     * Ring gauge: busur kiri = HP (isi dari bawah ke atas), busur kanan = EXP.
     * Bila ring EXP dimatikan, kedua sisi menampilkan HP. Digambar per baris piksel
     * dengan run-merging supaya jumlah fill kecil.
     */
    private static void ringGauge(GuiGraphicsExtractor g, int cx, int cy,
                                  float hpF, float ghostF, int hpCol,
                                  float expF, int expCol, boolean showExp) {
        final int RO = 28, RI = 24;
        final double ro2 = RO * RO, ri2 = RI * RI;
        for (int dy = -RO; dy < RO; dy++) {
            double fy = dy + 0.5;
            int runStart = -RO;
            int runColor = 0;
            for (int dx = -RO; dx <= RO; dx++) {
                int col = 0;
                if (dx < RO) {
                    double fx = dx + 0.5;
                    double d2 = fx * fx + fy * fy;
                    if (d2 >= ri2 && d2 < ro2 && Math.abs(fx) > 1.0) {   // celah 2px di atas & bawah
                        float shade = (float) ((Math.sqrt(d2) - RI) / (RO - RI));
                        double ang = Math.atan2(Math.abs(fx), -fy);       // 0 = atas, PI = bawah
                        float u = (float) (1.0 - ang / Math.PI);          // 0 = bawah, 1 = atas
                        boolean left = fx < 0;
                        int base;
                        if (left || !showExp) {
                            if (u <= hpF) base = hpCol;
                            else if (u <= ghostF) base = 0xFFEBDDB8;
                            else base = 0;
                        } else {
                            base = (u <= expF) ? expCol : 0;
                        }
                        if (base == 0) col = lerp(0xFF120D0A, 0xFF281D15, shade);
                        else col = lerp(mul(base, 0.70f), mul(base, 1.18f), shade);
                    }
                }
                if (col != runColor) {
                    if (runColor != 0) g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, runColor);
                    runStart = dx;
                    runColor = col;
                }
            }
        }
    }

    // ------------------------------------------------------------------ panel status

    private static void drawStatus(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c, int x, int y) {
        final int W = PANEL_W;
        frame(g, x, y, W, PANEL_H);

        float hp = p.getHealth();
        float max = Math.max(1f, p.getMaxHealth());

        // ---- header: plate nama (kiri) + plate angka HP (kanan)
        if (c.showName || c.showHealth) {
            String hpTxt = (int) Math.ceil(hp) + "/" + (int) Math.ceil(max);
            int right = x + W - 7;
            int hpW = Math.max(40, mc.font.width(hpTxt) + 10);
            int hpX = right - hpW;
            int nameX = x + 7;
            int nameR = c.showHealth ? hpX - 3 : right;
            if (c.showName) {
                plate(g, nameX, y + 7, nameR - nameX, 12);
                String name = fit(mc, p.getName().getString(), nameR - nameX - 8);
                g.text(mc.font, name, nameX + 4, y + 9, GOLD_TEXT, true);
            }
            if (c.showHealth) {
                plate(g, hpX, y + 7, hpW, 12);
                g.text(mc.font, hpTxt, hpX + (hpW - mc.font.width(hpTxt)) / 2, y + 9, 0xFFFFFFFF, true);
            }
        }

        final int bx = x + 20;
        final int bw = W - 27;

        // ---- HEALTH (10 segmen, tiap segmen = 10% HP, bisa terisi sebagian)
        if (c.showHealth) {
            float f = clamp01(hp / max);
            float t = clamp01((f - 0.2f) / 0.8f);
            int top = lerp(0xFFB01818, 0xFFFF6A5E, t);
            int bot = lerp(0xFF4E0707, 0xFFC41F1F, t);
            if (f < 0.3f) {
                float pulse = 0.8f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 140.0);
                top = mul(top, pulse);
                bot = mul(bot, pulse);
            }
            g.blit(RenderPipelines.GUI_TEXTURED, HEART, x + 7, y + 23, 0f, 0f, 9, 9, 9, 9, 9, 9);
            segBar(g, bx, y + 23, bw, 9, hp, max, shownHp < 0f ? hp : shownHp, 0f, top, bot);
        }

        // ---- HUNGER (garis kuning di bawah segmen = saturation)
        if (c.showHunger) {
            int food = p.getFoodData().getFoodLevel();
            float sat = Math.min(p.getFoodData().getSaturationLevel(), food);
            icon(g, FOOD, x + 8, y + 36);
            segBar(g, bx, y + 36, bw, 7, food, 20f, food, sat, 0xFFFFC04D, 0xFFC27A12);
        }

        // ---- ARMOR
        if (c.showArmor) {
            int arm = Math.min(20, p.getArmorValue());
            icon(g, ARMOR, x + 8, y + 46);
            segBar(g, bx, y + 46, bw, 7, arm, 20f, arm, 0f, 0xFFBBD5EC, 0xFF5F7E9E);
        }
    }

    // ------------------------------------------------------------------ panel koordinat + FPS

    private static void drawInfo(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c, int y, int w) {
        frame(g, 0, y, w, INFO_H);
        int ty = y + 6;

        if (c.showCoords) {
            String label = "XYZ ";
            String vals = (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " " + (int) Math.floor(p.getZ());
            g.text(mc.font, label, 8, ty, GOLD_TEXT, true);
            g.text(mc.font, vals, 8 + mc.font.width(label), ty, 0xFFFFFFFF, true);
        }
        if (c.showFps) {
            String num = String.valueOf(fps);
            String label = " FPS";
            int total = mc.font.width(num) + mc.font.width(label);
            int fx = w - 8 - total;
            int col = fps >= 60 ? 0xFF7CFC7C : (fps >= 30 ? 0xFFFFE066 : 0xFFFF6B6B);
            if (c.showCoords) {
                g.fill(fx - 6, y + 5, fx - 5, y + INFO_H - 5, 0xFF0A0806);   // pemisah beralur
                g.fill(fx - 5, y + 5, fx - 4, y + INFO_H - 5, 0x80C79A45);
            }
            g.text(mc.font, num, fx, ty, col, true);
            g.text(mc.font, label, fx + mc.font.width(num), ty, GOLD_TEXT, true);
        }
    }

    // ------------------------------------------------------------------ komponen gambar

    /** Frame logam: bevel 3D (terang di atas/kiri), garis perunggu, sudut terpotong, stud emas, bayangan. */
    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 1, y + 1, x + w + 3, y + h + 3, 0x22000000);                 // bayangan lembut
        g.fill(x + 2, y + 3, x + w + 2, y + h + 2, 0x44000000);                 // bayangan inti

        g.fill(x + 1, y, x + w - 1, y + h, OUTLINE);                            // outline, sudut terpotong
        g.fill(x, y + 1, x + w, y + h - 1, OUTLINE);

        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, METAL_MID);                  // badan logam
        g.fill(x + 1, y + 2, x + w - 1, y + h - 2, METAL_MID);

        g.fill(x + 2, y + 1, x + w - 2, y + 2, METAL_HL_TOP);                   // bevel
        g.fill(x + 1, y + 2, x + 2, y + h - 2, METAL_HL_LEFT);
        g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, METAL_SH_BOTTOM);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, METAL_SH_RIGHT);

        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, BRONZE_LINE);                // garis perunggu dalam
        vgrad(g, x + 4, y + 4, w - 8, h - 8, INNER_TOP, INNER_BOTTOM);          // area cekung bergradasi
        g.fill(x + 4, y + 4, x + w - 4, y + 5, 0xFF050403);                     // bayangan dalam atas/kiri
        g.fill(x + 4, y + 5, x + 5, y + h - 4, 0xFF050403);
        g.fill(x + 4, y + h - 5, x + w - 4, y + h - 4, 0x40E0BB62);             // pantulan bawah

        int st = 0xFFFFF0B8;                                                    // stud emas di 4 sudut
        g.fill(x + 2, y + 2, x + 4, y + 4, st);
        g.fill(x + w - 4, y + 2, x + w - 2, y + 4, st);
        g.fill(x + 2, y + h - 4, x + 4, y + h - 2, st);
        g.fill(x + w - 4, y + h - 4, x + w - 2, y + h - 2, st);
    }

    /** Badge kecil bergaya logam untuk teks Lv. */
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

    /** Plate gelap berbingkai tipis untuk nama & angka HP. */
    private static void plate(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        vgrad(g, x, y, w, h, 0xFF171009, 0xFF0A0705);
        g.outline(x, y, w, h, 0xFF4A3520);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x30FFFFFF);
    }

    /**
     * Bar 10 segmen. value/max menentukan isi (segmen terakhir bisa terisi sebagian),
     * ghost = jejak damage (pucat), accent = garis kuning di dasar segmen (saturation).
     */
    private static void segBar(GuiGraphicsExtractor g, int x, int y, int w, int h,
                               float value, float max, float ghost, float accent, int top, int bottom) {
        g.fill(x - 1, y - 1, x + w, y + h + 1, 0xFF050403);
        g.fill(x, y + h, x + w - 1, y + h + 1, 0x30E0BB62);
        for (int i = 0; i < SEGMENTS; i++) {
            int x0 = x + Math.round(i * (float) w / SEGMENTS);
            int x1 = x + Math.round((i + 1) * (float) w / SEGMENTS) - 1;
            int sw = x1 - x0;
            if (sw < 1) continue;

            vgrad(g, x0, y, sw, h, 0xFF2E211D, 0xFF1B1311);                    // slot kosong

            float fv = segFrac(value, max, i);
            int fw = Math.round(sw * fv);
            if (fv > 0f && fw == 0) fw = 1;
            float fg = segFrac(ghost, max, i);
            int gw = Math.round(sw * fg);
            if (gw > fw) vgrad(g, x0 + fw, y, gw - fw, h, 0xFFEBDDB8, 0xFFA89870);

            if (fw > 0) {
                vgrad(g, x0, y, fw, h, top, bottom);
                g.fill(x0, y, x0 + fw, y + 1, 0x77FFFFFF);                     // kilap atas
                int aw = Math.min(fw, Math.round(sw * segFrac(accent, max, i)));
                if (aw > 0) g.fill(x0, y + h - 1, x0 + aw, y + h, 0xFFFFF2A0);
            }
        }
    }

    private static float segFrac(float v, float max, int i) {
        return clamp01(v * SEGMENTS / max - i);
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

    private static void icon(GuiGraphicsExtractor g, Identifier tex, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, 7, 7, 9, 9, 9, 9);
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
