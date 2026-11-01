package dev.rpghud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class RpgHudClient implements ClientModInitializer {
    public static final String MOD_ID = "custom_rpg_hud";

    private static KeyMapping openKey;
    private static volatile boolean openRequested;

    /** Dipanggil dari command /rpghud gui (dibuka di tick berikutnya agar tidak bentrok dengan layar chat). */
    public static void requestOpen() { openRequested = true; }

    @Override
    public void onInitializeClient() {
        HudConfig.load();

        // Sembunyikan bar vanilla (opsional)
        hideVanilla(VanillaHudElements.HEALTH_BAR);
        hideVanilla(VanillaHudElements.ARMOR_BAR);
        hideVanilla(VanillaHudElements.FOOD_BAR);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "rpg_hud"),
                HudRenderer::render);

        // Tombol untuk membuka menu pengaturan (default: H, bisa diganti di Options > Controls)
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.custom_rpg_hud.open_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean open = false;
            while (openKey.consumeClick()) open = true;
            if (openRequested) { openRequested = false; open = true; }
            if (open && client.player != null) McCompat.openScreen(new ConfigScreen());
        });

        HudCommands.register();
    }

    private static void hideVanilla(Identifier id) {
        HudElementRegistry.replaceElement(id, original -> (HudElement) (graphics, delta) -> {
            HudConfig c = HudConfig.get();
            if (!(c.enabled && c.hideVanilla)) original.extractRenderState(graphics, delta);
        });
    }
}
