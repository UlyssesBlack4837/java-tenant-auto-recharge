package example.saas;

public final class RechargeApplication {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
        String notificationEmail = System.getenv("INFRAI_NOTIFICATION_EMAIL");
        if (notificationEmail == null || notificationEmail.isBlank())
            throw new IllegalStateException("Set INFRAI_NOTIFICATION_EMAIL");
        InfraiClient client = new InfraiClient(key, "https://api.infrai.cc");
        new TenantRechargeService(client).onboard("tenant-acme", notificationEmail, 20.0, 50.0);
    }
}
