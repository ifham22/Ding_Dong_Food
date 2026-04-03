
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class UserManager {

    public boolean registerUser(String name, String email, String address, String password) {
        try {
            // Validate inputs
            if (name == null || name.trim().isEmpty()
                    || email == null || email.trim().isEmpty()
                    || password == null || password.trim().isEmpty()) {
                System.err.println("[ERROR] Registration validation failed: missing required fields");
                return false;
            }

            // Check if email already exists
            List<User> existingUsers = getAllUsers();
            for (User user : existingUsers) {
                if (user.getEmail().equalsIgnoreCase(email)) {
                    System.err.println("[ERROR] Email already registered: " + email);
                    return false;
                }
            }

            int nextId = getNextUserId();
            System.out.println("[DEBUG] Registering new user with ID: " + nextId);

            try (FileWriter fw = new FileWriter("database/users.csv", true)) {
                fw.write(nextId + "," + name + "," + email + "," + address + "," + password + "\n");
                fw.flush();
            }
            System.out.println("[DEBUG] User registered successfully: " + email);
            return true;
        } catch (Exception e) {
            System.err.println("[ERROR] Registration exception: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public User authenticate(String nameOrEmail, String password) {
        List<User> users = getAllUsers();
        for (User user : users) {
            if ((user.getName().equalsIgnoreCase(nameOrEmail) || user.getEmail().equalsIgnoreCase(nameOrEmail))
                    && user.getPassword() != null && user.getPassword().equals(password)) {
                System.out.println("[DEBUG] User authenticated: " + user.getName());
                return user;
            }
        }
        System.out.println("[DEBUG] Authentication failed for: " + nameOrEmail);
        return null;
    }

    private int getNextUserId() {
        try (BufferedReader br = new BufferedReader(new FileReader("database/users.csv"))) {
            String line;
            int maxId = 0;
            br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        if (id > maxId) {
                            maxId = id;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("[ERROR] Invalid ID in CSV: " + parts[0]);
                    }
                }
            }
            return maxId + 1;
        } catch (Exception e) {
            System.err.println("[ERROR] getNextUserId error: " + e.getMessage());
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
                    try {
                        int id = Integer.parseInt(parts[0].trim());
                        String name = parts[1].trim();
                        String email = parts[2].trim();
                        String address = parts[3].trim();
                        String passwordVal = parts.length >= 5 ? parts[4].trim() : "";
                        users.add(new User(id, name, email, address, passwordVal));
                    } catch (NumberFormatException e) {
                        System.err.println("[ERROR] Invalid user record: " + line);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] getAllUsers error: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }
}
