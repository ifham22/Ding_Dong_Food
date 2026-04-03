public class Order {
    private int id;
    private int userId;
    private int restaurantId;
    private String status;
    private double total;

    public Order(int id, int userId, int restaurantId, String status, double total) {
        this.id = id;
        this.userId = userId;
        this.restaurantId = restaurantId;
        this.status = status;
        this.total = total;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public int getRestaurantId() { return restaurantId; }
    public String getStatus() { return status; }
    public double getTotal() { return total; }
}