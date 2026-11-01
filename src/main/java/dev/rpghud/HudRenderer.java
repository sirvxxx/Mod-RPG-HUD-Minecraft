package dev.rpghud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

/**
 * Titik masuk render HUD (v8). Menggambar tiap komponen (Name, Avatar/Status, Level, Coords, FPS)
 * di posisinya sendiri-sendiri, dengan layout sesuai tipe yang dipilih (Circle / Bar / Horizontal).
 *
 * Tanpa alokasi per frame:
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

    /** Layar edit / config memanggil ini supaya hitbox memakai ukuran layar yang sama dengan layar itu. */
    public static void setScreenSize(int w, int h) {
        HudWidget.screenW = w;
        HudWidget.screenH = h;
    }

    // ------------------------------------------------------------------ entry point

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        tick(p);
        HudConfig c = HudConfig.get();
        if (suppressHud || !c.enabled || p == null) return;
        setScreenSize(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
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

    /** Pastikan cache teks sudah terisi (dipanggil layar sebelum frame pertama dirender). */
    public static void ensure() {
        Minecraft mc = Minecraft.getInstance();
        if (HudState.ready || mc.player == null) return;
        HudState.theme(HudConfig.get());
        HudState.update(mc, mc.player, fps, shownHp);
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

        final var pose = g.pose();
        final float s = c.scale;
        for (int id = 0; id < HudWidget.COUNT; id++) {
            if (!HudWidget.visible(id, c)) continue;
            pose.pushMatrix();
            pose.translate((float) HudWidget.x(id, c), (float) HudWidget.y(id, c));
            pose.scale(s, s);
            drawWidget(id, g, mc, c);
            pose.popMatrix();
        }
    }

    /** Menggambar satu komponen dengan titik asal (0,0) = pojok kiri-atasnya. */
    private static void drawWidget(int id, GuiGraphicsExtractor g, Minecraft mc, HudConfig c) {
        switch (id) {
            case HudWidget.COORDS -> InfoPanels.drawCoords(g, mc);
            case HudWidget.FPS -> InfoPanels.drawFps(g, mc);
            default -> {
                switch (c.layoutType) {
                    case HudConfig.TYPE_BAR -> {
                        if (id == HudWidget.NAME) BarLayout.drawName(g, mc, c);
                        else if (id == HudWidget.AVATAR) BarLayout.drawAvatar(g, mc, c);
                        else BarLayout.drawLevel(g, mc);
                    }
                    case HudConfig.TYPE_HORIZONTAL -> HorizontalLayout.draw(g, mc, c);   // hanya AVATAR yang lolos visible()
                    default -> {
                        if (id == HudWidget.NAME) CircleLayout.drawName(g, mc);
                        else if (id == HudWidget.AVATAR) CircleLayout.drawAvatar(g, mc, c);
                        else CircleLayout.drawLevel(g, mc);
                    }
                }
            }
        }
    }
}
