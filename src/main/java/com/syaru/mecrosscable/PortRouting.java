package com.syaru.mecrosscable;

import java.util.Arrays;

/** Port order is DOWN, UP, NORTH, SOUTH, WEST, EAST. Every lane has exactly two ends. */
public final class PortRouting {
    private static final int[] DEFAULT = {1,1,2,2,0,0};
    private final int[] lanes;
    public PortRouting() { this(DEFAULT); }
    public PortRouting(int[] lanes) {
        if (!valid(lanes)) throw new IllegalArgumentException("Routing requires three disjoint two-port lanes");
        this.lanes = lanes.clone();
    }
    public int lane(int port) { return lanes[port]; }
    public int[] ports() { return lanes.clone(); }
    public boolean custom() { return !Arrays.equals(lanes, DEFAULT); }
    public PortRouting connect(int anchor, int target) {
        if (anchor < 0 || anchor >= 6 || target < 0 || target >= 6) throw new IllegalArgumentException("Invalid port");
        if (lanes[anchor] == lanes[target]) return this;
        int other = -1;
        for (int i=0;i<6;i++) if (i != anchor && lanes[i] == lanes[anchor]) other = i;
        int[] next = ports();
        next[other] = lanes[target]; next[target] = lanes[anchor];
        return new PortRouting(next);
    }
    public static boolean valid(int[] lanes) {
        if (lanes.length != 6) return false;
        int[] counts = new int[3];
        for (int lane : lanes) { if (lane < 0 || lane >= 3) return false; counts[lane]++; }
        return counts[0] == 2 && counts[1] == 2 && counts[2] == 2;
    }
}
