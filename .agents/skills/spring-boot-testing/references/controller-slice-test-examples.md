# Controller Slice Test Examples

Use these examples for focused Spring MVC controller tests. Apply every rule from `../SKILL.md`,
`spring-boot-patterns`, `modern-java-21`, and `project-naming-conventions`. Imports are omitted.

Snippets are patterns to adapt, not files to copy. They follow the [worked example rules](../../modern-java-21/references/worked-example-rules.md) that `modern-java-21` owns.

## Contents

- [Controller MVC slice excerpt](#controller-mvc-slice-excerpt)
- [Coverage expectations](#coverage-expectations)
- [Rejected controller tests](#rejected-controller-tests)

## Controller MVC slice excerpt

This example uses `@MockitoBean`, which is correct on both supported generations: it is available
from Spring Boot 3.4 onward and is the only option on Spring Boot 4, where `@MockBean` is removed.
On a Spring Boot 3 branch below 3.4 the established equivalent is `@MockBean`; preserve what the
project already uses rather than changing framework versions solely for a test. It is an excerpt,
not the complete required test set for `UserController`.

The slice annotations themselves are unchanged between generations. What changes around them is the
test dependency: on Spring Boot 4 the MVC slice arrives through `spring-boot-starter-webmvc-test`
rather than the single core test starter.

Every constructor dependency of the controller needs a mock bean, including one no shown test
exercises: without it the slice context fails to start. The two handlers below both go through the
application service, so `userService` is declared and left unstubbed; the handlers that call it
directly are covered by tests not shown here.

```java
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private UserManagementApplicationService userManagement;

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
        when(this.userManagement.register(
                request.organizationId(), request.username(), request.email(), request.password()))
                .thenReturn(createdUser);

        this.mockMvc.perform(post(UserController.USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        HttpHeaders.LOCATION,
                        "%s/%d".formatted(UserController.USERS_PATH, createdUser.id())))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(createdUser.id()))
                .andExpect(jsonPath("$.username").value(createdUser.username()))
                .andExpect(jsonPath("$.email").value(createdUser.email()));

        verify(this.userManagement).register(
                request.organizationId(), request.username(), request.email(), request.password());
    }

    @Test
    void usersPost_whenRequestIsInvalid_returnsValidationProblemWithoutDelegating()
            throws Exception {

        final UserCreateTO request = UserTestData.userCreateTOWithInvalidEmail();

        this.mockMvc.perform(post(UserController.USERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ApplicationError.VALIDATION_FAILED.type().toString()));

        verifyNoInteractions(this.userManagement);
    }

    @Test
    void usersUserIdGet_whenUserDoesNotExist_returnsNotFoundProblem() throws Exception {
        final Long userId = UserTestData.userId();
        when(this.userManagement.getProfile(userId))
                .thenThrow(new ResourceNotFoundException("User", userId));

        this.mockMvc.perform(get("%s/{userId}".formatted(UserController.USERS_PATH), userId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ApplicationError.RESOURCE_NOT_FOUND.type().toString()));

        verify(this.userManagement).getProfile(userId);
    }
}
```

The test reuses the route constant and the `ApplicationError` catalog instead of repeating the route
and the problem identifier as literals. `UserController.USERS_PATH` is the code-first form; under
contract-first the same test references the corresponding `ApiPaths` constant, and nothing else
changes, so a route or contract change fails at compile time rather than
in an assertion message. `@MockitoBean` fields are `private` and non-`final` under the fixture
exception in `modern-java-21`; `MockMvc` and `ObjectMapper` remain `final` and constructor-injected.

Focused MVC slice tests do not exercise or verify the Spring Security filter chain. The project's
`addFilters = false` convention excludes every servlet filter from this `MockMvc` slice, so use it to
prove the controller and MVC contract only. Do not use `@WithMockUser`, mock tokens, authority values,
or CSRF here. Full application integration tests own security verification; test another filter
separately when it owns a public contract.

**There is no `@Import(ApiExceptionHandler.class)`, and adding one is a mistake worth naming.**
`@WebMvcTest` already includes `@ControllerAdvice` beans in the slice, along with JSON
customization, converters, and argument resolvers — that is what makes the error-contract assertions
above work. Importing the advice explicitly is not merely redundant: it teaches that the slice does
not pick up the advice on its own, and the next person writes a test that imports it and passes for
the wrong reason, or omits it from a slice where it was genuinely needed. Import only focused MVC
configuration the slice does **not** discover automatically, and confirm that a given class is in
that category before importing it.

## Coverage expectations

For every controller, add at least one successful test for each handler and every applicable
validation, serialization, status, header, and error-contract case. Mock downstream
services so the test proves the MVC boundary and delegation only. Assert both the response and the
exact service input when delegation is part of the contract; assert no service interaction when MVC
validation rejects the request. Assert the RFC 9457 `type` URI for every error case; there is no
`code` member in the body.

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
