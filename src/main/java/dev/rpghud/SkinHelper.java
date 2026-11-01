package dev.rpghud;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;

public final class SkinHelper {
    private SkinHelper() {}

    /** Texture skin player yang sedang aktif (64x64). */
    public static Identifier bodyTexture(LocalPlayer p) {
        return p.getSkin().body().texturePath();
    }
}
