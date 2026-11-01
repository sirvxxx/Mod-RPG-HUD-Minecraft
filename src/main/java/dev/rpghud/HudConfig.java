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
    public static final int TYPE_CIRCLE = 0;
    public static final int TYPE_BAR = 1;
    private static HudConfig instance = new HudConfig();

    // Umum
    public boolean enabled = true;
    public boolean hideVanilla = true;
    /** id tema: hardcore, netherite, forest, celestial, cyber, classic (lihat HudTheme). */
    public String theme = HudTheme.DEFAULT_ID;
    /** 0 = Circle (melingkar), 1 = Bar (kotak konvensional). */
    public int layoutType = 0;
    public int x = 8;
    public int y = 8;
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
        instance.theme = HudTheme.byId(instance.theme).id;
        if (instance.layoutType != TYPE_BAR) instance.layoutType = TYPE_CIRCLE;
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
