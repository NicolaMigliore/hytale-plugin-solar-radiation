package io.github.nicolamigliore.radiation;

import org.joml.Vector3dc;
import org.joml.Vector3d;
import com.hypixel.hytale.math.shape.Box;

/** Short, exact voxel traversal. Missing terrain is blocked by the caller. */
public final class TreatmentGeometry {
    @FunctionalInterface
    public interface OpenVoxel { boolean test(int x, int y, int z); }

    private TreatmentGeometry() {}

    public static Vector3d surfaceOrigin(Vector3dc impact, Vector3dc outwardNormal) {
        var origin = new Vector3d(impact);
        if (outwardNormal != null && outwardNormal.lengthSquared() > 1e-12) {
            origin.add(new Vector3d(outwardNormal).normalize(0.001));
        }
        return origin;
    }

    public static boolean canTreat(Vector3dc impact, Vector3dc rayOrigin, Vector3dc position,
                                   Box body, double radius, OpenVoxel open) {
        // Range still uses the entity position. Visibility aims inside the living body,
        // rather than at feet that can rest a tiny distance inside a solid floor.
        double distance = impact.distanceSquared(position);
        if (!Double.isFinite(distance) || distance > radius * radius) return false;
        var aim = new Vector3d(position);
        if (body != null) aim.add(body.middleX(), body.middleY(), body.middleZ());
        else aim.add(0, 0.5, 0);
        return canReach(rayOrigin, aim, Double.POSITIVE_INFINITY, open);
    }

    public static boolean canReach(Vector3dc from, Vector3dc to, double radius, OpenVoxel open) {
        double dx = to.x() - from.x(), dy = to.y() - from.y(), dz = to.z() - from.z();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (!Double.isFinite(distanceSquared) || distanceSquared > radius * radius) return false;
        int[] cell = {(int) Math.floor(from.x()), (int) Math.floor(from.y()), (int) Math.floor(from.z())};
        double[] start = {from.x(), from.y(), from.z()}, delta = {dx, dy, dz};
        int[] step = new int[3];
        double[] next = new double[3], increment = new double[3];
        for (int i = 0; i < 3; i++) {
            step[i] = (int) Math.signum(delta[i]);
            increment[i] = step[i] == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / delta[i]);
            next[i] = step[i] == 0 ? Double.POSITIVE_INFINITY
                    : ((step[i] > 0 ? cell[i] + 1 : cell[i]) - start[i]) / delta[i];
        }
        if (!open.test(cell[0], cell[1], cell[2])) return false;
        while (true) {
            double t = Math.min(next[0], Math.min(next[1], next[2]));
            if (t > 1 || !Double.isFinite(t)) return true;
            int axes = 0;
            for (int i = 0; i < 3; i++) if (Math.abs(next[i] - t) < 1e-10) axes |= 1 << i;
            // Check all touched neighbors at an edge/corner; do not leak through diagonal walls.
            for (int mask = 1; mask < 8; mask++) {
                if ((mask & axes) != mask) continue;
                if (!open.test(cell[0] + ((mask & 1) != 0 ? step[0] : 0),
                        cell[1] + ((mask & 2) != 0 ? step[1] : 0),
                        cell[2] + ((mask & 4) != 0 ? step[2] : 0))) return false;
            }
            for (int i = 0; i < 3; i++) if ((axes & (1 << i)) != 0) {
                cell[i] += step[i];
                next[i] += increment[i];
            }
        }
    }
}
