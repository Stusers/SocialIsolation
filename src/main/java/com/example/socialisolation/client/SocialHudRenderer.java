package com.example.socialisolation.client;

import com.example.socialisolation.config.SocialConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/**
 * Draws a Social Meter bar above the hunger bar in the player HUD.
 *
 * Layout (mirrors the vanilla hunger bar position):
 *   - The hunger bar sits at screenHeight - 49 (top of the 10 food icons).
 *   - We draw our bar 10 px above that, so at screenHeight - 59.
 *   - Width matches the vanilla hunger bar: 81 px, right-aligned like hunger.
 *
 * Colours by tier:
 *   THRIVING  (>=70) → green  #55FF55
 *   NEUTRAL   (>=40) → yellow #FFFF55
 *   LONELY    (>=15) → orange #FF9900
 *   ISOLATED  (<15)  → red    #FF3333
 */
public class SocialHudRenderer implements LayeredDraw.Layer {

    // Bar dimensions
    private static final int BAR_WIDTH  = 81;
    private static final int BAR_HEIGHT = 5;
    /** Pixels above the top of the hunger icons row */
    private static final int OFFSET_ABOVE_HUNGER = 10;

    // Colours (ARGB)
    private static final int COLOUR_THRIVING  = 0xFF55FF55;
    private static final int COLOUR_NEUTRAL   = 0xFFFFFF55;
    private static final int COLOUR_LONELY    = 0xFFFF9900;
    private static final int COLOUR_ISOLATED  = 0xFFFF3333;
    private static final int COLOUR_BG        = 0xFF222222;
    private static final int COLOUR_BORDER    = 0xFF000000;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        // Only render when a player is in-game and not in a GUI screen
        if (mc.player == null || mc.screen != null) return;
        Player player = mc.player;

        // Don't render if dead or in spectator
        if (!player.isAlive() || player.isSpectator()) return;

        float meter = ClientSocialData.getSocialMeter();

        int screenWidth  = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Vanilla hunger bar: right-aligned, starts at screenWidth/2 + 10 (91 px wide, 10 icons × ~9px + 1)
        // Match vanilla exactly: hunger bar left edge = screenWidth/2 + 10
        int barLeft = screenWidth / 2 + 10;
        // Hunger bar top is at screenHeight - 49; we sit OFFSET_ABOVE_HUNGER px above it
        int barTop  = screenHeight - 49 - OFFSET_ABOVE_HUNGER - BAR_HEIGHT;

        int fillWidth = Math.round((meter / 100f) * BAR_WIDTH);
        int barColour = getColour(meter);

        // Border (1px inset)
        graphics.fill(barLeft - 1, barTop - 1, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT + 1, COLOUR_BORDER);
        // Background
        graphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, COLOUR_BG);
        // Fill
        if (fillWidth > 0) {
            graphics.fill(barLeft, barTop, barLeft + fillWidth, barTop + BAR_HEIGHT, barColour);
        }
    }

    private static int getColour(float meter) {
        float thriving  = SocialConfig.THRESHOLD_THRIVING.get().floatValue();
        float lonely    = SocialConfig.THRESHOLD_LONELY.get().floatValue();
        float isolated  = SocialConfig.THRESHOLD_ISOLATED.get().floatValue();

        if (meter >= thriving) return COLOUR_THRIVING;
        if (meter >= lonely)   return COLOUR_NEUTRAL;
        if (meter >= isolated) return COLOUR_LONELY;
        return COLOUR_ISOLATED;
    }
}

