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
        super(Component.translatable("custom_rpg_hud.config.title"));
    }

    @Override
    protected void init() {
        HudRenderer.suppressHud = true;   // preview milik layar ini yang dipakai

        int rows = 8;
        boxH = 14 + rows * ROW_H;
        boxLeft = (this.width - BOX_W) / 2;
        boxTop = Math.max(0, this.height - boxH - 4);
        int y0 = boxTop + 14;

        sliderX = addRenderableWidget(new CfgSlider(this, 0, boxLeft, y0, BOX_W, 20));
        sliderY = addRenderableWidget(new CfgSlider(this, 1, boxLeft, y0 + ROW_H, BOX_W, 20));
        sliderScale = addRenderableWidget(new CfgSlider(this, 2, boxLeft, y0 + 2 * ROW_H, BOX_W, 20));

        toggle(0, 3, "custom_rpg_hud.config.face", () -> HudConfig.get().showFace, v -> HudConfig.get().showFace = v);
        toggle(1, 3, "custom_rpg_hud.config.level", () -> HudConfig.get().showLevel, v -> HudConfig.get().showLevel = v);
        toggle(2, 3, "custom_rpg_hud.config.name", () -> HudConfig.get().showName, v -> HudConfig.get().showName = v);

        toggle(0, 4, "custom_rpg_hud.config.health", () -> HudConfig.get().showHealth, v -> HudConfig.get().showHealth = v);
        toggle(1, 4, "custom_rpg_hud.config.hunger", () -> HudConfig.get().showHunger, v -> HudConfig.get().showHunger = v);
        toggle(2, 4, "custom_rpg_hud.config.armor", () -> HudConfig.get().showArmor, v -> HudConfig.get().showArmor = v);

        toggle(0, 5, "custom_rpg_hud.config.coords", () -> HudConfig.get().showCoords, v -> HudConfig.get().showCoords = v);
        toggle(1, 5, "custom_rpg_hud.config.fps", () -> HudConfig.get().showFps, v -> HudConfig.get().showFps = v);
        toggle(2, 5, "custom_rpg_hud.config.hud", () -> HudConfig.get().enabled, v -> HudConfig.get().enabled = v);

        // baris 6: hide vanilla, pemilih tema, pemilih tipe layout (Circle / Bar)
        toggle(0, 6, "custom_rpg_hud.config.hide_vanilla", () -> HudConfig.get().hideVanilla, v -> HudConfig.get().hideVanilla = v);
        addRenderableWidget(Button.builder(themeText(), b -> {
            HudConfig c = HudConfig.get();
            c.theme = HudTheme.next(HudTheme.byId(c.theme)).id;
            HudConfig.save();
            b.setMessage(themeText());
        }).bounds(colX(1), rowY(6), COL_W, 20).build());
        addRenderableWidget(Button.builder(layoutText(), b -> {
            HudConfig c = HudConfig.get();
            c.layoutType = c.layoutType == HudConfig.TYPE_BAR ? HudConfig.TYPE_CIRCLE : HudConfig.TYPE_BAR;
            HudConfig.save();
            b.setMessage(layoutText());
            clampPos();
            syncSliders();
        }).bounds(colX(2), rowY(6), COL_W, 20).build());

        // baris 7: edit mode, reset, selesai
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.config.edit_mode"), b -> {
            HudConfig.save();
            McCompat.openScreen(new HudEditScreen());
        }).bounds(colX(0), rowY(7), COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.config.reset"), b -> {
            HudConfig.reset();
            this.rebuildWidgets();
        }).bounds(colX(1), rowY(7), COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.config.done"), b -> this.onClose())
                .bounds(colX(2), rowY(7), COL_W, 20).build());
    }

    private static Component layoutText() {
        boolean bar = HudConfig.get().layoutType == HudConfig.TYPE_BAR;
        return Component.translatable("custom_rpg_hud.config.layout",
                Component.translatable(bar ? "custom_rpg_hud.layout.bar" : "custom_rpg_hud.layout.circle"));
    }

    private static Component themeText() {
        HudTheme t = HudTheme.byId(HudConfig.get().theme);
        return Component.translatable("custom_rpg_hud.config.theme", Component.translatable(t.translationKey()));
    }

    private int colX(int col) { return boxLeft + col * (COL_W + COL_GAP); }
    private int rowY(int row) { return boxTop + 14 + row * ROW_H; }

    private void toggle(int col, int row, String key, Supplier<Boolean> get, Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(toggleText(key, get.get()), b -> {
            set.accept(!get.get());
            HudConfig.save();
            b.setMessage(toggleText(key, get.get()));
            clampPos();
            syncSliders();
        }).bounds(colX(col), rowY(row), COL_W, 20).build());
    }

    private static Component toggleText(String key, boolean on) {
        return Component.translatable(key).append(Component.literal(": "))
                .append(Component.translatable(on ? "custom_rpg_hud.config.on" : "custom_rpg_hud.config.off")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
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
        g.text(this.font, Component.translatable("custom_rpg_hud.config.hint").getString(),
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
            Component msg = switch (kind) {
                case 0 -> Component.translatable("custom_rpg_hud.config.slider_x", c.x);
                case 1 -> Component.translatable("custom_rpg_hud.config.slider_y", c.y);
                default -> Component.translatable("custom_rpg_hud.config.slider_scale",
                        String.format(Locale.ROOT, "%.2f", c.scale));
            };
            setMessage(msg);
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
