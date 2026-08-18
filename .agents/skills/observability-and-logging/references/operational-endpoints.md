# Operational endpoints

Use this reference when exposing actuator endpoints, writing a health indicator, or configuring
liveness and readiness probes. Apply every rule from `../SKILL.md` and `application-security`;
imports are omitted.

Snippets here follow the worked-example rules in `modern-java-21`: every identifier or build property a snippet uses is declared in that snippet or attributed to the file that declares it, and an excerpt names any omitted element that the configuration depends on.

## Contents

1. [Exposure and the management port](#exposure-and-the-management-port)
2. [Health groups and probes](#health-groups-and-probes)
3. [Writing a health indicator](#writing-a-health-indicator)
4. [Metrics scraping](#metrics-scraping)
5. [Testing operational endpoints](#testing-operational-endpoints)

## Exposure and the management port

Expose the smallest set the platform actually consumes, on a separate management port that is not
routed from the public ingress.

```yaml
management:
  server:
    port: 8081
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
```

`prometheus` belongs in the list only when the platform scrapes metrics and the project declares a
Prometheus registry. With a push export over OTLP the endpoint does not exist, and listing it
publishes nothing while implying a scrape target that is not there. Expose what the chosen export
model actually needs and nothing else.

Rules:

- Never expose `*`. It publishes environment, configprops, beans, mappings, heapdump, and threaddump, several of which leak secrets or allow a denial of service.
- Never expose `heapdump`, `threaddump`, `env`, or `configprops` on a publicly reachable port. A heap dump contains every credential the process holds.
- `shutdown` stays disabled.
- `show-details` is never `always` on a reachable endpoint. Health details name internal hosts, database versions, and failure reasons.
- `application-security` owns who may reach these endpoints: the dedicated filter chain, which endpoints are open, the operator credential, and what to do when the deployment cannot provide a separate port. This skill decides only what is exposed and in what shape. A separate port is network segmentation, not authentication.
- `info` contains build and version data only. Never put an environment URL, an account identifier, or anything operationally sensitive in it.
- The Prometheus endpoint is exposed only when the project uses a scraped registry. With a pushed registry, it is unnecessary.

## Health groups and probes

The single most consequential rule here: **an external dependency belongs in readiness, never in
liveness.**

Liveness answers "is this process broken beyond recovery, restart it". Readiness answers "can this
instance serve traffic right now". If the database appears in liveness, a thirty-second database
blip restarts every pod in the deployment at once, turning a brief degradation into an outage, and
the restarts do nothing because the process was never the problem.

```yaml
management:
  endpoint:
    health:
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState,db
```

- Liveness contains process state only. In practice that is `livenessState` and nothing else.
- Readiness contains the dependencies without which the instance cannot serve a request: the database, and a migration or startup gate if one exists.
- A dependency that has a fallback does not belong in readiness. If the application degrades gracefully without a cache, the cache must not remove the instance from the load balancer.
- Do not add a third-party API to either probe. Your availability must not be defined by someone else's.
- Probe endpoints must be cheap and fast. A probe that runs a real query on every call multiplies load exactly when the system is struggling.

## Writing a health indicator

Write one only when a real dependency is not already covered by auto-configuration. The datasource
indicator already exists; do not reimplement it.

```java
@Component
public class BillingGatewayHealthIndicator implements HealthIndicator {

    private static final String DETAIL_SYSTEM = "system";

    private final BillingGatewayClient billingGatewayClient;

    public BillingGatewayHealthIndicator(final BillingGatewayClient billingGatewayClient) {
        this.billingGatewayClient = billingGatewayClient;
    }

    @Override
    public Health health() {
        try {
            this.billingGatewayClient.ping();

            return Health.up().withDetail(DETAIL_SYSTEM, "billing").build();
        } catch (final BillingGatewayException exception) {
            return Health.down().withDetail(DETAIL_SYSTEM, "billing").build();
        }
    }
}
```

Rules:

- A health indicator never throws. An exception escaping it can fail the whole health endpoint.
- Details carry no host name, no version, no stack trace, no exception message. `show-details` may be enabled in an environment you did not anticipate.
- Bound the check with a timeout. A health endpoint that hangs is a health endpoint that fails every probe.
- Register the indicator in the group where it belongs; by default it lands in the overall health status, which may not be what you intended.
- Do not make an indicator perform a write, a migration, or anything with a side effect.

## Metrics scraping

- With a scraped registry, expose only the scrape endpoint and only on the management port.
- With a pushed registry, expose no scrape endpoint and configure the exporter interval per the platform standard.
- Do not build a project-specific metrics endpoint. The registry already has one.

## Testing operational endpoints

```java
@Test
void healthReadiness_whenApplicationIsRunning_returnsUp() throws Exception {
    this.mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
}
```

- Assert the probe groups return the expected status through the real endpoint, at the integration level.
- Assert that a disabled or unexposed endpoint is not reachable, and that an authenticated-only endpoint returns `401` without a credential. Those tests are what stop an accidental `include: *` or a dropped management chain from merging.
- Assert that a health indicator reports `DOWN` when its dependency fails, and that the response body contains no host, version, or exception detail.
- Do not assert full health payload shape; it changes with configuration and adds no value.
