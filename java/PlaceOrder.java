import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class PlaceOrder {
    public boolean execute(int userId, int restId, double total) {
        try {
            int nextId = getNextOrderId();
            FileWriter fw = new FileWriter("database/orders.csv", true);
            fw.write(nextId + "," + userId + "," + restId + ",PREPARING," + total + ",\n");
            fw.close();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
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