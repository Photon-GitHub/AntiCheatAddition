package de.photon.anticheataddition.modules.checks.targeting;

import de.photon.anticheataddition.modules.checks.targeting.TargetingAcquisitionAnalysis.TargetBox;
import de.photon.anticheataddition.util.mathematics.MathUtil;

/** Target-box geometry, kept separate from acquisition scoring and profile construction. */
final class TargetingGeometry
{
    private static final double RAY_EPSILON = 1E-9D;
    private TargetingGeometry() {}

    static TargetPoint closestPointForLook(final double eyeX,
                                           final double eyeY,
                                           final double eyeZ,
                                           final double yaw,
                                           final double pitch,
                                           final TargetBox targetBox)
    {
        final Direction direction = direction(yaw, pitch);
        final double intersectionDistance = rayIntersectionDistance(eyeX,
                                                                    eyeY,
                                                                    eyeZ,
                                                                    direction.x(),
                                                                    direction.y(),
                                                                    direction.z(),
                                                                    targetBox);
        if (Double.isFinite(intersectionDistance)) {
            return new TargetPoint(eyeX + direction.x() * intersectionDistance,
                                   eyeY + direction.y() * intersectionDistance,
                                   eyeZ + direction.z() * intersectionDistance);
        }

        // A ray missing a convex box is closest in angle to its silhouette, which lies on its edges.
        // Along each edge the cosine has at most one interior stationary point.
        TargetPoint closest = null;
        double smallestAngle = Double.POSITIVE_INFINITY;
        final double[] minimum = {targetBox.minimumX(), targetBox.minimumY(), targetBox.minimumZ()};
        final double[] maximum = {targetBox.maximumX(), targetBox.maximumY(), targetBox.maximumZ()};
        final double[] eye = {eyeX, eyeY, eyeZ};
        final double[] look = {direction.x(), direction.y(), direction.z()};
        for (int axis = 0; axis < 3; axis++) {
            final int other = (axis + 1) % 3;
            final int last = (axis + 2) % 3;
            for (int corner = 0; corner < 4; corner++) {
                final double[] point = minimum.clone();
                point[other] = (corner & 1) == 0 ? minimum[other] : maximum[other];
                point[last] = (corner & 2) == 0 ? minimum[last] : maximum[last];
                final double vx = point[0] - eye[0];
                final double vy = point[1] - eye[1];
                final double vz = point[2] - eye[2];
                final double projection = direction.x() * vx + direction.y() * vy + direction.z() * vz;
                final double squareLength = vx * vx + vy * vy + vz * vz;
                final double axisOffset = point[axis] - eye[axis];
                // For offset t along a unit axis, maximize (projection + look[axis] * t) / |point + t|.
                // Its derivative has a linear numerator; endpoints cover a zero denominator or an exterior root.
                final double denominator = look[axis] * axisOffset - projection;
                final double stationary = denominator == 0D ? 0D :
                        (projection * axisOffset - look[axis] * squareLength) / denominator;
                final double edgeLength = maximum[axis] - minimum[axis];
                for (double offset : new double[]{0D, edgeLength, Math.clamp(stationary, 0D, edgeLength)}) {
                    point[axis] = minimum[axis] + offset;
                    final TargetPoint candidate = new TargetPoint(point[0], point[1], point[2]);
                    final double angle = angularErrorToPoint(eyeX, eyeY, eyeZ, yaw, pitch, candidate);
                    if (angle < smallestAngle) {
                        smallestAngle = angle;
                        closest = candidate;
                    }
                }
            }
        }
        return closest;
    }

    static double angularErrorToPoint(final double eyeX,
                                      final double eyeY,
                                      final double eyeZ,
                                      final double yaw,
                                      final double pitch,
                                      final TargetPoint targetPoint)
    {
        final Direction direction = direction(yaw, pitch);
        final double offsetX = targetPoint.x() - eyeX;
        final double offsetY = targetPoint.y() - eyeY;
        final double offsetZ = targetPoint.z() - eyeZ;
        final double length = Math.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        if (length <= RAY_EPSILON) return 0D;
        final double dot = Math.clamp((direction.x() * offsetX +
                                       direction.y() * offsetY +
                                       direction.z() * offsetZ) / length,
                                      -1D,
                                      1D);
        return Math.toDegrees(Math.acos(dot));
    }

    private static Direction direction(final double yaw, final double pitch)
    {
        final double yawRadians = Math.toRadians(MathUtil.normalizeYaw(yaw));
        final double pitchRadians = Math.toRadians(pitch);
        final double cosPitch = Math.cos(pitchRadians);
        return new Direction(-cosPitch * Math.sin(yawRadians),
                             -Math.sin(pitchRadians),
                             cosPitch * Math.cos(yawRadians));
    }

    private static double rayIntersectionDistance(final double originX,
                                          final double originY,
                                          final double originZ,
                                          final double directionX,
                                          final double directionY,
                                          final double directionZ,
                                          final TargetBox box)
    {
        double minimumT = 0D;
        double maximumT = Double.POSITIVE_INFINITY;

        final double[] origin = {originX, originY, originZ};
        final double[] direction = {directionX, directionY, directionZ};
        final double[] minimum = {box.minimumX(), box.minimumY(), box.minimumZ()};
        final double[] maximum = {box.maximumX(), box.maximumY(), box.maximumZ()};

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(direction[axis]) <= RAY_EPSILON) {
                if (origin[axis] < minimum[axis] || origin[axis] > maximum[axis]) {
                    return Double.POSITIVE_INFINITY;
                }
                continue;
            }

            double first = (minimum[axis] - origin[axis]) / direction[axis];
            double second = (maximum[axis] - origin[axis]) / direction[axis];
            if (first > second) {
                final double temporary = first;
                first = second;
                second = temporary;
            }
            minimumT = Math.max(minimumT, first);
            maximumT = Math.min(maximumT, second);
            if (maximumT < minimumT) return Double.POSITIVE_INFINITY;
        }
        return maximumT >= 0D ? minimumT : Double.POSITIVE_INFINITY;
    }

    static double distanceToBoxCenter(final double x,
                                      final double y,
                                      final double z,
                                      final TargetBox targetBox)
    {
        final double deltaX = targetBox.centerX() - x;
        final double deltaY = targetBox.centerY() - y;
        final double deltaZ = targetBox.centerZ() - z;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    static double angularErrorToBox(final double eyeX, final double eyeY, final double eyeZ,
                                    final double yaw, final double pitch, final TargetBox box)
    {
        final TargetPoint closest = closestPointForLook(eyeX, eyeY, eyeZ, yaw, pitch, box);
        return closest == null ? Double.NaN : angularErrorToPoint(eyeX, eyeY, eyeZ, yaw, pitch, closest);
    }

    record TargetPoint(double x, double y, double z) {}
    private record Direction(double x, double y, double z) {}
}
