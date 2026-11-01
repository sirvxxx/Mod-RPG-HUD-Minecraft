package dev.rpghud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Custom RPG HUD.
 *  - Profil: medali lingkaran dengan gauge XP melengkung (ala Kingdom Hearts) atau frame kotak RPG.
 *  - Panel utama: nama, HP, dan 3 bar bersegmen (Health / Hunger / Armor), default 10 segmen.
 *  - Panel bawah: koordinat XYZ + FPS berwarna dinamis.
 */
public final class HudRenderer {
    private static final Identifier HEART = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/full.png");
    private static final Identifier FOOD  = Identifier.withDefaultNamespace("textures/gui/sprites/hud/food_full.png");
    private static final Identifier ARMOR = Identifier.withDefaultNamespace("textures/gui/sprites/hud/armor_full.png");

    // Dimensi layout (piksel GUI sebelum scale)
    public static final int CIRCLE = 64;    // diameter medali profil
    public static final int FACE_W = 52;    // lebar frame kotak
    public static final int PANEL_W = 140;
    public static final int PANEL_H = 64;
    public static final int GAP = 6;
    public static final int INFO_H = 19;

    // Palet logam
    private static final int OUTLINE = 0xFF0E0B08;
    private static final int METAL_MID = 0xFFB98A3E;
    private static final int METAL_HL_TOP = 0xFFF3DC8F;
    private static final int METAL_HL_LEFT = 0xFFDDB75E;
    private static final int METAL_SH_BOTTOM = 0xFF6A4A1E;
    private static final int METAL_SH_RIGHT = 0xFF7D5A26;
    private static final int BRONZE_LINE = 0xFF7A5524;
    private static final int INNER = 0xFF16110C;
    private static final int GOLD_TEXT = 0xFFFFD36B;

    // Warna ring gauge XP (indeks 0 = tepi dalam, 4 = tepi luar)
    private static final int[] XP_FILLED = {0xFFD6FFC2, 0xFFA2F58A, 0xFF5FD66A, 0xFF35B552, 0xFF1F8A3C};
    private static final int[] XP_EMPTY  = {0xFF08100D, 0xFF0C1713, 0xFF101D18, 0xFF142320, 0xFF18291F};
    private static final int[] METAL_RING = {0xFF6A4A1E, 0xFF9C7430, 0xFFD2A64E, 0xFFF3DC8F};

    /** true saat layar pengaturan terbuka (layar itu menggambar preview sendiri). */
    public static boolean suppressHud = false;

    private static long fpsWindowStart = System.currentTimeMillis();
    private static int frames;
    private static int fps;

    private static float shownHp = -1f;
    private static long lastNs = System.nanoTime();

    // Cache geometri ring (dihitung ulang hanya saat XP berubah)
    private static final int[] ringBuf = new int[CIRCLE * CIRCLE * 5];   // x, y, w, h, warna per rect
    private static int ringCount;
    private static int ringKey = -1;

    private HudRenderer() {}

    // ------------------------------------------------------------------ ukuran

    public static boolean statusVisible(HudConfig c) {
        return c.showName || c.showHealth || c.showHunger || c.showArmor;
    }

    public static boolean infoVisible(HudConfig c) {
        return c.showCoords || c.showFps;
    }

    public static int profileW(HudConfig c) {
        return c.circleProfile ? CIRCLE : FACE_W;
    }

    public static int width(HudConfig c) {
        int off = c.showFace ? profileW(c) + GAP : 0;
        int w = 0;
        if (statusVisible(c)) w = off + PANEL_W;
        else if (c.showFace) w = profileW(c);
        if (infoVisible(c)) w = Math.max(w, (c.showCoords && c.showFps) ? 168 : PANEL_W);
        return w;
    }

    public static int height(HudConfig c) {
        int h = (statusVisible(c) || c.showFace) ? PANEL_H : 0;
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
        int statusX = c.showFace ? profileW(c) + GAP : 0;

        if (c.showFace) {
            if (c.circleProfile) drawCircleProfile(g, mc, p, c);
            else drawSquareProfile(g, mc, p, c);
        }
        if (status) drawStatus(g, mc, p, c, statusX);
        if (infoVisible(c)) {
            int infoY = (status || c.showFace) ? PANEL_H + GAP : 0;
            drawInfo(g, mc, p, c, infoY, width(c));
        }

        pose.popMatrix();
    }

