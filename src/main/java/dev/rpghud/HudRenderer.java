package dev.rpghud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Renderer HUD bergaya RPG: frame logam dengan bevel + shadow, wajah skin di frame terpisah,
 * bar HP bergradasi dinamis, bar hunger/armor ramping, serta panel koordinat + FPS.
 */
public final class HudRenderer {
    private static final Identifier HEART = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/full.png");
    private static final Identifier FOOD  = Identifier.withDefaultNamespace("textures/gui/sprites/hud/food_full.png");
    private static final Identifier ARMOR = Identifier.withDefaultNamespace("textures/gui/sprites/hud/armor_full.png");

    // Dimensi layout (satuan piksel GUI sebelum scale)
    public static final int FACE = 40;
    public static final int PANEL_W = 124;
    public static final int PANEL_H = 40;
    public static final int GAP = 4;
    public static final int INFO_H = 15;

    // Palet logam / RPG klasik
    private static final int OUTLINE = 0xFF0E0B08;
    private static final int METAL_MID = 0xFFB98A3E;
    private static final int METAL_HL_TOP = 0xFFF3DC8F;
    private static final int METAL_HL_LEFT = 0xFFDDB75E;
    private static final int METAL_SH_BOTTOM = 0xFF6A4A1E;
    private static final int METAL_SH_RIGHT = 0xFF7D5A26;
    private static final int INNER = 0xFF16110C;
    private static final int GOLD_TEXT = 0xFFFFD36B;

    /** Jika true, HUD asli tidak digambar (dipakai saat layar pengaturan terbuka, yang punya preview sendiri). */
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

    public static int width(HudConfig c) {
        int off = c.showFace ? FACE + GAP : 0;
        int w = 0;
        if (statusVisible(c)) w = off + PANEL_W;
        else if (c.showFace) w = FACE;
        if (infoVisible(c)) w = Math.max(w, (c.showCoords && c.showFps) ? 168 : PANEL_W);
        return w;
    }

    public static int height(HudConfig c) {
        int h = (statusVisible(c) || c.showFace) ? PANEL_H : 0;
        if (infoVisible(c)) h += (h > 0 ? GAP : 0) + INFO_H;
        return h;
    }

    // ------------------------------------------------------------------ entry point

