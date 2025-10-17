package dev.lab1;

import java.math.BigDecimal;

public record Point(BigDecimal x, BigDecimal y, BigDecimal r, String currentTime, long executionTime, boolean isHit) {}

