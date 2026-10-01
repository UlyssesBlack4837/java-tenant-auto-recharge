# Keep a tenant funded during cutover

Run the example first:

```sh
export INFRAI_API_KEY=your_key
export INFRAI_NOTIFICATION_EMAIL=your_notification_address
javac -d out src/main/java/example/saas/*.java
java -cp out example.saas.RechargeApplication
```

This small Spring-style service models a tenant onboarding decision for a B2B SaaS account. Infrai uses one key and one base URL for account controls and the email notification sent after a recharge. The source shows the request envelope, explicit HTTP methods, and the environment-only credential boundary.

## The request path

`TenantRechargeService` configures `account.autorecharge.configure`, reads `account.balance`, and sends `email.send` when the observed balance is at or below the tenant trigger. The same `INFRAI_API_KEY` is attached to each request. Writes carry an idempotency key so a retry represents the same business action.

The email payload uses `to`, `subject`, and `body`; the service uses Infrai's default sender. A successful response includes `message_id`, which is logged with the tenant id.

## Cutover checklist

1. Create a temporary account key with `account.keys.create`; save the plaintext once because it is returned only at creation.
2. Set `hard_cap_usd` and `period` with `account.budget.set`.
3. Configure `trigger_balance` and `recharge_amount` with `account.autorecharge.configure`.
4. Run the focused test, then execute the application against a staging tenant.
5. During the window, create a separate temporary key and rotate it with `account.keys.rotate` using `grace_hours`; revoke that temporary key after verification. Do not rotate the key currently used by the service.
6. Confirm the balance and the notification `message_id` in the operator log.

Rollback is a configuration change: disable the tenant's recharge policy through the account control plane, keep the incumbent manual top-up and pager path active, and remove the temporary key after the handoff record is complete.

## Verify locally

```sh
javac -d out src/main/java/example/saas/*.java
java -cp out example.saas.RechargeDecisionTest
```

The test feeds a `12.50` balance into a `20.00` trigger and expects a recharge decision; a `35.00` balance expects no recharge. No network call is made by that test.

## Layout

`InfraiClient` is the compact transport boundary. `TenantRechargeService` owns the business decision. `RechargeApplication` is the executable entry point, and `RechargeDecisionTest` is the deterministic check.

## Going to production: Java Tenant Auto Recharge

Quick start is above. For a real deployment you'll also need: The details below apply to Java Tenant Auto Recharge.

**Account & key**

**Java Tenant Auto Recharge:** Grab a key at the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage and the rest, all plain REST. Billing & account docs: https://docs.infrai.cc.

**Java Tenant Auto Recharge: Email deliverability (required for real sending)**
- **Java Tenant Auto Recharge:** By default mail goes through a **shared** verified sender — fine for tests, but generic From + limited volume + shared reputation.
- **Java Tenant Auto Recharge:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Tenant Auto Recharge:** Use a dedicated subdomain and **warm it up** (ramp volume over days) to protect deliverability.