    /** Dipanggil Fabric tiap frame. */
    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        tickCounters(p);
        HudConfig c = HudConfig.get();
        if (suppressHud || !c.enabled || p == null) return;
        draw(g, mc, p, c);
    }

    /** Dipakai layar pengaturan: selalu menggambar, walau HUD dimatikan. */
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
        int statusX = c.showFace ? FACE + GAP : 0;

        if (c.showFace) drawFace(g, mc, p, c);
        if (status) drawStatus(g, mc, p, c, statusX);
        if (infoVisible(c)) {
            int infoY = (status || c.showFace) ? PANEL_H + GAP : 0;
            drawInfo(g, mc, p, c, infoY, width(c));
        }

        pose.popMatrix();
    }

    // ------------------------------------------------------------------ wajah

    private static void drawFace(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c) {
        frame(g, 0, 0, FACE, FACE);
        Identifier skin = SkinHelper.bodyTexture(p);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, 4, 4, 8f, 8f, 32, 32, 8, 8, 64, 64);   // wajah
        g.blit(RenderPipelines.GUI_TEXTURED, skin, 4, 4, 40f, 8f, 32, 32, 8, 8, 64, 64);  // lapisan hat
        g.outline(3, 3, 34, 34, 0xFF000000);

        if (c.showLevel) {
            String lv = String.valueOf(p.experienceLevel);
            int tw = mc.font.width(lv);
            int bw = Math.max(15, tw + 6);
            int bx = FACE - bw - 1;
            int by = FACE - 14;
            g.fill(bx, by, bx + bw, by + 13, OUTLINE);
            g.fill(bx + 1, by + 1, bx + bw - 1, by + 12, METAL_MID);
            g.fill(bx + 2, by + 2, bx + bw - 2, by + 11, 0xFF1C1610);
            g.text(mc.font, lv, bx + (bw - tw) / 2, by + 2, 0xFF9CFF7A, true);
        }
    }

    // ------------------------------------------------------------------ panel status

    private static void drawStatus(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c, int x) {
        final int W = PANEL_W;
        frame(g, x, 0, W, PANEL_H);

        float hp = p.getHealth();
        float max = Math.max(1f, p.getMaxHealth());

        if (c.showName || c.showHealth) {
            g.fill(x + 4, 4, x + W - 4, 14, 0xFF0D0A07);   // name plate (recessed)
            String hpTxt = (int) Math.ceil(hp) + "/" + (int) Math.ceil(max);
            int hpW = c.showHealth ? mc.font.width(hpTxt) : 0;
            if (c.showName) {
                String name = fit(mc, p.getName().getString(), W - 12 - hpW - 6);
                g.text(mc.font, name, x + 6, 5, GOLD_TEXT, true);
            }
            if (c.showHealth) g.text(mc.font, hpTxt, x + W - 6 - hpW, 5, 0xFFFFFFFF, true);
            g.fill(x + 4, 15, x + W - 4, 16, 0x99B98A3E);   // garis pemisah emas
        }

        if (c.showHealth) {
            float f = hp / max;
            float ghost = (shownHp < 0f ? hp : shownHp) / max;
            // Gradasi merah dinamis: makin sedikit HP, makin gelap + berdenyut
            float t = clamp01((f - 0.2f) / 0.8f);
            int top = lerp(0xFFB01818, 0xFFFF6A5E, t);
            int bot = lerp(0xFF4E0707, 0xFFC41F1F, t);
            if (f < 0.3f) {
                float pulse = 0.8f + 0.2f * (float) Math.sin(System.currentTimeMillis() / 140.0);
                top = mul(top, pulse);
                bot = mul(bot, pulse);
            }
            icon(g, HEART, x + 4, 18);
            bar(g, x + 14, 18, W - 18, 8, f, ghost, top, bot);
        }

        if (c.showHunger) {
            icon(g, FOOD, x + 4, 28);
            bar(g, x + 14, 29, 46, 5, p.getFoodData().getFoodLevel() / 20f, 0f, 0xFFFFC04D, 0xFFC27A12);
        }
        if (c.showArmor) {
            icon(g, ARMOR, x + 66, 28);
            bar(g, x + 76, 29, W - 4 - 76, 5, p.getArmorValue() / 20f, 0f, 0xFFBBD5EC, 0xFF5F7E9E);
        }
    }

    // ------------------------------------------------------------------ panel koordinat + FPS

    private static void drawInfo(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c, int y, int w) {
        frame(g, 0, y, w, INFO_H);
        int ty = y + 3;

        if (c.showCoords) {
            String label = "XYZ ";
            String vals = (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " " + (int) Math.floor(p.getZ());
            g.text(mc.font, label, 6, ty, GOLD_TEXT, true);
            g.text(mc.font, vals, 6 + mc.font.width(label), ty, 0xFFFFFFFF, true);
        }
        if (c.showFps) {
            String num = String.valueOf(fps);
            String label = " FPS";
            int total = mc.font.width(num) + mc.font.width(label);
            int fx = w - 6 - total;
            int col = fps >= 60 ? 0xFF7CFC7C : (fps >= 30 ? 0xFFFFE066 : 0xFFFF6B6B);
            g.text(mc.font, num, fx, ty, col, true);
            g.text(mc.font, label, fx + mc.font.width(num), ty, GOLD_TEXT, true);
        }
    }

    // ------------------------------------------------------------------ komponen gambar

    /** Frame logam: sudut terpotong, bevel terang/gelap, rivet di sudut, dan bayangan. */
    private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x55000000);                 // drop shadow

        g.fill(x + 1, y, x + w - 1, y + h, OUTLINE);                            // outline (sudut terpotong)
        g.fill(x, y + 1, x + w, y + h - 1, OUTLINE);

        g.fill(x + 2, y + 1, x + w - 2, y + h - 1, METAL_MID);                  // badan logam
        g.fill(x + 1, y + 2, x + w - 1, y + h - 2, METAL_MID);

        g.fill(x + 2, y + 1, x + w - 2, y + 2, METAL_HL_TOP);                   // bevel
        g.fill(x + 1, y + 2, x + 2, y + h - 2, METAL_HL_LEFT);
        g.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, METAL_SH_BOTTOM);
        g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, METAL_SH_RIGHT);

        g.fill(x + 3, y + 3, x + w - 3, y + h - 3, INNER);                      // area dalam (cekung)
        g.fill(x + 3, y + 3, x + w - 3, y + 4, 0xFF050403);
        g.fill(x + 3, y + 4, x + 4, y + h - 3, 0xFF050403);
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, 0x40E0BB62);

        int rv = 0xFFFFF0B8;                                                    // rivet
        g.fill(x + 2, y + 2, x + 3, y + 3, rv);
        g.fill(x + w - 3, y + 2, x + w - 2, y + 3, rv);
        g.fill(x + 2, y + h - 3, x + 3, y + h - 2, rv);
        g.fill(x + w - 3, y + h - 3, x + w - 2, y + h - 2, rv);
    }

    /** Bar bergradasi vertikal dengan bingkai gelap, kilap di atas, tick tiap 20%, dan ghost bar. */
    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h,
                            float frac, float ghost, int top, int bottom) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF050403);
        vgrad(g, x, y, w, h, 0xFF2B1D1A, 0xFF171010);

        int fw = Math.round(w * clamp01(frac));
        int gw = Math.round(w * clamp01(ghost));
        if (gw > fw) g.fill(x + fw, y, x + gw, y + h, 0xCCE9D3A5);
        if (fw > 0) {
            vgrad(g, x, y, fw, h, top, bottom);
            g.fill(x, y, x + fw, y + 1, 0x55FFFFFF);
        }
        for (int i = 1; i < 5; i++) {
            int tx = x + w * i / 5;
            g.fill(tx, y, tx + 1, y + h, 0x40000000);
        }
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
