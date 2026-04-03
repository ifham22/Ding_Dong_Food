import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class PlaceOrder {
    public boolean execute(int userId, int restId, List<Integer> menuItemIds, List<Integer> quantities) {
        try {
            System.out.println("PlaceOrder.execute() -> userId=" + userId + ", restId=" + restId + ", items=" + menuItemIds);
            double total = calculateTotal(menuItemIds, quantities);
            System.out.println("Calculated total: " + total);
            if (total == 0) {
                System.out.println("ERROR: Total is 0 - invalid order");
                return false; // invalid order
            }

            // Update quantities
            if (!updateQuantities(menuItemIds, quantities)) {
                System.out.println("ERROR: updateQuantities failed");
                return false;
            }

            int nextId = getNextOrderId();
            String items = buildItemsString(menuItemIds, quantities);
            System.out.println("Writing order: id=" + nextId + ", items=" + items);
            FileWriter fw = new FileWriter("database/orders.csv", true);
            fw.write(nextId + "," + userId + "," + restId + ",PREPARING," + total + "," + items + "\n");
            fw.close();
            System.out.println("Order written successfully");
            return true;
        } catch (Exception e) {
            System.out.println("PlaceOrder exception: " + e.getMessage());
            e.printStackTrace();
            return false;
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

    private boolean updateQuantities(List<Integer> menuItemIds, List<Integer> quantities) {
        try {
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader("database/menu.csv"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            }
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                if (parts.length >= 6) {
                    int id = Integer.parseInt(parts[0]);
                    int index = menuItemIds.indexOf(id);
                    if (index != -1) {
                        int currentQty = Integer.parseInt(parts[5]);
                        int orderedQty = quantities.get(index);
                        if (currentQty < orderedQty) return false; // insufficient stock
                        parts[5] = String.valueOf(currentQty - orderedQty);
                        lines.set(i, String.join(",", parts));
                    }
                }
            }
            try (FileWriter fw = new FileWriter("database/menu.csv")) {
                for (String l : lines) {
                    fw.write(l + "\n");
                }
            }
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private String buildItemsString(List<Integer> menuItemIds, List<Integer> quantities) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < menuItemIds.size(); i++) {
            if (i > 0) sb.append(";");
            sb.append(menuItemIds.get(i)).append(":").append(quantities.get(i));
        }
        return sb.toString();
    }

    private int getNextOrderId() {
        try (BufferedReader br = new BufferedReader(new FileReader("database/orders.csv"))) {
            String line;
            int maxId = 0;
            br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    int id = Integer.parseInt(parts[0]);
                    if (id > maxId) maxId = id;
                }
            }
            return maxId + 1;
        } catch (Exception e) {
            return 1;
        }
    }
}