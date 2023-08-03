package com.revolve44.serviceaccess1;

import android.graphics.Point;

public class AutoDataModel {
    public Point point = new Point();
    public int delay = 5000;

    public AutoDataModel(Point point, int delay) {
        this.point = point;
        this.delay = delay;
    }

    public AutoDataModel(Point point) {
        this.point = point;
    }
}