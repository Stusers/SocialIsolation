package com.example.socialisolation.client;

import com.example.socialisolation.util.ChunkRewardMath;

/**
 * Client-side cache of the local player's social meter, lifetime points, and OPAC config.
 * Updated whenever server packets are received.
 */
public class ClientSocialData {

    private static float socialMeter = 50.0f;
    private static float totalPointsRegained = 0f;
    private static int opacPointsPerChunk = 5000;
    private static int opacMaxChunks = 200;
    private static int thresholdThriving = 60;
    private static int thresholdLonely   = 40;
    private static int thresholdIsolated = 15;

    public static float getSocialMeter() { return socialMeter; }
    public static float getTotalPointsRegained() { return totalPointsRegained; }
    public static int getOpacPointsPerChunk() { return opacPointsPerChunk; }
    public static int getOpacMaxChunks() { return opacMaxChunks; }
    public static int getThresholdThriving() { return thresholdThriving; }
    public static int getThresholdLonely()   { return thresholdLonely; }
    public static int getThresholdIsolated() { return thresholdIsolated; }

    public static void setSocialMeter(float value) {
        socialMeter = Math.max(0f, Math.min(100f, value));
    }

    public static void setTotalPointsRegained(float value) {
        totalPointsRegained = Math.max(0f, value);
    }

    public static void setOpacConfig(int pointsPerChunk, int maxChunks,
                                     int thriving, int lonely, int isolated) {
        opacPointsPerChunk = pointsPerChunk;
        opacMaxChunks = maxChunks;
        thresholdThriving = thriving;
        thresholdLonely   = lonely;
        thresholdIsolated = isolated;
    }

    public static int chunksEarned() {
        return ChunkRewardMath.chunksEarned(totalPointsRegained, opacPointsPerChunk, opacMaxChunks);
    }

    public static float pointsSpentOnChunks(int chunks) {
        return ChunkRewardMath.pointsSpent(chunks, opacPointsPerChunk);
    }

    public static float pointsForNextChunk(int chunks) {
        return ChunkRewardMath.nextChunkCost(chunks, opacPointsPerChunk);
    }
}
