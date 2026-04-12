public class PaymentService {

    enum PaymentMethod {
        CASH, CARD, BKASH, NAGAD, ROCKET
    }

    enum PaymentStatus {
        PENDING, COMPLETED, FAILED
    }

    static class PaymentInfo {
        public String method;
        public double amount;
        public String status;
        public String transactionId;

        public PaymentInfo(String method, double amount, String status, String transactionId) {
            this.method = method;
            this.amount = amount;
            this.status = status;
            this.transactionId = transactionId;
        }
    }

    public boolean validatePaymentMethod(String method) {
        try {
            PaymentMethod.valueOf(method.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public PaymentInfo processPayment(String method, double amount) {
        // Normalize method name
        String normalizedMethod = method.trim().toUpperCase();
        
        // Map common names to enum values
        if (normalizedMethod.equals("CASH") || normalizedMethod.equals("COD")) {
            normalizedMethod = "CASH";
        } else if (normalizedMethod.equals("CARD") || normalizedMethod.equals("DEBIT")) {
            normalizedMethod = "CARD";
        } else if (normalizedMethod.contains("BKASH")) {
            normalizedMethod = "BKASH";
        } else if (normalizedMethod.contains("NAGAD")) {
            normalizedMethod = "NAGAD";
        } else if (normalizedMethod.contains("ROCKET")) {
            normalizedMethod = "ROCKET";
        }
        
        // Validate method
        if (!validatePaymentMethod(normalizedMethod)) {
            System.err.println("Invalid payment method: " + method + " (normalized: " + normalizedMethod + ")");
            return new PaymentInfo(method, amount, "FAILED", "INVALID_METHOD");
        }

        // Simulate payment processing
        String status = processPaymentMethod(normalizedMethod, amount);
        String transactionId = generateTransactionId();

        System.out.println("Payment processed: " + normalizedMethod + " - " + status);
        
        return new PaymentInfo(
            normalizedMethod,
            amount,
            status,
            transactionId
        );
    }

    private String processPaymentMethod(String method, double amount) {
        // Simulate different payment methods
        switch (method.toUpperCase()) {
            case "CASH":
                return "COMPLETED"; // Cash on delivery - always succeeds
            case "CARD":
                return simulateCardPayment(amount);
            case "BKASH":
                return simulateMobilePayment("bKash", amount);
            case "NAGAD":
                return simulateMobilePayment("Nagad", amount);
            case "ROCKET":
                return simulateMobilePayment("Rocket", amount);
            default:
                return "FAILED";
        }
    }

    private String simulateCardPayment(double amount) {
        // 95% success rate for card payments
        return System.currentTimeMillis() % 100 < 95 ? "COMPLETED" : "FAILED";
    }

    private String simulateMobilePayment(String provider, double amount) {
        // 98% success rate for mobile wallets
        return System.currentTimeMillis() % 100 < 98 ? "COMPLETED" : "FAILED";
    }

    private String generateTransactionId() {
        return "TXN" + System.currentTimeMillis();
    }

    public String getPaymentMethodDescription(String method) {
        switch (method.toUpperCase()) {
            case "CASH":
                return "Cash on Delivery";
            case "CARD":
                return "Credit/Debit Card";
            case "BKASH":
                return "bKash Mobile Money";
            case "NAGAD":
                return "Nagad Mobile Money";
            case "ROCKET":
                return "Rocket Mobile Money";
            default:
                return "Unknown";
        }
    }

    public double getPaymentFee(String method, double amount) {
        switch (method.toUpperCase()) {
            case "CASH":
                return 0; // No fee for cash
            case "CARD":
                return amount * 0.025; // 2.5% fee
            case "BKASH":
            case "NAGAD":
            case "ROCKET":
                return amount * 0.015; // 1.5% fee for mobile wallets
            default:
                return 0;
        }
    }
}
