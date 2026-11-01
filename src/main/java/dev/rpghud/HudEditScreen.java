package dev.rpghud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * HUD Edit Mode: kursor muncul, klik-tahan di area HUD lalu seret ke mana saja.
 * Posisi X/Y mengikuti kursor secara langsung dan tersimpan otomatis saat keluar.
 * Dibuka dengan tombol J (bisa diganti di Controls) atau /rpghud edit.
 */
public class HudEditScreen extends Screen {
    private static final int BAR_H = 30;   // area tombol di bawah (tidak memicu drag)

    private boolean dragging, wasDown;
    private int dragOffX, dragOffY;

    public HudEditScreen() {
        super(Component.translatable("custom_rpg_hud.edit.title"));
    }

    @Override
    protected void init() {
        HudRenderer.suppressHud = true;   // preview milik layar ini yang dipakai

        int bw = 62, gap = 4;
        int total = 6 * bw + 5 * gap;
        int x = (this.width - total) / 2;
        int y = this.height - BAR_H + 5;

        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.edit.scale_minus"), b -> {
            HudConfig c = HudConfig.get();
            c.scale = Math.max(0.5f, Math.round((c.scale - 0.1f) * 10f) / 10f);
            clampPos();
        }).bounds(x, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.edit.scale_plus"), b -> {
            HudConfig c = HudConfig.get();
            c.scale = Math.min(3f, Math.round((c.scale + 0.1f) * 10f) / 10f);
            clampPos();
        }).bounds(x + (bw + gap), y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.edit.center"), b -> {
            HudConfig c = HudConfig.get();
            c.x = Math.max(0, (this.width - hudW()) / 2);
            c.y = Math.max(0, (this.height - hudH()) / 2);
        }).bounds(x + 2 * (bw + gap), y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.edit.reset_pos"), b -> {
            HudConfig c = HudConfig.get();
            c.x = 8;
            c.y = 8;
            clampPos();
        }).bounds(x + 3 * (bw + gap), y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.edit.settings"), b -> {
            HudConfig.save();
            McCompat.openScreen(new ConfigScreen());
        }).bounds(x + 4 * (bw + gap), y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.config.done"), b -> this.onClose())
                .bounds(x + 5 * (bw + gap), y, bw, 20).build());
    }

    private int hudW() { return Math.max(1, Math.round(HudRenderer.width(HudConfig.get()) * HudConfig.get().scale)); }
    private int hudH() { return Math.max(1, Math.round(HudRenderer.height(HudConfig.get()) * HudConfig.get().scale)); }

    private void clampPos() {
        HudConfig c = HudConfig.get();
        c.x = Math.max(0, Math.min(c.x, Math.max(0, this.width - hudW())));
        c.y = Math.max(0, Math.min(c.y, Math.max(0, this.height - hudH())));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        HudConfig c = HudConfig.get();
        int w = hudW(), h = hudH();

        // Drag & drop: polling status tombol kiri mouse
        boolean down = Minecraft.getInstance().mouseHandler.isLeftPressed();
        boolean overHud = mouseX >= c.x && mouseX < c.x + w && mouseY >= c.y && mouseY < c.y + h;
        boolean overBar = mouseY >= this.height - BAR_H;

        if (down && !wasDown && overHud && !overBar) {
            dragging = true;
            dragOffX = mouseX - c.x;
            dragOffY = mouseY - c.y;
        }
        if (!down) dragging = false;
        if (dragging) {
            c.x = Math.max(0, Math.min(mouseX - dragOffX, Math.max(0, this.width - w)));
            c.y = Math.max(0, Math.min(mouseY - dragOffY, Math.max(0, this.height - h)));
        }
        wasDown = down;

        HudRenderer.preview(g);

        // bingkai edit: putih saat diseret, emas saat disorot
        int frameCol = dragging ? 0xFFFFFFFF : (overHud ? 0xFFFFD36B : 0x99FFD36B);
        g.outline(c.x - 2, c.y - 2, w + 4, h + 4, frameCol);

        String title = Component.translatable("custom_rpg_hud.edit.title").getString();
        String hint = Component.translatable("custom_rpg_hud.edit.hint").getString();
        g.text(this.font, title, (this.width - this.font.width(title)) / 2, 8, 0xFFFFD36B, true);
        g.text(this.font, hint, (this.width - this.font.width(hint)) / 2, 20, 0xFFFFFFFF, true);

        String info = Component.translatable("custom_rpg_hud.edit.info", c.x, c.y,
                String.format(Locale.ROOT, "%.1f", c.scale)).getString();
        g.text(this.font, info, (this.width - this.font.width(info)) / 2, this.height - BAR_H - 12, 0xFF9CFF7A, true);
    }

    @Override
    public void removed() {
        HudRenderer.suppressHud = false;
        HudConfig.save();
        super.removed();
    }
}
