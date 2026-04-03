public class PaymentProcessor {
    public boolean processPayment(int orderId, double amount, String method) {
        // Simple Dummy Integration for Payment Validation
        if ("CREDIT_CARD".equalsIgnoreCase(method) || "BKASH".equalsIgnoreCase(method)) {
            System.out.println("Processing " + amount + " via " + method + " for order " + orderId);
            return true; // Assume success
        }
        return false;
    }
}