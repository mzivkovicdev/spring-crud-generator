# Unit Test Examples

Use these examples for focused Java tests that do not load Spring. Apply every rule from
`../SKILL.md`, `modern-java-21`, and `project-naming-conventions`. Imports are omitted.

## Contents

- [Service unit test](#service-unit-test)
- [Test-data factory](#test-data-factory)
- [Rejected unit tests](#rejected-unit-tests)

## Service unit test

Keep the happy path first in source order, followed by exception cases. The tests remain independent;
the order is for readability only.

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Clock FIXED_CLOCK = UserTestData.fixedClock();

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        this.userService = new UserServiceImpl(
                FIXED_CLOCK,
                this.passwordEncoder,
                this.userRepository);
    }

    @Test
    void create_whenInputIsValid_savesAndReturnsUserDomain() {
        final UserCreateTestData input = UserTestData.validUserCreateData();
        final String passwordHash = UserTestData.passwordHash();
        final UserEntity savedUser = UserTestData.persistedUserEntity(input, passwordHash);

        when(this.passwordEncoder.encode(input.rawPassword())).thenReturn(passwordHash);
        when(this.userRepository.save(any(UserEntity.class))).thenReturn(savedUser);

        final UserDomain result = this.userService.create(
                input.username(), input.email(), input.rawPassword());

        assertThat(result.id()).isEqualTo(savedUser.getId());
        assertThat(result.username()).isEqualTo(input.username());
        assertThat(result.email()).isEqualTo(input.email());
        verify(this.userRepository).save(argThat(user ->
                user.getUsername().equals(input.username())
                        && user.getEmail().equals(input.email())
                        && user.getPasswordHash().equals(passwordHash)));
    }

    @Test
    void getById_whenUserExists_returnsUserDomain() {
        final UserCreateTestData input = UserTestData.validUserCreateData();
        final UserEntity existingUser = UserTestData.persistedUserEntity(
                input, UserTestData.passwordHash());
        when(this.userRepository.findById(existingUser.getId()))
                .thenReturn(Optional.of(existingUser));

        final UserDomain result = this.userService.getById(existingUser.getId());

        assertThat(result.id()).isEqualTo(existingUser.getId());
        assertThat(result.username()).isEqualTo(existingUser.getUsername());
        assertThat(result.email()).isEqualTo(existingUser.getEmail());
    }

    @Test
    void updateById_whenUserExists_savesAndReturnsUpdatedUserDomain() {
        final UserCreateTestData input = UserTestData.validUserCreateData();
        final UserEntity existingUser = UserTestData.persistedUserEntity(
                input, UserTestData.passwordHash());
        final String updatedUsername = UserTestData.updatedUsername();
        final String updatedEmail = UserTestData.updatedEmail();
        when(this.userRepository.findById(existingUser.getId()))
                .thenReturn(Optional.of(existingUser));
        when(this.userRepository.save(existingUser)).thenReturn(existingUser);

        final UserDomain result = this.userService.updateById(
                existingUser.getId(), updatedUsername, updatedEmail);

        final ArgumentCaptor<UserEntity> savedUser = ArgumentCaptor.forClass(UserEntity.class);
        verify(this.userRepository).save(savedUser.capture());

        assertThat(savedUser.getValue().getUsername()).isEqualTo(updatedUsername);
        assertThat(savedUser.getValue().getEmail()).isEqualTo(updatedEmail);
        assertThat(result.id()).isEqualTo(existingUser.getId());
        assertThat(result.username()).isEqualTo(updatedUsername);
        assertThat(result.email()).isEqualTo(updatedEmail);

        verifyNoInteractions(this.passwordEncoder);
    }

    @Test
    void getById_whenUserDoesNotExist_throwsResourceNotFoundException() {
        final Long userId = UserTestData.userId();
        when(this.userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> this.userService.getById(userId));
    }

    @Test
    void updateById_whenUserDoesNotExist_throwsResourceNotFoundExceptionWithoutWriting() {
        final Long userId = UserTestData.userId();
        final UserCreateTestData input = UserTestData.validUserCreateData();
        when(this.userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> this.userService.updateById(userId, input.username(), input.email()));

        verify(this.userRepository, never()).save(any(UserEntity.class));
        verifyNoInteractions(this.passwordEncoder);
    }
}
```

Use the project assertion style consistently. Verify exact persistence fields only when those fields
are the service's responsibility. Do not assert MapStruct internals or repeat the complete mapping in
the test. Every behavioral application service needs direct unit coverage for its decisions, returned
state, exceptions, repository writes, and prohibited interactions where applicable; full integration
coverage does not replace these tests.

## Test-data factory

Keep generator use behind focused scenario methods. This example assumes Instancio is already the
approved project dependency; use Podam or the existing project solution when that is the standard.

```java
record UserCreateTestData(
        String username,
        String email,
        String rawPassword) {
}

final class UserTestData {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final ZoneId UTC = ZoneOffset.UTC;

    private UserTestData() {
    }

    static UserCreateTestData validUserCreateData() {
        return Instancio.of(UserCreateTestData.class)
                .generate(field(UserCreateTestData::username),
                        generator -> generator.string().alphaNumeric().minLength(8).maxLength(20))
                .generate(field(UserCreateTestData::email),
                        generator -> generator.net().email())
                .generate(field(UserCreateTestData::rawPassword),
                        generator -> generator.string().alphaNumeric().length(24))
                .create();
    }

    static Clock fixedClock() {
        return Clock.fixed(NOW, UTC);
    }

    static Long userId() {
        return Instancio.gen().longs().min(1L).get();
    }

    static String passwordHash() {
        return Instancio.gen().string().alphaNumeric().length(60).get();
    }

    static String updatedEmail() {
        return Instancio.gen().net().email().get();
    }

    static String updatedUsername() {
        return Instancio.gen().string().alphaNumeric().minLength(8).maxLength(20).get();
    }

    static UserEntity persistedUserEntity(
            final UserCreateTestData input,
            final String passwordHash) {

        return Instancio.of(UserEntity.class)
                .set(field(UserEntity::getId), userId())
                .set(field(UserEntity::getUsername), input.username())
                .set(field(UserEntity::getEmail), input.email())
                .set(field(UserEntity::getPasswordHash), passwordHash)
                .set(field(UserEntity::getCreatedAt), NOW)
                .create();
    }
}
```

Configure the project's global seed or print the seed on failure. A specific boundary value may be
explicit in its scenario factory, such as an empty username or an amount exactly at the supported
maximum.

## Rejected unit tests

```java
// Wrong: a full application context for service logic.
@SpringBootTest
class UserServiceTest {
}
```

```java
// Wrong: impossible failure, no observable assertion, and arbitrary hardcoded object data.
@Test
void testUser() {
    final UserEntity user = new UserEntity(1L, "x", "x@example.test");
    assertNotNull(user);
}
```
