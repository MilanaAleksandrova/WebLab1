package dev.lab1;

import java.math.BigDecimal;

class Params {
    private BigDecimal x, y, r;

    public Params(String queryString) throws ValidationException {
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];

                try {
                    switch (key) {
                        case "x" -> {
                            x = parseBigDecimal(value);
                            validate(x, -5, 3);
                        }
                        case "y" -> {
                            y = parseBigDecimal(value);
                            validate(y, -3, 5);
                        }
                        case "r" -> {
                            r = parseBigDecimal(value);
                            validate(r, 1, 5);
                        }
                        default -> throw new ValidationException("Unknown parameter: " + key);
                    }
                } catch (NumberFormatException e) {
                    throw new ValidationException("Invalid number format for " + key);
                }
            }
        }

        if (x == null) throw new ValidationException("X parameter is required");
        if (y == null) throw new ValidationException("Y parameter is required");
        if (r == null) throw new ValidationException("R parameter is required");
    }

    private static BigDecimal parseBigDecimal(String value) {
        return new BigDecimal(value.replace(',', '.'));
    }

    private static void validate(BigDecimal val, int min, int max) throws ValidationException {
        BigDecimal minBD = BigDecimal.valueOf(min);
        BigDecimal maxBD = BigDecimal.valueOf(max);
        if (val.compareTo(minBD) < 0 || val.compareTo(maxBD) > 0) {
            throw new ValidationException("Value must be between " + min + " and " + max);
        }
    }

    public BigDecimal getX() { return x; }
    public BigDecimal getY() { return y; }
    public BigDecimal getR() { return r; }
}
