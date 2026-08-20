# Scheduler Test Examples

Use these examples whenever scheduled work changes. Apply every rule from `../SKILL.md`,
`spring-boot-patterns`, `modern-java-21`, and `project-naming-conventions`; apply
`spring-data-jpa` and `application-security` when the job touches those boundaries. Imports are
omitted.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

- [Scheduler unit test](#scheduler-unit-test)
- [Scheduler integration test](#scheduler-integration-test)
- [Additional scheduler cases](#additional-scheduler-cases)
- [Rejected scheduler tests](#rejected-scheduler-tests)

## Scheduler unit test

Keep the `@Scheduled` component thin and invoke it directly as a plain Java object. Test the happy
path before the failure behavior defined by the job contract. Test the delegated business service
separately for its own decisions.

```java
@ExtendWith(MockitoExtension.class)
class ExpiredReservationCleanupJobTest {

    @Mock
    private ReservationCleanupService reservationCleanupService;

    private ExpiredReservationCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        this.cleanupJob = new ExpiredReservationCleanupJob(this.reservationCleanupService);
    }

    @Test
    void cleanUpExpiredReservations_whenTriggered_delegatesCleanup() {
        this.cleanupJob.cleanUpExpiredReservations();

        verify(this.reservationCleanupService).removeExpiredReservations();
    }

    @Test
    void cleanUpExpiredReservations_whenCleanupFails_propagatesFailure() {
        final ReservationCleanupException failure = ReservationTestData.cleanupFailure();
        doThrow(failure)
                .when(this.reservationCleanupService)
                .removeExpiredReservations();

        final ReservationCleanupException result = assertThrows(
                ReservationCleanupException.class,
                this.cleanupJob::cleanUpExpiredReservations);

        assertThat(result).isSameAs(failure);
    }
}
```

Adapt the negative case to the actual error policy. If the job deliberately translates, records, or
handles a failure, assert that stable behavior instead of requiring propagation.

## Scheduler integration test

Use the real scheduled trigger and verify a durable effect. The example assumes the project enables
this job through typed configuration, supplies a short test-only delay, and uses an isolated
supported database. Do not annotate the test with `@Transactional`; the scheduler runs on another
thread and must observe committed fixture data.

```java
@SpringBootTest(properties = {
        "jobs.expired-reservation-cleanup.enabled=true",
        "jobs.expired-reservation-cleanup.fixed-delay=100ms"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExpiredReservationCleanupJobIntegrationTest {

    private static final Duration MAXIMUM_WAIT = Duration.ofSeconds(5);

    private final ReservationRepository reservationRepository;

    ExpiredReservationCleanupJobIntegrationTest(
            @Autowired final ReservationRepository reservationRepository) {

        this.reservationRepository = reservationRepository;
    }

    @Test
    void cleanUpExpiredReservations_whenSchedulerIsEnabled_removesExpiredReservation() {
        final ReservationEntity expiredReservation = this.reservationRepository.saveAndFlush(
                ReservationTestData.expiredReservationEntity());
        final Long reservationId = expiredReservation.getId();

        await()
                .atMost(MAXIMUM_WAIT)
                .untilAsserted(() -> assertThat(
                        this.reservationRepository.existsById(reservationId)).isFalse());
    }
}
```

Match property names and timing to the actual job. Use the project Awaitility configuration when it
exists. Assert state, an event, or another real effect rather than spying on the scheduled bean; the
test must prove that scheduling, wiring, transaction boundaries, and the job behavior work together.
`@DirtiesContext(AFTER_CLASS)` is a justified exception here because this test intentionally starts a
repeating scheduler; closing the context after the class prevents that scheduler from leaking into a
cached test context. Prefer a project-owned explicit scheduler shutdown when one already exists.

## Additional scheduler cases

Add only contract-relevant cases:

- a disabled job does not run when explicit enablement is part of the configuration contract;
- an invalid cron, zone, or required property fails configuration as designed;
- overlapping executions do not duplicate effects when overlap protection is required;
- distributed locking or a claim protocol works against the shared supported database;
- retries, checkpoints, restart recovery, and partial failure preserve idempotency;
- tenant, authorization, batch, timeout, and observability rules from the owner skills hold.

Keep unrelated schedulers disabled in this test context when they can mutate the same state. Do not
turn optional cases into generic tests when the production job cannot reach those states.

## Rejected scheduler tests

```java
// Wrong: this proves only a manual call, not that Spring scheduling works.
@SpringBootTest
class ExpiredReservationCleanupJobIntegrationTest {

    @Autowired
    private ExpiredReservationCleanupJob cleanupJob;

    @Test
    void cleanUpExpiredReservations() {
        this.cleanupJob.cleanUpExpiredReservations();
    }
}
```

```java
// Wrong: exact sleeps and invocation counts make repeating scheduler tests slow and flaky.
Thread.sleep(1_000L);
verify(cleanupJob, times(10)).cleanUpExpiredReservations();
```
