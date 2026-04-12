import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

class Coupon {
    public int id;
    public String code;
    public int restaurantId;
    public String discountType; // "percentage" or "amount"
    public double discountValue;
    public double minOrder;
    public String expiryDate;
    public int active;

    public Coupon(int id, String code, int restaurantId, String discountType, double discountValue, double minOrder, String expiryDate, int active) {
        this.id = id;
        this.code = code;
        this.restaurantId = restaurantId;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minOrder = minOrder;
        this.expiryDate = expiryDate;
        this.active = active;
    }
}

public class CouponManager {
    public List<Coupon> getAllCoupons() {
        List<Coupon> coupons = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("database/coupons.csv"))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 8) {
                    Coupon c = new Coupon(
                        Integer.parseInt(values[0]),
                        values[1],
                        Integer.parseInt(values[2]),
                        values[3],
                        Double.parseDouble(values[4]),
                        Double.parseDouble(values[5]),
                        values[6],
                        Integer.parseInt(values[7])
                    );
                    if (c.active == 1) {
                        coupons.add(c);
                    }
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return coupons;
    }

    public Coupon getCouponByCode(String code) {
        try (BufferedReader br = new BufferedReader(new FileReader("database/coupons.csv"))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 8 && values[1].equalsIgnoreCase(code) && Integer.parseInt(values[7]) == 1) {
                    return new Coupon(
                        Integer.parseInt(values[0]),
                        values[1],
                        Integer.parseInt(values[2]),
                        values[3],
                        Double.parseDouble(values[4]),
                        Double.parseDouble(values[5]),
                        values[6],
                        Integer.parseInt(values[7])
                    );
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return null;
    }

    public List<Coupon> getCouponsByRestaurant(int restaurantId) {
        List<Coupon> coupons = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("database/coupons.csv"))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 8) {
                    int restId = Integer.parseInt(values[2]);
                    int active = Integer.parseInt(values[7]);
                    // Include if it's a common coupon (restId=0) OR specific to this restaurant
                    if ((restId == 0 || restId == restaurantId) && active == 1) {
                        coupons.add(new Coupon(
                            Integer.parseInt(values[0]),
                            values[1],
                            restId,
                            values[3],
                            Double.parseDouble(values[4]),
                            Double.parseDouble(values[5]),
                            values[6],
                            active
                        ));
                    }
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return coupons;
    }

    public boolean isValidCouponForRestaurant(String couponCode, int restaurantId) {
        Coupon coupon = getCouponByCode(couponCode);
        if (coupon == null) return false;
        // Valid if it's a common coupon (restId=0) OR specific to this restaurant
        return coupon.restaurantId == 0 || coupon.restaurantId == restaurantId;
    }

    public int getCouponUsageCount(int userId, int couponId, String monthYear) {
        int count = 0;
        try (BufferedReader br = new BufferedReader(new FileReader("database/coupon_usage.csv"))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 4) {
                    int uid = Integer.parseInt(values[0]);
                    int cid = Integer.parseInt(values[1]);
                    String month = values[3].trim();
                    if (uid == userId && cid == couponId && month.equals(monthYear)) {
                        count = Integer.parseInt(values[2]);
                        break;
                    }
                }
            }
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        return count;
    }

    public void recordCouponUsage(int userId, int couponId, String couponCode, String monthYear) {
        try {
            java.util.List<String> allLines = new java.util.ArrayList<>();
            boolean found = false;
            
            // Read existing usage
            try (BufferedReader br = new BufferedReader(new FileReader("database/coupon_usage.csv"))) {
                String line = br.readLine(); // header
                if (line != null) allLines.add(line);
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    if (values.length >= 4) {
                        int uid = Integer.parseInt(values[0]);
                        int cid = Integer.parseInt(values[1]);
                        String month = values[3].trim();
                        
                        if (uid == userId && cid == couponId && month.equals(monthYear)) {
                            int newCount = Integer.parseInt(values[2]) + 1;
                            String timestamp = java.time.LocalDateTime.now().toString();
                            allLines.add(uid + "," + cid + "," + couponCode + "," + newCount + "," + monthYear + "," + timestamp);
                            found = true;
                        } else {
                            allLines.add(line);
                        }
                    }
                }
            }
            
            // If not found, add new entry
            if (!found) {
                String timestamp = java.time.LocalDateTime.now().toString();
                allLines.add(userId + "," + couponId + "," + couponCode + ",1," + monthYear + "," + timestamp);
            }
            
            // Write back
            try (java.io.FileWriter fw = new java.io.FileWriter("database/coupon_usage.csv");
                 java.io.BufferedWriter bw = new java.io.BufferedWriter(fw)) {
                for (String line : allLines) {
                    bw.write(line);
                    bw.newLine();
                }
                bw.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double calculateDiscount(Coupon coupon, double orderTotal) {
        if (coupon == null || orderTotal < coupon.minOrder) {
            return 0;
        }
        if ("percentage".equals(coupon.discountType)) {
            return orderTotal * (coupon.discountValue / 100);
        } else if ("amount".equals(coupon.discountType)) {
            return Math.min(coupon.discountValue, orderTotal);
        }
        return 0;
    }
}
