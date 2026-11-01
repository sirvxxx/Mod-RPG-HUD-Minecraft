package dev.rpghud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class HudRenderer {
    private static final Identifier HEART  = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/full.png");
    private static final Identifier FOOD   = Identifier.withDefaultNamespace("textures/gui/sprites/hud/food_full.png");
    private static final Identifier ARMOR  = Identifier.withDefaultNamespace("textures/gui/sprites/hud/armor_full.png");

    private HudRenderer() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        HudConfig c = HudConfig.get();
        if (p == null || !c.enabled) return;

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) c.x, (float) c.y);
        pose.scale(c.scale, c.scale);

        // ---------- Frame + wajah skin (dinamis dari skin player lokal) ----------
        g.fill(0, 0, 36, 36, 0xFF6B4A2B);          // border cokelat
        g.fill(2, 2, 34, 34, 0xFF1B1B1B);          // dasar gelap
        Identifier skin = SkinHelper.bodyTexture(p);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, 4, 4, 8f, 8f, 28, 28, 8, 8, 64, 64);   // wajah
        g.blit(RenderPipelines.GUI_TEXTURED, skin, 4, 4, 40f, 8f, 28, 28, 8, 8, 64, 64);  // lapisan hat

        // ---------- Nama + angka HP ----------
        String name = p.getName().getString();
        float hp = p.getHealth();
        float max = Math.max(1f, p.getMaxHealth());
        g.fill(40, 0, 116, 12, 0xFF6B4A2B);
        g.fill(41, 1, 115, 11, 0xFF3A2A1A);
        g.text(mc.font, name, 44, 2, 0xFFFFD04A, true);
        g.fill(116, 0, 152, 12, 0xFF6B4A2B);
        g.fill(117, 1, 151, 11, 0xFF2A2A2A);
        g.text(mc.font, (int) Math.ceil(hp) + "/" + (int) Math.ceil(max), 120, 2, 0xFFFFFFFF, true);

        // ---------- Health ----------
        g.blit(RenderPipelines.GUI_TEXTURED, HEART, 40, 15, 0f, 0f, 9, 9, 9, 9);
        bar(g, 52, 16, 100, 8, hp / max, 0xFFD32F2F);

        // ---------- Hunger ----------
        int food = p.getFoodData().getFoodLevel();
        g.blit(RenderPipelines.GUI_TEXTURED, FOOD, 40, 27, 0f, 0f, 9, 9, 9, 9);
        bar(g, 52, 28, 52, 8, food / 20f, 0xFFF0A030);

        // ---------- Armor ----------
        int armor = p.getArmorValue();
        g.blit(RenderPipelines.GUI_TEXTURED, ARMOR, 107, 27, 0f, 0f, 9, 9, 9, 9);
        bar(g, 119, 28, 33, 8, armor / 20f, 0xFF9AA5B1);

        pose.popMatrix();
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float frac, int color) {
        frac = Math.max(0f, Math.min(1f, frac));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF6B4A2B);
        g.fill(x, y, x + w, y + h, 0xFF202020);
        int fw = Math.round(w * frac);
        if (fw > 0) {
            g.fill(x, y, x + fw, y + h, color);
            g.fill(x, y, x + fw, y + 2, 0x55FFFFFF); // highlight tipis
        }
    }
}
