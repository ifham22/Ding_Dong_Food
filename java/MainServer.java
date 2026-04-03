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

    public static void main(String[] args) throws Exception {
        int port = 8081;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // API Endpoints
        server.createContext("/api/restaurants", new ApiRestaurantsHandler());
        server.createContext("/api/menu", new ApiMenuHandler());
        server.createContext("/api/order", new ApiOrderHandler());
        server.createContext("/api/search", new ApiSearchHandler());
        server.createContext("/api/register-restaurant", new ApiRegisterRestaurantHandler());
        server.createContext("/api/add-menu", new ApiAddMenuHandler());
        server.createContext("/api/delete-menu", new ApiDeleteMenuHandler());
        server.createContext("/api/register-user", new ApiRegisterUserHandler());
        server.createContext("/api/users", new ApiUsersHandler());
        server.createContext("/api/place-order", new ApiPlaceOrderHandler());
        server.createContext("/api/schedule-order", new ApiScheduleOrderHandler());

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
            // Enable CORS
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            
            if ("GET".equals(exchange.getRequestMethod())) {
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("[");
                try (BufferedReader br = new BufferedReader(new FileReader("database/restaurants.csv"))) {
                    String line = br.readLine(); // skip header
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(",");
                        if (values.length < 6 || !values[5].equals("1")) continue;
                        
                        if (!first) jsonBuilder.append(",");
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
            if (input == null) return "";
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
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.contains("restId=")) {
                    sendResponse(exchange, 400, "{\"error\":\"Missing restId\"}");
                    return;
                }
                int restId = Integer.parseInt(query.split("restId=")[1].split("&")[0]);
                StringBuilder json = new StringBuilder("[");
                try (BufferedReader br = new BufferedReader(new FileReader("database/menu.csv"))) {
                    String line = br.readLine(); // header
                    boolean first = true;
                    while ((line = br.readLine()) != null) {
                        String[] values = line.split(",");
                        if (values.length < 7 || !values[1].equals(String.valueOf(restId)) || !values[6].equals("1")) continue;
                        
                        if (!first) json.append(",");
                        json.append("{");
                        json.append("\"id\":").append(values[0]).append(",");
                        json.append("\"name\":\"").append(escapeJson(values[2])).append("\",");
                        json.append("\"description\":\"").append(escapeJson(values[3])).append("\",");
                        json.append("\"price\":").append(values[4]).append(",");
                        json.append("\"quantity\":").append(values[5]);
                        json.append("}");
                        first = false;
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
            if (input == null) return "";
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
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
                        if (values.length >= 6 && values[5].equals("1") && results.contains(values[1])) {
                            if (!first) json.append(",");
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
            if (input == null) return "";
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes());
                // Assume body is name=...&address=...&cuisineType=...
                String[] params = body.split("&");
                String name = java.net.URLDecoder.decode(params[0].split("=")[1], "UTF-8");
                String address = java.net.URLDecoder.decode(params[1].split("=")[1], "UTF-8");
                String cuisineType = java.net.URLDecoder.decode(params[2].split("=")[1], "UTF-8");
                String imageUrl = ""; // default empty
                
                RestaurantRegistrar rr = new RestaurantRegistrar();
                boolean success = rr.register(name, address, cuisineType, imageUrl);
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

    static class ApiAddMenuHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes());
                String[] params = body.split("&");
                String name = java.net.URLDecoder.decode(params[0].split("=")[1], "UTF-8");
                String email = java.net.URLDecoder.decode(params[1].split("=")[1], "UTF-8");
                String address = java.net.URLDecoder.decode(params[2].split("=")[1], "UTF-8");
                
                UserManager um = new UserManager();
                boolean success = um.registerUser(name, email, address);
                if (success) {
                    sendResponse(exchange, 200, "{\"message\":\"User registered\"}");
                } else {
                    sendResponse(exchange, 500, "{\"error\":\"Failed to register user\"}");
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

    static class ApiUsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if ("GET".equals(exchange.getRequestMethod())) {
                UserManager um = new UserManager();
                List<User> users = um.getAllUsers();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < users.size(); i++) {
                    User u = users.get(i);
                    if (i > 0) json.append(",");
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
            if (input == null) return "";
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes());
                // Assume body is userId=...&restId=...&items=1:2;3:1 (menuId:qty;...)
                String[] params = body.split("&");
                int userId = Integer.parseInt(java.net.URLDecoder.decode(params[0].split("=")[1], "UTF-8"));
                int restId = Integer.parseInt(java.net.URLDecoder.decode(params[1].split("=")[1], "UTF-8"));
                String itemsStr = java.net.URLDecoder.decode(params[2].split("=")[1], "UTF-8");
                
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
                    // Process payment
                    double total = calculateTotal(menuItemIds, quantities);
                    PaymentProcessor pp = new PaymentProcessor();
                    boolean paymentSuccess = pp.processPayment(0, total, "CREDIT_CARD"); // dummy orderId
                    if (paymentSuccess) {
                        sendResponse(exchange, 200, "{\"message\":\"Order placed and payment processed\"}");
                    } else {
                        sendResponse(exchange, 500, "{\"error\":\"Payment failed\"}");
                    }
                } else {
                    sendResponse(exchange, 500, "{\"error\":\"Failed to place order\"}");
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
            } catch (Exception e) { e.printStackTrace(); }
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
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
                    if (path.endsWith(".css")) contentType = "text/css";
                    else if (path.endsWith(".js")) contentType = "application/javascript";
                    else contentType = "text/html";
                }
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, file.length());
                
                try (OutputStream os = exchange.getResponseBody();
                     FileInputStream fs = new FileInputStream(file)) {
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
}
