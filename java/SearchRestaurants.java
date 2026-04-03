import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class SearchRestaurants {
    public List<String> searchNearest(String location) {
        List<String> results = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("database/restaurants.csv"))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 6 && values[5].equals("1")) {
                    results.add(values[1]);
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return results;
    }
}