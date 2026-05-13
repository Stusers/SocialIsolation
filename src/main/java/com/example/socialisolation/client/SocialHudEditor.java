package com.example.socialisolation.client;

import com.example.socialisolation.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.network.chat.Component;

/**
 * Allows the player to drag the Social Meter HUD bar to any position
 * while in a GUI screen (chat, inventory, etc.).
 *
 * How to use:
 *   1. Open chat (press T)
 *   2. Click and drag the social meter bar with the left mouse button
 *   3. The new position is saved to client config (socialisolation-client.toml)
 *
 * The bar can also be resized by scrolling the mouse wheel while hovering over it.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class SocialHudEditor {

    private static boolean isDragging = false;
    private static double dragStartMouseX = 0;
    private static double dragStartMouseY = 0;
    private static int dragStartOffsetX = 0;
    private static int dragStartOffsetY = 0;
    private static int dragStartScalePercent = 100;

    private static final int BAR_WIDTH  = SocialHudRenderer.BAR_WIDTH;
    private static final int BAR_HEIGHT = SocialHudRenderer.BAR_HEIGHT;

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!isChatOrInventory(screen)) return;
        if (event.getButton() != 0) return; // left click only

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double mx = event.getMouseX();
        double my = event.getMouseY();

        // Check if mouse is over the bar
        if (isMouseOverBar(mc, mx, my)) {
            isDragging = true;
            dragStartMouseX = mx;
            dragStartMouseY = my;
            dragStartOffsetX = ClientConfig.HUD_OFFSET_X.get();
            dragStartOffsetY = ClientConfig.HUD_OFFSET_Y.get();
            dragStartScalePercent = (int) (ClientConfig.HUD_SCALE.get() * 100);
        }
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (!isDragging) return;
        if (!isChatOrInventory(event.getScreen())) return;

        double mx = event.getMouseX();
        double my = event.getMouseY();

        int deltaX = (int) Math.round(mx - dragStartMouseX);
        int deltaY = (int) Math.round(my - dragStartMouseY);

        ClientConfig.HUD_OFFSET_X.set(dragStartOffsetX + deltaX);
        ClientConfig.HUD_OFFSET_Y.set(dragStartOffsetY + deltaY);
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!isDragging) return;
        if (event.getButton() != 0) return;
        isDragging = false;
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        Screen screen = event.getScreen();
        if (!isChatOrInventory(screen)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double mx = event.getMouseX();
        double my = event.getMouseY();

        if (isMouseOverBar(mc, mx, my)) {
            double currentScale = ClientConfig.HUD_SCALE.get();
            double delta = event.getScrollDeltaY() > 0 ? 0.1 : -0.1;
            double newScale = Math.round((currentScale + delta) * 10.0) / 10.0;
            newScale = Math.max(0.5, Math.min(2.0, newScale));
            ClientConfig.HUD_SCALE.set(newScale);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        isDragging = false;
    }

    // ── Helpers ──

    private static boolean isChatOrInventory(Screen screen) {
        return screen instanceof ChatScreen;
    }

    /**
     * Checks if the given screen-space mouse coordinates are over the social meter bar.
     */
    public static boolean isMouseOverBar(Minecraft mc, double mx, double my) {
        int[] pos = SocialHudRenderer.computeBarPosition(mc);
        double scale = ClientConfig.HUD_SCALE.get();
        int w = (int) (BAR_WIDTH * scale);
        int h = (int) (BAR_HEIGHT * scale);

        // Include some padding and the label area for easier grabbing
        int labelPad = 40;
        int grabX = pos[0] - labelPad;
        int grabY = pos[1] - 2;
        int grabW = w + labelPad + 2;
        int grabH = h + 4;

        return mx >= grabX && mx < grabX + grabW && my >= grabY && my < grabY + grabH;
    }

    public static boolean isDragging() {
        return isDragging;
    }
}

