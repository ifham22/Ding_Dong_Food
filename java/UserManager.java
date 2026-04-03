import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class UserManager {
    public boolean registerUser(String name, String email, String address) {
        try {
            int nextId = getNextUserId();
            FileWriter fw = new FileWriter("database/users.csv", true);
            fw.write(nextId + "," + name + "," + email + "," + address + "\n");
            fw.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getNextUserId() {
        try (BufferedReader br = new BufferedReader(new FileReader("database/users.csv"))) {
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

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("database/users.csv"))) {
            String line = br.readLine(); // header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    int id = Integer.parseInt(parts[0]);
                    String name = parts[1];
                    String email = parts[2];
                    String address = parts[3];
                    users.add(new User(id, name, email, address));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
}