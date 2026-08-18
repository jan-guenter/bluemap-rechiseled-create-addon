/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import java.util.Map;
import java.util.Optional;

/** Frozen, content-free shaft and tool transforms for all 24 exact states. */
record MechanicalChiselRenderPlan(
        String shaftModel,
        AffineTransform shaftTransform,
        Optional<AffineTransform> toolTransform
) {

    static Optional<MechanicalChiselRenderPlan> select(Map<String, String> properties) {
        ChiselDirection facing = ChiselDirection.parse(properties.get("facing"))
                .orElse(null);
        Boolean alongFirst = flag(properties.get("axis_along_first"));
        Boolean flipped = flag(properties.get("flipped"));
        if (facing == null || alongFirst == null || flipped == null) {
            return Optional.empty();
        }

        String shaftModel;
        AffineTransform shaftTransform;
        if (facing.horizontal()) {
            shaftModel = "create:block/shaft_half";
            shaftTransform = partialFacing(facing.opposite());
        } else {
            shaftModel = "create:block/shaft";
            shaftTransform = shaft(axis(facing, alongFirst));
        }
        Optional<AffineTransform> tool = facing.axis() == ChiselDirection.Axis.Y
                ? Optional.of(tool(facing, alongFirst, flipped)) : Optional.empty();
        return Optional.of(new MechanicalChiselRenderPlan(
                shaftModel, shaftTransform, tool
        ));
    }

    private static AffineTransform partialFacing(ChiselDirection facing) {
        return AffineTransform.identity().centered()
                .rotateY(facing.horizontalAngle())
                .rotateX(facing.verticalAngle()).uncentered();
    }

    private static ChiselDirection.Axis axis(
            ChiselDirection facing,
            boolean alongFirst
    ) {
        return switch (facing.axis()) {
            case X -> alongFirst ? ChiselDirection.Axis.Y : ChiselDirection.Axis.Z;
            case Y -> alongFirst ? ChiselDirection.Axis.X : ChiselDirection.Axis.Z;
            case Z -> alongFirst ? ChiselDirection.Axis.X : ChiselDirection.Axis.Y;
        };
    }

    private static AffineTransform shaft(ChiselDirection.Axis axis) {
        return switch (axis) {
            case X -> AffineTransform.identity().centered().rotateZ(-90F).uncentered();
            case Y -> AffineTransform.identity();
            case Z -> AffineTransform.identity().centered().rotateX(90F).uncentered();
        };
    }

    private static AffineTransform tool(
            ChiselDirection facing,
            boolean alongFirst,
            boolean flipped
    ) {
        AffineTransform transform = AffineTransform.identity()
                .translate(0.5F, 0.5F, 0.5F);
        if (flipped) {
            transform = transform.rotateY(180F);
        }
        transform = transform.rotateY(alongFirst ? 180F : 90F);
        if (facing == ChiselDirection.DOWN) {
            transform = transform.rotateZ(180F);
        }
        return transform
                .translate(0.25F, 0.6F, 0F)
                .scale(0.5F, 0.5F, 0.5F)
                .translate(0.2F, -0.2F, 0F)
                .translate(-0.2F, 0.2F, 0F)
                // Vanilla item/generated FIXED transform, then ItemRenderer centering.
                .rotateY(180F)
                .translate(-0.5F, -0.5F, -0.5F);
    }

    private static Boolean flag(String value) {
        return switch (value == null ? "" : value) {
            case "true" -> true;
            case "false" -> false;
            default -> null;
        };
    }
}
