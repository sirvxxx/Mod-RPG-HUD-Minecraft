package dev.rpghud;

/**
 * Lima komponen HUD independen. Tiap komponen punya posisi X/Y + visibilitas sendiri (lihat HudConfig.WidgetCfg).
 * Kelas ini menghitung ukuran, posisi layar, dan hitbox tiap komponen. Hanya aritmetika int: aman dipanggil per frame.
 */
final class HudWidget {
    private HudWidget() {}

    static final int NAME = HudConfig.W_NAME;
    static final int AVATAR = HudConfig.W_AVATAR;
    static final int LEVEL = HudConfig.W_LEVEL;
    static final int COORDS = HudConfig.W_COORDS;
    static final int FPS = HudConfig.W_FPS;
    static final int COUNT = HudConfig.W_COUNT;

    /** Kunci terjemahan nama komponen (untuk menu config dan layar edit). */
    static final String[] LANG_KEYS = {
            "custom_rpg_hud.widget.name", "custom_rpg_hud.widget.avatar", "custom_rpg_hud.widget.level",
            "custom_rpg_hud.widget.coords", "custom_rpg_hud.widget.fps"
    };

    private static final int ORIGIN = 8;          // pojok kiri-atas posisi bawaan

    /** Ukuran layar (piksel GUI), diisi HudRenderer / layar edit tiap frame. */
    static int screenW = 427, screenH = 240;

    // ------------------------------------------------------------------ logika

    /** Type Horizontal hanya memakai Avatar/Status (bar utama), Coords, dan FPS. */
    static boolean applies(int id, int type) {
        return type != HudConfig.TYPE_HORIZONTAL || (id != NAME && id != LEVEL);
    }

    static boolean visible(int id, HudConfig c) {
        return applies(id, c.layoutType) && c.widget(id).visible;
    }

    // ------------------------------------------------------------------ ukuran (belum diskalakan)

    static int width(int id, HudConfig c) {
        switch (id) {
            case COORDS: return HudState.coordsPanelW;
            case FPS: return HudState.fpsPanelW;
            default: break;
        }
        switch (c.layoutType) {
            case HudConfig.TYPE_BAR:
                return id == NAME ? BarLayout.NAME_W : (id == AVATAR ? BarLayout.avatarWidth(c) : BarLayout.levelWidth());
            case HudConfig.TYPE_HORIZONTAL:
                return HorizontalLayout.W;
            default:
                return id == NAME ? CircleLayout.D : (id == AVATAR ? CircleLayout.D : CircleLayout.levelWidth());
        }
    }

    static int height(int id, HudConfig c) {
        switch (id) {
            case COORDS:
            case FPS: return CircleLayout.INFO_H;
            default: break;
        }
        switch (c.layoutType) {
            case HudConfig.TYPE_BAR:
                return id == AVATAR ? BarLayout.AVATAR_H : BarLayout.CAPSULE_H;
            case HudConfig.TYPE_HORIZONTAL:
                return HorizontalLayout.H;
            default:
                return id == AVATAR ? CircleLayout.D : CircleLayout.CAPSULE_H;
        }
    }

    // ------------------------------------------------------------------ posisi bawaan (offset belum diskalakan dari ORIGIN)

    private static int defOffX(int id, HudConfig c) {
        switch (c.layoutType) {
            case HudConfig.TYPE_BAR:
                switch (id) {
                    case NAME: return BarLayout.PANEL_X;
                    case AVATAR: return 0;
                    case LEVEL: return (BarLayout.FACE - BarLayout.levelWidth()) / 2;
                    case COORDS: return 0;
                    default: return HudState.coordsPanelW + 4;
                }
            case HudConfig.TYPE_HORIZONTAL:
                return 0;
            default:
                switch (id) {
                    case NAME:
                    case AVATAR: return 0;
                    case LEVEL: return (CircleLayout.D - CircleLayout.levelWidth()) / 2;
                    default: return CircleLayout.D + 3;
                }
        }
    }

