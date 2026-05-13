package com.example.socialisolation.client;

import com.example.socialisolation.config.SocialConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Draws a polished Social Meter bar above the hunger bar in the player HUD.
 *
 * Features:
 *   - Rounded-corner bar with 1px border
 *   - Tier name label left of the bar
 *   - Percentage text inside the bar
 *   - Smooth colour interpolation between tiers
 *   - Subtle shadow/glow behind the bar
 *   - Only renders when player is alive and no GUI screen is open
 */
public class SocialHudRenderer implements LayeredDraw.Layer {

    private static final int BAR_WIDTH  = 90;
    private static final int BAR_HEIGHT = 8;
    private static final int OFFSET_ABOVE_HUNGER = 12;

    // Tier colours (ARGB)
    private static final int C_THRIVING = 0xFF4CAF50; // green
    private static final int C_NEUTRAL  = 0xFFFFC107; // amber
    private static final int C_LONELY   = 0xFFFF9800; // orange
    private static final int C_ISOLATED = 0xFFF44336; // red
    private static final int C_BG       = 0xCC1A1A1A; // dark translucent bg
    private static final int C_BORDER   = 0xFF444444; // subtle border
    private static final int C_TEXT     = 0xFFFFFFFF; // white text
    private static final int C_SHADOW   = 0x66000000; // shadow

    // For smooth interpolation between colours
    private float lastMeter = 50f;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        Player player = mc.player;
        if (!player.isAlive() || player.isSpectator()) return;

        float rawMeter = ClientSocialData.getSocialMeter();
        // Smooth interpolation for visual fill (lerp toward actual value)
        lastMeter = Mth.lerp(0.15f, lastMeter, rawMeter);
        float meter = lastMeter;

        int screenWidth  = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Position: right side, above hunger bar
        int barRight = screenWidth / 2 + 91; // vanilla hunger right edge
        int barLeft  = barRight - BAR_WIDTH;
        int barTop   = screenHeight - 49 - OFFSET_ABOVE_HUNGER - BAR_HEIGHT;

        int fillWidth = Math.round((meter / 100f) * (BAR_WIDTH - 2)); // -2 for inner padding
        int barColour = interpolateColour(meter);
        String tierName = getTierName(meter);
        String pctText = Math.round(meter) + "%";

        // ── Shadow / glow behind bar ──
        drawShadow(graphics, barLeft - 1, barTop - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2);

        // ── Background (rounded look via inset) ──
        graphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, C_BG);
        // Border
        graphics.fill(barLeft - 1, barTop - 1, barLeft + BAR_WIDTH + 1, barTop, C_BORDER); // top
        graphics.fill(barLeft - 1, barTop + BAR_HEIGHT, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT + 1, C_BORDER); // bottom
        graphics.fill(barLeft - 1, barTop, barLeft, barTop + BAR_HEIGHT, C_BORDER); // left
        graphics.fill(barLeft + BAR_WIDTH, barTop, barLeft + BAR_WIDTH + 1, barTop + BAR_HEIGHT, C_BORDER); // right

        // ── Fill ──
        if (fillWidth > 0) {
            int fillLeft = barLeft + 1;
            int fillTop  = barTop + 1;
            graphics.fill(fillLeft, fillTop, fillLeft + fillWidth, fillTop + BAR_HEIGHT - 2, barColour);
            // Highlight line at top of fill for "glossy" look
            graphics.fill(fillLeft, fillTop, fillLeft + fillWidth, fillTop + 1, lighten(barColour, 40));
        }

        // ── Tier label (left of bar) ──
        int labelX = barLeft - mc.font.width(tierName) - 4;
        int labelY = barTop + (BAR_HEIGHT - mc.font.lineHeight) / 2;
        graphics.drawString(mc.font, tierName, labelX, labelY, barColour, true);

        // ── Percentage text (centered in bar) ──
        int textX = barLeft + (BAR_WIDTH - mc.font.width(pctText)) / 2;
        int textY = barTop + (BAR_HEIGHT - mc.font.lineHeight) / 2;
        // Draw shadow behind text for readability
        graphics.drawString(mc.font, pctText, textX + 1, textY + 1, C_SHADOW, false);
        graphics.drawString(mc.font, pctText, textX, textY, C_TEXT, false);
    }

    // ── Helpers ──

    private static void drawShadow(GuiGraphics g, int x, int y, int w, int h) {
        // Soft shadow: 2px offset, dark translucent
        g.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x44000000);
    }

    private static String getTierName(float meter) {
        if (meter >= SocialConfig.THRESHOLD_THRIVING.get().floatValue())  return "Thriving";
        if (meter >= SocialConfig.THRESHOLD_LONELY.get().floatValue())    return "Neutral";
        if (meter >= SocialConfig.THRESHOLD_ISOLATED.get().floatValue())  return "Lonely";
        return "Isolated";
    }

    /** Linearly interpolate between the two nearest tier colours. */
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
