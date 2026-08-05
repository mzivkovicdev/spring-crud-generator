# Controller Slice Test Examples

Use these examples for focused Spring MVC controller tests. Apply every rule from `../SKILL.md`,
`spring-boot-patterns`, `modern-java-21`, and `project-naming-conventions`. Imports are omitted.

## Contents

- [Controller MVC slice excerpt](#controller-mvc-slice-excerpt)
- [Coverage expectations](#coverage-expectations)
- [Rejected controller tests](#rejected-controller-tests)

## Controller MVC slice excerpt

Use the mock-bean mechanism supported by the inspected Spring version. This example uses
`@MockitoBean`; preserve the established equivalent on an older supported project instead of
changing framework versions solely for the test. It is an excerpt, not the complete required test
set for `UserController`.

```java
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ApiExceptionHandler.class)
class UserControllerTest {

    private static final String USERS_PATH = "/api/v1/users";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    UserControllerTest(
            @Autowired final MockMvc mockMvc,
            @Autowired final ObjectMapper objectMapper) {

        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    void usersPost_whenRequestIsValid_returnsCreatedUser() throws Exception {
        final UserCreateTO request = UserTestData.validUserCreateTO();
        final UserDomain createdUser = UserTestData.createdUserDomain(request);
        when(this.userService.create(
                request.username(), request.email(), request.password()))
                .thenReturn(createdUser);

        this.mockMvc.perform(post(USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "%s/%d".formatted(USERS_PATH, createdUser.id())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdUser.id()))
                .andExpect(jsonPath("$.username").value(createdUser.username()))
                .andExpect(jsonPath("$.email").value(createdUser.email()));

        verify(this.userService).create(
                request.username(), request.email(), request.password());
    }

    @Test
    void usersPost_whenRequestIsInvalid_returnsValidationProblemWithoutDelegating()
            throws Exception {

        final UserCreateTO request = UserTestData.userCreateTOWithInvalidEmail();

        this.mockMvc.perform(post(USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(this.userService);
    }

    @Test
    void usersUserIdGet_whenUserDoesNotExist_returnsNotFoundProblem() throws Exception {
        final Long userId = UserTestData.userId();
        when(this.userService.getById(userId))
                .thenThrow(new ResourceNotFoundException("User", userId));

        this.mockMvc.perform(get("%s/{userId}".formatted(USERS_PATH), userId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        verify(this.userService).getById(userId);
    }
}
```

Focused MVC slice tests do not exercise or verify the Spring Security filter chain. Disabling filters
here is the project's deliberate test-level boundary, not a workaround for a failing security test.
Do not use `@WithMockUser`, mock tokens, authority values, or CSRF in this slice.
Full application integration tests own security verification. Ensure the project's
`@RestControllerAdvice`, JSON customization, converters, and argument resolvers required by the
public contract are included in the slice. Import only focused MVC configuration that the slice does
not discover automatically.

## Coverage expectations

For every controller, add at least one successful test for each handler and every applicable
validation, serialization, status, header, and error-contract case. Mock downstream
services so the test proves the MVC boundary and delegation only. Assert both the response and the
exact service input when delegation is part of the contract; assert no service interaction when MVC
validation rejects the request.

Keep the corresponding full application integration tests. Repeating an important route at both
levels is intentional when the slice proves MVC behavior and the integration test proves real wiring,
transactions, persistence, migrations, and committed state.

## Rejected controller tests

```java
// Wrong: direct construction does not prove Spring MVC routing, validation, or error handling.
class UserControllerTest {

    private final UserController controller = new UserController(mock(UserService.class));
}
```

```java
// Wrong: full application integration is not the required focused controller slice.
@SpringBootTest
class UserControllerTest {
}
```
