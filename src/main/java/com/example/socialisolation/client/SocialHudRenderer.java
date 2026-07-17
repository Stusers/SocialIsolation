package com.example.socialisolation.client;

import com.example.socialisolation.config.ClientConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Draws the Social Meter bar and (when OPAC config is present) a land claim
 * progress bar below it showing XP-to-next-chunk style progress.
 *
 * Position and scale are controlled by ClientConfig (drag in chat to reposition).
 */
public class SocialHudRenderer implements LayeredDraw.Layer {

    static final int BAR_WIDTH  = 90;
    static final int BAR_HEIGHT = 8;
    private static final int CLAIM_BAR_HEIGHT = 5;
    private static final int CLAIM_BAR_GAP    = 3; // pixels between social bar and claim bar

    private static final int C_THRIVING  = 0xFF4CAF50;
    private static final int C_NEUTRAL   = 0xFFFFC107;
    private static final int C_LONELY    = 0xFFFF9800;
    private static final int C_ISOLATED  = 0xFFF44336;
    private static final int C_BG        = 0xCC1A1A1A;
    private static final int C_BORDER    = 0xFF444444;
    private static final int C_TEXT      = 0xFFFFFFFF;
    private static final int C_SHADOW    = 0x66000000;
    private static final int C_CLAIM     = 0xFF7B68EE; // medium slate blue for land bar
    private static final int C_CLAIM_MAX = 0xFFFFD700; // gold when at cap

    private float lastMeter = 50f;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!ClientConfig.HUD_ENABLED.get()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
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

        // Tier label
        if (scale >= 0.6) {
            int labelX = barLeft - mc.font.width(tierName) - 4;
            int labelY = barTop + (scaledH - mc.font.lineHeight) / 2;
            graphics.drawString(mc.font, tierName, labelX, labelY, barColour, true);
        }

        // Percentage text
        int textX = barLeft + (scaledW - mc.font.width(pctText)) / 2;
        int textY = barTop + (scaledH - mc.font.lineHeight) / 2;
        graphics.drawString(mc.font, pctText, textX + 1, textY + 1, C_SHADOW, false);
        graphics.drawString(mc.font, pctText, textX, textY, C_TEXT, false);

        // ── Land claim progress bar ───────────────────────────────────────────
        int pointsPerChunk = ClientSocialData.getOpacPointsPerChunk();
        if (pointsPerChunk > 0) {
            renderClaimBar(graphics, barLeft, barTop + scaledH + CLAIM_BAR_GAP, scaledW, scale);
        }
    }

    private static void renderClaimBar(GuiGraphics graphics, int left, int top, int width, double scale) {
        int chunks = ClientSocialData.chunksEarned();
        int maxChunks = ClientSocialData.getOpacMaxChunks();
        float totalPoints = ClientSocialData.getTotalPointsRegained();
        float spent = ClientSocialData.pointsSpentOnChunks(chunks);
        float nextCost = ClientSocialData.pointsForNextChunk(chunks);

        boolean atCap = chunks >= maxChunks;
        float progress = atCap ? 1f : Math.min((totalPoints - spent) / nextCost, 1f);

        int barH = (int) Math.max(3, CLAIM_BAR_HEIGHT * scale);
        int fillW = Math.round(progress * (width - 2));
        int colour = atCap ? C_CLAIM_MAX : C_CLAIM;

        // Background + border
        graphics.fill(left, top, left + width, top + barH, C_BG);
        graphics.fill(left - 1, top - 1, left + width + 1, top, C_BORDER);
        graphics.fill(left - 1, top + barH, left + width + 1, top + barH + 1, C_BORDER);
        graphics.fill(left - 1, top, left, top + barH, C_BORDER);
        graphics.fill(left + width, top, left + width + 1, top + barH, C_BORDER);

        // Fill
        if (fillW > 0) {
            graphics.fill(left + 1, top + 1, left + 1 + fillW, top + barH - 1, colour);
            graphics.fill(left + 1, top + 1, left + 1 + fillW, top + 2, lighten(colour, 40));
        }

    }

    static int[] computeBarPosition(Minecraft mc) {
        int screenWidth  = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int offsetX = ClientConfig.HUD_OFFSET_X.get();
        int offsetY = ClientConfig.HUD_OFFSET_Y.get();

        int defaultLeft = screenWidth / 2 + 91 - BAR_WIDTH;
        int defaultTop  = screenHeight - 49 - 12 - BAR_HEIGHT;

        return new int[] { defaultLeft + offsetX, defaultTop + offsetY };
    }

    private static String getTierName(float meter) {
        if (meter >= ClientSocialData.getThresholdThriving()) return "Thriving";
        if (meter >= ClientSocialData.getThresholdLonely())   return "Neutral";
        if (meter >= ClientSocialData.getThresholdIsolated()) return "Lonely";
        return "Isolated";
    }

    private static int interpolateColour(float meter) {
        float t = ClientSocialData.getThresholdThriving();
        float l = ClientSocialData.getThresholdLonely();
        float i = ClientSocialData.getThresholdIsolated();

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
