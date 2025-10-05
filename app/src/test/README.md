# HexViewer Unit Tests

## Overview

This directory contains comprehensive unit tests for the HexViewer Android application. The tests focus on core business logic, utility functions, and critical data structures to ensure code correctness, especially after memory leak fixes.

## Test Structure

```
app/src/test/
├── java/com/galaxyjoy/hexviewer/
│   ├── models/              # Model layer tests
│   │   ├── LineEntryTest.java
│   │   ├── FileDataTest.java
│   │   ├── RecentlyOpenedTest.java
│   │   └── ... (other model tests)
│   ├── util/                # Utility layer tests
│   │   ├── SysHelperTest.java
│   │   ├── memory/
│   │   │   └── MemoryMonitorTest.java
│   │   └── io/
│   │       └── ... (I/O tests)
│   └── ui/                  # UI layer tests
│       └── undoredo/
│           ├── UnDoRedoTest.java
│           └── commands/
│               └── ... (command tests)
└── resources/
    └── robolectric.properties
```

## Running Tests

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests "com.galaxyjoy.hexviewer.models.LineEntryTest"
```

### Run Tests with Coverage
```bash
./gradlew testDebugUnitTest jacocoTestReport
```

### Run Tests for Specific Flavor
```bash
./gradlew testDevDebugUnitTest
./gradlew testProductionReleaseUnitTest
```

## Test Coverage Goals

- **Models Package**: 90%+ coverage
- **Utilities Package**: 80%+ coverage
- **UnDoRedo Package**: 95%+ coverage (critical functionality)
- **Overall**: 70%+ coverage

## Test Frameworks Used

### JUnit 4
Primary testing framework for writing and running tests.

### Mockito
Mocking framework for creating test doubles:
- Mock Android components (Activity, Context, etc.)
- Verify method interactions
- Stub method returns

### Robolectric
Android testing framework that allows running tests on JVM:
- Simulates Android framework classes
- Provides shadow implementations
- Faster than instrumented tests

### Google Truth
Fluent assertion library for more readable test assertions:
```java
assertThat(result).contains("expected");
assertThat(list).hasSize(5);
```

## Test Categories

### 1. Model Tests

#### LineEntryTest
Tests the core LineEntry model that represents a line in the hex viewer.
- Constructor validation
- Getter/setter methods
- Deep copy functionality
- Edge cases (null, empty data)

#### FileDataTest
Tests file metadata and state management.
- File creation and initialization
- Sequential file handling
- Offset management
- State flags (isNotFound, isAccessError)

#### RecentlyOpenedTest
Tests recently opened file list management.
- Adding/removing files
- Persistence across app restarts
- Duplicate handling
- Encoding/decoding file data

### 2. Utility Tests

#### SysHelperTest
Tests hex conversion and buffer formatting utilities.
- Hex string to byte array conversion
- Buffer formatting (Wireshark-like display)
- Size to human-readable conversion
- Validation methods
- Character processing

#### MemoryMonitorTest
Tests memory monitoring functionality (critical for leak prevention).
- Start/stop monitoring
- Threshold detection
- Memory info calculation
- Listener callbacks
- Auto-close support

### 3. UnDoRedo Tests

#### UnDoRedoTest
Tests the undo/redo stack management (critical functionality).
- Command insertion
- State change tracking
- Clear/reset operations
- Multiple command types

## Best Practices

### Test Naming Convention
```java
should_ExpectedBehavior_When_StateUnderTest()
```

Example:
```java
@Test
public void should_ReturnTrue_When_HexLineIsValid() {
    // Test implementation
}
```

### Test Structure (AAA Pattern)
```java
@Test
public void should_DoSomething_When_Condition() {
    // Arrange: Set up test data
    String input = "test data";

    // Act: Execute the method under test
    String result = methodUnderTest(input);

    // Assert: Verify the expected outcome
    assertEquals("expected", result);
}
```

### Using @Before and @After
```java
@Before
public void setUp() {
    // Initialize test fixtures
    testObject = new TestObject();
}

@After
public void tearDown() {
    // Clean up resources
    if (testObject != null) {
        testObject.cleanup();
    }
}
```

### Mocking Android Components
```java
@Mock
private Context mockContext;

@Before
public void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mockContext.getString(anyInt())).thenReturn("test string");
}
```

## Common Patterns

### Testing with Robolectric
```java
@RunWith(RobolectricTestRunner.class)
public class MyTest {
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
    }
}
```

### Testing Async Operations
```java
@Test
public void should_HandleAsync_When_OperationCompletes() {
    AtomicBoolean cancel = new AtomicBoolean(false);

    // Perform async operation
    asyncMethod(cancel);

    // Use Robolectric's shadow looper to advance time
    ShadowLooper.idleMainLooper();

    // Assert results
    assertTrue(operationCompleted);
}
```

### Testing Exceptions
```java
@Test(expected = IllegalArgumentException.class)
public void should_ThrowException_When_InvalidInput() {
    methodThatThrows(null);
}
```

## Troubleshooting

### Robolectric SDK Mismatch
If you encounter SDK version errors, update `robolectric.properties`:
```properties
sdk=34
```

### Mock Injection Failures
Ensure you call `MockitoAnnotations.openMocks(this)` in `@Before`:
```java
@Before
public void setUp() {
    MockitoAnnotations.openMocks(this);
}
```

### Resource Not Found
Make sure `robolectric.properties` is in `src/test/resources/`.

## Continuous Integration

These tests are designed to run in CI environments:
- Fast execution (no emulator required)
- Deterministic results
- Comprehensive coverage reports

## Contributing

When adding new features:
1. Write tests first (TDD approach)
2. Ensure tests pass before committing
3. Maintain coverage thresholds
4. Follow naming conventions
5. Add JavaDoc comments explaining what the test validates

## Resources

- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://site.mockito.org/)
- [Robolectric Documentation](http://robolectric.org/)
- [Google Truth Documentation](https://truth.dev/)
- [Android Testing Guide](https://developer.android.com/training/testing)
