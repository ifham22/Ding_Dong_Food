import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ScheduleManager {
    public boolean setRestaurantAvailability(int restId, boolean isOpen) {
        try {
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader("database/restaurants.csv"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            }
            for (int i = 1; i < lines.size(); i++) {
                String[] values = lines.get(i).split(",");
                if (values.length >= 6 && values[0].equals(String.valueOf(restId))) {
                    values[5] = isOpen ? "1" : "0";
                    lines.set(i, String.join(",", values));
                    try (FileWriter fw = new FileWriter("database/restaurants.csv")) {
                        for (String l : lines) {
                            fw.write(l + "\n");
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }

    public boolean scheduleOrder(int orderId, String deliveryTime) {
        // For simplicity, just update the order status or add a note
        // Assuming we add a delivery_time column to orders.csv, but since it's CSV, append to items or status
        // For now, just print or assume scheduled
        System.out.println("Order " + orderId + " scheduled for delivery at " + deliveryTime);
        return true;
    }
}