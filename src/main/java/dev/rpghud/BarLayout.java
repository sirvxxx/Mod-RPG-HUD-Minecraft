package dev.rpghud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Type Bar: panel persegi panjang berbingkai logam.
 *   kiri  : kotak foto skin + badge "Lv N" di bawahnya
 *   kanan : baris nama + angka HP (20/20), bar HP (penuh), lalu bar Hunger dan Armor berdampingan
 *   bawah : satu bar Coords (XYZ) + FPS
 * Semua bar 10 kotak horizontal. Bar digambar dari daftar persegi yang di-cache
 * (dibangun ulang hanya saat nilainya berubah) dengan trik isi kontinu + garis pemisah,
 * sehingga hanya ~60 persegi untuk ketiga bar.
 */
final class BarLayout {
    private BarLayout() {}

    static final int FACE = 40;
    static final int LEVEL_H = 17;
    private static final int LEVEL_GAP = 3;
    static final int PANEL_X = 44;
    static final int PANEL_W = 170;
    static final int PANEL_H = 52;
    private static final int PANEL_Y = 4;
    static final int INFO_H = 19;
    private static final int GAP = 3;
    private static final int ICON = HudDraw.ICON;

    // posisi bar relatif ke pojok kiri-atas panel
    private static final int HP_Y = 22, HP_H = 10;
    private static final int ROW2_Y = 35, ROW2_H = 9;
    private static final int HP_X = 18, HP_W = PANEL_W - 7 - HP_X;          // 145
    private static final int HALF = (PANEL_W - 14 - 6) / 2;                  // 75
    private static final int HUN_X = 18, HUN_W = HALF - 11;                   // 64
    private static final int ARM_ICON_X = 7 + HALF + 6;                       // 88
    private static final int ARM_X = ARM_ICON_X + 11, ARM_W = HUN_W;

    private static final float[] SHADE_K = {1.25f, 1.0f, 0.78f};   // atas terang, tengah, bawah gelap

    private static final RectBatch BARS = new RectBatch(128);
    private static long barsKey = Long.MIN_VALUE;
    private static final int[] SEP = new int[11];

    // ------------------------------------------------------------------ ukuran

    private static boolean panelVisible(HudConfig c) {
        return c.showName || c.showHealth || c.showHunger || c.showArmor;
    }

    private static boolean infoVisible(HudConfig c) { return c.showCoords || c.showFps; }

    private static int topHeight(HudConfig c) {
        int left = c.showFace ? (c.showLevel ? FACE + LEVEL_GAP + LEVEL_H : FACE) : 0;
        int panel = panelVisible(c) ? (c.showFace ? PANEL_Y : 0) + PANEL_H : 0;
        return Math.max(left, panel);
    }

    private static int mainWidth(HudConfig c) {
        boolean panel = panelVisible(c);
        if (panel) return (c.showFace ? PANEL_X : 0) + PANEL_W;
        return c.showFace ? FACE : 0;
    }

    private static int infoWidth(HudConfig c) {
        int need = 14;                                                        // padding kiri + kanan 7px
        if (c.showCoords) need += ICON + 3 + HudState.coordsLabelW + HudState.coordsValsW;
        if (c.showFps) need += ICON + 3 + HudState.fpsTextW;
        if (c.showCoords && c.showFps) need += 10;
        return Math.max(mainWidth(c), need);
    }

    static int width(HudConfig c) {
        int w = mainWidth(c);
        if (infoVisible(c)) w = Math.max(w, infoWidth(c));
        return w;
    }

    static int height(HudConfig c) {
        int h = topHeight(c);
        if (infoVisible(c)) h += (h > 0 ? GAP : 0) + INFO_H;
        return h;
    }

    // ------------------------------------------------------------------ gambar

    static void draw(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c) {
        final HudTheme t = HudState.T;
        final boolean face = c.showFace;
        final int top = topHeight(c);

        if (face) {
            // kotak foto skin: area dalam bingkai 32x32 = 8x8 diperbesar 4x
            HudDraw.frame(g, t, 0, 0, FACE, FACE);
            g.blit(RenderPipelines.GUI_TEXTURED, HudState.skin, 4, 4, 8f, 8f, 32, 32, 8, 8, 64, 64);
            g.blit(RenderPipelines.GUI_TEXTURED, HudState.skin, 4, 4, 40f, 8f, 32, 32, 8, 8, 64, 64);

            if (c.showLevel) {
                int bw = Math.max(34, Math.min(FACE, HudState.levelW + 14));
                int bx = (FACE - bw) / 2;
                int by = FACE + LEVEL_GAP;
                HudDraw.badge(g, t, bx, by, bw, LEVEL_H);
                g.text(mc.font, HudState.levelStr, bx + (bw - HudState.levelW) / 2, by + 5, t.level, true);
            }
        }

        if (panelVisible(c)) drawPanel(g, mc, c, t, face ? PANEL_X : 0, face ? PANEL_Y : 0);
        if (infoVisible(c)) drawInfo(g, mc, c, t, top > 0 ? top + GAP : 0, infoWidth(c));
    }

