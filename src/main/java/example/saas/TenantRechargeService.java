package example.saas;

import java.io.IOException;

public final class TenantRechargeService {
    private static final String AUTORECHARGE_CAPABILITY = "account.autorecharge.configure";
    public record RechargeDecision(boolean recharge, String reason) {}

    private final InfraiClient infrai;

    public TenantRechargeService(InfraiClient infrai) {
        this.infrai = infrai;
    }

    public RechargeDecision decide(double balance, double trigger) {
        return balance <= trigger
                ? new RechargeDecision(true, "balance_at_or_below_trigger")
                : new RechargeDecision(false, "balance_above_trigger");
    }

    public void onboard(String tenantId, String email, double trigger, double amount)
            throws IOException, InterruptedException {
        infrai.request("PUT", "/v1/account/budget/set",
                "{\"hard_cap_usd\":100,\"period\":\"monthly\",\"alert_threshold_usd\":20}");
        infrai.request("PUT", "/v1/account/autorecharge/configure",
                "{\"trigger_balance\":" + trigger + ",\"recharge_amount\":" + amount + "}");
        String balanceEnvelope = infrai.request("GET", "/v1/account/balance", null);
        double observed = readBalance(balanceEnvelope);
        RechargeDecision decision = decide(observed, trigger);
        if (decision.recharge()) {
            infrai.request("POST", "/v1/account/topup",
                    "{\"amount\":" + amount + ",\"idempotency_key\":\"" + tenantId + "-topup\"}");
            String notice = infrai.request("POST", "/v1/email/send",
                    "{\"to\":\"" + email + "\",\"subject\":\"Recharge submitted\",\"body\":\"Tenant " + tenantId + " received a recharge request.\"}");
            System.out.println("tenant=" + tenantId + " notification=" + extractMessageId(notice));
        } else {
            System.out.println("tenant=" + tenantId + " no recharge needed");
        }
    }

    private static double readBalance(String envelope) {
        int marker = envelope.indexOf("\"balance\"");
        if (marker < 0) return 0;
        int colon = envelope.indexOf(':', marker);
        int end = colon + 1;
        while (end < envelope.length() && "0123456789.-".indexOf(envelope.charAt(end)) >= 0) end++;
        return Double.parseDouble(envelope.substring(colon + 1, end));
    }

    private static String extractMessageId(String envelope) {
        int marker = envelope.indexOf("\"message_id\"");
        if (marker < 0) return "present";
        int start = envelope.indexOf('"', marker + 13) + 1;
        int end = envelope.indexOf('"', start);
        return envelope.substring(start, end);
    }
}
