# Spring cache examples

Worked code for the rules in `../SKILL.md`. Identifiers follow the conventions in
`spring-boot-patterns`; imports are omitted.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

**The cache abstraction itself is unchanged between the supported Spring Boot generations.** The
annotations, the `CacheManager`, `CachingConfigurer`, and the transaction-aware decorator are the
same on both. What differs is the JSON library that serializes values on a store that serializes —
`build-and-dependencies` records that it changes major version between generations, and
[invalidation and consistency](invalidation-and-consistency.md#changing-the-shape-of-a-cached-value)
states what that means for entries written before the upgrade.

## Contents

1. [Cache names are constants](#cache-names-are-constants)
2. [Where the read-through sits](#where-the-read-through-sits)
3. [Single-flight](#single-flight)
4. [Invalidating after commit](#invalidating-after-commit)
5. [Failing open](#failing-open)
6. [Configuration](#configuration)
7. [Rejected code](#rejected-code)

## Cache names are constants

A cache name appears in the annotation, in the configuration, in the eviction, in the meters, and in
the tests. Typed five times, it is five chances to create a second cache nobody notices — an
annotation naming a cache the configuration never configured silently gets the provider's defaults,
which on some providers means no TTL and no bound.

```java
public final class CacheNames {

    public static final String PERMISSIONS_BY_ROLE = "permissions-by-role";
    public static final String PRODUCTS_BY_SKU = "products-by-sku";

    private CacheNames() {
    }
}
```

`project-naming-conventions` owns the name's form. This holder follows the same pattern as the
shared-bound constants `spring-boot-patterns` declares, and lives beside them.

## Where the read-through sits

**Cache at the aggregate service boundary, and cache the domain object.** That is the layer that
owns the read, and the domain object is the value the caller receives anyway.

Two properties of the architecture make this the right place rather than a convention:

- **A domain model is already immutable**, because `spring-boot-patterns` requires it. That satisfies this skill's immutability rule for free, and it is why caching the domain object is safe on a local cache where caching an entity would not be.
- **An entity must not be cached at all.** It is mutable, it carries lazy associations that will not initialize outside the persistence context, and it is a persistence type escaping the boundary that `spring-boot-patterns` closes.

```java
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    // Constructor omitted; the dependency is final and constructor-injected.

    /**
     * Returns a product by tenant and stock-keeping unit.
     *
     * <p>Served from {@code products-by-sku} when present. The value may be up to the cache's
     * recorded staleness budget old; a write through this service invalidates it after commit.
     *
     * @param tenantId owning tenant, part of the cache key
     * @param sku      stock-keeping unit
     * @return the product
     * @throws ResourceNotFoundException when no product exists for the tenant and unit
     */
    @Cacheable(cacheNames = CacheNames.PRODUCTS_BY_SKU, key = "#tenantId + ':' + #sku")
    public ProductDomain getBySku(final Long tenantId, final String sku) {
        return this.productRepository.findByTenantIdAndSku(tenantId, sku)
                .map(ProductDomainMapper.INSTANCE::mapProductEntityToProductDomain)
                .orElseThrow(() -> new ResourceNotFoundException("Product", sku));
    }
}
```

Four things in that method are the rules applied:

- **The tenant is a parameter**, so it is part of the identity rather than ambient state a key expression reaches for. [Cache design](cache-design.md#key-identity) states why that matters.
- **The key expression is explicit**, so a signature change is a diff rather than a silent re-keying.
- **The Javadoc states the staleness**, because a caller reading this method needs to know the value may be old — that is a contract fact, not an implementation detail, and it is exactly the "non-obvious behavior" the Javadoc policy `modern-java-21` owns asks for.
- **The exception is thrown, not cached.** `@Cacheable` does not store a value when the method throws, so a miss for a missing product runs the query every time. When that matters, [negative caching](cache-design.md#negative-caching) is a deliberate design with its own shorter TTL, not something to reach by returning `null`.

**Never annotate a Spring Data repository method.** A repository is a framework-generated proxy;
adding a second proxy to it is fragile, it caches entities, and it puts the cache below the boundary
where `spring-data-jpa` owns behavior. Cache above the repository, in the service.

## Single-flight

```java
@Cacheable(cacheNames = CacheNames.PERMISSIONS_BY_ROLE, sync = true)
public PermissionSetDomain permissionsFor(final String roleCode) {
    return this.permissionDerivation.derive(roleCode);
}
```

`sync = true` makes concurrent callers for the same key wait for one load instead of all running it.
Its limits are part of the decision, not footnotes:

- **It coordinates one JVM.** With several instances, one load per instance still runs. [Stampede](cache-design.md#stampede) states when that is acceptable and what it costs to do better.
- **It applies to one cache only** — the attribute cannot be combined with several cache names — and `unless` does not apply, because the value is stored before the caller sees it.
- **Not every provider implements it natively.** Where it is emulated, verify the behaviour under real concurrency rather than trusting the attribute.
- **The loader now runs while others block on it**, so a slow load holds those threads. Keep the loader inside the request budget `spring-boot-patterns` records, exactly as any other wait.

## Invalidating after commit

Both shapes are correct. Choose one for the project, and record which.

### The after-commit listener

The write publishes an event; the listener evicts. This is the shape to prefer when a write
invalidates more than one cache, because the complete set is visible in one method.

```java
public record ProductChangedEvent(Long tenantId, String sku) {
}
```

```java
@Transactional
public ProductDomain rename(final Long tenantId, final String sku, final String name) {

    final ProductEntity product = this.productRepository.findByTenantIdAndSku(tenantId, sku)
            .orElseThrow(() -> new ResourceNotFoundException("Product", sku));

    product.setName(name);

    this.eventPublisher.publishEvent(new ProductChangedEvent(tenantId, sku));

    return ProductDomainMapper.INSTANCE.mapProductEntityToProductDomain(
            this.productRepository.save(product));
}
```

```java
@Component
public class ProductCacheInvalidationListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProductCacheInvalidationListener.class);

    private final CacheManager cacheManager;

    // Constructor omitted; the dependency is final and constructor-injected.

    /**
     * Evicts every cache holding a value derived from the changed product.
     *
     * <p>Runs after commit, so a concurrent reader cannot repopulate from the pre-write row. A
     * failure here is logged and swallowed: the entry's TTL is what repairs it.
     *
     * @param event the committed product change
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductChanged(final ProductChangedEvent event) {
        try {
            this.evict(CacheNames.PRODUCTS_BY_SKU, event.tenantId() + ":" + event.sku());
        } catch (final RuntimeException failure) {
            LOGGER.atWarn()
                    .addKeyValue("operation", "cache.evict")
                    .addKeyValue("outcome", "failure")
                    .setCause(failure)
                    .log("Cache eviction failed; entry will expire on its TTL");
        }
    }

    private void evict(final String cacheName, final String key) {
        final Cache cache = this.cacheManager.getCache(cacheName);

        if (cache != null) {
            cache.evict(key);
        }
    }
}
```

Why it is written this way:

- **The listener is a thin boundary that delegates**, like a controller — the shape `spring-boot-patterns` requires of every after-commit listener.
- **It catches its own failure**, because an exception thrown in an after-commit listener produces no HTTP error, no rollback, and no caller-side signal. Logging it once at its own boundary is the rule `observability-and-logging` states for exactly this case.
- **It is a `WARN`, not an `ERROR`.** A failed eviction is a known, bounded condition with a repair — the TTL — so it is an expected failure by the definition the error catalog uses. Meter it; do not page on a single occurrence.
- **The event carries the identity, not the entity.** The transaction that produced it has committed and the entity is detached; the key inputs are all the listener needs.

### The transaction-aware cache manager

```java
@Configuration(proxyBeanMethods = false)
class CacheConfiguration {

    /**
     * Wraps the provider's cache manager so writes and evictions are deferred until the
     * surrounding transaction commits.
     *
     * <p>Without this decorator, {@code @CacheEvict} on a transactional method evicts before the
     * commit, and a concurrent reader repopulates the cache from the pre-write row.
     *
     * @param delegate the provider's cache manager
     * @return the transaction-aware cache manager
     */
    @Bean
    CacheManager cacheManager(final CacheManager delegate) {
        return new TransactionAwareCacheManagerProxy(delegate);
    }
}
```

Less code, applied everywhere at once. The cost is that **nothing at the call site says the ordering
is handled**, so this bean becomes configuration nobody may delete — which is precisely the reason
the Javadoc above says what removing it would do. Where the project chooses this shape, the
annotation form is then correct on a transactional method, and that is the only context in which it
is.

## Failing open

```java
@Configuration(proxyBeanMethods = false)
class CacheErrorConfiguration implements CachingConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheErrorConfiguration.class);

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    private static final class LoggingCacheErrorHandler implements CacheErrorHandler {

        @Override
        public void handleCacheGetError(
                final RuntimeException exception, final Cache cache, final Object key) {

            log("get", cache, exception);
        }

        @Override
        public void handleCachePutError(
                final RuntimeException exception,
                final Cache cache,
                final Object key,
                final @Nullable Object value) {

            log("put", cache, exception);
        }

        @Override
        public void handleCacheEvictError(
                final RuntimeException exception, final Cache cache, final Object key) {

            log("evict", cache, exception);
        }

        @Override
        public void handleCacheClearError(final RuntimeException exception, final Cache cache) {
            log("clear", cache, exception);
        }

        private static void log(
                final String operation, final Cache cache, final RuntimeException exception) {

            LOGGER.atWarn()
                    .addKeyValue("operation", "cache." + operation)
                    .addKeyValue("cacheName", cache.getName())
                    .addKeyValue("outcome", "failure")
                    .setCause(exception)
                    .log("Cache operation failed; continuing against the source");
        }
    }
}
```

Rules this implements:

- **The default handler rethrows**, so without this bean a cache outage is an application outage. That is the inversion `../SKILL.md` names.
- **The key is never logged.** It can carry a tenant or a subject identifier, and `application-security` decides what may appear in a log; the cache name is bounded and sufficient to find the problem.
- **A get error and an evict error are not the same event.** A failed read falls through to the source and is harmless; a failed eviction leaves a stale entry that only the TTL repairs. They are logged as separate operations so the meters can separate them, and `observability-and-logging` owns the counter.
- **This handler does not make the cache optional in a test.** A test that asserts the source was not called a second time still fails when the cache is silently erroring — which is correct, and is why the required scenario list includes a test that the operation succeeds *with the cache unavailable*, separately.

## Configuration

TTL and size are configuration, and their form belongs to the chosen provider. What does not vary is
that **every cache declares both, and the values come from the register**.

```yaml
spring:
  cache:
    # Pinned so a transitive dependency cannot change the provider silently.
    type: caffeine
    cache-names:
      - products-by-sku
      - permissions-by-role
```

The provider-specific half — expiry, maximum size, the serializer on a store that serializes — is
declared the way that provider expects, either in properties or through a customizer bean.
[Technology and topology](technology-and-topology.md#the-property-checklist) states which of those
knobs the chosen technology actually has; a design that needs one the provider lacks is a design to
change, not a gap to work around.

Two rules hold whatever the provider:

- **No cache runs on provider defaults.** A cache reached through an annotation naming a cache the configuration never mentions gets whatever the provider does by default, which on the plain in-process manager is unbounded and eternal.
- **Declare the cache names**, so a typo in an annotation fails rather than quietly creating a sixth cache with default behavior.

## Rejected code

```java
// Wrong: self-invocation. The proxy is bypassed, so the cache annotation never applies - the
// method simply runs every time and nothing reports it. modern-java-21 and spring-boot-patterns
// both state this for every proxy annotation; it is listed here because a cache that silently
// does nothing looks exactly like a cache that is working.
public ProductDomain getOrLoad(final Long tenantId, final String sku) {
    return this.getBySku(tenantId, sku);
}
```

```java
// Wrong: an entity in the cache. Mutable, carries lazy associations that cannot initialize
// outside the persistence context, and a persistence type past the boundary.
@Cacheable(cacheNames = CacheNames.PRODUCTS_BY_SKU, key = "#sku")
public ProductEntity findEntity(final String sku) { ... }
```

```java
// Wrong: the cached value is mutable. On a serializing store every caller gets a copy and this
// is harmless; on a local cache the first caller to sort or clear the list has changed what
// every later caller receives. The same code, correct on one technology and not the other.
@Cacheable(cacheNames = CacheNames.PERMISSIONS_BY_ROLE)
public List<String> permissionCodes(final String roleCode) {
    return new ArrayList<>(this.permissionDerivation.derive(roleCode).codes());
}
```

```java
// Wrong: a cached authorization decision. A revocation now takes effect when the entry expires,
// and the check that would have caught it no longer runs.
@Cacheable(cacheNames = "access-decisions", key = "#subjectId + ':' + #documentId")
public boolean mayRead(final Long subjectId, final Long documentId) { ... }
```

```java
// Wrong: no key expression on a multi-parameter method. The generated key composes the arguments
// in declaration order, so adding a parameter re-keys every entry and reordering two of the same
// type produces collisions between different calls. Neither fails at compile time.
@Cacheable(cacheNames = CacheNames.PRODUCTS_BY_SKU)
public ProductDomain getBySku(final Long tenantId, final String sku) { ... }
```

```java
// Wrong: caching in front of a defective query. The N+1 is still there, now with staleness on
// top, and the first cache miss after a deploy pays all of it at once.
@Cacheable(cacheNames = "order-summaries", key = "#orderId")
public OrderSummaryDomain summary(final Long orderId) {
    return this.orderRepository.findById(orderId)
            .map(order -> new OrderSummaryDomain(
                    order.getLines().stream().map(this::describeLine).toList()))
            .orElseThrow();
}
```
