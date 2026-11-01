package dev.rpghud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Type Bar: panel persegi panjang berbingkai logam.
 *   Name   : kapsul berisi nama (kiri) + angka HP 20/20 (kanan)
 *   Avatar : kotak foto skin (kiri) + panel bar HP (penuh), Hunger dan Armor berdampingan (kanan)
 *   Level  : badge "Lv N" (default di bawah foto)
 * Bar 10 kotak horizontal digambar dari daftar persegi ter-cache (dibangun ulang hanya saat nilainya berubah),
 * dengan trik isi kontinu + garis pemisah: ~60 persegi untuk ketiga bar.
 */
final class BarLayout {
    private BarLayout() {}

    static final int FACE = 40;
    static final int FACE_Y = 2;                    // foto skin rata tengah terhadap panel setinggi 44
    static final int PANEL_X = 44;
    static final int PANEL_W = 170;
    static final int PANEL_H = 44;
    static final int AVATAR_W = PANEL_X + PANEL_W;  // 214 (dengan foto)
    static final int AVATAR_H = PANEL_H;
    static final int NAME_W = PANEL_W;
    static final int CAPSULE_H = 17;
    private static final int ICON = HudDraw.ICON;

    // posisi bar relatif ke pojok kiri-atas panel
    private static final int HP_Y = 9, HP_H = 12;
    private static final int ROW2_Y = 25, ROW2_H = 10;
    private static final int HP_X = 18, HP_W = PANEL_W - 7 - HP_X;          // 145
    private static final int HALF = (PANEL_W - 14 - 6) / 2;                  // 75
    private static final int HUN_X = 18, HUN_W = HALF - 11;                   // 64
    private static final int ARM_ICON_X = 7 + HALF + 6;                       // 88
    private static final int ARM_X = ARM_ICON_X + 11, ARM_W = HUN_W;

    private static final float[] SHADE_K = {1.25f, 1.0f, 0.78f};   // atas terang, tengah, bawah gelap

    private static final RectBatch BARS = new RectBatch(128);
    private static long barsKey = Long.MIN_VALUE;
    private static final int[] SEP = new int[11];

    static int avatarWidth(HudConfig c) { return c.showFace ? AVATAR_W : PANEL_W; }

    static int levelWidth() { return Math.max(34, Math.min(FACE, HudState.levelW + 14)); }

    // ------------------------------------------------------------------ komponen (titik asal = pojok kiri-atas komponen)

    /** Component 1: kapsul nama + angka HP. */
    static void drawName(GuiGraphicsExtractor g, Minecraft mc, HudConfig c) {
        final HudTheme t = HudState.T;
        HudDraw.badge(g, t, 0, 0, NAME_W, CAPSULE_H);
        g.text(mc.font, HudState.nameBar, 7, 5, t.text, true);
        if (c.showHealth) g.text(mc.font, HudState.hpStr, NAME_W - 7 - HudState.hpStrW, 5, 0xFFFFFFFF, true);
    }

    /** Component 2: foto skin berbingkai + panel bar HP / Hunger / Armor. */
    static void drawAvatar(GuiGraphicsExtractor g, Minecraft mc, HudConfig c) {
        final HudTheme t = HudState.T;
        final int px = c.showFace ? PANEL_X : 0;

        if (c.showFace) {
            // area dalam bingkai 32x32 = kepala 8x8 diperbesar 4x
            HudDraw.frame(g, t, 0, FACE_Y, FACE, FACE);
            g.blit(RenderPipelines.GUI_TEXTURED, HudState.skin, 4, FACE_Y + 4, 8f, 8f, 32, 32, 8, 8, 64, 64);
            g.blit(RenderPipelines.GUI_TEXTURED, HudState.skin, 4, FACE_Y + 4, 40f, 8f, 32, 32, 8, 8, 64, 64);
        }

        HudDraw.frame(g, t, px, 0, PANEL_W, PANEL_H);
        if (c.showHealth) HudDraw.icon(g, t.iconHeart, px + 7, HP_Y + (HP_H - ICON) / 2);
        if (c.showHunger) HudDraw.icon(g, t.iconFood, px + 7, ROW2_Y + (ROW2_H - ICON) / 2);
        if (c.showArmor) HudDraw.icon(g, t.iconArmor, px + ARM_ICON_X, ROW2_Y + (ROW2_H - ICON) / 2);

        long key = HudState.statKey(c.showHealth, c.showHunger, c.showArmor);
        if (key != barsKey) {
            barsKey = key;
            rebuildBars(t, c.showHealth, c.showHunger, c.showArmor);
        }
        BARS.draw(g, px, 0);
    }

    /** Component 3: badge level. */
    static void drawLevel(GuiGraphicsExtractor g, Minecraft mc) {
        final HudTheme t = HudState.T;
        int bw = levelWidth();
        HudDraw.badge(g, t, 0, 0, bw, CAPSULE_H);
        g.text(mc.font, HudState.levelStr, (bw - HudState.levelW) / 2, 5, t.level, true);
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
