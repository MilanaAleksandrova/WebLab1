package dev.lab1;

class Params {
    private float x, y, r;

    public Params(String queryString) throws ValidationException {
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];

                try {
                    switch (key) {
                        case "x":
                            x = Float.parseFloat(value);
                            if (x < -5 || x > 3) throw new ValidationException("X must be between -5 and 3");
                            break;
                        case "y":
                            y = Float.parseFloat(value);
                            if (y < -3 || y > 5) throw new ValidationException("Y must be between -3 and 5");
                            break;
                        case "r":
                            r = Float.parseFloat(value);
                            if (r < 1 || r > 5) throw new ValidationException("R must be between 1 and 5");
                            break;
                    }
                } catch (NumberFormatException e) {
                    throw new ValidationException("Invalid number format for " + key);
                }
            }
        }

        if (Float.isNaN(x)) throw new ValidationException("X parameter is required");
        if (Float.isNaN(y)) throw new ValidationException("Y parameter is required");
        if (Float.isNaN(r)) throw new ValidationException("R parameter is required");
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getR() { return r; }
}
