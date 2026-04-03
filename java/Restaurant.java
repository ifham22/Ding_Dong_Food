public class Restaurant {
    private int id;
    private String name;
    private String address;
    private String cuisineType;
    private String imageUrl;
    private String password;
    private boolean open;

    public Restaurant(int id, String name, String address, String cuisineType, String imageUrl, String password, boolean open) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.cuisineType = cuisineType;
        this.imageUrl = imageUrl;
        this.password = password;
        this.open = open;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCuisineType() { return cuisineType; }
    public String getImageUrl() { return imageUrl; }
    public String getPassword() { return password; }
    public boolean isOpen() { return open; }
}