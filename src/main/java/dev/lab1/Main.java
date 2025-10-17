package dev.lab1;

import com.fastcgi.FCGIInterface;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.math.BigDecimal;

public class Main {
    private static final List<Point> table = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        var fcgi = new FCGIInterface();
        while (fcgi.FCGIaccept() >= 0) {
            try {
                String requestMethod = System.getProperty("REQUEST_METHOD");
                if (requestMethod == null || !requestMethod.equals("POST")) {
                    String json = gson.toJson(Map.of(
                            "error", true,
                            "message", "Method not allowed. Only POST requests are supported.",
                            "timestamp", LocalDateTime.now().format(formatter)
                    ));
                    sendJsonResponse(json, 405);
                    continue;
                }

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

                Point pointObj = new Point(params.getX(), params.getY(), params.getR(),
                        currentTime, executionTime, result);
                table.add(0, pointObj);

                String json = gson.toJson(Map.of("results", table));
                sendJsonResponse(json, 200);

            } catch (ValidationException e) {
                String json = gson.toJson(Map.of(
                        "error", true,
                        "message", e.getMessage(),
                        "timestamp", LocalDateTime.now().format(formatter)
                ));
                sendJsonResponse(json, 400);
            } catch (Exception e) {
                String json = gson.toJson(Map.of(
                        "error", true,
                        "message", "Internal server error: " + e.getMessage(),
                        "timestamp", LocalDateTime.now().format(formatter)
                ));
                sendJsonResponse(json, 500);
            }
        }
    }

    private static void sendJsonResponse(String json, int statusCode) {
        String statusText;
        switch (statusCode) {
            case 200 -> statusText = "OK";
            case 400 -> statusText = "Bad Request";
            case 405 -> statusText = "Method Not Allowed";
            default -> statusText = "Internal Server Error";
        }

        String headers = String.format(
                "HTTP/1.1 %d %s\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: %d\r\n" +
                        "\r\n",
                statusCode,
                statusText,
                json.getBytes(StandardCharsets.UTF_8).length
        );

        try {
            System.out.write(headers.getBytes(StandardCharsets.UTF_8));
            System.out.write(json.getBytes(StandardCharsets.UTF_8));
            System.out.flush();
        } catch (IOException e) {
            System.err.println("Error when sending the response: " + e.getMessage());
        }

    }

    private static boolean calculate(BigDecimal x, BigDecimal y, BigDecimal r) {
        BigDecimal zero = BigDecimal.ZERO;

        if (x.compareTo(zero) >= 0 && y.compareTo(zero) >= 0) {
            return x.compareTo(r) <= 0 && y.compareTo(r) <= 0;
        }

        if (x.compareTo(zero) <= 0 && y.compareTo(zero) <= 0) {
            BigDecimal x2 = x.multiply(x);
            BigDecimal y2 = y.multiply(y);
            BigDecimal r2 = r.multiply(r);
            return x2.add(y2).compareTo(r2) <= 0;
        }

        if (x.compareTo(zero) <= 0 && y.compareTo(zero) >= 0) {
            BigDecimal twoX = x.multiply(BigDecimal.valueOf(2));
            BigDecimal expr = twoX.add(r);
            return y.compareTo(expr) <= 0;
        }

        return false;
    }
}
