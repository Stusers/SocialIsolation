package com.example.socialisolation.client;

/**
 * Client-side cache of the local player's social meter.
 * Updated whenever a SocialMeterPayload packet is received from the server.
 * Only accessed on the client thread.
 */
public class ClientSocialData {

    private static float socialMeter = 50.0f;

    public static float getSocialMeter() {
        return socialMeter;
    }

    public static void setSocialMeter(float value) {
        socialMeter = Math.max(0f, Math.min(100f, value));
    }
}

