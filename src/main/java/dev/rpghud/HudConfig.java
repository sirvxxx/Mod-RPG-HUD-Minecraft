package dev.rpghud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Konfigurasi disimpan di .minecraft/config/custom_rpg_hud.json */
public class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("custom_rpg_hud.json");
    private static HudConfig instance = new HudConfig();

    // Umum
    public boolean enabled = true;
    public boolean hideVanilla = true;
    public int x = 4;
    public int y = 4;
    public float scale = 1.0f;

    // Elemen yang bisa dinyalakan / dimatikan
    public boolean showFace = true;
    public boolean showLevel = true;
    public boolean showName = true;
    public boolean showHealth = true;
    public boolean showHunger = true;
    public boolean showArmor = true;
    public boolean showCoords = true;
    public boolean showFps = true;

    public static HudConfig get() { return instance; }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                HudConfig c = GSON.fromJson(Files.readString(FILE), HudConfig.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            System.err.println("[RPG HUD] Gagal baca config: " + e);
        }
        instance.scale = Math.max(0.5f, Math.min(3f, instance.scale));
        instance.x = Math.max(0, instance.x);
        instance.y = Math.max(0, instance.y);
        save();
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(instance));
        } catch (Exception e) {
            System.err.println("[RPG HUD] Gagal simpan config: " + e);
        }
    }

    public static void reset() {
        instance = new HudConfig();
        save();
    }
}
