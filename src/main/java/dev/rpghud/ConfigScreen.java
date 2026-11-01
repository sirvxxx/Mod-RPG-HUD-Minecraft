package dev.rpghud;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Menu pengaturan: slider X / Y / Scale, drag-and-drop panel HUD dengan mouse,
 * dan tombol toggle untuk tiap elemen. Dibuka dengan tombol H atau /rpghud gui.
 */
public class ConfigScreen extends Screen {
    private static final int BOX_W = 304;
    private static final int ROW_H = 22;
    private static final int COL_W = 98;
    private static final int COL_GAP = 5;

    private CfgSlider sliderX, sliderY, sliderScale;
    private int boxLeft, boxTop, boxH;
    private boolean dragging, wasDown;
    private int dragOffX, dragOffY;

    public ConfigScreen() {
        super(Component.literal("Custom RPG HUD"));
    }

    @Override
    protected void init() {
        HudRenderer.suppressHud = true;   // preview milik layar ini yang dipakai

        int rows = 7;
        boxH = 14 + rows * ROW_H;
        boxLeft = (this.width - BOX_W) / 2;
        boxTop = Math.max(0, this.height - boxH - 4);
        int y0 = boxTop + 14;

        sliderX = addRenderableWidget(new CfgSlider(this, 0, boxLeft, y0, BOX_W, 20));
        sliderY = addRenderableWidget(new CfgSlider(this, 1, boxLeft, y0 + ROW_H, BOX_W, 20));
        sliderScale = addRenderableWidget(new CfgSlider(this, 2, boxLeft, y0 + 2 * ROW_H, BOX_W, 20));

        toggle(0, 3, "Face", () -> HudConfig.get().showFace, v -> HudConfig.get().showFace = v);
        toggle(1, 3, "Level", () -> HudConfig.get().showLevel, v -> HudConfig.get().showLevel = v);
        toggle(2, 3, "Name", () -> HudConfig.get().showName, v -> HudConfig.get().showName = v);

        toggle(0, 4, "Health", () -> HudConfig.get().showHealth, v -> HudConfig.get().showHealth = v);
        toggle(1, 4, "Hunger", () -> HudConfig.get().showHunger, v -> HudConfig.get().showHunger = v);
        toggle(2, 4, "Armor", () -> HudConfig.get().showArmor, v -> HudConfig.get().showArmor = v);

        toggle(0, 5, "Coords", () -> HudConfig.get().showCoords, v -> HudConfig.get().showCoords = v);
        toggle(1, 5, "FPS", () -> HudConfig.get().showFps, v -> HudConfig.get().showFps = v);
        toggle(2, 5, "HUD", () -> HudConfig.get().enabled, v -> HudConfig.get().enabled = v);

        toggle(0, 6, "Hide vanilla", () -> HudConfig.get().hideVanilla, v -> HudConfig.get().hideVanilla = v);
        addRenderableWidget(Button.builder(Component.literal("Reset"), b -> {
            HudConfig.reset();
            this.rebuildWidgets();
        }).bounds(colX(1), rowY(6), COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(colX(2), rowY(6), COL_W, 20).build());
    }

    private int colX(int col) { return boxLeft + col * (COL_W + COL_GAP); }
    private int rowY(int row) { return boxTop + 14 + row * ROW_H; }

    private void toggle(int col, int row, String label, Supplier<Boolean> get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(toggleText(label, get.get()), b -> {
            set.accept(!get.get());
            HudConfig.save();
            b.setMessage(toggleText(label, get.get()));
            clampPos();
            syncSliders();
        }).bounds(colX(col), rowY(row), COL_W, 20).build());
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + ": ")
                .append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    // ------------------------------------------------------------------ posisi / ukuran

    private int hudW() { return Math.max(1, Math.round(HudRenderer.width(HudConfig.get()) * HudConfig.get().scale)); }
    private int hudH() { return Math.max(1, Math.round(HudRenderer.height(HudConfig.get()) * HudConfig.get().scale)); }
    int maxX() { return Math.max(1, this.width - hudW()); }
    int maxY() { return Math.max(1, this.height - hudH()); }

    void clampPos() {
        HudConfig c = HudConfig.get();
        c.x = Math.max(0, Math.min(c.x, Math.max(0, this.width - hudW())));
        c.y = Math.max(0, Math.min(c.y, Math.max(0, this.height - hudH())));
    }

    void syncSliders() {
        if (sliderX != null) sliderX.sync();
        if (sliderY != null) sliderY.sync();
        if (sliderScale != null) sliderScale.sync();
    }

    // ------------------------------------------------------------------ render + drag & drop

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);

        HudConfig c = HudConfig.get();
        HudRenderer.preview(g);
        g.outline(c.x - 1, c.y - 1, hudW() + 2, hudH() + 2, dragging ? 0xFFFFFFFF : 0x99FFD36B);
        g.text(this.font, "Custom RPG HUD - tarik panel HUD dengan mouse, atau pakai slider",
                boxLeft + 2, boxTop + 2, 0xFFFFD36B, true);

        // Drag & drop: polling status tombol kiri mouse
        boolean down = Minecraft.getInstance().mouseHandler.isLeftPressed();
        boolean overHud = mouseX >= c.x && mouseX < c.x + hudW() && mouseY >= c.y && mouseY < c.y + hudH();
        boolean overBox = mouseX >= boxLeft && mouseX < boxLeft + BOX_W && mouseY >= boxTop && mouseY < boxTop + boxH;

        if (down && !wasDown && overHud && !overBox) {
            dragging = true;
            dragOffX = mouseX - c.x;
            dragOffY = mouseY - c.y;
        }
        if (!down) dragging = false;
        if (dragging) {
            c.x = Math.max(0, Math.min(mouseX - dragOffX, Math.max(0, this.width - hudW())));
            c.y = Math.max(0, Math.min(mouseY - dragOffY, Math.max(0, this.height - hudH())));
            syncSliders();
        }
        wasDown = down;
    }

    @Override
    public void removed() {
        HudRenderer.suppressHud = false;
        HudConfig.save();
        super.removed();
    }

    // ------------------------------------------------------------------ slider

    /** kind: 0 = X, 1 = Y, 2 = Scale */
    private static final class CfgSlider extends AbstractSliderButton {
        private final ConfigScreen owner;
        private final int kind;

        CfgSlider(ConfigScreen owner, int kind, int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), 0.0);
            this.owner = owner;
            this.kind = kind;
            sync();
        }

        void sync() {
            HudConfig c = HudConfig.get();
            double r;
            switch (kind) {
                case 0 -> r = c.x / (double) owner.maxX();
                case 1 -> r = c.y / (double) owner.maxY();
                default -> r = (c.scale - 0.5) / 2.5;
            }
            this.value = Math.max(0.0, Math.min(1.0, r));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (owner == null) return;
            HudConfig c = HudConfig.get();
            String text = switch (kind) {
                case 0 -> "X: " + c.x;
                case 1 -> "Y: " + c.y;
                default -> String.format(Locale.ROOT, "Scale: %.2fx", c.scale);
            };
            setMessage(Component.literal(text));
        }

        @Override
        protected void applyValue() {
            HudConfig c = HudConfig.get();
            switch (kind) {
                case 0 -> c.x = (int) Math.round(this.value * owner.maxX());
                case 1 -> c.y = (int) Math.round(this.value * owner.maxY());
                default -> {
                    c.scale = Math.round((0.5 + this.value * 2.5) * 20) / 20f;
                    owner.clampPos();
                    owner.syncSliders();
                }
            }
        }
    }
}
