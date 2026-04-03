public class Restaurant {
    private int id;
    private String name;
    private boolean opex;

    public Restaurant(int id, String name, boolean opex) {
        this.id = id;
        this.name = name;
        this.opex = opex;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public boolean isOpen() { return opex; }
}