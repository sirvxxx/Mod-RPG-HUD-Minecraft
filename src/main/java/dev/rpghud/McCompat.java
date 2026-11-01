package dev.rpghud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Method;

/**
 * Jembatan antar versi Minecraft. Di 26.1.2 method-nya setScreen(Screen),
 * di 26.2 berganti nama menjadi setScreenAndShow(Screen). Dipanggil lewat reflection
 * supaya satu source bisa dibuild untuk keduanya.
 */
public final class McCompat {
    private McCompat() {}

    public static void openScreen(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        for (String name : new String[]{"setScreenAndShow", "setScreen"}) {
            try {
                Method m;
                try {
                    m = Minecraft.class.getMethod(name, Screen.class);
                } catch (NoSuchMethodException e) {
                    m = Minecraft.class.getDeclaredMethod(name, Screen.class);
                    m.setAccessible(true);
                }
                m.invoke(mc, screen);
                return;
            } catch (NoSuchMethodException ignored) {
                // coba nama berikutnya
            } catch (Exception e) {
                System.err.println("[RPG HUD] Gagal membuka layar: " + e);
                return;
            }
        }
        System.err.println("[RPG HUD] Method setScreen tidak ditemukan di versi ini.");
    }
}
