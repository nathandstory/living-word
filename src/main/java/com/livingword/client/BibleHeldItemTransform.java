package com.livingword.client;

public record BibleHeldItemTransform(
    float sideOffset,
    float verticalOffset,
    float depthOffset,
    float xRotationDegrees,
    float yRotationDegrees,
    float zRotationDegrees,
    float scale
) {
    public static BibleHeldItemTransform forView(float viewPitchDegrees, boolean rightHand) {
        float side = rightHand ? 1.0F : -1.0F;
        float gaze = clamp((viewPitchDegrees - 8.0F) / 52.0F);

        return new BibleHeldItemTransform(
            side * lerp(0.56F, 0.18F, gaze),
            lerp(-0.56F, -0.18F, gaze),
            lerp(-0.76F, -0.96F, gaze),
            lerp(-18.0F, 42.0F, gaze),
            side * lerp(44.0F, 8.0F, gaze),
            side * lerp(-9.0F, -1.5F, gaze),
            lerp(0.92F, 1.18F, gaze)
        );
    }

    public static float twoHandedBookScale(float tilt) {
        return lerp(0.52F, 0.64F, clamp(tilt));
    }

    public static float twoHandedBaseVerticalOffset(float equipProgress, float tilt) {
        return -0.16F + equipProgress * -1.2F + clamp(tilt) * -0.45F;
    }

    public static BibleHeldItemTransform handGrip(boolean rightHand) {
        float side = rightHand ? 1.0F : -1.0F;
        return new BibleHeldItemTransform(
            side * 0.2F,
            -0.58F,
            0.36F,
            43.0F,
            92.0F,
            side * -36.0F,
            1.0F
        );
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    private static float clamp(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
