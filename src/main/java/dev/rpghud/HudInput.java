package dev.rpghud;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Status tombol kiri mouse dibaca langsung dari GLFW (lapisan yang sama yang dibaca Minecraft),
 * sehingga drag & drop tidak bergantung pada signature mouseClicked/mouseDragged/mouseReleased
 * yang berubah antar versi (parameter MouseButtonEvent di 26.x) dan tidak bergantung pada
 * MouseHandler.isLeftPressed yang tidak ikut diperbarui saat layar terbuka.
 */
final class HudInput {
    private HudInput() {}

    private static long handle;

    static boolean leftDown() {
        long h = handle();
        if (h != 0L) return GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        return Minecraft.getInstance().mouseHandler.isLeftPressed();
    }

    /** Handle jendela GLFW. Nama method Window berubah antar versi (getWindow / handle), jadi dicari lewat reflection sekali saja. */
    private static long handle() {
        if (handle != 0L) return handle;
        try {
            Object w = Minecraft.getInstance().getWindow();
            for (String name : new String[]{"handle", "getWindow"}) {
                try {
                    Object r = w.getClass().getMethod(name).invoke(w);
                    if (r instanceof Long l && l != 0L) { handle = l; break; }
                } catch (ReflectiveOperationException ignored) {
                    // coba nama berikutnya
                }
            }
        } catch (Throwable ignored) {
            // lanjut ke fallback
        }
        if (handle == 0L) handle = GLFW.glfwGetCurrentContext();
        return handle;
    }
}
