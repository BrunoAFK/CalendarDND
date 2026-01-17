# Testing

Testing strategy and commands for Calendar DND.

## Test Commands

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.brunoafk.calendardnd.domain.engine.AutomationEngineTest"

# Run tests with coverage
./gradlew testDebugUnitTest jacocoTestReport

# Run instrumented tests (requires device)
./gradlew connectedAndroidTest

# Run lint checks
./gradlew lint
```

## Test Structure

```
app/src/
├── test/                    # Unit tests (JVM)
│   └── java/
│       └── com/brunoafk/calendardnd/
│           └── domain/      # Domain layer tests
└── androidTest/             # Instrumented tests (device)
    └── java/
        └── com/brunoafk/calendardnd/
```

## Unit Tests

Unit tests run on JVM without Android dependencies.

### Domain Layer Tests

The domain layer is pure Kotlin, making it highly testable:

**AutomationEngine Tests**:
- Automation disabled behavior
- Permission missing behavior
- Active DND window handling
- User suppression detection
- No active window handling

**MeetingWindowResolver Tests**:
- Single meeting window
- Overlapping meetings merged
- Back-to-back meetings merged
- Non-overlapping meetings separate
- Empty meeting list

### Example Test

```kotlin
class AutomationEngineTest {

    @Test
    fun `when automation disabled, should disable DND if app owns it`() {
        val input = EngineInput(
            automationEnabled = false,
            dndSetByApp = true,
            // ... other fields
        )

        val output = AutomationEngine.run(input)

        assertEquals(Decision.DISABLE_DND, output.decision)
    }
}
```

## Instrumented Tests

Run on device or emulator. Test Android-specific functionality:

- Permission flows
- DataStore operations
- WorkManager scheduling
- Compose UI

```bash
# Run all instrumented tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.brunoafk.calendardnd.ExampleInstrumentedTest
```

## Manual Testing

### Acceptance Test Checklist

Test these scenarios before release:

**Setup Flow**:
- [ ] Fresh install shows onboarding
- [ ] All permissions can be granted
- [ ] Calendar selection works
- [ ] Privacy screen shown

**Core Automation**:
- [ ] DND activates when meeting starts
- [ ] DND deactivates when meeting ends
- [ ] Back-to-back meetings stay silent
- [ ] User override is respected

**Filters**:
- [ ] Calendar filter excludes unselected calendars
- [ ] Busy-only filter works
- [ ] All-day filter works
- [ ] Minimum duration filter works
- [ ] Keyword filter works

**Settings**:
- [ ] All settings persist after app restart
- [ ] Language change applies immediately
- [ ] DND mode selection works

**Background**:
- [ ] Alarms fire correctly
- [ ] Survives device restart
- [ ] Works after time zone change

### Testing with Mock Events

1. Create a test calendar in your calendar app
2. Add events with various configurations:
   - Short events (5 min)
   - Long events (2 hours)
   - Back-to-back events
   - Overlapping events
   - All-day events
   - Busy vs Free events
3. Set events to start in 2-3 minutes
4. Observe DND behavior

### Debug Tools

Use built-in debug tools for testing:

1. **Debug Logs**: Settings > Debug Tools > Debug Logs
   - View engine decisions
   - Check event detection
   - Verify timing

2. **Run Engine Now**: Settings > Debug Tools
   - Force immediate engine execution
   - Test without waiting for alarm

3. **Debug Overlay**: Settings > Debug Tools
   - See real-time state on screen

## Mocking

### Calendar Repository Mock

For unit testing without real calendar:

```kotlin
class FakeCalendarRepository : ICalendarRepository {
    var activeInstances = emptyList<CalendarInstance>()
    var nextInstance: CalendarInstance? = null

    override suspend fun getActiveInstances() = activeInstances
    override suspend fun getNextInstance() = nextInstance
}
```

### Test Fixtures

Common test data patterns:

```kotlin
object TestFixtures {
    fun createMeeting(
        startMs: Long = System.currentTimeMillis(),
        durationMinutes: Int = 30,
        busy: Boolean = true
    ) = CalendarInstance(
        id = 1L,
        calendarId = 1L,
        title = "Test Meeting",
        startMs = startMs,
        endMs = startMs + durationMinutes * 60_000,
        isBusy = busy,
        isAllDay = false
    )
}
```

## Code Coverage

Generate coverage report:

```bash
./gradlew testDebugUnitTest jacocoTestReport
```

Report location: `app/build/reports/jacoco/`

## Lint

Static analysis for code quality:

```bash
# Run lint
./gradlew lint

# View report
open app/build/reports/lint-results-debug.html
```

## CI Integration

Recommended CI workflow:

```yaml
steps:
  - name: Run tests
    run: ./gradlew test

  - name: Run lint
    run: ./gradlew lint

  - name: Build
    run: ./gradlew assembleDebug
```

## Troubleshooting Tests

### Tests Not Finding Classes

```bash
./gradlew clean test
```

### Instrumented Tests Failing

1. Check device is connected: `adb devices`
2. Check device has enough storage
3. Try uninstalling app first: `adb uninstall com.brunoafk.calendardnd`

### Flaky Tests

For time-dependent tests, use fixed timestamps:

```kotlin
// Don't do this
val now = System.currentTimeMillis()

// Do this
val fixedTime = 1700000000000L  // Fixed timestamp for tests
```
