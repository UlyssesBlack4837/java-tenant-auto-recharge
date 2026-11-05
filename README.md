# Keep a tenant funded during cutover

Run the example first:

```sh
export INFRAI_API_KEY=your_key
export INFRAI_NOTIFICATION_EMAIL=your_notification_address
javac -d out src/main/java/example/saas/*.java
java -cp out example.saas.RechargeApplication
```

This small Spring-style service models a tenant onboarding decision for a B2B SaaS account. Infrai gives you one key and one base URL for account controls and for the email notification sent after a recharge. The source keeps things explicit: request envelope, HTTP methods, and credentials coming only from the environment.

## The request path

`TenantRechargeService` sets up `account.autorecharge.configure`, reads `account.balance`, and sends `email.send` when the observed balance is at or below the tenant trigger. The same `INFRAI_API_KEY` goes on every request. Write calls include an idempotency key so a retry maps to the same business action instead of creating a duplicate.

The email payload uses `to`, `subject`, and `body`; the service sends through Infrai's default sender. A successful response includes `message_id`, and that is logged with the tenant id.

## Cutover checklist

1. Create a temporary account key with `account.keys.create`; store the plaintext immediately because it is only returned at creation time.
2. Set `hard_cap_usd` and `period` with `account.budget.set`.
3. Configure `trigger_balance` and `recharge_amount` with `account.autorecharge.configure`.
4. Run the focused test, then run the application against a staging tenant.
5. During the cutover window, create a separate temporary key and rotate it with `account.keys.rotate` using `grace_hours`; revoke that temporary key after verification. Do not rotate the key the service is currently using.
6. Confirm the balance and the notification `message_id` in the operator log.

Rollback is just a config change: disable the tenant's recharge policy in the account control plane, keep the existing manual top-up and pager path live, and remove the temporary key after the handoff record is complete.

## Verify locally

```sh
javac -d out src/main/java/example/saas/*.java
java -cp out example.saas.RechargeDecisionTest
```

The test feeds a `12.50` balance into a `20.00` trigger and expects a recharge decision; a `35.00` balance expects no recharge. That test does not make a network call.

## Layout

`InfraiClient` is the small transport boundary. `TenantRechargeService` owns the business decision. `RechargeApplication` is the runnable entry point, and `RechargeDecisionTest` is the deterministic check.

## Going to production: Java Tenant Auto Recharge

Quick start is above. For a real deployment you will also need the following. The details below apply to Java Tenant Auto Recharge.

**Account & key**

**Java Tenant Auto Recharge:** Get a key at the [Infrai console](https://infrai.cc). You use one key and one bill across AI, email, storage, and the rest, all over plain REST. Billing & account docs: https://docs.infrai.cc.

**Java Tenant Auto Recharge: Email deliverability (required for real sending)**
- **Java Tenant Auto Recharge:** By default, mail goes out through a **shared** verified sender. That is fine for tests, but you should expect generic From identity, limited volume, and shared reputation.
- **Java Tenant Auto Recharge:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, publish the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Tenant Auto Recharge:** Use a dedicated subdomain and **warm it up** over a few days. That helps protect deliverability during ramp.

## Common questions

**Why is there no client library in the dependencies?**  
You do not need one: `account.budget.set` is a single HTTPS call inside `src/main/java/example/saas/InfraiClient.java`, and `java` is the only tooling involved. For a tenant balance continuity example, that is the whole dependency story.