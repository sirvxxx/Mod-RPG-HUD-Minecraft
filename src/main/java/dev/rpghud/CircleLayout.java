package dev.rpghud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Type Circle: kepala skin di tengah lingkaran, dikelilingi bar kotak melengkung
 * (kiri = HP 20 kotak, kanan-atas = Hunger 10, kanan-bawah = Armor 10), nama di atas,
 * kotak "Lv N" di bawah (hanya teks), panel Coords + FPS di samping kanan.
 *
 * Track kosong + garis tepi kotak dipanggang di ring.png; kode hanya menggambar kotak yang terisi,
 * sebagai daftar persegi ter-cache yang dibangun ulang hanya saat nilainya berubah.
 */
final class CircleLayout {
    private CircleLayout() {}

    static final int D = 92;
    private static final int R = D / 2;
    private static final int RING_TEX = 96;          // ring.png (92 + ruang bayangan)
    static final int CAPSULE_H = 17;                 // tinggi 17 -> area dalam 11px, teks (7px) punya 2px lega atas-bawah
    static final int INFO_H = InfoPanels.H;

    private static final int BAND_RI = 33;
    private static final int BAND_RO = 40;
    private static final double CELL_DEG = 9.0;      // 40 kotak x 9 derajat = 360 derajat
    private static final float[] SHADE_K = {0.78f, 1.0f, 1.25f};   // shade isi: dalam / tengah / luar

    // pixel cincin: dihitung sekali seumur proses
    private static int[] bandX, bandY, bandSec, bandCell, bandShade;
    private static float[] bandPf;
    private static int bandCount;

    private static final float[] HP_C = new float[20];
    private static final float[] GH_C = new float[20];
    private static final float[] FO_C = new float[10];
    private static final float[] SA_C = new float[10];
    private static final float[] AR_C = new float[10];

    private static final RectBatch RING = new RectBatch(1100);
    private static long ringKey = Long.MIN_VALUE;

    static int levelWidth() { return Math.max(56, HudState.levelW + 22); }

    // ------------------------------------------------------------------ komponen (titik asal = pojok kiri-atas komponen)

    /** Component 1: kapsul nama. */
    static void drawName(GuiGraphicsExtractor g, Minecraft mc) {
        final HudTheme t = HudState.T;
        HudDraw.badge(g, t, 0, 0, D, CAPSULE_H);
        g.text(mc.font, HudState.nameCircle, (D - HudState.nameCircleW) / 2, 5, t.text, true);
    }

    /** Component 2: kepala skin + cincin bar kotak (HP kiri, Hunger kanan-atas, Armor kanan-bawah). */
    static void drawAvatar(GuiGraphicsExtractor g, Minecraft mc, HudConfig c) {
        final HudTheme t = HudState.T;

        // rim + alas cincin + cincin dalam + bayangan + track kotak kosong: satu tekstur
        g.blit(RenderPipelines.GUI_TEXTURED, t.ring, 0, 0, 0f, 0f, RING_TEX, RING_TEX, RING_TEX, RING_TEX, RING_TEX, RING_TEX);

        // cincin kotak: bangun ulang hanya kalau kuncinya berubah
        long key = HudState.statKey(c.showHealth, c.showHunger, c.showArmor);
        if (key != ringKey) {
            ringKey = key;
            rebuildRing(t, c.showHealth, c.showHunger, c.showArmor);
        }
        RING.draw(g, R, R);

        // kepala skin 40x40 (8x8 diperbesar 5x) + layer topi, muat di dalam cincin dalam
        if (c.showFace) {
            g.blit(RenderPipelines.GUI_TEXTURED, HudState.skin, R - 20, R - 20, 8f, 8f, 40, 40, 8, 8, 64, 64);
            g.blit(RenderPipelines.GUI_TEXTURED, HudState.skin, R - 20, R - 20, 40f, 8f, 40, 40, 8, 8, 64, 64);
        }
    }

