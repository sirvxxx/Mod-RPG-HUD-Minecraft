package dev.rpghud;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Menu pengaturan: skala, visibilitas tiap komponen, bagian avatar, tema, tipe layout.
 * Posisi tiap komponen diatur di layar HUD Edit Mode (tombol J). Dibuka dengan tombol H atau /rpghud gui.
 */
public class ConfigScreen extends Screen {
    private static final int BOX_W = 304;
    private static final int ROW_H = 22;
    private static final int COL_W = 98;
    private static final int COL_GAP = 5;
    private static final int ROWS = 7;

    private int boxLeft, boxTop, boxH;

    public ConfigScreen() {
        super(Component.translatable("custom_rpg_hud.config.title"));
    }

    @Override
    protected void init() {
        HudRenderer.suppressHud = true;   // preview milik layar ini yang dipakai
        HudRenderer.setScreenSize(this.width, this.height);
        HudRenderer.ensure();

        boxH = 14 + ROWS * ROW_H;
        boxLeft = (this.width - BOX_W) / 2;
        boxTop = Math.max(0, this.height - boxH - 4);

        // baris 0: skala
        addRenderableWidget(new ScaleSlider(boxLeft, rowY(0), BOX_W, 20));

        // baris 1-2: visibilitas komponen (Component 1..5) + HUD on/off
        widgetToggle(0, 1, HudWidget.NAME);
        widgetToggle(1, 1, HudWidget.AVATAR);
        widgetToggle(2, 1, HudWidget.LEVEL);
        widgetToggle(0, 2, HudWidget.COORDS);
        widgetToggle(1, 2, HudWidget.FPS);
        toggle(2, 2, "custom_rpg_hud.config.hud", () -> HudConfig.get().enabled, v -> HudConfig.get().enabled = v, true);

        // baris 3: bagian dalam komponen Avatar & Status
        toggle(0, 3, "custom_rpg_hud.config.health", () -> HudConfig.get().showHealth, v -> HudConfig.get().showHealth = v, true);
        toggle(1, 3, "custom_rpg_hud.config.hunger", () -> HudConfig.get().showHunger, v -> HudConfig.get().showHunger = v, true);
        toggle(2, 3, "custom_rpg_hud.config.armor", () -> HudConfig.get().showArmor, v -> HudConfig.get().showArmor = v, true);

        // baris 4: wajah, hide vanilla, tema
        toggle(0, 4, "custom_rpg_hud.config.face", () -> HudConfig.get().showFace, v -> HudConfig.get().showFace = v,
                HudConfig.get().layoutType != HudConfig.TYPE_HORIZONTAL);
        toggle(1, 4, "custom_rpg_hud.config.hide_vanilla", () -> HudConfig.get().hideVanilla, v -> HudConfig.get().hideVanilla = v, true);
        addRenderableWidget(Button.builder(themeText(), b -> {
            HudConfig c = HudConfig.get();
            c.theme = HudTheme.next(HudTheme.byId(c.theme)).id;
            HudConfig.save();
            b.setMessage(themeText());
        }).bounds(colX(2), rowY(4), COL_W, 20).build());

        // baris 5: tipe layout, edit mode, reset posisi
        addRenderableWidget(Button.builder(layoutText(), b -> {
            HudConfig c = HudConfig.get();
            c.layoutType = (c.layoutType + 1) % HudConfig.TYPE_COUNT;
            HudConfig.save();
            this.rebuildWidgets();            // toggle komponen mengikuti tipe baru
        }).bounds(colX(0), rowY(5), COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.config.edit_mode"), b -> {
            HudConfig.save();
            McCompat.openScreen(new HudEditScreen());
        }).bounds(colX(1), rowY(5), COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.config.reset_pos"), b -> {
            HudConfig.get().resetPositions();
            HudConfig.save();
        }).bounds(colX(2), rowY(5), COL_W, 20).build());

        // baris 6: reset semua, selesai
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.config.reset"), b -> {
            HudConfig.reset();
            this.rebuildWidgets();
        }).bounds(colX(0), rowY(6), COL_W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("custom_rpg_hud.config.done"), b -> this.onClose())
                .bounds(colX(1), rowY(6), 2 * COL_W + COL_GAP, 20).build());
    }

    private static Component layoutText() {
        int t = HudConfig.get().layoutType;
        String key = t == HudConfig.TYPE_BAR ? "custom_rpg_hud.layout.bar"
                : (t == HudConfig.TYPE_HORIZONTAL ? "custom_rpg_hud.layout.horizontal" : "custom_rpg_hud.layout.circle");
        return Component.translatable("custom_rpg_hud.config.layout", Component.translatable(key));
    }

    private static Component themeText() {
        HudTheme t = HudTheme.byId(HudConfig.get().theme);
        return Component.translatable("custom_rpg_hud.config.theme", Component.translatable(t.translationKey()));
    }

    private int colX(int col) { return boxLeft + col * (COL_W + COL_GAP); }
    private int rowY(int row) { return boxTop + 14 + row * ROW_H; }

    /** Toggle visibilitas satu komponen (non-aktif kalau komponen itu tidak dipakai tipe layout sekarang). */
    private void widgetToggle(int col, int row, int id) {
        toggle(col, row, HudWidget.LANG_KEYS[id],
                () -> HudConfig.get().widget(id).visible,
                v -> HudConfig.get().widget(id).visible = v,
                HudWidget.applies(id, HudConfig.get().layoutType));
    }

    private void toggle(int col, int row, String key, Supplier<Boolean> get, Consumer<Boolean> set, boolean enabled) {
        Button btn = addRenderableWidget(Button.builder(toggleText(key, get.get()), b -> {
            set.accept(!get.get());
            HudConfig.save();
            b.setMessage(toggleText(key, get.get()));
        }).bounds(colX(col), rowY(row), COL_W, 20).build());
        btn.active = enabled;
    }

    private static Component toggleText(String key, boolean on) {
        return Component.translatable(key).append(Component.literal(": "))
                .append(Component.translatable(on ? "custom_rpg_hud.config.on" : "custom_rpg_hud.config.off")
                        .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    // ------------------------------------------------------------------ render

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        HudRenderer.setScreenSize(this.width, this.height);
        HudRenderer.preview(g);
        g.text(this.font, Component.translatable("custom_rpg_hud.config.hint").getString(),
                boxLeft + 2, boxTop + 2, 0xFFFFD36B, true);
    }

    @Override
    public void removed() {
        HudRenderer.suppressHud = false;
        HudConfig.save();
        super.removed();
    }

    // ------------------------------------------------------------------ slider skala

    private static final class ScaleSlider extends AbstractSliderButton {
        ScaleSlider(int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty(), (HudConfig.get().scale - 0.5) / 2.5);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("custom_rpg_hud.config.slider_scale",
                    String.format(Locale.ROOT, "%.2f", HudConfig.get().scale)));
        }

        @Override
        protected void applyValue() {
            HudConfig.get().scale = Math.round((0.5 + this.value * 2.5) * 20) / 20f;
        }
    }
}
