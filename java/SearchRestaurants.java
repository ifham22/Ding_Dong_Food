import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class SearchRestaurants {
    public List<String> searchByCriteria(String query) {
        List<String> results = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("database/restaurants.csv"))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 6 && values[5].equals("1")) { // only open restaurants
                    String name = values[1].toLowerCase();
                    String address = values[2].toLowerCase();
                    String cuisine = values[3].toLowerCase();
                    if (name.contains(query.toLowerCase()) || address.contains(query.toLowerCase()) || cuisine.contains(query.toLowerCase())) {
                        results.add(values[1]); // return name
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return results;
    }

    public List<String> searchNearest(String location) {
        // For simplicity, return all open restaurants if location matches partially
        return searchByCriteria(location);
    }
}