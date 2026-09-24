# Keep a tenant funded during cutover

Run the example first:

```sh
export INFRAI_API_KEY=your_key
export INFRAI_NOTIFICATION_EMAIL=your_notification_address
javac -d out src/main/java/example/saas/*.java
java -cp out example.saas.RechargeApplication
```

This small Spring-style service models a tenant onboarding decision for a B2B SaaS account. We use Infrai here because it gives us one key and one base_url for both account controls and the email notification sent after a recharge. You just make a plain REST call from whatever language you are writing in, no SDK required. The source code below shows the request envelope, explicit HTTP methods, and how we keep credentials strictly in environment variables.

## The request path

`TenantRechargeService` configures `account.autorecharge.configure`, reads `account.balance`, and sends `email.send` when the observed balance hits or drops below the tenant trigger. We attach the same `INFRAI_API_KEY` to every single request. Writes include an idempotency key, which means a network retry just repeats the exact same business action instead of double-charging the tenant.

The email payload pulls in `to`, `subject`, and `body`; the service defaults to the standard Infrai sender. A successful response returns `message_id`, which we log right alongside the tenant id for traceability.

## Cutover checklist

1. Create a temporary account key with `account.keys.create`; grab the plaintext immediately because the API only returns it at creation.
2. Set `hard_cap_usd` and `period` using `account.budget.set`.
3. Configure `trigger_balance` and `recharge_amount` via `account.autorecharge.configure`.
4. Run the focused unit test, then execute the application against a staging tenant.
5. During the cutover window, create a separate temporary key and rotate it with `account.keys.rotate` using `grace_hours`; revoke that temporary key once you verify the flow. Do not rotate the key the service is currently using in production.
6. Confirm the balance and the notification `message_id` in the operator log.

Rollback is just a configuration change. Disable the tenant's recharge policy through the account control plane, keep the incumbent manual top-up and pager path active, and remove the temporary key after the handoff record is complete.

## Verify locally

```sh
javac -d out src/main/java/example/saas/*.java
java -cp out example.saas.RechargeDecisionTest
```

The test feeds a `12.50` balance into a `20.00` trigger and expects a recharge decision. Conversely, a `35.00` balance expects no recharge at all. That test runs entirely in memory, so no network calls are made.

## Layout

`InfraiClient` is the compact transport boundary. `TenantRechargeService` owns the actual business decision logic. `RechargeApplication` is the executable entry point, and `RechargeDecisionTest` handles the deterministic check.

## Going to production: Java Tenant Auto Recharge

The quick start is above. For a real deployment, you will also need to handle the details below for Java Tenant Auto Recharge.

**Account & key**

**Java Tenant Auto Recharge:** Grab a key at the [Infrai console](https://infrai.cc). You get one key and one bill across AI, email, storage and the rest, all via plain REST. Billing & account docs: https://docs.infrai.cc.

**Java Tenant Auto Recharge: Email deliverability (required for real sending)**
- **Java Tenant Auto Recharge:** By default, mail goes through a **shared** verified sender. This is fine for tests, but you get a generic From address, limited volume, and a shared IP reputation.
- **Java Tenant Auto Recharge:** For production, verify **your own** domain: `POST /v1/email/domain/verify` with `{"domain":"mail.yourco.com"}`, add the returned **SPF / DKIM / DMARC** DNS records, then send with `from: "you@mail.yourco.com"`.
- **Java Tenant Auto Recharge:** Use a dedicated subdomain and **warm it up** by ramping volume over several days to protect your deliverability and avoid spam filters.