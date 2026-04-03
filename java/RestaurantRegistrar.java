import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class RestaurantRegistrar {
    public boolean register(String name, String address, String cuisineType, String imageUrl, String password) {
        try {
            int nextId = getNextRestId();
            try (FileWriter fw = new FileWriter("database/restaurants.csv", true)) {
                fw.write(nextId + "," + name + "," + address + "," + cuisineType + "," + imageUrl + "," + password + ",1\n");
            }
            return true;
        } catch (Exception e) { 
            e.printStackTrace(); 
            return false; 
        }
    }

    public Restaurant authenticate(String name, String password) {
        List<Restaurant> restaurants = getAllRestaurants();
        for (Restaurant r : restaurants) {
            if (r.getName().equalsIgnoreCase(name) && r.getPassword() != null && r.getPassword().equals(password)) {
                return r;
            }
        }
        return null;
    }

    public Restaurant getRestaurantById(int id) {
        List<Restaurant> restaurants = getAllRestaurants();
        for (Restaurant r : restaurants) {
            if (r.getId() == id) return r;
        }
        return null;
    }

    public List<Restaurant> getAllRestaurants() {
        List<Restaurant> restaurants = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("database/restaurants.csv"))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 7) {
                    int id = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim();
                    String address = parts[2].trim();
                    String cuisineType = parts[3].trim();
                    String imageUrl = parts[4].trim();
                    String password = parts[5].trim();
                    boolean open = parts[6].trim().equals("1");
                    restaurants.add(new Restaurant(id, name, address, cuisineType, imageUrl, password, open));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return restaurants;
    }

    private int getNextRestId() {
        try (BufferedReader br = new BufferedReader(new FileReader("database/restaurants.csv"))) {
            String line;
            int maxId = 0;
            br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    int id = Integer.parseInt(parts[0].trim());
                    if (id > maxId) maxId = id;
                }
            }
            return maxId + 1;
        } catch (Exception e) {
            return 1;
        }
    }
}