    // ------------------------------------------------------------------ profil: medali lingkaran

    private static void drawCircleProfile(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c) {
        // bayangan lingkaran (digeser 2px)
        for (int y = 0; y < CIRCLE + 3; y++) {
            double dy = y + 0.5 - 34.0;
            if (Math.abs(dy) >= 32.0) continue;
            int hw = (int) Math.round(Math.sqrt(32.0 * 32.0 - dy * dy));
            g.fill(34 - hw, y, 34 + hw, y + 1, 0x44000000);
        }
        // dasar gelap di dalam medali
        for (int y = 0; y < CIRCLE; y++) {
            double dy = y + 0.5 - 32.0;
            if (Math.abs(dy) >= 23.0) continue;
            int hw = (int) Math.round(Math.sqrt(23.0 * 23.0 - dy * dy));
            g.fill(32 - hw, y, 32 + hw, y + 1, INNER);
        }
        // wajah skin 8x8 diperbesar 5x = 40x40, di tengah medali
        Identifier skin = SkinHelper.bodyTexture(p);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, 12, 12, 8f, 8f, 40, 40, 8, 8, 64, 64);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, 12, 12, 40f, 8f, 40, 40, 8, 8, 64, 64);

        // ring gauge XP + rim logam (menutup sudut wajah)
        int key = c.showLevel ? Math.max(0, Math.min(100, (int) (p.experienceProgress * 100f))) : 0;
        if (key != ringKey) buildRing(key);
        for (int i = 0; i < ringCount; i++) {
            int o = i * 5;
            g.fill(ringBuf[o], ringBuf[o + 1], ringBuf[o] + ringBuf[o + 2], ringBuf[o + 1] + ringBuf[o + 3], ringBuf[o + 4]);
        }

        if (c.showLevel) {
            String lv = "Lv " + p.experienceLevel;
            int tw = mc.font.width(lv);
            int bw = Math.max(34, tw + 12);
            int bh = 14;
            int bx = (CIRCLE - bw) / 2;
            int by = CIRCLE - bh;
            g.fill(bx, by, bx + bw, by + bh, OUTLINE);
            g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, METAL_MID);
            g.fill(bx + 1, by + 1, bx + bw - 1, by + 2, METAL_HL_TOP);
            g.fill(bx + 2, by + 2, bx + bw - 2, by + bh - 2, INNER);
            g.text(mc.font, lv, bx + (bw - tw) / 2, by + 3, 0xFF9CFF7A, true);
        }
    }

    /**
     * Hitung ulang geometri ring. Piksel berwarna sama pada satu baris digabung jadi garis,
     * lalu garis identik (x, lebar, warna sama) di baris berurutan digabung jadi satu persegi panjang
     * supaya jumlah operasi gambar per frame lebih sedikit.
     */
    private static void buildRing(int key) {
        int cnt = 0;
        int[] prev = new int[CIRCLE + 1];
        int[] cur = new int[CIRCLE + 1];
        int prevN = 0;
        for (int y = 0; y < CIRCLE; y++) {
            int curN = 0;
            int start = 0;
            int color = 0;
            for (int x = 0; x <= CIRCLE; x++) {
                int col = x < CIRCLE ? ringPixel(x, y, key) : 0;
                if (col == color) continue;
                if (color != 0) {
                    int w = x - start;
                    int found = -1;
                    for (int k = 0; k < prevN; k++) {
                        int o = prev[k] * 5;
                        if (ringBuf[o] == start && ringBuf[o + 2] == w && ringBuf[o + 4] == color) { found = prev[k]; break; }
                    }
                    if (found >= 0) {
                        ringBuf[found * 5 + 3]++;
                        cur[curN++] = found;
                    } else {
                        int o = cnt * 5;
                        ringBuf[o] = start; ringBuf[o + 1] = y; ringBuf[o + 2] = w; ringBuf[o + 3] = 1; ringBuf[o + 4] = color;
                        cur[curN++] = cnt;
                        cnt++;
                    }
                }
                color = col;
                start = x;
            }
            int[] tmp = prev; prev = cur; cur = tmp;
            prevN = curN;
        }
        ringCount = cnt;
        ringKey = key;
    }

    /** Warna piksel ring (0 = transparan). Pita: bayangan dalam, alur gauge, pemisah, rim logam, outline. */
    private static int ringPixel(int x, int y, int key) {
        double dx = x + 0.5 - 32.0;
        double dy = y + 0.5 - 32.0;
        double r = Math.sqrt(dx * dx + dy * dy);
        if (r < 22.5 || r >= 32.0) return 0;
        if (r < 23.5) return 0xFF050403;                 // bayangan dalam

        if (r < 28.5) {                                  // alur gauge XP (5px)
            int band = Math.min(4, (int) ((r - 23.5)));
            double th = Math.atan2(dx, -dy);             // 0 = jam 12, searah jarum jam
            if (th < 0) th += 2 * Math.PI;
            double pr = th / (2 * Math.PI);
            double seg = pr * 10.0;
            double frac = seg - Math.floor(seg);
            if (frac * (2 * Math.PI * r / 10.0) < 0.8) return 0xFF050403;   // pemisah 10 segmen
            return (pr * 100.0 < key) ? XP_FILLED[band] : XP_EMPTY[band];
        }
        if (r < 29.3) return 0xFF050403;                 // celah gelap

        if (r < 31.3) {                                  // rim logam 3D (cahaya dari kiri atas)
            if (Math.abs(Math.abs(dx) - Math.abs(dy)) < 1.3 && r > 29.6 && r < 31.0) return 0xFFFFF0B8;   // stud diagonal
            double nx = dx / r, ny = dy / r;
            double s = -0.7071 * (nx + ny);
            int idx = (int) Math.floor((s + 1.0) * 2.0);
            return METAL_RING[Math.max(0, Math.min(3, idx))];
        }
        return OUTLINE;
    }

    // ------------------------------------------------------------------ profil: frame kotak

    private static void drawSquareProfile(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c) {
        frame(g, 0, 0, FACE_W, PANEL_H);
        Identifier skin = SkinHelper.bodyTexture(p);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, 6, 7, 8f, 8f, 40, 40, 8, 8, 64, 64);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, 6, 7, 40f, 8f, 40, 40, 8, 8, 64, 64);
        g.outline(5, 6, 42, 42, 0xFF000000);
        if (c.showLevel) {
            String lv = "Lv " + p.experienceLevel;
            g.text(mc.font, lv, (FACE_W - mc.font.width(lv)) / 2, 51, 0xFF9CFF7A, true);
        }
    }

    // ------------------------------------------------------------------ panel status

    private static void drawStatus(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c, int x) {
        final int W = PANEL_W;
        final int unit = Math.max(1, c.unitPerSeg);
        frame(g, x, 0, W, PANEL_H);

        float hp = p.getHealth();
        float max = Math.max(1f, p.getMaxHealth());

        // ---- baris atas: username (kiri) + angka HP (kanan)
        if (c.showName || c.showHealth) {
            String hpTxt = (int) Math.ceil(hp) + "/" + (int) Math.ceil(max);
            int right = x + W - 7;
            int hpW = Math.max(38, mc.font.width(hpTxt) + 8);
            int hpX = right - hpW;
            int nameX = x + 7;
            int nameR = c.showHealth ? hpX - 3 : right;
            if (c.showName) {
                plate(g, nameX, 8, nameR - nameX, 12);
                String name = fit(mc, p.getName().getString(), nameR - nameX - 8);
                g.text(mc.font, name, nameX + 4, 9, GOLD_TEXT, true);
            }
            if (c.showHealth) {
                plate(g, hpX, 8, hpW, 12);
                g.text(mc.font, hpTxt, hpX + (hpW - mc.font.width(hpTxt)) / 2, 9, 0xFFFFFFFF, true);
            }
        }

        final int bx = x + 20;
        final int bw = W - 27;

        // ---- HEALTH
        if (c.showHealth) {
            int n = segCount(max, unit);
            int filled = segFill(hp, max, n);
            int ghost = segFill(shownHp < 0f ? hp : shownHp, max, n);
            float f = hp / max;
            float t = clamp01((f - 0.2f) / 0.8f);
            int top = lerp(0xFFB01818, 0xFFFF6A5E, t);
            int bot = lerp(0xFF4E0707, 0xFFC41F1F, t);
            if (f < 0.3f) {
                float pulse = 0.8f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 140.0);
                top = mul(top, pulse);
                bot = mul(bot, pulse);
            }
            g.blit(RenderPipelines.GUI_TEXTURED, HEART, x + 7, 25, 0f, 0f, 9, 9, 9, 9, 9, 9);
            segBar(g, bx, 25, bw, 9, n, filled, ghost, 0, top, bot);
        }

        // ---- HUNGER (garis kuning = saturation)
        if (c.showHunger) {
            int food = p.getFoodData().getFoodLevel();
            int n = segCount(20f, unit);
            int filled = segFill(food, 20f, n);
            float sat = Math.min(p.getFoodData().getSaturationLevel(), food);
            int accent = Math.min(filled, (int) Math.floor(sat * n / 20f));
            icon(g, FOOD, x + 8, 39);
            segBar(g, bx, 40, bw, 5, n, filled, 0, accent, 0xFFFFC04D, 0xFFC27A12);
        }

        // ---- ARMOR
        if (c.showArmor) {
            int arm = Math.min(20, p.getArmorValue());
            int n = segCount(20f, unit);
            int filled = segFill(arm, 20f, n);
            icon(g, ARMOR, x + 8, 50);
            segBar(g, bx, 51, bw, 5, n, filled, 0, 0, 0xFFBBD5EC, 0xFF5F7E9E);
        }
    }

    // ------------------------------------------------------------------ panel bawah: XYZ + FPS

    private static void drawInfo(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c, int y, int w) {
        frame(g, 0, y, w, INFO_H);
        int ty = y + 5;

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
            if (c.showCoords) g.fill(fx - 5, y + 5, fx - 4, y + INFO_H - 5, 0x99B98A3E);
            g.text(mc.font, num, fx, ty, col, true);
            g.text(mc.font, label, fx + mc.font.width(num), ty, GOLD_TEXT, true);
        }
    }

    // ------------------------------------------------------------------ komponen gambar

    /** Frame logam: pita 3px dengan bevel, garis perunggu, sudut terpotong, stud emas, dan bayangan. */
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
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, INNER);
        g.fill(x + 4, y + 4, x + w - 4, y + 5, 0xFF050403);
        g.fill(x + 4, y + 5, x + 5, y + h - 4, 0xFF050403);
        g.fill(x + 4, y + h - 5, x + w - 4, y + h - 4, 0x40E0BB62);

        int st = 0xFFFFF0B8;
        g.fill(x + 2, y + 2, x + 4, y + 4, st);
        g.fill(x + w - 4, y + 2, x + w - 2, y + 4, st);
        g.fill(x + 2, y + h - 4, x + 4, y + h - 2, st);
        g.fill(x + w - 4, y + h - 4, x + w - 2, y + h - 2, st);
    }

    private static void plate(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF0D0A07);
        g.outline(x, y, w, h, 0xFF3A2A18);
    }

    /** Bar bersegmen: n kotak, filled terisi penuh, ghost = jejak damage, accent = garis kuning saturation. */
    private static void segBar(GuiGraphicsExtractor g, int x, int y, int w, int h,
                               int n, int filled, int ghost, int accent, int top, int bottom) {
        g.fill(x - 1, y - 1, x + w, y + h + 1, 0xFF050403);
        for (int i = 0; i < n; i++) {
            int x0 = x + Math.round(i * (float) w / n);
            int x1 = x + Math.round((i + 1) * (float) w / n) - 1;
            if (x1 <= x0) x1 = x0 + 1;
            int sw = x1 - x0;
            if (i < filled) {
                vgrad(g, x0, y, sw, h, top, bottom);
                g.fill(x0, y, x1, y + 1, 0x66FFFFFF);
                if (i < accent) g.fill(x0, y + h - 1, x1, y + h, 0xFFFFF2A0);
            } else if (i < ghost) {
                vgrad(g, x0, y, sw, h, 0xFFEBDDB8, 0xFFA89870);
            } else {
                vgrad(g, x0, y, sw, h, 0xFF2E211D, 0xFF1B1311);
            }
        }
    }

    private static int segCount(float max, int unit) {
        int n = (int) Math.ceil(max / unit - 1e-4);
        return Math.max(1, Math.min(40, n));
    }

    private static int segFill(float value, float max, int n) {
        int f = (int) Math.ceil(value * n / max - 1e-3f);
        f = Math.max(0, Math.min(n, f));
        if (value > 0f && f == 0) f = 1;
        return f;
    }

    private static void vgrad(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bottom) {
        for (int i = 0; i < h; i++) {
            float t = h <= 1 ? 0f : (float) i / (h - 1);
            g.fill(x, y + i, x + w, y + i + 1, lerp(top, bottom, t));
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
