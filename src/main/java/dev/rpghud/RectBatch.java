package dev.rpghud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Daftar persegi berwarna yang di-cache. Dibangun ulang HANYA saat nilai (HP, lapar, armor, tema, ...) berubah;
 * tiap frame cukup memanggil draw() yang hanya melooping array int (tanpa alokasi objek baru).
 * addRun() menggabungkan garis horizontal yang menumpuk vertikal menjadi satu persegi (lebih sedikit fill).
 */
final class RectBatch {
    private int[] x0, y0, x1, y1, col;
    private int n;

    // status penggabungan vertikal
    private int[] prevAct = new int[160], curAct = new int[160];
    private int prevN, curN, curRow = Integer.MIN_VALUE;

    RectBatch(int capacity) {
        x0 = new int[capacity]; y0 = new int[capacity]; x1 = new int[capacity];
        y1 = new int[capacity]; col = new int[capacity];
    }

    int size() { return n; }

    void clear() {
        n = 0;
        prevN = 0;
        curN = 0;
        curRow = Integer.MIN_VALUE;
    }

    void add(int ax0, int ay0, int ax1, int ay1, int color) {
        if (ax1 <= ax0 || ay1 <= ay0) return;
        if (n == x0.length) grow();
        x0[n] = ax0; y0[n] = ay0; x1[n] = ax1; y1[n] = ay1; col[n] = color;
        n++;
    }

    /** Tambah garis horizontal 1px di baris y. Baris harus ditambahkan berurutan naik. */
    void addRun(int rx0, int rx1, int y, int color) {
        if (rx1 <= rx0) return;
        if (y != curRow) {
            if (y == curRow + 1) {
                int[] t = prevAct; prevAct = curAct; curAct = t;
                prevN = curN;
            } else {
                prevN = 0;
            }
            curN = 0;
            curRow = y;
        }
        for (int j = 0; j < prevN; j++) {
            int i = prevAct[j];
            if (x0[i] == rx0 && x1[i] == rx1 && col[i] == color && y1[i] == y) {
                y1[i] = y + 1;
                if (curN < curAct.length) curAct[curN++] = i;
                return;
            }
        }
        int idx = n;
        add(rx0, y, rx1, y + 1, color);
        if (curN < curAct.length) curAct[curN++] = idx;
    }

    void draw(GuiGraphicsExtractor g, int ox, int oy) {
        for (int i = 0; i < n; i++) {
            g.fill(ox + x0[i], oy + y0[i], ox + x1[i], oy + y1[i], col[i]);
        }
    }

    private void grow() {
        int cap = x0.length * 2;
        x0 = java.util.Arrays.copyOf(x0, cap); y0 = java.util.Arrays.copyOf(y0, cap);
        x1 = java.util.Arrays.copyOf(x1, cap); y1 = java.util.Arrays.copyOf(y1, cap);
        col = java.util.Arrays.copyOf(col, cap);
    }
}
