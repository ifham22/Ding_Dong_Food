public class MenuItem {
    private int id;
    private int restaurantId;
    private String name;
    private int quantity;
    private boolean available;

    public MenuItem(int id, int restaurantId, String name, int quantity, boolean available) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.quantity = quantity;
        this.available = available;
    }

    public int getId() { return id; }
    public int getRestaurantId() { return restaurantId; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public boolean isAvailable() { return available; }
}