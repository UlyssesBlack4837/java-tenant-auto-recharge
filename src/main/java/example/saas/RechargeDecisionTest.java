package example.saas;

public final class RechargeDecisionTest {
    public static void main(String[] args) {
        TenantRechargeService service = new TenantRechargeService(null);
        if (!service.decide(12.50, 20.00).recharge()) throw new AssertionError("low balance should recharge");
        if (service.decide(35.00, 20.00).recharge()) throw new AssertionError("healthy balance should not recharge");
        System.out.println("RechargeDecisionTest passed");
    }
}
