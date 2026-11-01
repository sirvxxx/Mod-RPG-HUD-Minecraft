package dev.rpghud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Component 4 (XYZ) dan Component 5 (FPS): panel logam yang sama untuk semua tipe layout. Titik asal = pojok kiri-atas panel. */
final class InfoPanels {
    private InfoPanels() {}

    static final int H = 19;

    static void drawCoords(GuiGraphicsExtractor g, Minecraft mc) {
        final HudTheme t = HudState.T;
        HudDraw.frame(g, t, 0, 0, HudState.coordsPanelW, H);
        HudDraw.icon(g, t.iconCoords, 7, 5);
        int tx = 7 + HudDraw.ICON + 3;
        g.text(mc.font, HudState.coordsLabel, tx, 6, t.text, true);
        g.text(mc.font, HudState.coordsVals, tx + HudState.coordsLabelW, 6, 0xFFFFFFFF, true);
    }

    static void drawFps(GuiGraphicsExtractor g, Minecraft mc) {
        final HudTheme t = HudState.T;
        HudDraw.frame(g, t, 0, 0, HudState.fpsPanelW, H);
        HudDraw.icon(g, t.iconFps, 7, 5);
        int tx = 7 + HudDraw.ICON + 3;
        g.text(mc.font, HudState.fpsNum, tx, 6, HudRenderer.fpsColor(), true);
        g.text(mc.font, HudState.fpsLabel, tx + HudState.fpsNumW, 6, t.text, true);
    }
}
