package dev.rpghud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class RpgHudClient implements ClientModInitializer {
    public static final String MOD_ID = "custom_rpg_hud";

    @Override
    public void onInitializeClient() {
        HudConfig.load();

        // Sembunyikan bar vanilla (opsional, lewat config / command)
        hideVanilla(VanillaHudElements.HEALTH_BAR);
        hideVanilla(VanillaHudElements.ARMOR_BAR);
        hideVanilla(VanillaHudElements.FOOD_BAR);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "rpg_hud"),
                HudRenderer::render);

        HudCommands.register();
    }

    private static void hideVanilla(Identifier id) {
        HudElementRegistry.replaceElement(id, original -> (HudElement) (graphics, delta) -> {
            HudConfig c = HudConfig.get();
            if (!(c.enabled && c.hideVanilla)) original.extractRenderState(graphics, delta);
        });
    }
}
