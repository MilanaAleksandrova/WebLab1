package dev.lab1;

import com.fastcgi.FCGIInterface;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    private static final String HTTP_RESPONSE = """
            HTTP/1.1 200 OK
            Content-Type: application/json
            Content-Length: %d
            
            %s
            """;
    private static final String HTTP_ERROR = """
            HTTP/1.1 400 Bad Request
            Content-Type: application/json
            Content-Length: %d
            
            %s
            """;

    private static final List<Result> results = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        var fcgi = new FCGIInterface();
        while (fcgi.FCGIaccept() >= 0) {
            try {
                String contentLengthStr = System.getProperty("CONTENT_LENGTH");
                if (contentLengthStr == null || contentLengthStr.isEmpty()) {
                    throw new ValidationException("Missing content length");
                }

                int contentLength = Integer.parseInt(contentLengthStr);
                if (contentLength <= 0) {
                    throw new ValidationException("Invalid content length");
                }

                byte[] postData = new byte[contentLength];
                System.in.read(postData);
                String postString = new String(postData, StandardCharsets.UTF_8);

                var params = new Params(postString);

                long startTime = System.nanoTime();
                boolean result = calculate(params.getX(), params.getY(), params.getR());
                long endTime = System.nanoTime();

                long executionTime = (endTime - startTime) / 1000;
                String currentTime = LocalDateTime.now().format(formatter);

                Result resultObj = new Result(params.getX(), params.getY(), params.getR(),
                        currentTime, executionTime, result);
                results.add(0, resultObj);

                String json = buildResponseJson(resultObj);
                String response = String.format(HTTP_RESPONSE,
                        json.getBytes(StandardCharsets.UTF_8).length, json);
                System.out.print(response);

            } catch (ValidationException e) {
                String json = String.format("""
                    {
                        "error": true,
                        "message": "%s",
                        "timestamp": "%s"
                    }
                    """, e.getMessage(), LocalDateTime.now().format(formatter));
                String response = String.format(HTTP_ERROR,
                        json.getBytes(StandardCharsets.UTF_8).length, json);
                System.out.print(response);
            } catch (Exception e) {
                String json = String.format("""
                    {
                        "error": true,
                        "message": "Internal server error: %s",
                        "timestamp": "%s"
                    }
                    """, e.getMessage(), LocalDateTime.now().format(formatter));
                String response = String.format(HTTP_ERROR,
                        json.getBytes(StandardCharsets.UTF_8).length, json);
                System.out.print(response);
            }
        }
    }

    private static String buildResponseJson(Result result) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"results\": [\n");

        for (int i = 0; i < Math.min(results.size(), 10); i++) {
            Result r = results.get(i);
            json.append("    {\n");
            json.append("      \"x\": ").append(r.x).append(",\n");
            json.append("      \"y\": ").append(r.y).append(",\n");
            json.append("      \"r\": ").append(r.r).append(",\n");
            json.append("      \"currentTime\": \"").append(r.currentTime).append("\",\n");
            json.append("      \"executionTime\": ").append(r.executionTime).append(",\n");
            json.append("      \"hit\": ").append(r.hit).append("\n");
            json.append(i < Math.min(results.size() - 1, 9) ? "    },\n" : "    }\n");
        }

        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }

    private static boolean calculate(float x, float y, float r) {
        if (x >= 0 && y >= 0) {
            return x <= r && y <= r/2;
        }
        if (x <= 0 && y >= 0) {
            return y <= x + r/2;
        }
        if (x <= 0 && y <= 0) {
            return (x * x + y * y) <= (r * r);
        }
        return false;
    }

    static class Result {
        float x, y, r;
        String currentTime;
        long executionTime;
        boolean hit;

        Result(float x, float y, float r, String currentTime, long executionTime, boolean hit) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.currentTime = currentTime;
            this.executionTime = executionTime;
            this.hit = hit;
        }
    }
}

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
                            if (y <= -3 || y >= 5) throw new ValidationException("Y must be between -3 and 5");
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

class ValidationException extends Exception {
    public ValidationException(String message) {
        super(message);
    }
}