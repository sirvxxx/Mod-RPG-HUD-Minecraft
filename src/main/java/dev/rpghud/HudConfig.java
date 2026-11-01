package dev.rpghud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Konfigurasi disimpan di .minecraft/config/custom_rpg_hud.json
 *
 * Tiap komponen HUD (Name, Avatar/Status, Level, Coords, FPS) punya posisi X/Y dan visibilitas sendiri,
 * dan disimpan terpisah untuk tiap tipe layout. X/Y = AUTO (-1) berarti memakai posisi bawaan layout.
 */
public class HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("custom_rpg_hud.json");

    public static final int TYPE_CIRCLE = 0;
    public static final int TYPE_BAR = 1;
    public static final int TYPE_HORIZONTAL = 2;
    public static final int TYPE_COUNT = 3;

    /** Posisi belum pernah digeser: pakai posisi bawaan. */
    public static final int AUTO = -1;

    // id komponen
    public static final int W_NAME = 0;     // Component 1: Player Name Tag
    public static final int W_AVATAR = 1;   // Component 2: Avatar & Status Ring/Bar
    public static final int W_LEVEL = 2;    // Component 3: Level Badge
    public static final int W_COORDS = 3;   // Component 4: XYZ Coordinates
    public static final int W_FPS = 4;      // Component 5: FPS Counter
    public static final int W_COUNT = 5;

    /** Posisi (piksel GUI, pojok kiri-atas komponen) + visibilitas satu komponen. */
    public static final class WidgetCfg {
        public boolean visible = true;
        public int x = AUTO;
        public int y = AUTO;
    }

    private static HudConfig instance = new HudConfig();

    // Umum
    public boolean enabled = true;
    public boolean hideVanilla = true;
    /** id tema: hardcore, netherite, forest, celestial, cyber, classic (lihat HudTheme). */
    public String theme = HudTheme.DEFAULT_ID;
    /** 0 = Circle, 1 = Bar, 2 = Horizontal (menggantikan bar vanilla). */
    public int layoutType = TYPE_CIRCLE;
    public float scale = 1.0f;

    // Bagian dalam komponen Avatar & Status
    public boolean showFace = true;
    public boolean showHealth = true;
    public boolean showHunger = true;
    public boolean showArmor = true;

    // Komponen per tipe layout
    public WidgetCfg[] circle = newSet();
    public WidgetCfg[] bar = newSet();
    public WidgetCfg[] horizontal = newSet();

    private static WidgetCfg[] newSet() {
        WidgetCfg[] s = new WidgetCfg[W_COUNT];
        for (int i = 0; i < W_COUNT; i++) s[i] = new WidgetCfg();
        return s;
    }

    private static WidgetCfg[] fix(WidgetCfg[] in) {
        WidgetCfg[] out = newSet();
        if (in != null) {
            for (int i = 0; i < W_COUNT && i < in.length; i++) {
                if (in[i] == null) continue;
                out[i].visible = in[i].visible;
                out[i].x = Math.max(AUTO, in[i].x);
                out[i].y = Math.max(AUTO, in[i].y);
            }
        }
        return out;
    }

    /** Konfigurasi komponen untuk tipe layout yang sedang aktif. */
    public WidgetCfg widget(int id) {
        WidgetCfg[] set = layoutType == TYPE_BAR ? bar : (layoutType == TYPE_HORIZONTAL ? horizontal : circle);
        return set[id];
    }

    /** Kembalikan semua komponen ke posisi bawaan (visibilitas tidak diubah). */
    public void resetPositions() {
        for (WidgetCfg[] set : new WidgetCfg[][]{circle, bar, horizontal}) {
            for (WidgetCfg w : set) { w.x = AUTO; w.y = AUTO; }
        }
    }

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
        if (instance.layoutType < 0 || instance.layoutType >= TYPE_COUNT) instance.layoutType = TYPE_CIRCLE;
        instance.scale = Math.max(0.5f, Math.min(3f, instance.scale));
        instance.circle = fix(instance.circle);
        instance.bar = fix(instance.bar);
        instance.horizontal = fix(instance.horizontal);
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
