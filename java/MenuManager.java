import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class MenuManager {
    public boolean addMenuItem(int restId, String name, String description, double price, int initialQty) {
        try {
            int nextId = getNextMenuId();
            FileWriter fw = new FileWriter("database/menu.csv", true);
            fw.write(nextId + "," + restId + "," + name + "," + description + "," + price + "," + initialQty + "," + (initialQty > 0 ? 1 : 0) + "\n");
            fw.close();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public boolean setAvailability(int menuId, boolean available) {
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
                    values[6] = available ? "1" : "0";
                    lines.set(i, String.join(",", values));
                    try (FileWriter fw = new FileWriter("database/menu.csv")) {
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

    public boolean deleteMenuItem(int menuId) {
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
                    lines.remove(i);
                    try (FileWriter fw = new FileWriter("database/menu.csv")) {
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

    private int getNextMenuId() {
        try (BufferedReader br = new BufferedReader(new FileReader("database/menu.csv"))) {
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