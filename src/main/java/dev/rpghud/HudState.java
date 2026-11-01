package dev.rpghud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Cache semua nilai turunan yang dipakai render. Isinya hanya diubah saat sumbernya berubah
 * (level naik, koordinat pindah blok, FPS berubah, ...) atau tiap 128 frame (bahasa / nama / skin).
 * Hasilnya render per-frame tidak membuat String, Component, atau objek baru.
 */
final class HudState {
    private HudState() {}

    static HudTheme T = HudTheme.byId(null);
    private static String themeRef;
    static boolean ready;
    private static int frame;

    // ---- teks + lebar yang sudah dihitung
    static String nameCircle = "", nameBar = "";
    static int nameCircleW, nameBarW;
    static String levelStr = "";
    static int levelW;
    static String coordsLabel = "XYZ ";
    static int coordsLabelW = 18;
    static String coordsVals = "0 0 0";
    static int coordsValsW = 30;
    static String fpsNum = "0";
    static int fpsNumW = 6;
    static String fpsLabel = " FPS";
    static int fpsLabelW = 24;
    static int fpsTextW = 42;                // lebar "000 FPS" (tetap, supaya panel tidak berubah-ubah)
    static String hpStr = "0/0";
    static int hpStrW = 18;
    static int coordsPanelW = 124, fpsPanelW = 72;
    static Identifier skin;

    // ---- statistik player (terkuantisasi, jadi kunci cache render)
    static int hpQ, gQ, food, satQ, arm, pulseQ;   // hp/ghost 0..320, sat 0..40, pulseQ 0 = tidak berdenyut
    static float hpFrac;

    private static String rawName = "";
    private static int lastLevel = -1, lcx = Integer.MIN_VALUE, lcy, lcz, lastFps = -1, lastHp = -1, lastMax = -1;

    static void theme(HudConfig c) {
        if (c.theme != themeRef) {          // perbandingan referensi: murah, string tema selalu literal
            T = HudTheme.byId(c.theme);
            themeRef = c.theme;
        }
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    static void update(Minecraft mc, LocalPlayer p, int fps, float ghostHp) {
        frame++;
        boolean refresh = !ready || (frame & 127) == 0;
        if (refresh) {
            rawName = p.getName().getString();
            nameCircle = HudDraw.fit(mc, rawName, CircleLayout.D - 16);
            nameCircleW = mc.font.width(nameCircle);
            nameBar = HudDraw.fit(mc, rawName, BarLayout.PANEL_W - 14 - 48);
            nameBarW = mc.font.width(nameBar);
            coordsLabel = tr("custom_rpg_hud.hud.coords") + " ";
            coordsLabelW = mc.font.width(coordsLabel);
            fpsLabel = " " + tr("custom_rpg_hud.hud.fps");
            fpsLabelW = mc.font.width(fpsLabel);
            fpsTextW = mc.font.width("000" + fpsLabel);
            skin = SkinHelper.bodyTexture(p);
            lastLevel = -1; lcx = Integer.MIN_VALUE; lastFps = -1; lastHp = -1;
        } else if ((frame & 15) == 0) {
            skin = SkinHelper.bodyTexture(p);       // skin bisa selesai di-download belakangan
        }

        int lv = p.experienceLevel;
        if (lv != lastLevel) {
            lastLevel = lv;
            levelStr = tr("custom_rpg_hud.hud.level", lv);
            levelW = mc.font.width(levelStr);
        }

        int bx = (int) Math.floor(p.getX()), by = (int) Math.floor(p.getY()), bz = (int) Math.floor(p.getZ());
        if (bx != lcx || by != lcy || bz != lcz) {
            lcx = bx; lcy = by; lcz = bz;
            coordsVals = bx + " " + by + " " + bz;
            coordsValsW = mc.font.width(coordsVals);
        }

        if (fps != lastFps) {
            lastFps = fps;
            fpsNum = Integer.toString(fps);
            fpsNumW = mc.font.width(fpsNum);
        }

        float max = Math.max(1f, p.getMaxHealth());
        float hp = p.getHealth();
        int hpI = (int) Math.ceil(hp), maxI = (int) Math.ceil(max);
        if (hpI != lastHp || maxI != lastMax) {
            lastHp = hpI; lastMax = maxI;
            hpStr = hpI + "/" + maxI;
            hpStrW = mc.font.width(hpStr);
        }

        // ---- statistik terkuantisasi
        hpFrac = HudDraw.clamp01(hp / max);
        hpQ = Math.round(hpFrac * 320f);
        gQ = Math.round(HudDraw.clamp01((ghostHp < 0f ? hp : ghostHp) / max) * 320f);
        food = p.getFoodData().getFoodLevel();
        float sat = Math.min(p.getFoodData().getSaturationLevel(), food);
        satQ = Math.min(40, (int) (sat * 2f));
        arm = Math.min(20, p.getArmorValue());
        pulseQ = hpFrac < 0.3f ? 1 + (int) ((Math.sin(System.currentTimeMillis() / 140.0) + 1.0) * 3.49) : 0;

        coordsPanelW = Math.max(110, coordsLabelW + coordsValsW + 7 + HudDraw.ICON + 3 + 8);
        fpsPanelW = Math.max(72, fpsTextW + 7 + HudDraw.ICON + 3 + 8);
        ready = true;
    }

    /** Kunci cache untuk bar status: berubah hanya jika tampilan bar benar-benar berubah. */
    static long statKey(boolean sH, boolean sF, boolean sA) {
        return (long) hpQ | ((long) gQ << 9) | ((long) food << 18) | ((long) satQ << 23)
                | ((long) arm << 29) | ((long) pulseQ << 34)
                | ((sH ? 1L : 0L) << 37) | ((sF ? 1L : 0L) << 38) | ((sA ? 1L : 0L) << 39)
                | ((long) T.index << 40);
    }

    /** Warna HP (berdenyut saat kritis); pulseQ sudah terkuantisasi jadi 7 langkah. */
    static int hpBase() {
        return pulseQ == 0 ? T.hp : HudDraw.mul(T.hp, 0.8f + 0.2f * ((pulseQ - 1) / 6f));
    }
}
