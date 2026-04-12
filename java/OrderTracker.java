import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class OrderTracker {
    
    enum OrderStatus {
        PENDING, PREPARING, READY, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    }

    static class OrderInfo {
        public int orderId;
        public int userId;
        public int restaurantId;
        public String status;
        public String paymentMethod;
        public String paymentStatus;
        public double total;
        public double discount;
        public double finalTotal;
        public String items;
        public String couponCode;
        public int deliveryTime;
        public String orderedAt;

        public OrderInfo(int orderId, int userId, int restaurantId, String status, String paymentMethod, 
                        String paymentStatus, double total, double discount, double finalTotal, 
                        String items, String couponCode, int deliveryTime, String orderedAt) {
            this.orderId = orderId;
            this.userId = userId;
            this.restaurantId = restaurantId;
            this.status = status;
            this.paymentMethod = paymentMethod;
            this.paymentStatus = paymentStatus;
            this.total = total;
            this.discount = discount;
            this.finalTotal = finalTotal;
            this.items = items;
            this.couponCode = couponCode;
            this.deliveryTime = deliveryTime;
            this.orderedAt = orderedAt;
        }
    }

    public List<OrderInfo> getUserOrders(int userId) {
        List<OrderInfo> orders = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("database/orders.csv"))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 13 && Integer.parseInt(values[1]) == userId) {
                    orders.add(new OrderInfo(
                        Integer.parseInt(values[0]),
                        Integer.parseInt(values[1]),
                        Integer.parseInt(values[2]),
                        values[3],
                        values[4],
                        values[5],
                        Double.parseDouble(values[6]),
                        Double.parseDouble(values[7]),
                        Double.parseDouble(values[8]),
                        values[9],
                        values[10],
                        Integer.parseInt(values[11]),
                        values[12]
                    ));
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return orders;
    }

    public OrderInfo getOrderById(int orderId) {
        try (BufferedReader br = new BufferedReader(new FileReader("database/orders.csv"))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 13 && Integer.parseInt(values[0]) == orderId) {
                    return new OrderInfo(
                        Integer.parseInt(values[0]),
                        Integer.parseInt(values[1]),
                        Integer.parseInt(values[2]),
                        values[3],
                        values[4],
                        values[5],
                        Double.parseDouble(values[6]),
                        Double.parseDouble(values[7]),
                        Double.parseDouble(values[8]),
                        values[9],
                        values[10],
                        Integer.parseInt(values[11]),
                        values[12]
                    );
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return null;
    }

    public boolean updateOrderStatus(int orderId, String newStatus) {
        List<String> lines = new ArrayList<>();
        boolean found = false;
        try (BufferedReader br = new BufferedReader(new FileReader("database/orders.csv"))) {
            String line = br.readLine();
            if (line != null) lines.add(line); // add header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 13 && Integer.parseInt(values[0]) == orderId) {
                    values[3] = newStatus;
                    lines.add(String.join(",", values));
                    found = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
            return false;
        }

        if (!found) return false;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter("database/orders.csv"))) {
            for (String line : lines) {
                bw.write(line);
                bw.newLine();
            }
            return true;
        } catch (Exception e) { 
            e.printStackTrace(); 
            return false;
        }
    }

    public String getOrderStatus(int orderId) {
        OrderInfo order = getOrderById(orderId);
        return order != null ? order.status : "NOT_FOUND";
    }
}