    private static int defOffY(int id, HudConfig c) {
        switch (c.layoutType) {
            case HudConfig.TYPE_BAR: {
                int top = BarLayout.CAPSULE_H + 4;                       // di bawah kapsul nama
                switch (id) {
                    case NAME: return 0;
                    case AVATAR: return top;
                    case LEVEL: return top + BarLayout.FACE_Y + BarLayout.FACE + 3;
                    default: return top + BarLayout.FACE_Y + BarLayout.FACE + 3 + CircleLayout.CAPSULE_H + 4;
                }
            }
            case HudConfig.TYPE_HORIZONTAL:
                return id == FPS ? CircleLayout.INFO_H + 4 : 0;
            default: {
                int top = CircleLayout.CAPSULE_H + 4;
                switch (id) {
                    case NAME: return 0;
                    case AVATAR: return top;
                    case LEVEL: return top + CircleLayout.D + 4;
                    case COORDS: return top + CircleLayout.D / 2 - 21;
                    default: return top + CircleLayout.D / 2 - 21 + CircleLayout.INFO_H + 4;
                }
            }
        }
    }

    // ------------------------------------------------------------------ posisi layar (sudah diskalakan)

    static int scaledW(int id, HudConfig c) { return Math.max(1, Math.round(width(id, c) * c.scale)); }
    static int scaledH(int id, HudConfig c) { return Math.max(1, Math.round(height(id, c) * c.scale)); }

    private static int clampX(int v, int id, HudConfig c) { return Math.max(0, Math.min(v, Math.max(0, screenW - scaledW(id, c)))); }
    private static int clampY(int v, int id, HudConfig c) { return Math.max(0, Math.min(v, Math.max(0, screenH - scaledH(id, c)))); }

    static int x(int id, HudConfig c) {
        HudConfig.WidgetCfg w = c.widget(id);
        if (w.x >= 0) return clampX(w.x, id, c);
        if (c.layoutType == HudConfig.TYPE_HORIZONTAL && id == AVATAR) return clampX((screenW - scaledW(id, c)) / 2, id, c);
        return clampX(ORIGIN + Math.round(defOffX(id, c) * c.scale), id, c);
    }

    static int y(int id, HudConfig c) {
        HudConfig.WidgetCfg w = c.widget(id);
        if (w.y >= 0) return clampY(w.y, id, c);
        // Type Horizontal: bawah-tengah, tepat di atas hotbar (hotbar setinggi 22px)
        if (c.layoutType == HudConfig.TYPE_HORIZONTAL && id == AVATAR) return clampY(screenH - 26 - scaledH(id, c), id, c);
        return clampY(ORIGIN + Math.round(defOffY(id, c) * c.scale), id, c);
    }

    // ------------------------------------------------------------------ hitbox

    /** Apakah kursor (piksel GUI) berada di dalam komponen ini? Hanya komponen ini yang diperiksa. */
    static boolean isHovered(int id, double mouseX, double mouseY) {
        HudConfig c = HudConfig.get();
        if (!visible(id, c)) return false;
        int x = x(id, c), y = y(id, c);
        return mouseX >= x && mouseX < x + scaledW(id, c) && mouseY >= y && mouseY < y + scaledH(id, c);
    }

    /** Komponen paling atas di bawah kursor (yang digambar paling akhir), atau -1. */
    static int topAt(double mouseX, double mouseY) {
        for (int id = COUNT - 1; id >= 0; id--) {
            if (isHovered(id, mouseX, mouseY)) return id;
        }
        return -1;
    }

    /** Simpan posisi baru satu komponen (dibatasi di dalam layar). Komponen lain tidak tersentuh. */
    static void setPos(int id, int x, int y) {
        HudConfig c = HudConfig.get();
        HudConfig.WidgetCfg w = c.widget(id);
        w.x = clampX(x, id, c);
        w.y = clampY(y, id, c);
    }
}
