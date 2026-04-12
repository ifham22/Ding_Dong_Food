
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MainServer {

    // Helper to add CORS headers to all responses
    static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    // Helper to handle OPTIONS preflight requests
    static boolean handlePreflight(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        int port = 8081;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Endpoints
        server.createContext("/api/restaurants", new ApiRestaurantsHandler());
        server.createContext("/api/menu", new ApiMenuHandler());
        server.createContext("/api/order", new ApiOrderHandler());
        server.createContext("/api/search", new ApiSearchHandler());
        server.createContext("/api/register-restaurant", new ApiRegisterRestaurantHandler());
        server.createContext("/api/restaurant-login", new ApiRestaurantLoginHandler());
        server.createContext("/api/add-menu", new ApiAddMenuHandler());
        server.createContext("/api/delete-menu", new ApiDeleteMenuHandler());
        server.createContext("/api/register-user", new ApiRegisterUserHandler());
        server.createContext("/api/login", new ApiLoginHandler());
        server.createContext("/api/update-profile", new ApiUpdateProfileHandler());
        server.createContext("/api/users", new ApiUsersHandler());
        server.createContext("/api/place-order", new ApiPlaceOrderHandler());
        server.createContext("/api/schedule-order", new ApiScheduleOrderHandler());
        server.createContext("/api/coupons", new ApiCouponsHandler());
        server.createContext("/api/validate-coupon", new ApiValidateCouponHandler());
        server.createContext("/api/order-status", new ApiOrderStatusHandler());
        server.createContext("/api/user-orders", new ApiUserOrdersHandler());
        server.createContext("/api/payment", new ApiPaymentHandler());
        server.createContext("/api/search-menu", new ApiSearchMenuHandler());

        // Static File Server
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null);
        server.start();
        System.out.println("Java Pure Backend Started!");
        System.out.println("Server listening on http://localhost:" + port);
    }

    static class ApiRestaurantsHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("[");
                try (BufferedReader br = new BufferedReader(new FileReader("database/restaurants.csv"))) {
                    String line = br.readLine(); // skip header
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(",");
                        if (values.length < 7 || !values[6].equals("1")) {
                            continue;
                        }

                        if (!first) {
                            jsonBuilder.append(",");
                        }
                        jsonBuilder.append("{");
                        jsonBuilder.append("\"id\":").append(values[0]).append(",");
                        jsonBuilder.append("\"name\":\"").append(escapeJson(values[1])).append("\",");
                        jsonBuilder.append("\"address\":\"").append(escapeJson(values[2])).append("\",");
                        jsonBuilder.append("\"cuisineType\":\"").append(escapeJson(values[3])).append("\",");
                        jsonBuilder.append("\"imageUrl\":\"").append(escapeJson(values[4])).append("\"");
                        jsonBuilder.append("}");
                        first = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    String error = "{\"error\": \"Failed to read CSV: " + e.getMessage() + "\"}";
                    sendResponse(exchange, 500, error);
                    return;
                }

                jsonBuilder.append("]");
                sendResponse(exchange, 200, jsonBuilder.toString());
            } else {
                exchange.sendResponseHeaders(405, -1);// 405 Method Not Allowed
            }
        }

        private String escapeJson(String input) {
            if (input == null) {
                return "";
            }
            return input.replace("\"", "\\\"");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiMenuHandler implements HttpHandler {

        private double getPricingMultiplier(String location) {
            if (location == null) return 1.0;
            location = location.toLowerCase().trim();
            switch (location) {
                case "iut":
                    return 0.8; // Student-friendly (20% discount)
                case "pallabi":
                    return 1.0; // Standard
                case "banani":
                case "gulshan":
                    return 1.3; // High-class (30% premium)
                default:
                    return 1.0;
            }
        }

        private String getRestaurantLocation(int restId) {
            try (BufferedReader br = new BufferedReader(new FileReader("database/restaurants.csv"))) {
                String line = br.readLine(); // header
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3 && Integer.parseInt(parts[0]) == restId) {
                        return parts[2]; // address field
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private String[] parseCSVLine(String line) {
            java.util.List<String> fields = new java.util.ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    fields.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
            fields.add(current.toString());
            return fields.toArray(new String[0]);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.contains("restId=")) {
                    sendResponse(exchange, 400, "{\"error\":\"Missing restId\"}");
                    return;
                }
                int restId = Integer.parseInt(query.split("restId=")[1].split("&")[0]);
                
                // Get restaurant location for pricing
                String location = getRestaurantLocation(restId);
                double multiplier = getPricingMultiplier(location);
                
                StringBuilder json = new StringBuilder("[");
                try (BufferedReader br = new BufferedReader(new FileReader("database/menu.csv"))) {
                    String line = br.readLine(); // header
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        String[] values = parseCSVLine(line);
                        if (values.length < 8 || !values[1].equals(String.valueOf(restId))) {
                            continue;
                        }

                        if (!first) {
                            json.append(",");
                        }
                        
                        // Parse base price and apply location multiplier
                        double basePrice = Double.parseDouble(values[5].trim());
                        double adjustedPrice = Math.round(basePrice * multiplier * 100.0) / 100.0;
                        
                        json.append("{");
                        json.append("\"id\":").append(values[0].trim()).append(",");
                        json.append("\"name\":\"").append(escapeJson(values[2].trim())).append("\",");
                        json.append("\"description\":\"").append(escapeJson(values[3].trim())).append("\",");
                        json.append("\"category\":\"").append(escapeJson(values[4].trim())).append("\",");
                        json.append("\"price\":").append(adjustedPrice).append(",");
                        json.append("\"basePrice\":").append(basePrice).append(",");
                        String sizes = values[7].trim();
                        json.append("\"hasSizes\":").append(!"-".equals(sizes) ? "true" : "false").append(",");
                        if (!"-".equals(sizes)) {
                            json.append("\"sizes\":\"").append(escapeJson(sizes)).append("\"");
                        } else {
                            json.append("\"sizes\":null");
                        }
                        json.append("}");
                        first = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":\"CSV error: " + e.getMessage() + "\"}");
                    return;
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private String escapeJson(String input) {
            if (input == null) {
                return "";
            }
            return input.replace("\"", "\\\"");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiOrderHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes());
                // Expect body: userId=1&restId=2&items=1:2;2:1
                String[] params = body.split("&");
                int userId = Integer.parseInt(params[0].split("=")[1]);
                int restId = Integer.parseInt(params[1].split("=")[1]);
                String itemsStr = params.length > 2 ? params[2].split("=")[1] : "";

                List<Integer> menuItemIds = new ArrayList<>();
                List<Integer> quantities = new ArrayList<>();

                if (!itemsStr.isEmpty()) {
                    itemsStr = java.net.URLDecoder.decode(itemsStr, "UTF-8");
                    String[] itemPairs = itemsStr.split(";");
                    for (String pair : itemPairs) {
                        String[] pq = pair.split(":");
                        if (pq.length == 2) {
                            menuItemIds.add(Integer.parseInt(pq[0]));
                            quantities.add(Integer.parseInt(pq[1]));
                        }
                    }
                }

                if (menuItemIds.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Invalid items\"}");
                    return;
                }

                PlaceOrder po = new PlaceOrder();
                boolean success = po.execute(userId, restId, menuItemIds, quantities);
                if (success) {
                    sendResponse(exchange, 200, "{\"message\":\"Order placed\"}");
                } else {
                    sendResponse(exchange, 500, "{\"error\":\"Failed to place order\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiSearchHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.contains("q=")) {
                    sendResponse(exchange, 400, "{\"error\":\"Missing q\"}");
                    return;
                }
                String q = java.net.URLDecoder.decode(query.split("q=")[1].split("&")[0], "UTF-8");
                SearchRestaurants sr = new SearchRestaurants();
                List<String> results = sr.searchByCriteria(q);
                StringBuilder json = new StringBuilder("[");
                try (BufferedReader br = new BufferedReader(new FileReader("database/restaurants.csv"))) {
                    String line = br.readLine(); // header
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(",");
                        if (values.length >= 7 && values[6].equals("1") && results.contains(values[1])) {
                            if (!first) {
                                json.append(",");
                            }
                            json.append("{");
                            json.append("\"id\":").append(values[0]).append(",");
                            json.append("\"name\":\"").append(escapeJson(values[1])).append("\",");
                            json.append("\"address\":\"").append(escapeJson(values[2])).append("\",");
                            json.append("\"cuisineType\":\"").append(escapeJson(values[3])).append("\",");
                            json.append("\"imageUrl\":\"").append(escapeJson(values[4])).append("\"");
                            json.append("}");
                            first = false;
                        }
                    }
                } catch (Exception e) {
                    sendResponse(exchange, 500, "{\"error\":\"CSV error\"}");
                    return;
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private String escapeJson(String input) {
            if (input == null) {
                return "";
            }
            return input.replace("\"", "\\\"");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiRegisterRestaurantHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes());
                String[] params = body.split("&");

                String name = "";
                String address = "";
                String cuisineType = "";
                String password = "";
                String imageUrl = "";

                for (String param : params) {
                    if (param.startsWith("name=")) {
                        name = java.net.URLDecoder.decode(param.substring(5), "UTF-8");
                    } else if (param.startsWith("address=")) {
                        address = java.net.URLDecoder.decode(param.substring(8), "UTF-8");
                    } else if (param.startsWith("cuisineType=")) {
                        cuisineType = java.net.URLDecoder.decode(param.substring(12), "UTF-8");
                    } else if (param.startsWith("password=")) {
                        password = java.net.URLDecoder.decode(param.substring(9), "UTF-8");
                    }
                }

                RestaurantRegistrar rr = new RestaurantRegistrar();
                boolean success = rr.register(name, address, cuisineType, imageUrl, password);
                if (success) {
                    sendResponse(exchange, 200, "{\"message\":\"Restaurant registered\"}");
                } else {
                    sendResponse(exchange, 500, "{\"error\":\"Failed to register\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiRestaurantLoginHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes());
                String[] params = body.split("&");

                String name = "";
                String password = "";

                for (String param : params) {
                    if (param.startsWith("name=")) {
                        name = java.net.URLDecoder.decode(param.substring(5), "UTF-8");
                    } else if (param.startsWith("password=")) {
                        password = java.net.URLDecoder.decode(param.substring(9), "UTF-8");
                    }
                }

                RestaurantRegistrar rr = new RestaurantRegistrar();
                Restaurant r = rr.authenticate(name, password);

                if (r != null) {
                    String json = "{\"restaurant\":{\"id\":" + r.getId() + ",\"name\":\"" + escapeJson(r.getName()) + "\",\"address\":\"" + escapeJson(r.getAddress()) + "\",\"cuisineType\":\"" + escapeJson(r.getCuisineType()) + "\"}}";
                    sendResponse(exchange, 200, json);
                } else {
                    sendResponse(exchange, 401, "{\"error\":\"Invalid restaurant name or password\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private String escapeJson(String input) {
            if (input == null) {
                return "";
            }
            return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiAddMenuHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes());
                // Assume body is restId=...&name=...&description=...&price=...&qty=...
                String[] params = body.split("&");
                int restId = Integer.parseInt(java.net.URLDecoder.decode(params[0].split("=")[1], "UTF-8"));
                String name = java.net.URLDecoder.decode(params[1].split("=")[1], "UTF-8");
                String description = java.net.URLDecoder.decode(params[2].split("=")[1], "UTF-8");
                double price = Double.parseDouble(java.net.URLDecoder.decode(params[3].split("=")[1], "UTF-8"));
                int qty = Integer.parseInt(java.net.URLDecoder.decode(params[4].split("=")[1], "UTF-8"));

                MenuManager mm = new MenuManager();
                boolean success = mm.addMenuItem(restId, name, description, price, qty);
                if (success) {
                    sendResponse(exchange, 200, "{\"message\":\"Menu item added\"}");
                } else {
                    sendResponse(exchange, 500, "{\"error\":\"Failed to add menu item\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiDeleteMenuHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("DELETE".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.contains("menuId=")) {
                    sendResponse(exchange, 400, "{\"error\":\"Missing menuId\"}");
                    return;
                }
                int menuId = Integer.parseInt(query.split("menuId=")[1].split("&")[0]);

                MenuManager mm = new MenuManager();
                boolean success = mm.deleteMenuItem(menuId);
                if (success) {
                    sendResponse(exchange, 200, "{\"message\":\"Menu item deleted\"}");
                } else {
                    sendResponse(exchange, 500, "{\"error\":\"Failed to delete menu item\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiRegisterUserHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes());
                    System.out.println("[DEBUG] Registration body: " + body);

                    // Parse URL-encoded parameters
                    String name = "";
                    String email = "";
                    String password = "";
                    String address = "";

                    String[] params = body.split("&");
                    for (String param : params) {
                        if (param.startsWith("name=")) {
                            name = java.net.URLDecoder.decode(param.substring(5), "UTF-8");
                        } else if (param.startsWith("email=")) {
                            email = java.net.URLDecoder.decode(param.substring(6), "UTF-8");
                        } else if (param.startsWith("password=")) {
                            password = java.net.URLDecoder.decode(param.substring(9), "UTF-8");
                        } else if (param.startsWith("address=")) {
                            address = java.net.URLDecoder.decode(param.substring(8), "UTF-8");
                        }
                    }

                    System.out.println("[DEBUG] Parsed: name=" + name + ", email=" + email + ", password=" + password + ", address=" + address);

                    if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                        sendResponse(exchange, 400, "{\"error\":\"Name, email, and password are required\"}");
                        return;
                    }

                    UserManager um = new UserManager();
                    boolean success = um.registerUser(name, email, address, password);
                    if (success) {
                        System.out.println("[DEBUG] User registered successfully");
                        sendResponse(exchange, 200, "{\"message\":\"User registered successfully\"}");
                    } else {
                        System.out.println("[DEBUG] User registration failed");
                        sendResponse(exchange, 500, "{\"error\":\"Failed to register user\"}");
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Registration error: " + e.getMessage());
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":\"Server error: " + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private String escapeJson(String input) {
            if (input == null) {
                return "";
            }
            return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiLoginHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes());
                String[] params = body.split("&");

                String user = "";
                String password = "";

                for (String param : params) {
                    if (param.startsWith("user=")) {
                        user = java.net.URLDecoder.decode(param.substring(5), "UTF-8");
                    } else if (param.startsWith("password=")) {
                        password = java.net.URLDecoder.decode(param.substring(9), "UTF-8");
                    }
                }

                UserManager um = new UserManager();
                User u = um.authenticate(user, password);

                if (u != null) {
                    String json = "{\"user\":{\"id\":" + u.getId() + ",\"name\":\"" + escapeJson(u.getName()) + "\",\"email\":\"" + escapeJson(u.getEmail()) + "\",\"address\":\"" + escapeJson(u.getAddress()) + "\"}}";
                    sendResponse(exchange, 200, json);
                } else {
                    sendResponse(exchange, 401, "{\"error\":\"Invalid username or password\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private String escapeJson(String input) {
            if (input == null) {
                return "";
            }
            return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiUpdateProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    int userId = 0;
                    String name = "";
                    String email = "";
                    String address = "";
                    
                    // Parse parameters
                    String[] params = body.split("&");
                    for (String param : params) {
                        if (param.contains("=")) {
                            String[] kv = param.split("=", 2);
                            String key = kv[0];
                            String value = kv.length > 1 ? java.net.URLDecoder.decode(kv[1], "UTF-8") : "";
                            
                            if ("userId".equals(key)) userId = Integer.parseInt(value);
                            else if ("name".equals(key)) name = value;
                            else if ("email".equals(key)) email = value;
                            else if ("address".equals(key)) address = value;
                        }
                    }
                    
                    System.out.println("Updating profile for user: " + userId + ", name: " + name + ", email: " + email + ", address: " + address);
                    
                    if (userId <= 0 || name.isEmpty() || email.isEmpty() || address.isEmpty()) {
                        String json = "{\"success\":false,\"error\":\"All fields are required\"}";
                        sendResponse(exchange, 400, json);
                        return;
                    }
                    
                    // Update user in CSV
                    java.util.List<String> allLines = new java.util.ArrayList<>();
                    boolean updated = false;
                    
                    try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("database/users.csv"))) {
                        String line = br.readLine(); // header
                        if (line != null) allLines.add(line);
                        
                        while ((line = br.readLine()) != null) {
                            String[] parts = line.split(",");
                            if (parts.length >= 5 && Integer.parseInt(parts[0]) == userId) {
                                // Update: id,name,email,address,password
                                String updatedLine = parts[0] + "," + name + "," + email + "," + address + "," + parts[4];
                                allLines.add(updatedLine);
                                updated = true;
                                System.out.println("Profile updated in CSV: " + updatedLine);
                            } else {
                                allLines.add(line);
                            }
                        }
                    }
                    
                    // Write back
                    if (updated) {
                        try (java.io.FileWriter fw = new java.io.FileWriter("database/users.csv");
                             java.io.BufferedWriter bw = new java.io.BufferedWriter(fw)) {
                            for (String line : allLines) {
                                bw.write(line);
                                bw.newLine();
                            }
                            bw.flush();
                        }
                        String json = "{\"success\":true,\"message\":\"Profile updated successfully\"}";
                        sendResponse(exchange, 200, json);
                    } else {
                        String json = "{\"success\":false,\"error\":\"User not found\"}";
                        sendResponse(exchange, 404, json);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error updating profile: " + e.getMessage());
                    e.printStackTrace();
                    String json = "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                    sendResponse(exchange, 500, json);
                }
            }
        }
        
        private String escapeJson(String input) {
            if (input == null) return "";
            return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiUsersHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                UserManager um = new UserManager();
                List<User> users = um.getAllUsers();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < users.size(); i++) {
                    User u = users.get(i);
                    if (i > 0) {
                        json.append(",");
                    }
                    json.append("{")
                            .append("\"id\":").append(u.getId()).append(",")
                            .append("\"name\":\"").append(escapeJson(u.getName())).append("\",")
                            .append("\"email\":\"").append(escapeJson(u.getEmail())).append("\",")
                            .append("\"address\":\"").append(escapeJson(u.getAddress())).append("\"")
                            .append("}");
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private String escapeJson(String input) {
            if (input == null) {
                return "";
            }
            return input.replace("\"", "\\\"");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiPlaceOrderHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes());
                    System.out.println("Place order request body: " + body);
                    
                    // Assume body is userId=...&restId=...&items=1:2;3:1 (menuId:qty;...)
                    String[] params = body.split("&");
                    if (params.length < 3) {
                        sendResponse(exchange, 400, "{\"error\":\"Missing parameters\"}");
                        return;
                    }
                    
                    int userId = Integer.parseInt(java.net.URLDecoder.decode(params[0].split("=")[1], "UTF-8"));
                    int restId = Integer.parseInt(java.net.URLDecoder.decode(params[1].split("=")[1], "UTF-8"));
                    String itemsStr = java.net.URLDecoder.decode(params[2].split("=")[1], "UTF-8");

                    System.out.println("Parsed: userId=" + userId + ", restId=" + restId + ", items=" + itemsStr);

                    List<Integer> menuItemIds = new ArrayList<>();
                    List<Integer> quantities = new ArrayList<>();
                    String[] itemPairs = itemsStr.split(";");
                    for (String pair : itemPairs) {
                        String[] pq = pair.split(":");
                        menuItemIds.add(Integer.parseInt(pq[0]));
                        quantities.add(Integer.parseInt(pq[1]));
                    }

                    PlaceOrder po = new PlaceOrder();
                    boolean success = po.execute(userId, restId, menuItemIds, quantities);
                    if (success) {
                        sendResponse(exchange, 200, "{\"message\":\"Order placed successfully\"}");
                    } else {
                        sendResponse(exchange, 500, "{\"error\":\"Order validation failed - check stock or menu items\"}");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Number parsing error: " + e.getMessage());
                    sendResponse(exchange, 400, "{\"error\":\"Invalid number format\"}");
                } catch (Exception e) {
                    System.out.println("Order processing error: " + e.getMessage());
                    e.printStackTrace();
                    sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private double calculateTotal(List<Integer> menuItemIds, List<Integer> quantities) {
            double total = 0;
            try (BufferedReader br = new BufferedReader(new FileReader("database/menu.csv"))) {
                String line = br.readLine(); // header
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 5) {
                        int id = Integer.parseInt(parts[0]);
                        double price = Double.parseDouble(parts[4]);
                        int index = menuItemIds.indexOf(id);
                        if (index != -1) {
                            total += price * quantities.get(index);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return total;
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiScheduleOrderHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes());
                String[] params = body.split("&");
                int orderId = Integer.parseInt(java.net.URLDecoder.decode(params[0].split("=")[1], "UTF-8"));
                String deliveryTime = java.net.URLDecoder.decode(params[1].split("=")[1], "UTF-8");

                ScheduleManager sm = new ScheduleManager();
                boolean success = sm.scheduleOrder(orderId, deliveryTime);
                if (success) {
                    sendResponse(exchange, 200, "{\"message\":\"Order scheduled\"}");
                } else {
                    sendResponse(exchange, 500, "{\"error\":\"Failed to schedule\"}");
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class StaticFileHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/index.html";
            }

            // Path inside the 'html' folder!
            File file = new File("html" + path);

            if (file.exists() && !file.isDirectory()) {
                String contentType = Files.probeContentType(file.toPath());
                if (contentType == null) {
                    if (path.endsWith(".css")) {
                        contentType = "text/css"; 
                    }else if (path.endsWith(".js")) {
                        contentType = "application/javascript"; 
                    }else {
                        contentType = "text/html";
                    }
                }
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, file.length());

                try (OutputStream os = exchange.getResponseBody(); FileInputStream fs = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = fs.read(buffer)) != -1) {
                        os.write(buffer, 0, count);
                    }
                }
            } else {
                String response = "404 File not found.";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    // NEW HANDLERS FOR COUPONS, ORDERS, PAYMENT, AND SEARCH

    static class ApiCouponsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                CouponManager cm = new CouponManager();
                List<Coupon> coupons;
                StringBuilder json = new StringBuilder("[");

                if (query != null && query.contains("restaurant_id=")) {
                    int restId = Integer.parseInt(query.split("restaurant_id=")[1].split("&")[0]);
                    coupons = cm.getCouponsByRestaurant(restId);
                } else {
                    coupons = cm.getAllCoupons();
                }

                boolean first = true;
                for (Coupon coupon : coupons) {
                    if (!first) json.append(",");
                    json.append("{");
                    json.append("\"id\":").append(coupon.id).append(",");
                    json.append("\"code\":\"").append(coupon.code).append("\",");
                    json.append("\"restaurantId\":").append(coupon.restaurantId).append(",");
                    json.append("\"discountType\":\"").append(coupon.discountType).append("\",");
                    json.append("\"discountValue\":").append(coupon.discountValue).append(",");
                    json.append("\"minOrder\":").append(coupon.minOrder);
                    json.append("}");
                    first = false;
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiValidateCouponHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    String code = "";
                    double orderTotal = 0;
                    int restaurantId = 0;
                    int userId = 0;
                    String monthYear = "";
                    
                    // Parse all parameters
                    String[] params = body.split("&");
                    for (String param : params) {
                        if (param.contains("=")) {
                            String[] kv = param.split("=", 2);
                            String key = kv[0];
                            String value = kv.length > 1 ? java.net.URLDecoder.decode(kv[1], "UTF-8") : "";
                            
                            if ("code".equals(key)) code = value;
                            else if ("total".equals(key)) orderTotal = Double.parseDouble(value);
                            else if ("restaurantId".equals(key)) restaurantId = Integer.parseInt(value);
                            else if ("userId".equals(key)) userId = Integer.parseInt(value);
                            else if ("monthYear".equals(key)) monthYear = value;
                        }
                    }
                    
                    System.out.println("Validating coupon: " + code + " for restaurant: " + restaurantId + ", user: " + userId + ", month: " + monthYear);

                    CouponManager cm = new CouponManager();
                    Coupon coupon = cm.getCouponByCode(code);

                    if (coupon == null) {
                        String json = "{\"valid\":false,\"message\":\"Coupon not found\"}";
                        sendResponse(exchange, 200, json);
                        return;
                    }

                    // Check if coupon is valid for this restaurant
                    if (!cm.isValidCouponForRestaurant(code, restaurantId)) {
                        String json = "{\"valid\":false,\"message\":\"This coupon is not valid for the selected restaurant\"}";
                        sendResponse(exchange, 200, json);
                        return;
                    }

                    // Check minimum order
                    if (orderTotal < coupon.minOrder) {
                        String json = "{\"valid\":false,\"message\":\"Minimum order of Tk " + coupon.minOrder + " required\"}";
                        sendResponse(exchange, 200, json);
                        return;
                    }

                    // Check usage limit (max 2 times per user per month)
                    int usageCount = cm.getCouponUsageCount(userId, coupon.id, monthYear);
                    if (usageCount >= 2) {
                        String json = "{\"valid\":false,\"message\":\"You have reached the usage limit (max 2 times per month) for this coupon\"}";
                        sendResponse(exchange, 200, json);
                        return;
                    }

                    // All validations passed
                    double discount = cm.calculateDiscount(coupon, orderTotal);
                    String json = "{\"valid\":true,\"discount\":" + discount + ",\"code\":\"" + coupon.code + "\",\"couponId\":" + coupon.id + "}";
                    System.out.println("Coupon validated successfully: " + json);
                    sendResponse(exchange, 200, json);
                    
                } catch (Exception e) {
                    System.err.println("Error validating coupon: " + e.getMessage());
                    e.printStackTrace();
                    String json = "{\"valid\":false,\"message\":\"Error validating coupon: " + e.getMessage().replace("\"", "\\\"") + "\"}";
                    sendResponse(exchange, 200, json);
                }
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiOrderStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.contains("order_id=")) {
                    sendResponse(exchange, 400, "{\"error\":\"Missing order_id\"}");
                    return;
                }

                int orderId = Integer.parseInt(query.split("order_id=")[1].split("&")[0]);
                OrderTracker tracker = new OrderTracker();
                OrderTracker.OrderInfo order = tracker.getOrderById(orderId);

                if (order != null) {
                    String json = "{\"orderId\":" + order.orderId + ",\"status\":\"" + order.status + 
                                  "\",\"paymentStatus\":\"" + order.paymentStatus + "\",\"deliveryTime\":" + 
                                  order.deliveryTime + "}";
                    sendResponse(exchange, 200, json);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Order not found\"}");
                }
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiUserOrdersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.contains("user_id=")) {
                    sendResponse(exchange, 400, "{\"error\":\"Missing user_id\"}");
                    return;
                }

                int userId = Integer.parseInt(query.split("user_id=")[1].split("&")[0]);
                OrderTracker tracker = new OrderTracker();
                List<OrderTracker.OrderInfo> orders = tracker.getUserOrders(userId);

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                for (OrderTracker.OrderInfo order : orders) {
                    if (!first) json.append(",");
                    json.append("{\"orderId\":").append(order.orderId).append(",\"restaurantId\":").append(order.restaurantId)
                        .append(",\"status\":\"").append(order.status).append("\",\"paymentStatus\":\"").append(order.paymentStatus)
                        .append("\",\"total\":").append(order.finalTotal).append(",\"deliveryTime\":")
                        .append(order.deliveryTime).append("}");
                    first = false;
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiPaymentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    byte[] requestBody = exchange.getRequestBody().readAllBytes();
                    String body = new String(requestBody);
                    System.out.println("Payment request body: " + body);
                    
                    String method = "Cash"; // default
                    double amount = 0;
                    int userId = 0;
                    int restaurantId = 0;
                    double discount = 0;
                    String coupon = "";
                    double subtotal = 0;
                    String cartItemsJson = "[]";
                    
                    // Parse all parameters
                    if (!body.isEmpty()) {
                        String[] params = body.split("&");
                        for (String param : params) {
                            if (param.contains("=")) {
                                String[] kv = param.split("=", 2);
                                String key = kv[0];
                                String value = kv.length > 1 ? java.net.URLDecoder.decode(kv[1], "UTF-8") : "";
                                
                                if ("paymentMethod".equals(key)) {
                                    method = value;
                                } else if ("amount".equals(key)) {
                                    amount = Double.parseDouble(value);
                                } else if ("userId".equals(key)) {
                                    userId = Integer.parseInt(value);
                                } else if ("restaurantId".equals(key)) {
                                    restaurantId = Integer.parseInt(value);
                                } else if ("discount".equals(key)) {
                                    discount = Double.parseDouble(value);
                                } else if ("subtotal".equals(key)) {
                                    subtotal = Double.parseDouble(value);
                                } else if ("coupon".equals(key)) {
                                    coupon = value;
                                } else if ("cartItems".equals(key)) {
                                    cartItemsJson = value;
                                }
                            }
                        }
                    }
                    
                    System.out.println("Payment details - User: " + userId + ", Restaurant: " + restaurantId + ", Method: " + method + ", Amount: " + amount);
                    
                    PaymentService ps = new PaymentService();
                    PaymentService.PaymentInfo payment = ps.processPayment(method, amount);

                    // Save order to database if payment is successful
                    int orderId = 0;
                    if ("COMPLETED".equals(payment.status)) {
                        orderId = saveOrderToDatabase(userId, restaurantId, subtotal, discount, amount, coupon, method, "PENDING", "COMPLETED", cartItemsJson);
                        System.out.println("Order created with ID: " + orderId);
                        
                        // Record coupon usage if a coupon was applied
                        if (!coupon.isEmpty()) {
                            CouponManager cm = new CouponManager();
                            Coupon couponObj = cm.getCouponByCode(coupon);
                            if (couponObj != null) {
                                String monthYear = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
                                cm.recordCouponUsage(userId, couponObj.id, coupon, monthYear);
                                System.out.println("Coupon usage recorded: user=" + userId + ", coupon=" + coupon + ", month=" + monthYear);
                            }
                        }
                    }

                    String json = "{\"method\":\"" + escapeJson(payment.method) + "\",\"amount\":" + payment.amount + 
                                  ",\"status\":\"" + payment.status + "\",\"transactionId\":\"" + 
                                  escapeJson(payment.transactionId) + "\",\"orderId\":" + orderId + "}";
                    
                    System.out.println("Payment response: " + json);
                    sendResponse(exchange, 200, json);
                } catch (Exception e) {
                    System.err.println("Payment error: " + e.getMessage());
                    e.printStackTrace();
                    String errorJson = "{\"error\":\"" + escapeJson(e.getMessage()) + "\",\"status\":\"FAILED\"}";
                    sendResponse(exchange, 500, errorJson);
                }
            }
        }

        private int saveOrderToDatabase(int userId, int restaurantId, double subtotal, double discount, double finalTotal, String coupon, String paymentMethod, String status, String paymentStatus, String cartItemsJson) {
            try {
                // Parse cartItems to build items string (id1:qty1;id2:qty2;...)
                String itemsStr = parseCartItemsToString(cartItemsJson);
                
                // Read current orders to find max ID
                int maxId = 0;
                java.util.List<String> allLines = new java.util.ArrayList<>();
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader("database/orders.csv"))) {
                    String line = br.readLine(); // header
                    if (line != null) allLines.add(line);
                    while ((line = br.readLine()) != null) {
                        allLines.add(line);
                        String[] parts = line.split(",");
                        if (parts.length > 0) {
                            int id = Integer.parseInt(parts[0]);
                            if (id > maxId) maxId = id;
                        }
                    }
                }
                
                int orderId = maxId + 1;
                String orderedAt = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                int deliveryTime = 30; // default 30 minutes
                
                // Format: id,user_id,rest_id,status,payment_method,payment_status,total,discount,final_total,items,coupon_code,delivery_time,ordered_at
                String newOrder = String.format("%d,%d,%d,%s,%s,%s,%.2f,%.2f,%.2f,%s,%s,%d,%s",
                    orderId, userId, restaurantId, status, paymentMethod, paymentStatus, 
                    subtotal, discount, finalTotal, itemsStr, coupon, deliveryTime, orderedAt);
                
                try (java.io.FileWriter fw = new java.io.FileWriter("database/orders.csv", true);
                     java.io.BufferedWriter bw = new java.io.BufferedWriter(fw)) {
                    bw.newLine();
                    bw.write(newOrder);
                    bw.flush();
                    System.out.println("Order saved to CSV: " + newOrder);
                }
                
                return orderId;
            } catch (Exception e) {
                System.err.println("Error saving order: " + e.getMessage());
                e.printStackTrace();
                return 0;
            }
        }

        private String parseCartItemsToString(String cartItemsJson) {
            // Simple regex parsing to extract {id: X, quantity: Y} from JSON array
            StringBuilder sb = new StringBuilder();
            try {
                // Extract all {id: X, quantity: Y} objects from the JSON array
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{[^}]*\"id\"\\s*:\\s*(\\d+)[^}]*\"quantity\"\\s*:\\s*(\\d+)[^}]*\\}");
                java.util.regex.Matcher matcher = pattern.matcher(cartItemsJson);
                boolean first = true;
                while (matcher.find()) {
                    if (!first) sb.append(";");
                    String id = matcher.group(1);
                    String qty = matcher.group(2);
                    sb.append(id).append(":").append(qty);
                    first = false;
                }
            } catch (Exception e) {
                System.err.println("Error parsing cartItems JSON: " + e.getMessage());
            }
            return sb.toString();
        }
        
        private String escapeJson(String input) {
            if (input == null) return "";
            return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            byte[] bytes = responseText.getBytes("UTF-8");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ApiSearchMenuHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handlePreflight(exchange)) {
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String searchTerm = "";
                if (query != null && query.contains("q=")) {
                    searchTerm = java.net.URLDecoder.decode(query.split("q=")[1].split("&")[0], "UTF-8").toLowerCase();
                }

                StringBuilder json = new StringBuilder("[");
                try (BufferedReader br = new BufferedReader(new FileReader("database/menu.csv"))) {
                    String line = br.readLine(); // skip header
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(",");
                        if (values.length >= 5) {
                            String name = values[1].toLowerCase();
                            if (name.contains(searchTerm)) {
                                if (!first) json.append(",");
                                json.append("{");
                                json.append("\"id\":").append(values[0]).append(",");
                                json.append("\"name\":\"").append(values[1]).append("\",");
                                json.append("\"restaurantId\":").append(values[2]).append(",");
                                json.append("\"price\":").append(values[4]);
                                json.append("}");
                                first = false;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString());
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = responseText.getBytes();
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}
