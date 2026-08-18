/* SPDX-License-Identifier: MIT */
package io.github.janguenter.bluemap.rechiseledcreate.adapter.bluemap522;

import java.util.Arrays;

/** Immutable row-major affine transform for stable installed-resource parts. */
final class AffineTransform {

    private static final int MATRIX_SIZE = 4;
    private final float[] matrix;

    private AffineTransform(float[] matrix) {
        this.matrix = matrix;
    }

    static AffineTransform identity() {
        return new AffineTransform(new float[]{
                1F, 0F, 0F, 0F,
                0F, 1F, 0F, 0F,
                0F, 0F, 1F, 0F,
                0F, 0F, 0F, 1F
        });
    }

    AffineTransform translate(float x, float y, float z) {
        return postMultiply(new float[]{
                1F, 0F, 0F, x,
                0F, 1F, 0F, y,
                0F, 0F, 1F, z,
                0F, 0F, 0F, 1F
        });
    }

    AffineTransform scale(float x, float y, float z) {
        return postMultiply(new float[]{
                x, 0F, 0F, 0F,
                0F, y, 0F, 0F,
                0F, 0F, z, 0F,
                0F, 0F, 0F, 1F
        });
    }

    AffineTransform rotateX(float degrees) {
        double radians = Math.toRadians(degrees);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        return postMultiply(new float[]{
                1F, 0F, 0F, 0F,
                0F, cosine, -sine, 0F,
                0F, sine, cosine, 0F,
                0F, 0F, 0F, 1F
        });
    }

    AffineTransform rotateY(float degrees) {
        double radians = Math.toRadians(degrees);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        return postMultiply(new float[]{
                cosine, 0F, sine, 0F,
                0F, 1F, 0F, 0F,
                -sine, 0F, cosine, 0F,
                0F, 0F, 0F, 1F
        });
    }

    AffineTransform rotateZ(float degrees) {
        double radians = Math.toRadians(degrees);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        return postMultiply(new float[]{
                cosine, -sine, 0F, 0F,
                sine, cosine, 0F, 0F,
                0F, 0F, 1F, 0F,
                0F, 0F, 0F, 1F
        });
    }

    AffineTransform centered() {
        return translate(0.5F, 0.5F, 0.5F);
    }

    AffineTransform uncentered() {
        return translate(-0.5F, -0.5F, -0.5F);
    }

    Point transform(float x, float y, float z) {
        return new Point(
                matrix[0] * x + matrix[1] * y + matrix[2] * z + matrix[3],
                matrix[4] * x + matrix[5] * y + matrix[6] * z + matrix[7],
                matrix[8] * x + matrix[9] * y + matrix[10] * z + matrix[11]
        );
    }

    boolean finite() {
        for (float value : matrix) {
            if (!Float.isFinite(value)) {
                return false;
            }
        }
        return true;
    }

    float[] copyValues() {
        return Arrays.copyOf(matrix, matrix.length);
    }

    private AffineTransform postMultiply(float[] right) {
        float[] result = new float[MATRIX_SIZE * MATRIX_SIZE];
        for (int row = 0; row < MATRIX_SIZE; row++) {
            for (int column = 0; column < MATRIX_SIZE; column++) {
                float value = 0F;
                for (int index = 0; index < MATRIX_SIZE; index++) {
                    value += matrix[row * MATRIX_SIZE + index]
                            * right[index * MATRIX_SIZE + column];
                }
                result[row * MATRIX_SIZE + column] = value;
            }
        }
        return new AffineTransform(result);
    }

    record Point(float x, float y, float z) {
    }
}
