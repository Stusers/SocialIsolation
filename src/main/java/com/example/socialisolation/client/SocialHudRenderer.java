package com.example.socialisolation.client;

import com.example.socialisolation.config.ClientConfig;
import com.example.socialisolation.config.SocialConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Draws a polished Social Meter bar in the player HUD.
 *
 * Position and scale are controlled by ClientConfig (editable via drag-and-drop in chat).
 * Default position is above the hunger bar on the right side.
 */
public class SocialHudRenderer implements LayeredDraw.Layer {

    static final int BAR_WIDTH  = 90;
    static final int BAR_HEIGHT = 8;

    private static final int C_THRIVING = 0xFF4CAF50; // green
    private static final int C_NEUTRAL  = 0xFFFFC107; // amber
    private static final int C_LONELY   = 0xFFFF9800; // orange
    private static final int C_ISOLATED = 0xFFF44336; // red
    private static final int C_BG       = 0xCC1A1A1A; // dark translucent bg
    private static final int C_BORDER   = 0xFF444444; // subtle border
    private static final int C_TEXT     = 0xFFFFFFFF; // white text
    private static final int C_SHADOW   = 0x66000000; // shadow

    private float lastMeter = 50f;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!ClientConfig.HUD_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        Player player = mc.player;
        if (!player.isAlive() || player.isSpectator()) return;

        float rawMeter = ClientSocialData.getSocialMeter();
        lastMeter = Mth.lerp(0.15f, lastMeter, rawMeter);
        float meter = lastMeter;

        int[] pos = computeBarPosition(mc);
        int barLeft = pos[0];
        int barTop  = pos[1];
        double scale = ClientConfig.HUD_SCALE.get();

        int scaledW = (int) (BAR_WIDTH * scale);
        int scaledH = (int) (BAR_HEIGHT * scale);
        int fillWidth = Math.round((meter / 100f) * (scaledW - 2));
        int barColour = interpolateColour(meter);
        String tierName = getTierName(meter);
        String pctText = Math.round(meter) + "%";

        // If dragging in chat, draw a highlight border around the bar to show it's editable
        if (SocialHudEditor.isDragging()) {
            graphics.fill(barLeft - 2, barTop - 2, barLeft + scaledW + 2, barTop + scaledH + 2, 0x88FFFFFF);
        }

        // Shadow
        graphics.fill(barLeft + 2, barTop + 2, barLeft + scaledW + 2, barTop + scaledH + 2, 0x44000000);

        // Background + border
        graphics.fill(barLeft, barTop, barLeft + scaledW, barTop + scaledH, C_BG);
        graphics.fill(barLeft - 1, barTop - 1, barLeft + scaledW + 1, barTop, C_BORDER);
        graphics.fill(barLeft - 1, barTop + scaledH, barLeft + scaledW + 1, barTop + scaledH + 1, C_BORDER);
        graphics.fill(barLeft - 1, barTop, barLeft, barTop + scaledH, C_BORDER);
        graphics.fill(barLeft + scaledW, barTop, barLeft + scaledW + 1, barTop + scaledH, C_BORDER);

        // Fill
        if (fillWidth > 0) {
            int fillLeft = barLeft + 1;
            int fillTop  = barTop + 1;
            graphics.fill(fillLeft, fillTop, fillLeft + fillWidth, fillTop + scaledH - 2, barColour);
            graphics.fill(fillLeft, fillTop, fillLeft + fillWidth, fillTop + 1, lighten(barColour, 40));
        }

        // Label
        if (scale >= 0.6) {
            int labelX = barLeft - mc.font.width(tierName) - 4;
            int labelY = barTop + (scaledH - mc.font.lineHeight) / 2;
            graphics.drawString(mc.font, tierName, labelX, labelY, barColour, true);
        }

        // Percentage
        int textX = barLeft + (scaledW - mc.font.width(pctText)) / 2;
        int textY = barTop + (scaledH - mc.font.lineHeight) / 2;
        graphics.drawString(mc.font, pctText, textX + 1, textY + 1, C_SHADOW, false);
        graphics.drawString(mc.font, pctText, textX, textY, C_TEXT, false);
    }

    /**
     * Computes the bar position based on config offsets relative to the default position.
     * Returns [barLeft, barTop].
     */
    static int[] computeBarPosition(Minecraft mc) {
        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int offsetX = ClientConfig.HUD_OFFSET_X.get();
        int offsetY = ClientConfig.HUD_OFFSET_Y.get();

        // Default position: right side, above hunger bar
        int defaultLeft = screenWidth / 2 + 91 - BAR_WIDTH;
        int defaultTop  = screenHeight - 49 - 12 - BAR_HEIGHT;

        return new int[] { defaultLeft + offsetX, defaultTop + offsetY };
    }

    private static String getTierName(float meter) {
        if (meter >= SocialConfig.THRESHOLD_THRIVING.get().floatValue())  return "Thriving";
        if (meter >= SocialConfig.THRESHOLD_LONELY.get().floatValue())    return "Neutral";
        if (meter >= SocialConfig.THRESHOLD_ISOLATED.get().floatValue())  return "Lonely";
        return "Isolated";
    }

    private static int interpolateColour(float meter) {
        float t = SocialConfig.THRESHOLD_THRIVING.get().floatValue();
        float l = SocialConfig.THRESHOLD_LONELY.get().floatValue();
        float i = SocialConfig.THRESHOLD_ISOLATED.get().floatValue();

        if (meter >= t) return C_THRIVING;
        if (meter >= l) return lerpColour(meter, l, t, C_LONELY, C_NEUTRAL);
        if (meter >= i) return lerpColour(meter, i, l, C_ISOLATED, C_LONELY);
        return C_ISOLATED;
    }

    private static int lerpColour(float value, float min, float max, int cMin, int cMax) {
        float t = Mth.clamp((value - min) / (max - min), 0f, 1f);
        return blend(cMin, cMax, t);
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) Mth.lerp(t, ar, br);
        int g = (int) Mth.lerp(t, ag, bg);
        int bl = (int) Mth.lerp(t, ab, bb);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static int lighten(int colour, int amount) {
        int r = Math.min(255, ((colour >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((colour >> 8) & 0xFF) + amount);
        int b = Math.min(255, (colour & 0xFF) + amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
