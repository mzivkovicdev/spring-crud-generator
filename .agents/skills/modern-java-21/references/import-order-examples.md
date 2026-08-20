# Import order examples

Use this reference when an import block is ambiguous, when a tool disagrees with the required order,
or when checking an existing file. The rule itself, including the seven groups and their order, is
in `../SKILL.md`; this file only shows it applied.

A wildcard import is the one violation that cannot be corrected mechanically: expanding it requires
knowing which types the file actually references, so replace it by listing those types explicitly
rather than by deleting the line.

## Correct default ordering

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.validation.Valid;

import javax.crypto.Cipher;
import javax.sql.DataSource;

import com.acme.customer.Customer;
import com.acme.customer.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.s3.S3Client;

```

## Incorrect

Every line below violates a different part of the rule; the trailing comment names which.

```java
import java.util.*;                     // wildcard
import org.junit.jupiter.api.Test;
import java.time.Instant;               // not sorted
import com.acme.customer.Customer;
import java.util.Optional;              // duplicate java.util group
import static org.mockito.Mockito.*;    // wildcard static import
import java.time.Clock;                 // unused import
```
