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
                if (values.length >= 7 && values[6].trim().equals("1")) { // check open status
                    String name = values[1].toLowerCase().trim();
                    String address = values[2].toLowerCase().trim();
                    String cuisine = values[3].toLowerCase().trim();
                    String queryLower = query.toLowerCase().trim();
                    
                    // Match by location (address)
                    if (address.contains(queryLower)) {
                        results.add(values[1]); // return name
                    }
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return results;
    }

    public List<String> searchNearest(String location) {
        return searchByCriteria(location);
    }
}