package dev.rpghud;

import net.minecraft.resources.Identifier;

/**
 * Tema warna HUD. Bingkai, kapsul, lingkaran, dan ikon dibaca dari tekstur PNG:
 *   assets/custom_rpg_hud/textures/gui/<id>/{frame,badge,ring,icon_coords,icon_fps}.png
 * Warna teks dan bar (dihitung lewat kode) diatur di sini. Preset ini dihasilkan oleh
 * tools/gen_assets.py --java supaya selalu sinkron dengan PNG.
 */
public final class HudTheme {
    public static final String DEFAULT_ID = "netherite";

    public final String id;
    public final int text, level, hp, food, armor, ghost, sat;
    /** warna kotak kosong: [0] sisi dalam, [1] tengah, [2] sisi luar, [3] garis tepi. */
    public final int[] empty;
    /** warna alas cincin (dipakai menutup track sektor yang dimatikan). */
    public final int bandBack;
    public final Identifier frame, badge, ring, iconCoords, iconFps, iconHeart, iconFood, iconArmor,
            hTube, hDiamond, hSlotOn, hSlotOff;
    /** posisi di ALL (dipakai untuk kunci cache, 0..7). */
    public int index;

    private HudTheme(String id, int text, int level, int hp, int food, int armor, int ghost, int sat,
                     int e0, int e1, int e2, int e3, int bandBack) {
        this.id = id;
        this.text = text;
        this.level = level;
        this.hp = hp;
        this.food = food;
        this.armor = armor;
        this.ghost = ghost;
        this.sat = sat;
        this.empty = new int[]{e0, e1, e2, e3};
        this.bandBack = bandBack;
        this.frame = tex(id, "frame");
        this.badge = tex(id, "badge");
        this.ring = tex(id, "ring");
        this.iconCoords = tex(id, "icon_coords");
        this.iconFps = tex(id, "icon_fps");
        this.iconHeart = tex(id, "icon_heart");
        this.iconFood = tex(id, "icon_food");
        this.iconArmor = tex(id, "icon_armor");
        this.hTube = tex(id, "h_tube");
        this.hDiamond = tex(id, "h_diamond");
        this.hSlotOn = tex(id, "h_slot_on");
        this.hSlotOff = tex(id, "h_slot_off");
    }

    private static Identifier tex(String id, String name) {
        return Identifier.fromNamespaceAndPath(RpgHudClient.MOD_ID, "textures/gui/" + id + "/" + name + ".png");
    }

    public static final HudTheme[] ALL = {
            new HudTheme("hardcore", 0xFFFF5A4F, 0xFFFF7B6B, 0xFFC81414, 0xFFE8801A, 0xFFB4B6BC, 0xFFE8C8C0, 0xFFFFD9A0, 0xFF1B1112, 0xFF2A1C1D, 0xFF382627, 0xFF0A0506, 0xFF060405),
            new HudTheme("netherite", 0xFFFFD36B, 0xFF9CFF7A, 0xFFE02A2A, 0xFFFF9F1C, 0xFFC4CAD2, 0xFFEBDDB8, 0xFFFFF2A0, 0xFF1E1511, 0xFF2C211A, 0xFF3A2D24, 0xFF0B0705, 0xFF07050B),
            new HudTheme("forest", 0xFF7DFFA0, 0xFFB8FF6A, 0xFFD8322E, 0xFFE8A22A, 0xFFB0BFB8, 0xFFE6E0B8, 0xFFF4F08A, 0xFF14201A, 0xFF1F2E25, 0xFF2B3D31, 0xFF070D09, 0xFF050B07),
            new HudTheme("celestial", 0xFF9CC8FF, 0xFFC8E0FF, 0xFFE0455A, 0xFFFFB347, 0xFFD6DEF2, 0xFFE8E8FF, 0xFFFFF0B0, 0xFF131828, 0xFF1D243A, 0xFF293250, 0xFF080A12, 0xFF060810),
            new HudTheme("cyber", 0xFF5CF2FF, 0xFF7DFFE0, 0xFFFF3860, 0xFFFFC400, 0xFF9DB2C4, 0xFFD0F0F5, 0xFFFFFFA0, 0xFF0E1A20, 0xFF16262E, 0xFF203541, 0xFF050C10, 0xFF03070A),
            new HudTheme("classic", 0xFFF0C878, 0xFFB8E086, 0xFFC0362C, 0xFFE08A2A, 0xFFB8B0A0, 0xFFEBDDB8, 0xFFF2DC8A, 0xFF261A12, 0xFF35271C, 0xFF443426, 0xFF100A06, 0xFF0D0805),
    };

    static {
        for (int i = 0; i < ALL.length; i++) ALL[i].index = i;
    }

    public static HudTheme byId(String id) {
        if (id != null) {
            for (HudTheme t : ALL) if (t.id.equals(id)) return t;
        }
        for (HudTheme t : ALL) if (t.id.equals(DEFAULT_ID)) return t;
        return ALL[0];
    }

    public static HudTheme next(HudTheme current) {
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i] == current) return ALL[(i + 1) % ALL.length];
        }
        return ALL[0];
    }

    public String translationKey() { return "custom_rpg_hud.theme." + id; }
}
