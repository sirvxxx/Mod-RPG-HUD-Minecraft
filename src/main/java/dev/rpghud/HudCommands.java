package dev.rpghud;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

/**
 * /rpghud gui | edit | type <circle|bar> | theme <id> | x <n> | y <n> | scale <0.5-3> | hidevanilla <bool> | toggle | reset
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
                .then(ClientCommands.literal("x")
                    .then(ClientCommands.argument("value", IntegerArgumentType.integer(0, 4000))
                        .executes(ctx -> {
                            HudConfig.get().x = IntegerArgumentType.getInteger(ctx, "value");
                            HudConfig.save();
                            ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.x", HudConfig.get().x));
                            return 1;
                        })))
                .then(ClientCommands.literal("y")
                    .then(ClientCommands.argument("value", IntegerArgumentType.integer(0, 4000))
                        .executes(ctx -> {
                            HudConfig.get().y = IntegerArgumentType.getInteger(ctx, "value");
                            HudConfig.save();
                            ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.y", HudConfig.get().y));
                            return 1;
                        })))
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
                            boolean bar = n.equals("bar");
                            if (!bar && !n.equals("circle")) {
                                ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.type_unknown"));
                                return 0;
                            }
                            HudConfig.get().layoutType = bar ? HudConfig.TYPE_BAR : HudConfig.TYPE_CIRCLE;
                            HudConfig.save();
                            ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.type",
                                    Component.translatable(bar ? "custom_rpg_hud.layout.bar" : "custom_rpg_hud.layout.circle")));
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
                .then(ClientCommands.literal("reset")
                    .executes(ctx -> {
                        HudConfig.reset();
                        ctx.getSource().sendFeedback(Component.translatable("custom_rpg_hud.cmd.reset"));
                        return 1;
                    }))
            ));
    }
}
