package com.syaru.mecrosscable;

public final class ChannelDisplay {
    private ChannelDisplay() { }

    public static int litSegments(int used, int capacity, int segments, boolean powered, boolean infinite) {
        if (!powered || used <= 0 || segments <= 0) return 0;
        if (infinite) return segments;
        if (capacity <= 0) return 0;
        return (int) Math.min(segments, Math.max(1L, (long) used * segments / capacity));
    }
}
