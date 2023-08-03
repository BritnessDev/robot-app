package com.revolve44.serviceaccess1;

import android.graphics.Point;

public class KeyboardPoints {
    private String key;
    private Point pos;

    public static KeyboardPoints[] keyPoints = new KeyboardPoints[] {
            new KeyboardPoints("a", new Point(109, 1890)),
            new KeyboardPoints("b", new Point(649, 2038)),
            new KeyboardPoints("c", new Point(433, 2038)),
            new KeyboardPoints("d", new Point(325, 2038)),
            new KeyboardPoints("e", new Point(271, 1742)),
            new KeyboardPoints("f", new Point(433, 1890)),
            new KeyboardPoints("g", new Point(541, 1890)),
            new KeyboardPoints("h", new Point(649, 1890)),
            new KeyboardPoints("i", new Point(811, 1742)),
            new KeyboardPoints("j", new Point(757, 1890)),
            new KeyboardPoints("k", new Point(865, 1890)),
            new KeyboardPoints("l", new Point(973, 1890)),
            new KeyboardPoints("m", new Point(865, 1890)),
            new KeyboardPoints("n", new Point(757, 2038)),
            new KeyboardPoints("o", new Point(919, 1742)),
            new KeyboardPoints("p", new Point(1029,1742)),
            new KeyboardPoints("q", new Point(55, 1742)),
            new KeyboardPoints("r", new Point(379, 1742)),
            new KeyboardPoints("s", new Point(217, 1890)),
            new KeyboardPoints("t", new Point(487, 1742)),
            new KeyboardPoints("u", new Point(703, 1742)),
            new KeyboardPoints("v", new Point(541, 2038)),
            new KeyboardPoints("w", new Point(163, 1742)),
            new KeyboardPoints("x", new Point(325, 2038)),
            new KeyboardPoints("y", new Point(595, 1742)),
            new KeyboardPoints("z", new Point(217, 2038)),
    };

    public KeyboardPoints(String key, Point pos) {
        this.key = key;
        this.pos = pos;
    }

    public static Point keyPointsByKey(String key) {
        for(int i = 0 ; i < keyPoints.length; i ++) {
            if(keyPoints[i].key.equals(key))
                return keyPoints[i].pos;
        }

        return new Point(0,0);
    }

}
