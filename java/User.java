public class User {
    private int id;
    private String name;
    private String email;
    private String address;
    private String password;

    public User(int id, String name, String email, String address, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.address = address;
        this.password = password;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getAddress() { return address; }
    public String getPassword() { return password; }
}