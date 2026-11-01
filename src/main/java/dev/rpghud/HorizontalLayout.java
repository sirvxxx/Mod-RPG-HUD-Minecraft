package dev.rpghud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Type 3 (Modern RPG Horizontal Bar): pengganti bar vanilla di bawah-tengah layar, tepat di atas hotbar.
 *
 *   [slot1 slot2] ===HP=== < diamond: level + pita XP hijau > ===Hunger=== [slot3 slot4]
 *
 *  - Tabung kiri  = HP (merah), isi menyusut dari ujung luar menuju intan.
 *  - Tabung kanan = Hunger (oranye), isi menyusut dari ujung luar menuju intan.
 *  - Intan tengah = progres XP (garis tepi intan terisi hijau dari kiri ke kanan) + angka level di dalamnya.
 *  - 4 slot armor: 1 = helm, 2 = baju (ujung kiri atas/bawah), 3 = celana, 4 = sepatu (ujung kanan atas/bawah).
 *    Terisi = warna aksen tema aktif, kosong = hitam pekat.
 * Bingkai, tabung, intan, dan slot semua tekstur PNG tema (outline otomatis mengikuti tema).
 */
final class HorizontalLayout {
    private HorizontalLayout() {}

    static final int W = 264;
    static final int H = 40;
    private static final int CX = W / 2, CY = H / 2;

    private static final int TUBE_Y = CY - 6;                  // tabung setinggi 12
    private static final int TUBE_W = CX - 8 - 12;             // 112
    private static final int LX0 = 20, LX1 = CX - 17;          // saluran isi kiri  (95px)
    private static final int RX0 = CX + 17, RX1 = W - 20;      // saluran isi kanan (95px)
    private static final int FILL_W = LX1 - LX0;
    private static final int CH_Y0 = CY - 3, CH_Y1 = CY + 3;   // 6 baris saluran

    private static final int XP_GREEN = 0xFF8CFF6A;
    private static final int XP_SPAN = 34;                     // lebar jalur XP (piksel)

    private static final RectBatch FILL = new RectBatch(32);
    private static final RectBatch XP = new RectBatch(160);
    private static long fillKey = Long.MIN_VALUE, xpKey = Long.MIN_VALUE;

    // jalur XP = piksel intan dengan jarak Manhattan d dalam [15,17) (sama dengan tools/gen_assets.py)
    private static int[] xpX, xpY, xpPos, xpShade;
    private static int xpCount;

    // ------------------------------------------------------------------ gambar

    static void draw(GuiGraphicsExtractor g, Minecraft mc, HudConfig c) {
        final HudTheme t = HudState.T;

        // 1. tabung HP (kiri) dan Hunger (kanan)
        HudDraw.hslice(g, t.hTube, 20, 12, 4, 12, TUBE_Y, TUBE_W);
        HudDraw.hslice(g, t.hTube, 20, 12, 4, CX + 8, TUBE_Y, TUBE_W);

        // 2. isi HP / Hunger (cache)
        int hpPx = Math.round(HudState.hpQ / 320f * FILL_W);
        int gPx = Math.round(HudState.gQ / 320f * FILL_W);
        int foodPx = Math.round(HudState.food / 20f * FILL_W);
        int satPx = Math.min(foodPx, Math.round(HudState.satQ / 40f * FILL_W));
        long key = (long) hpPx | ((long) gPx << 7) | ((long) foodPx << 14) | ((long) satPx << 21)
                | ((long) HudState.pulseQ << 28) | ((long) t.index << 31)
                | ((c.showHealth ? 1L : 0L) << 34) | ((c.showHunger ? 1L : 0L) << 35);
        if (key != fillKey) {
            fillKey = key;
            rebuildFill(t, c.showHealth, c.showHunger, hpPx, gPx, foodPx, satPx);
        }
        FILL.draw(g, 0, 0);

        // 3. ujung (cap) + 4 slot armor
        HudDraw.badge(g, t, 0, CY - 15, 16, 30);
        HudDraw.badge(g, t, W - 16, CY - 15, 16, 30);
        if (c.showArmor) {
            int m = HudState.slotMask;
            slot(g, t, 4, CY - 13, (m & 1) != 0);            // armor 1: helm
            slot(g, t, 4, CY + 5, (m & 2) != 0);             // armor 2: baju
            slot(g, t, W - 12, CY - 13, (m & 4) != 0);       // armor 3: celana
            slot(g, t, W - 12, CY + 5, (m & 8) != 0);        // armor 4: sepatu
        }

        // 4. intan + pita XP hijau + angka level
        g.blit(RenderPipelines.GUI_TEXTURED, t.hDiamond, CX - 20, CY - 20, 0f, 0f, 40, 40, 40, 40, 40, 40);
        int xpPx = Math.round(HudState.xpFrac * XP_SPAN);
        if ((long) xpPx != xpKey) {
            xpKey = xpPx;
            rebuildXp(xpPx);
        }
        XP.draw(g, CX, CY);
        g.text(mc.font, HudState.levelNumStr, CX - HudState.levelNumW / 2, CY - 3, XP_GREEN, true);
    }

