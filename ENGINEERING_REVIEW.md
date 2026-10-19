# Engineering review: consistency, retries and operational limits

This independent reference implementation demonstrates an order/payment saga. The payment adapter is a simulator. The notes below distinguish implementation behavior from work required for a real deployment.

## Start with the code

- [OrderService](order-service/src/main/java/com/skp/commerce/order/service/OrderService.java): the order and its outbox entry are written in one database transaction; an existing idempotency key returns the original order.
- [OutboxRelay](order-service/src/main/java/com/skp/commerce/order/outbox/OutboxRelay.java): waits for a Kafka acknowledgement before marking each row published.
- [PaymentService](payment-service/src/main/java/com/skp/commerce/payment/service/PaymentService.java): checks for an existing payment, passes the order ID to the gateway as an idempotency key, and records the payment outcome and event.
- [Tests](order-service/src/test/java/com/skp/commerce/order): inspect the assertions alongside the implementation.
- [CI](.github/workflows/ci.yml): Maven verification runs before container publication. Successful CI is evidence for the checks it runs, not an end-to-end production guarantee.

## Tradeoffs and failure boundaries

| Boundary | Current behavior | Review before production |
|---|---|---|
| Database commit versus event publication | The outbox avoids publishing the initial event independently of the business transaction. | Monitor unpublished row age, retry failures and table growth; define retention and recovery procedures. |
| Kafka acknowledgement versus outbox commit | A process can publish successfully and stop before the published marker commits. The event can be delivered again. | Verify duplicate delivery against real databases and brokers. Do not assume exactly-once delivery. |
| Multiple relay instances | Each relay queries an unpublished batch; the shown query does not claim or lock rows across workers. | Choose row claiming, a leader, or CDC; test worker failure and per-aggregate ordering with multiple replicas. A Kafka key alone does not establish application-level ordering across publishers. |
| External payment versus local transaction | The gateway call happens before the payment record is committed. A database rollback cannot undo an external charge. | Require durable provider-side idempotency, resolve ambiguous timeouts and reconcile provider records with local state. |
| Concurrent duplicate payment events | The existence check is separate from the gateway call; both callers can pass the check. | Database uniqueness alone cannot prevent duplicate external calls. Test the provider's idempotency contract under concurrency. |
| Reused API idempotency key | The current lookup returns the existing order without comparing the new request body. | Define key ownership, expiry and payload fingerprinting; reject reuse for a different request. |

## Validation work to prioritize

These are proposed acceptance checks, not completed test results:

1. Stop a relay after broker acknowledgement but before database commit; restart and confirm duplicate processing preserves business state.
2. Deliver the same order concurrently to payment consumers and simulate a provider timeout after successful charging.
3. Run two relay instances with overlapping batches and inspect order, duplicate handling and recovery.
4. Submit the same API key with a different payload and define the expected conflict response.
5. Exercise broker/database outages, dead-letter recovery, backup restoration and cache behavior.
6. Record a reproducible load-test setup, data size, latency percentiles, error rate and resource cost before making scale claims.

## Scope

No measured throughput, production availability, real payment processing or employer deployment is claimed. The value of this project is in its inspectable design and the opportunity to test its boundaries.
