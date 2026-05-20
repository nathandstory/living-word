package com.livingword.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BibleHeldItemTransformTest {
    @Test
    void lookingDownRaisesBookTowardThePlayersView() {
        BibleHeldItemTransform forward = BibleHeldItemTransform.forView(0.0F, true);
        BibleHeldItemTransform lookingDown = BibleHeldItemTransform.forView(60.0F, true);

        assertTrue(lookingDown.verticalOffset() > forward.verticalOffset());
        assertTrue(lookingDown.xRotationDegrees() > forward.xRotationDegrees());
        assertTrue(Math.abs(lookingDown.sideOffset()) < Math.abs(forward.sideOffset()));
    }

    @Test
    void leftAndRightHandsMirrorSideRotation() {
        BibleHeldItemTransform right = BibleHeldItemTransform.forView(45.0F, true);
        BibleHeldItemTransform left = BibleHeldItemTransform.forView(45.0F, false);

        assertTrue(right.sideOffset() > 0.0F);
        assertTrue(left.sideOffset() < 0.0F);
        assertTrue(right.yRotationDegrees() > 0.0F);
        assertTrue(left.yRotationDegrees() < 0.0F);
    }

    @Test
    void twoHandedBookScaleStaysComfortablyInView() {
        assertTrue(BibleHeldItemTransform.twoHandedBookScale(0.0F) < 0.7F);
        assertTrue(BibleHeldItemTransform.twoHandedBookScale(1.0F) < 0.7F);
        assertTrue(BibleHeldItemTransform.twoHandedBookScale(1.0F) > BibleHeldItemTransform.twoHandedBookScale(0.0F));
    }

    @Test
    void twoHandedReadingPoseSitsLowEnoughForArmsToStayAttached() {
        assertTrue(BibleHeldItemTransform.twoHandedBaseVerticalOffset(0.0F, 0.0F) < -0.12F);
        assertTrue(BibleHeldItemTransform.twoHandedBaseVerticalOffset(0.0F, 1.0F) < -0.55F);
        assertTrue(BibleHeldItemTransform.twoHandedBaseVerticalOffset(1.0F, 0.0F)
            < BibleHeldItemTransform.twoHandedBaseVerticalOffset(0.0F, 0.0F));
    }

    @Test
    void twoHandedBibleHandsGripTheSmallerBookInsteadOfFloatingBelowIt() {
        BibleHeldItemTransform right = BibleHeldItemTransform.handGrip(true);
        BibleHeldItemTransform left = BibleHeldItemTransform.handGrip(false);

        assertTrue(right.sideOffset() > 0.12F && right.sideOffset() < 0.28F);
        assertTrue(left.sideOffset() < -0.12F && left.sideOffset() > -0.28F);
        assertTrue(right.verticalOffset() > -0.8F, "hands should be raised from vanilla map position for the smaller Bible model");
        assertTrue(left.verticalOffset() > -0.8F, "hands should be raised from vanilla map position for the smaller Bible model");
        assertTrue(right.depthOffset() > 0.25F && right.depthOffset() < 0.5F);
        assertTrue(left.depthOffset() > 0.25F && left.depthOffset() < 0.5F);
    }
}