    private static void slot(GuiGraphicsExtractor g, HudTheme t, int x, int y, boolean on) {
        g.blit(RenderPipelines.GUI_TEXTURED, on ? t.hSlotOn : t.hSlotOff, x, y, 0f, 0f, 8, 8, 8, 8, 8, 8);
    }

    // ------------------------------------------------------------------ cache isi tabung

    private static void rows3(int x0, int x1, int light, int mid, int dark) {
        if (x1 <= x0) return;
        FILL.add(x0, CH_Y0, x1, CH_Y0 + 1, light);
        FILL.add(x0, CH_Y0 + 1, x1, CH_Y1 - 1, mid);
        FILL.add(x0, CH_Y1 - 1, x1, CH_Y1, dark);
    }

    private static void rebuildFill(HudTheme t, boolean showHp, boolean showFood,
                                    int hpPx, int gPx, int foodPx, int satPx) {
        FILL.clear();
        if (showHp) {
            int base = HudState.hpBase();
            if (gPx > hpPx) {
                rows3(LX1 - gPx, LX1 - hpPx, HudDraw.mul(t.ghost, 1.25f), t.ghost, HudDraw.mul(t.ghost, 0.78f));
            }
            rows3(LX1 - hpPx, LX1, HudDraw.mul(base, 1.25f), base, HudDraw.mul(base, 0.78f));
        }
        if (showFood) {
            rows3(RX0, RX0 + foodPx, HudDraw.mul(t.food, 1.25f), t.food, HudDraw.mul(t.food, 0.78f));
            if (satPx > 0) FILL.add(RX0, CH_Y1 - 1, RX0 + satPx, CH_Y1, t.sat);   // saturation: garis terang di dasar
        }
    }

    // ------------------------------------------------------------------ cache pita XP (garis tepi intan)

    private static void initXp() {
        if (xpX != null) return;
        int[] xs = new int[256], ys = new int[256], ps = new int[256], sh = new int[256];
        int n = 0;
        for (int y = 0; y < 40; y++) {
            for (int x = 0; x < 40; x++) {
                double fx = x + 0.5 - 20.0, fy = y + 0.5 - 20.0;
                double d = Math.abs(fx) + Math.abs(fy);
                if (d < 15.0 || d >= 17.0) continue;
                xs[n] = x - 20; ys[n] = y - 20;
                ps[n] = (int) Math.floor(fx + 17.0);                 // 0..33: posisi horizontal pada jalur
                sh[n] = d < 16.0 ? 0 : 1;
                n++;
            }
        }
        xpX = java.util.Arrays.copyOf(xs, n); xpY = java.util.Arrays.copyOf(ys, n);
        xpPos = java.util.Arrays.copyOf(ps, n); xpShade = java.util.Arrays.copyOf(sh, n);
        xpCount = n;
    }

    private static void rebuildXp(int xpPx) {
        initXp();
        XP.clear();
        final int dark = HudDraw.mul(XP_GREEN, 0.78f);
        int runY = 0, runX0 = 0, runX1 = 0, runCol = 0;
        for (int i = 0; i < xpCount; i++) {
            int col = 0;
            if (xpPos[i] < xpPx) col = xpShade[i] == 0 ? dark : XP_GREEN;
            int y = xpY[i], x = xpX[i];
            if (runCol != 0 && col == runCol && y == runY && x == runX1) {
                runX1 = x + 1;
            } else {
                if (runCol != 0) XP.addRun(runX0, runX1, runY, runCol);
                runCol = col; runY = y; runX0 = x; runX1 = x + 1;
            }
        }
        if (runCol != 0) XP.addRun(runX0, runX1, runY, runCol);
    }
}
