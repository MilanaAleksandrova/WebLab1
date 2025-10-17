package dev.lab1;

import java.math.BigDecimal;

class Point {
    BigDecimal x, y, r;
    String currentTime;
    long executionTime;
    boolean hit;

    Point(BigDecimal x, BigDecimal y, BigDecimal r, String currentTime, long executionTime, boolean hit) {
        this.x = x;
        this.y = y;
        this.r = r;
        this.currentTime = currentTime;
        this.executionTime = executionTime;
        this.hit = hit;
    }
}