    /** Component 3: kotak level (hanya teks, 2px lega atas-bawah). */
    static void drawLevel(GuiGraphicsExtractor g, Minecraft mc) {
        final HudTheme t = HudState.T;
        int bw = levelWidth();
        HudDraw.badge(g, t, 0, 0, bw, CAPSULE_H);
        g.text(mc.font, HudState.levelStr, (bw - HudState.levelW) / 2, 5, t.level, true);
    }

    // ------------------------------------------------------------------ cincin kotak (dibangun saat berubah)

    private static void cells(float[] out, int n, float frac) {
        for (int k = 0; k < n; k++) out[k] = HudDraw.clamp01(frac * n - k);
    }

    /** Hitung sekali: tiap pixel cincin -> sektor, kotak, posisi dalam kotak, shade (3 = garis tepi). */
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
                boolean edge = r < RI + 1.0 || r >= RO - 1.0
                        || p < edgeDeg
                        || (k == cellsN - 1 && p > CELL_DEG - edgeDeg);

                xs[n] = dx; ys[n] = dy; secs[n] = sec; cls[n] = k;
                pfs[n] = (float) Math.max(0.0, Math.min(1.0, (p - edgeDeg) / (CELL_DEG - edgeDeg)));
                shs[n] = edge ? 3 : (r < RI + 2.0 ? 0 : (r >= RO - 2.0 ? 2 : 1));
                n++;
            }
        }
        bandX = java.util.Arrays.copyOf(xs, n); bandY = java.util.Arrays.copyOf(ys, n);
        bandSec = java.util.Arrays.copyOf(secs, n); bandCell = java.util.Arrays.copyOf(cls, n);
        bandShade = java.util.Arrays.copyOf(shs, n); bandPf = java.util.Arrays.copyOf(pfs, n);
        bandCount = n;
    }

    private static void rebuildRing(HudTheme t, boolean showHp, boolean showFood, boolean showArmor) {
        initBand();
        cells(HP_C, 20, HudState.hpQ / 320f);
        cells(GH_C, 20, HudState.gQ / 320f);
        cells(FO_C, 10, HudState.food / 20f);
        cells(SA_C, 10, HudState.satQ / 40f);
        cells(AR_C, 10, HudState.arm / 20f);
        final int hpBase = HudState.hpBase();

        RING.clear();
        int runY = 0, runX0 = 0, runX1 = 0, runCol = 0;
        for (int i = 0; i < bandCount; i++) {
            int sec = bandSec[i];
            int col = 0;                                     // 0 = tidak digambar (track di ring.png sudah benar)
            boolean on = sec == 0 ? showHp : (sec == 1 ? showFood : showArmor);
            if (!on) {
                col = t.bandBack;                            // sektor dimatikan: tutup track-nya
            } else {
                int sh = bandShade[i];
                if (sh != 3) {                               // garis tepi sudah ada di tekstur
                    int k = bandCell[i];
                    float pf = bandPf[i];
                    int base = 0;
                    boolean accent = false;
                    switch (sec) {
                        case 0 -> {
                            float f = HP_C[k];
                            if (f >= 0.999f || pf < f) base = hpBase;
                            else if (GH_C[k] >= 0.999f || pf < GH_C[k]) base = t.ghost;
                        }
                        case 1 -> {
                            float f = FO_C[k];
                            if (f >= 0.999f || pf < f) {
                                base = t.food;
                                accent = sh == 2 && (SA_C[k] >= 0.999f || pf < SA_C[k]);
                            }
                        }
                        default -> {
                            float f = AR_C[k];
                            if (f >= 0.999f || pf < f) base = t.armor;
                        }
                    }
                    if (base != 0) col = accent ? t.sat : HudDraw.mul(base, SHADE_K[sh]);
                }
            }

            int y = bandY[i], x = bandX[i];
            if (runCol != 0 && col == runCol && y == runY && x == runX1) {
                runX1 = x + 1;
            } else {
                if (runCol != 0) RING.addRun(runX0, runX1, runY, runCol);
                runCol = col; runY = y; runX0 = x; runX1 = x + 1;
            }
        }
        if (runCol != 0) RING.addRun(runX0, runX1, runY, runCol);
    }
}
