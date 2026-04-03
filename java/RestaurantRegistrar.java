import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class RestaurantRegistrar {
    public boolean register(String name, String address, String cuisineType, String imageUrl) {
        try {
            int nextId = getNextRestId();
            FileWriter fw = new FileWriter("database/restaurants.csv", true);
            fw.write(nextId + "," + name + "," + address + "," + cuisineType + "," + imageUrl + ",1\n");
            fw.close();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    private int getNextRestId() {
        try (BufferedReader br = new BufferedReader(new FileReader("database/restaurants.csv"))) {
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