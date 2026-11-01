package dev.rpghud;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Helper gambar bersama: panel 9-slice dari tekstur PNG, ikon, dan util warna. Tanpa alokasi. */
final class HudDraw {
    static final int ICON = 8;

    private HudDraw() {}

    /** Panel logam 9-slice dari frame.png (20x20, border 5px) + bayangan. */
    static void frame(GuiGraphicsExtractor g, HudTheme t, int x, int y, int w, int h) {
        g.fill(x + 1, y + 1, x + w + 3, y + h + 3, 0x22000000);
        g.fill(x + 2, y + 3, x + w + 2, y + h + 2, 0x44000000);
        slice(g, t.frame, 20, 20, 5, x, y, w, h);
    }

    /** Kapsul logam 9-slice dari badge.png (16x16, border 4px) + bayangan. */
    static void badge(GuiGraphicsExtractor g, HudTheme t, int x, int y, int w, int h) {
        g.fill(x + 1, y + 2, x + w + 1, y + h + 2, 0x55000000);
        slice(g, t.badge, 16, 16, 4, x, y, w, h);
    }

    static void icon(GuiGraphicsExtractor g, Identifier tex, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, ICON, ICON, ICON, ICON, ICON, ICON);
    }

    /** 9-slice: 4 sudut apa adanya, 4 sisi dan pusat diregangkan. tw/th = ukuran PNG, b = tebal border. */
    static void slice(GuiGraphicsExtractor g, Identifier tex, int tw, int th, int b,
                      int x, int y, int w, int h) {
        int cw = tw - 2 * b, ch = th - 2 * b;     // area tengah di PNG
        int mw = w - 2 * b, mh = h - 2 * b;       // area tengah di layar
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, b, b, b, b, tw, th);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x + w - b, y, (float) (tw - b), 0f, b, b, b, b, tw, th);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + h - b, 0f, (float) (th - b), b, b, b, b, tw, th);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x + w - b, y + h - b, (float) (tw - b), (float) (th - b), b, b, b, b, tw, th);
        if (mw > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + b, y, (float) b, 0f, mw, b, cw, b, tw, th);
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + b, y + h - b, (float) b, (float) (th - b), mw, b, cw, b, tw, th);
        }
        if (mh > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + b, 0f, (float) b, b, mh, b, ch, tw, th);
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + w - b, y + b, (float) (tw - b), (float) b, b, mh, b, ch, tw, th);
        }
        if (mw > 0 && mh > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x + b, y + b, (float) b, (float) b, mw, mh, cw, ch, tw, th);
        }
    }

    static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    static int mul(int c, float k) {
        int r = Math.min(255, (int) (((c >> 16) & 255) * k));
        int g = Math.min(255, (int) (((c >> 8) & 255) * k));
        int b = Math.min(255, (int) ((c & 255) * k));
        return (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Potong teks supaya muat (dipanggil jarang, bukan per frame). */
    static String fit(net.minecraft.client.Minecraft mc, String s, int maxW) {
        while (s.length() > 1 && mc.font.width(s) > maxW) s = s.substring(0, s.length() - 1);
        return s;
    }
}
