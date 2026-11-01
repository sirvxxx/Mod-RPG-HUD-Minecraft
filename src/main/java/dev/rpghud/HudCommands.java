package dev.rpghud;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

/**
 * /rpghud gui | edit | type <circle|bar|horizontal> | theme <id> | scale <0.5-3> | hidevanilla <bool> | toggle | resetpos | reset
 */
public final class HudCommands {
    private HudCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("rpghud")
                .then(ClientCommands.literal("gui")
                    .executes(ctx -> {
                        RpgHudClient.requestOpen();
                        return 1;
                    }))
                .then(ClientCommands.literal("edit")
                    .executes(ctx -> {
                        RpgHudClient.requestEdit();
                        return 1;
                    }))
                .then(ClientCommands.literal("scale")
                    .then(ClientCommands.argument("value", FloatArgumentType.floatArg(0.5f, 3f))
                        .executes(ctx -> {
                            HudConfig.get().scale = FloatArgumentType.getFloat(ctx, "value");
                            HudConfig.save();
                            ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.scale", HudConfig.get().scale));
                            return 1;
                        })))
                .then(ClientCommands.literal("type")
                    .then(ClientCommands.argument("name", StringArgumentType.word())
                        .executes(ctx -> {
                            String n = StringArgumentType.getString(ctx, "name").toLowerCase(java.util.Locale.ROOT);
                            int type;
                            String key;
                            switch (n) {
                                case "circle" -> { type = HudConfig.TYPE_CIRCLE; key = "custom_rpg_hud.layout.circle"; }
                                case "bar" -> { type = HudConfig.TYPE_BAR; key = "custom_rpg_hud.layout.bar"; }
                                case "horizontal" -> { type = HudConfig.TYPE_HORIZONTAL; key = "custom_rpg_hud.layout.horizontal"; }
                                default -> {
                                    ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.type_unknown"));
                                    return 0;
                                }
                            }
                            HudConfig.get().layoutType = type;
                            HudConfig.save();
                            ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.type", Component.translatable(key)));
                            return 1;
                        })))
                .then(ClientCommands.literal("theme")
                    .then(ClientCommands.argument("id", StringArgumentType.word())
                        .executes(ctx -> {
                            String id = StringArgumentType.getString(ctx, "id").toLowerCase(java.util.Locale.ROOT);
                            for (HudTheme t : HudTheme.ALL) {
                                if (t.id.equals(id)) {
                                    HudConfig.get().theme = t.id;
                                    HudConfig.save();
                                    ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.theme",
                                            Component.translatable(t.translationKey())));
                                    return 1;
                                }
                            }
                            StringBuilder all = new StringBuilder();
                            for (HudTheme t : HudTheme.ALL) all.append(all.length() == 0 ? "" : ", ").append(t.id);
                            ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.theme_unknown", all.toString()));
                            return 0;
                        })))
                .then(ClientCommands.literal("hidevanilla")
                    .then(ClientCommands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            HudConfig.get().hideVanilla = BoolArgumentType.getBool(ctx, "value");
                            HudConfig.save();
                            ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.hide_vanilla", HudConfig.get().hideVanilla));
                            return 1;
                        })))
                .then(ClientCommands.literal("toggle")
                    .executes(ctx -> {
                        HudConfig.get().enabled = !HudConfig.get().enabled;
                        HudConfig.save();
                        ctx.getSource().sendFeedback(Component.translatable(HudConfig.get().enabled ? "custom_rpg_hud.cmd.on" : "custom_rpg_hud.cmd.off"));
                        return 1;
                    }))
                .then(ClientCommands.literal("resetpos")
                    .executes(ctx -> {
                        HudConfig.get().resetPositions();
                        HudConfig.save();
                        ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.reset_pos"));
                        return 1;
                    }))
                .then(ClientCommands.literal("reset")
                    .executes(ctx -> {
                        HudConfig.reset();
                        ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.reset"));
                        return 1;
                    }))
            ));
    }
}
