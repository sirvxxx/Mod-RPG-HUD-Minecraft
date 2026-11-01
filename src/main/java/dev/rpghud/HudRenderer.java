package dev.rpghud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * Titik masuk render HUD (v7). Memilih layout (Circle / Bar) dari config dan menjaga
 * semua cache supaya render per-frame tidak mengalokasikan objek baru:
 *   - HudState   : string, lebar teks, statistik terkuantisasi (diperbarui hanya saat berubah)
 *   - RectBatch  : daftar persegi bar yang dibangun ulang hanya saat nilai bar berubah
 *   - HudTheme   : Identifier tekstur dibuat sekali
 */
public final class HudRenderer {
    private HudRenderer() {}

    /** true saat layar pengaturan / edit terbuka (layar itu menggambar preview sendiri). */
    public static boolean suppressHud = false;

    private static long fpsWindowStart = System.currentTimeMillis();
    private static int frames;
    private static int fps;

    /** HP yang "turun pelan" untuk jejak damage; -1 = belum diinisialisasi. */
    static float shownHp = -1f;
    private static long lastNs = System.nanoTime();

    // ------------------------------------------------------------------ ukuran (dipakai layar config / edit)

    private static void ensure(HudConfig c) {
        if (HudState.ready) { HudState.theme(c); return; }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        HudState.theme(c);
        HudState.update(mc, mc.player, fps, shownHp);
    }

    public static int width(HudConfig c) {
        ensure(c);
        return c.layoutType == HudConfig.TYPE_BAR ? BarLayout.width(c) : CircleLayout.width(c);
    }

    public static int height(HudConfig c) {
        ensure(c);
        return c.layoutType == HudConfig.TYPE_BAR ? BarLayout.height(c) : CircleLayout.height(c);
    }

    // ------------------------------------------------------------------ entry point

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        tick(p);
        HudConfig c = HudConfig.get();
        if (suppressHud || !c.enabled || p == null) return;
        draw(g, mc, p, c);
    }

    public static void preview(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        draw(g, mc, mc.player, HudConfig.get());
    }

    /** Warna angka FPS: hijau >= 60, kuning >= 30, merah di bawahnya. */
    static int fpsColor() {
        return fps >= 60 ? 0xFF7CFC7C : (fps >= 30 ? 0xFFFFE066 : 0xFFFF6B6B);
    }

    private static void tick(LocalPlayer p) {
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

    private static void draw(GuiGraphicsExtractor g, Minecraft mc, LocalPlayer p, HudConfig c) {
        HudState.theme(c);
        HudState.update(mc, p, fps, shownHp);

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) c.x, (float) c.y);
        pose.scale(c.scale, c.scale);

        if (c.layoutType == HudConfig.TYPE_BAR) BarLayout.draw(g, mc, p, c);
        else CircleLayout.draw(g, mc, p, c);

        pose.popMatrix();
    }
}