    private static void drawPanel(GuiGraphicsExtractor g, Minecraft mc, HudConfig c, HudTheme t, int px, int py) {
        HudDraw.frame(g, t, px, py, PANEL_W, PANEL_H);

        // baris judul: nama (kiri) + angka HP (kanan), teks 7px di tengah baris 12px
        if (c.showName) g.text(mc.font, HudState.nameBar, px + 7, py + 9, t.text, true);
        if (c.showHealth) g.text(mc.font, HudState.hpStr, px + PANEL_W - 7 - HudState.hpStrW, py + 9, 0xFFFFFFFF, true);

        if (c.showHealth) HudDraw.icon(g, t.iconHeart, px + 7, py + HP_Y + 1);
        if (c.showHunger) HudDraw.icon(g, t.iconFood, px + 7, py + ROW2_Y);
        if (c.showArmor) HudDraw.icon(g, t.iconArmor, px + ARM_ICON_X, py + ROW2_Y);

        long key = HudState.statKey(c.showHealth, c.showHunger, c.showArmor);
        if (key != barsKey) {
            barsKey = key;
            rebuildBars(t, c.showHealth, c.showHunger, c.showArmor);
        }
        BARS.draw(g, px, py);
    }

    private static void drawInfo(GuiGraphicsExtractor g, Minecraft mc, HudConfig c, HudTheme t, int y, int w) {
        HudDraw.frame(g, t, 0, y, w, INFO_H);
        int ty = y + 6;
        if (c.showCoords) {
            HudDraw.icon(g, t.iconCoords, 7, y + 5);
            int tx = 7 + ICON + 3;
            g.text(mc.font, HudState.coordsLabel, tx, ty, t.text, true);
            g.text(mc.font, HudState.coordsVals, tx + HudState.coordsLabelW, ty, 0xFFFFFFFF, true);
        }
        if (c.showFps) {
            int groupW = ICON + 3 + HudState.fpsNumW + HudState.fpsLabelW;
            int fx = w - 7 - groupW;
            if (c.showCoords) {
                g.fill(fx - 6, y + 5, fx - 5, y + INFO_H - 5, t.empty[3]);
                g.fill(fx - 5, y + 5, fx - 4, y + INFO_H - 5, 0x60FFFFFF & (t.text | 0xFF000000));
            }
            HudDraw.icon(g, t.iconFps, fx, y + 5);
            int tx = fx + ICON + 3;
            g.text(mc.font, HudState.fpsNum, tx, ty, HudRenderer.fpsColor(), true);
            g.text(mc.font, HudState.fpsLabel, tx + HudState.fpsNumW, ty, t.text, true);
        }
    }

    // ------------------------------------------------------------------ bar 10 kotak (dibangun saat berubah)

    private static void rebuildBars(HudTheme t, boolean showHp, boolean showFood, boolean showArmor) {
        BARS.clear();
        if (showHp) {
            addBar(BARS, t, HP_X, HP_Y, HP_W, HP_H, HudState.hpQ / 320f, HudState.gQ / 320f, 0f,
                    HudState.hpBase(), t.ghost);
        }
        if (showFood) {
            addBar(BARS, t, HUN_X, ROW2_Y, HUN_W, ROW2_H, HudState.food / 20f, 0f, HudState.satQ / 40f,
                    t.food, 0);
        }
        if (showArmor) {
            addBar(BARS, t, ARM_X, ROW2_Y, ARM_W, ROW2_H, HudState.arm / 20f, 0f, 0f, t.armor, 0);
        }
    }

    /** Ujung isi (piksel) untuk pecahan v pada bar 10 kotak; kotak terakhir terisi sebagian. */
    private static int extent(float v, int[] sep) {
        if (v <= 0f) return sep[0] + 1;
        if (v >= 1f) return sep[10];
        float s = v * 10f;
        int k = (int) s;
        float r = s - k;
        return sep[k] + 1 + Math.round(r * (sep[k + 1] - sep[k] - 1));
    }

    private static void rows3(RectBatch rb, int x0, int x1, int y, int h, int light, int mid, int dark) {
        if (x1 <= x0) return;
        rb.add(x0, y + 1, x1, y + 2, light);
        rb.add(x0, y + 2, x1, y + h - 2, mid);
        rb.add(x0, y + h - 2, x1, y + h - 1, dark);
    }

    /**
     * Bar horizontal 10 kotak: garis luar 1px, kotak berbayang (terang atas / gelap bawah),
     * garis pemisah 1px antar kotak. Isi digambar kontinu lalu ditimpa pemisah, jadi tepinya selalu rapi.
     */
    private static void addBar(RectBatch rb, HudTheme t, int x, int y, int w, int h,
                               float v, float ghost, float acc, int base, int ghostCol) {
        for (int i = 0; i <= 10; i++) SEP[i] = x + Math.round(i * (w - 1) / 10f);
        final int ix0 = x + 1, ix1 = SEP[10];
        final int edge = t.empty[3];

        rb.add(x, y, x + w, y + h, edge);                                        // garis luar
        rows3(rb, ix0, ix1, y, h, t.empty[2], t.empty[1], t.empty[0]);           // kotak kosong

        int fill = extent(v, SEP);
        if (ghostCol != 0 && ghost > v) {
            int ge = extent(ghost, SEP);
            rows3(rb, fill, ge, y, h, HudDraw.mul(ghostCol, SHADE_K[0]), ghostCol, HudDraw.mul(ghostCol, SHADE_K[2]));
        }
        if (v > 0f) {
            rows3(rb, ix0, fill, y, h, HudDraw.mul(base, SHADE_K[0]), base, HudDraw.mul(base, SHADE_K[2]));
        }
        if (acc > 0f) {                                                           // saturation: garis terang di dasar
            int ae = Math.min(fill, extent(acc, SEP));
            if (ae > ix0) rb.add(ix0, y + h - 2, ae, y + h - 1, t.sat);
        }
        for (int i = 1; i < 10; i++) rb.add(SEP[i], y + 1, SEP[i] + 1, y + h - 1, edge);   // pemisah antar kotak
    }
}
