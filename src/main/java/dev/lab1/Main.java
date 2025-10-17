package dev.lab1;

import com.fastcgi.FCGIInterface;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.math.BigDecimal;

public class Main {
    //TODO: To other data-class
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
    private static final String HTTP_METHOD_NOT_ALLOWED = """ 
            HTTP/1.1 405 Method Not Allowed
            Content-Type: application/json
            Content-Length: %d
            
            %s
            """;

    private static final List<Point> table = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) {
        var fcgi = new FCGIInterface();
        while (fcgi.FCGIaccept() >= 0) {
            try {
                String requestMethod = System.getProperty("REQUEST_METHOD");
                if (requestMethod == null || !requestMethod.equals("POST")) {
                    String json = String.format("""
                        {
                            "error": true,
                            "message": "Method not allowed. Only POST requests are supported.",
                            "timestamp": "%s"
                        }
                        """, LocalDateTime.now().format(formatter));
                    String response = String.format(HTTP_METHOD_NOT_ALLOWED,
                            json.getBytes(StandardCharsets.UTF_8).length, json);
                    System.out.print(response);
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

                String json = buildResponseJson();
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

    private static String buildResponseJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"results\": [\n");

        for (int i = 0; i < Math.min(table.size(), 10); i++) {
            Point r = table.get(i);
            json.append("    {\n");
            json.append("      \"x\": ").append(r.x).append(",\n");
            json.append("      \"y\": ").append(r.y).append(",\n");
            json.append("      \"r\": ").append(r.r).append(",\n");
            json.append("      \"currentTime\": \"").append(r.currentTime).append("\",\n");
            json.append("      \"executionTime\": ").append(r.executionTime).append(",\n");
            json.append("      \"hit\": ").append(r.hit).append("\n");
            json.append(i < Math.min(table.size() - 1, 9) ? "    },\n" : "    }\n");
        }//TODO: to Gson

        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }

    private static boolean calculate(BigDecimal x, BigDecimal y, BigDecimal r) {
        BigDecimal zero = BigDecimal.ZERO;

        if (x.compareTo(zero) > 0 && y.compareTo(zero) > 0) {
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