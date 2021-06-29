package de.photon.aacadditionpro.util.mathematics;

import de.photon.aacadditionpro.util.datastructure.ImmutablePair;
import lombok.experimental.UtilityClass;
import lombok.val;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides an implementation of the Bresenham algorithm
 *
 * @see <a href="https://en.wikipedia.org/wiki/Bresenham's_line_algorithm">Wikipedia</a>
 */
@UtilityClass
public final class Bresenham
{
    /**
     * Lists the coordinates of every point in the grid in between the two points (x0, y0) and (x1, y1)
     */
    public static List<ImmutablePair<Integer, Integer>> bresenham(int x0, int y0, int x1, int y1)
    {
        if (Math.abs(y1 - y0) < Math.abs(x1 - x0)) {
            if (x0 > x1) return bresenhamLow(x1, y1, x0, y0);
            else return bresenhamLow(x0, y0, x1, y1);
        } else {
            if (y0 > y1) return bresenhamHigh(x1, y1, x0, y0);
            else return bresenhamHigh(x0, y0, x1, y1);
        }
    }

    private static List<ImmutablePair<Integer, Integer>> bresenhamLow(int x1, int y1, int x0, int y0)
    {
        val list = new ArrayList<ImmutablePair<Integer, Integer>>();

        int dx = x1 - x0;
        int dy = y1 - y0;
        int yi = 1;

        if (dy < 0) {
            yi = -1;
            dy = -dy;
        }

        int d = (dy << 1) - dx;
        int y = y0;

        for (int x = x0; x <= x1; ++x) {
            list.add(ImmutablePair.of(x, y));

            if (d > 0) {
                y += yi;
                d += ((dy - dx) << 1);
            } else d += dy << 1;
        }
        return list;
    }

    private static List<ImmutablePair<Integer, Integer>> bresenhamHigh(int x1, int y1, int x0, int y0)
    {
        val list = new ArrayList<ImmutablePair<Integer, Integer>>();

        int dx = x1 - x0;
        int dy = y1 - y0;
        int xi = 1;

        if (dx < 0) {
            xi = -1;
            dx = -dx;
        }

        int d = (dx << 1) - dy;
        int x = x0;

        for (int y = y0; y <= y1; ++y) {
            list.add(ImmutablePair.of(x, y));

            if (d > 0) {
                x += xi;
                d += ((dx - dy) << 1);
            } else d += dx << 1;
        }
        return list;
    }
}
