package dev.rpghud;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

/**
 * /rpghud gui | edit | x <n> | y <n> | scale <0.5-3> | hidevanilla <bool> | toggle | reset
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
                            ctx.getSource().sendFeedback(Component.literal("RPG HUD X = " + HudConfig.get().x));
                            return 1;
                        })))
                .then(ClientCommands.literal("y")
                    .then(ClientCommands.argument("value", IntegerArgumentType.integer(0, 4000))
                        .executes(ctx -> {
                            HudConfig.get().y = IntegerArgumentType.getInteger(ctx, "value");
                            HudConfig.save();
                            ctx.getSource().sendFeedback(Component.literal("RPG HUD Y = " + HudConfig.get().y));
                            return 1;
                        })))
                .then(ClientCommands.literal("scale")
                    .then(ClientCommands.argument("value", FloatArgumentType.floatArg(0.5f, 3f))
                        .executes(ctx -> {
                            HudConfig.get().scale = FloatArgumentType.getFloat(ctx, "value");
                            HudConfig.save();
                            ctx.getSource().sendFeedback(Component.literal("RPG HUD scale = " + HudConfig.get().scale));
                            return 1;
                        })))
                .then(ClientCommands.literal("hidevanilla")
                    .then(ClientCommands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> {
                            HudConfig.get().hideVanilla = BoolArgumentType.getBool(ctx, "value");
                            HudConfig.save();
                            ctx.getSource().sendFeedback(Component.literal("Sembunyikan bar vanilla = " + HudConfig.get().hideVanilla));
                            return 1;
                        })))
                .then(ClientCommands.literal("toggle")
                    .executes(ctx -> {
                        HudConfig.get().enabled = !HudConfig.get().enabled;
                        HudConfig.save();
                        ctx.getSource().sendFeedback(Component.literal("RPG HUD " + (HudConfig.get().enabled ? "ON" : "OFF")));
                        return 1;
                    }))
                .then(ClientCommands.literal("reset")
                    .executes(ctx -> {
                        HudConfig.reset();
                        ctx.getSource().sendFeedback(Component.literal("RPG HUD di-reset ke default"));
                        return 1;
                    }))
            ));
    }
}
