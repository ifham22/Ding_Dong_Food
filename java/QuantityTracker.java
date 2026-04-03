import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class QuantityTracker {
    public boolean deductMenuQuantity(int menuId, int amount) {
        try {
            List<String> lines = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader("database/menu.csv"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            }
            for (int i = 1; i < lines.size(); i++) {
                String[] values = lines.get(i).split(",");
                if (values.length >= 7 && values[0].equals(String.valueOf(menuId))) {
                    int qty = Integer.parseInt(values[5]);
                    if (qty >= amount) {
                        values[5] = String.valueOf(qty - amount);
                        lines.set(i, String.join(",", values));
                        try (FileWriter fw = new FileWriter("database/menu.csv")) {
                            for (String l : lines) {
                                fw.write(l + "\n");
                            }
                        }
                        return true;
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        return false;
    }
}