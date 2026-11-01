package dev.rpghud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * HUD Edit Mode (tombol J atau /rpghud edit).
 *
 * Kursor muncul; klik-tahan satu komponen lalu seret ke mana saja. Hanya komponen yang diklik yang bergerak
 * (Name, Avatar/Status, Level, Coords, FPS punya posisi sendiri-sendiri). Komponen yang sedang diseret diberi
 * kotak cyan tipis. Posisi disimpan ke config JSON saat tombol mouse dilepas.
 *
 * Event mouse dibaca dari status tombol GLFW (HudInput) dan dibagi menjadi tiga handler dengan semantik yang
 * sama dengan mouseClicked / mouseDragged / mouseReleased: onMousePressed -> onMouseDragged -> onMouseReleased.
 * Cara ini tidak bergantung pada signature method Screen yang berubah antar versi Minecraft.
 */
public class HudEditScreen extends Screen {
    private static final int BAR_H = 30;                  // area tombol di bawah (tidak memicu drag)
    private static final int CYAN = 0xFF00E5FF;

    private int activeId = -1;                            // komponen yang sedang diseret
    private int grabDx, grabDy;                           // selisih kursor terhadap pojok kiri-atas komponen
    private boolean wasDown;

    public HudEditScreen() {
        super(Component.translatable("custom_rpg_hud.edit.title"));
    }

    @Override
    protected void init() {
        HudRenderer.suppressHud = true;                   // preview milik layar ini yang dipakai
        HudRenderer.setScreenSize(this.width, this.height);
        HudRenderer.ensure();

        int bw = 70, gap = 4;
        int total = 5 * bw + 4 * gap;
        int x = (this.width - total) / 2;
        int y = this.height - BAR_H + 5;

        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.edit.scale_minus"), b -> {
            HudConfig c = HudConfig.get();
            c.scale = Math.max(0.5f, Math.round((c.scale - 0.1f) * 10f) / 10f);
        }).bounds(x, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.edit.scale_plus"), b -> {
            HudConfig c = HudConfig.get();
            c.scale = Math.min(3f, Math.round((c.scale + 0.1f) * 10f) / 10f);
        }).bounds(x + (bw + gap), y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.edit.reset_pos"), b -> {
            HudConfig.get().resetPositions();
            HudConfig.save();
        }).bounds(x + 2 * (bw + gap), y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.edit.settings"), b -> {
            HudConfig.save();
            McCompat.openScreen(new ConfigScreen());
        }).bounds(x + 3 * (bw + gap), y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.config.done"), b -> this.onClose())
                .bounds(x + 4 * (bw + gap), y, bw, 20).build());
    }

    // ------------------------------------------------------------------ mouse: pressed -> dragged -> released

    /** Setara mouseClicked: pilih komponen paling atas di bawah kursor dan mulai menyeretnya. */
    private void onMousePressed(int mouseX, int mouseY) {
        if (mouseY >= this.height - BAR_H) return;        // klik di area tombol bawah
        int id = HudWidget.topAt(mouseX, mouseY);         // hanya satu komponen yang dipilih
        if (id < 0) return;
        HudConfig c = HudConfig.get();
        activeId = id;
        grabDx = mouseX - HudWidget.x(id, c);
        grabDy = mouseY - HudWidget.y(id, c);
    }

    /** Setara mouseDragged: posisi komponen aktif mengikuti kursor secara real-time. */
    private void onMouseDragged(int mouseX, int mouseY) {
        if (activeId < 0) return;
        HudWidget.setPos(activeId, mouseX - grabDx, mouseY - grabDy);
    }

    /** Setara mouseReleased: lepas komponen dan simpan koordinatnya ke config JSON. */
    private void onMouseReleased() {
        if (activeId < 0) return;
        activeId = -1;
        HudConfig.save();
    }

    // ------------------------------------------------------------------ render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        HudRenderer.setScreenSize(this.width, this.height);

        boolean down = HudInput.leftDown();
        if (down && !wasDown) onMousePressed(mouseX, mouseY);
        else if (down) onMouseDragged(mouseX, mouseY);
        else if (wasDown) onMouseReleased();
        wasDown = down;

        HudRenderer.preview(g);

        HudConfig c = HudConfig.get();
        int hover = activeId >= 0 ? activeId : HudWidget.topAt(mouseX, mouseY);
        for (int id = 0; id < HudWidget.COUNT; id++) {
            if (!HudWidget.visible(id, c)) continue;
            int x = HudWidget.x(id, c), y = HudWidget.y(id, c);
            int w = HudWidget.scaledW(id, c), h = HudWidget.scaledH(id, c);
            if (id == activeId) {
                g.fill(x, y, x + w, y + h, 0x2200E5FF);                       // sorotan cyan tipis
                g.outline(x - 1, y - 1, w + 2, h + 2, CYAN);
            } else {
                g.outline(x - 1, y - 1, w + 2, h + 2, id == hover ? 0xCCFFFFFF : 0x55FFFFFF);
            }
        }

        String title = Component.translatable("custom_rpg_hud.edit.title").getString();
        String hint = Component.translatable("custom_rpg_hud.edit.hint").getString();
        g.text(this.font, title, (this.width - this.font.width(title)) / 2, 8, 0xFFFFD36B, true);
        g.text(this.font, hint, (this.width - this.font.width(hint)) / 2, 20, 0xFFFFFFFF, true);

        if (hover >= 0) {
            String info = Component.translatable("custom_rpg_hud.edit.info",
                    Component.translatable(HudWidget.LANG_KEYS[hover]).getString(),
                    HudWidget.x(hover, c), HudWidget.y(hover, c),
                    String.format(Locale.ROOT, "%.1f", c.scale)).getString();
            g.text(this.font, info, (this.width - this.font.width(info)) / 2, this.height - BAR_H - 12, 0xFF9CFF7A, true);
        }
    }

    @Override
    public void removed() {
        HudRenderer.suppressHud = false;
        HudConfig.save();
        super.removed();
    }
}
