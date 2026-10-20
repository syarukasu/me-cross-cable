package com.syaru.mecrosscable;

/** Visual color inheritance only; it must never change a node's actual grid color. */
public final class CableAppearance {
    private CableAppearance() { }

    public static int lineColor(int configured, int first, int second, int neutral) {
        if (configured != neutral) return configured;
        if (first == neutral) return second;
        if (second == neutral || first == second) return first;
        return neutral;
    }
}
