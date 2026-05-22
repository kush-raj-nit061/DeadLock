# DeadLock — Quick Start Guide for Developers

## Project Structure Overview

```
DeadLock/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/ingray/deadlock/
│   │       │   ├── MainActivity.kt          ← App entry point (navigation)
│   │       │   ├── DeadLockApp.kt          ← Hilt application class
│   │       │   ├── domain/                 ← Business logic (interfaces)
│   │       │   ├── data/                   ← Data implementation (Room, DataStore)
│   │       │   ├── di/                     ← Dependency injection (Hilt modules)
│   │       │   ├── service/                ← Android services (accessibility, foreground)
│   │       │   ├── overlay/                ← Lock overlay activity
│   │       │   └── ui/                     ← Compose screens, theme, components
│   │       ├── res/
│   │       │   ├── xml/
│   │       │   │   ├── accessibility_service_config.xml
│   │       │   │   └── data_extraction_rules.xml
│   │       │   └── values/
│   │       │       └── strings.xml
│   │       └── AndroidManifest.xml         ← All permissions, services, receivers
│   ├── build.gradle.kts                    ← Dependencies & build config
│   └── proguard-rules.pro                  ← Minification rules
├── gradle/
│   └── libs.versions.toml                  ← Version catalog
├── build.gradle.kts                        ← Root gradle config
├── settings.gradle.kts                     ← Project setup
├── README.md                               ← Full documentation
└── IMPLEMENTATION_CHECKLIST.md             ← Component checklist
```

## Key Files & Their Purposes

### Core Application
- **MainActivity.kt**: Navigation host, bottom nav bar, 5-screen routing
- **DeadLockApp.kt**: Hilt @HiltAndroidApp, enables dependency injection
- **LockOverlayActivity.kt**: Full-screen immersive lock overlay (can't press back)

### Domain (Pure Kotlin, No Android Dependencies)
- **FocusSession.kt**: Active session model with timer logic
- **FocusMode.kt**: 5 focus modes (Soft, Deep Work, Monk, Detox, Exam)
- **AnalyticsEvent.kt**: Event types for tracking user behavior
- **SessionRepository.kt**: Interface for session management
- **AnalyticsRepository.kt**: Interface for analytics

### Data (Android Framework, Local Storage)
- **DeadLockDatabase.kt**: Room database, contains 2 tables
- **FocusSessionDao.kt**: Session CRUD + queries (observeActiveSession, etc.)
- **AnalyticsEventDao.kt**: Event CRUD + aggregations
- **SessionRepositoryImpl.kt**: Session logic, timer validation, lock checking
- **AnalyticsRepositoryImpl.kt**: Discipline score, streak, weekly stats calculation
- **SettingsRepositoryImpl.kt**: User preferences via DataStore

### Services (Background Execution)
- **DeadLockAccessibilityService.kt**: Monitors foreground app, detects bypass attempts
- **LockEnforcementService.kt**: Foreground service, validates timer every 5 seconds
- **BootReceiver.kt**: Listens for BOOT_COMPLETED, restarts enforcement

### UI (Jetpack Compose)
- **DashboardScreen.kt**: Home page with stats, active session, weekly chart
- **LockConfigScreen.kt**: Session setup (app selection, duration, mode)
- **FocusSessionScreen.kt**: Active session UI with countdown timer, math challenge
- **AnalyticsScreen.kt**: Discipline score, stats, most blocked apps
- **SettingsScreen.kt**: Permissions status, feature toggles

### ViewModels (State Management)
- **DashboardViewModel.kt**: Combines session + analytics streams
- **LockConfigViewModel.kt**: Manages app list, selection, session startup
- **FocusSessionViewModel.kt**: Tracks remaining time, handles math challenge
- **AnalyticsViewModel.kt**: Loads & refreshes analytics summary
- **SettingsViewModel.kt**: Manages settings, checks permissions

### Theme & UI Components
- **Color.kt**: Neon palette (Cyan, Red, Green, Purple, Amber, etc.)
- **Theme.kt**: Dark MaterialTheme (always dark, no light theme)
- **Type.kt**: Typography hierarchy
- **DeadLockComponents.kt**: Reusable UI components (GlassCard, charts, etc.)

---

## How It Works: User Flow

### 1. First Launch
- User grants **Accessibility Service** permission
- User grants **Overlay** permission
- DeadLockAccessibilityService starts monitoring

### 2. Starting a Focus Session
```
Dashboard → "Start Focus Session" 
  → LockConfigScreen (pick apps, duration, mode)
  → Tap "INITIATE LOCKDOWN"
  → LockConfigViewModel.startSession()
    → SessionRepository.startSession() (creates session in DB)
    → Starts LockEnforcementService (foreground)
    → Navigate to FocusSessionScreen
```

### 3. During Session
```
AccessibilityService detects foreground app change
  → SessionRepository.isPackageLocked(packageName)?
    → YES: Show LockOverlayActivity (full-screen block)
    → NO: Allow app normally
  
Distraction attempt detected?
  → SessionRepository.recordDistractionAttempt()
  → AnalyticsRepository.recordEvent(DISTRACTION_ATTEMPT)
  → Update distraction counter on screen

Timer expires?
  → SessionRepository.completeSession()
  → AnalyticsRepository.recordEvent(SESSION_COMPLETED)
  → Back to DashboardScreen
```

### 4. Emergency Unlock (if enabled)
```
User taps "Emergency Unlock"
  → Show confirmation dialog
  → User confirms
  → Show math challenge dialog
  → User solves: ${mathA} + ${mathB} = ?
  → Correct? → SessionRepository.cancelSession()
  → Incorrect? → Show error, try again
  
All unlock attempts logged:
  → AnalyticsRepository.recordEvent(UNLOCK_ATTEMPT)
```

### 5. Device Reboot
```
Device reboots
  → BootReceiver receives BOOT_COMPLETED
  → Calls SessionRepository.getActiveSession()
  → Active session exists? → Restart LockEnforcementService
  → Analytics: recordEvent(REBOOT_RECOVERY)
  → Session continues where it left off
```

---

## Build & Run

### Prerequisites
```bash
# Ensure you have:
# - Android Studio Giraffe or later
# - Android SDK 26+ (minSdk)
# - Kotlin 2.2.10
# - Gradle 8.x
```

### Build
```bash
cd /path/to/DeadLock
./gradlew build          # Full build
./gradlew assembleDebug  # Debug APK only
```

### Install & Run
```bash
./gradlew installDebug   # Install to device
```

### Using Android Studio
1. Open project → `File → Open → DeadLock`
2. Select device/emulator
3. Run → `app` (green play button)

---

## Required Permissions (AndroidManifest.xml)

### Mandatory for Core Functionality
```xml
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```

### User Must Grant at Runtime
1. **Accessibility Service**
   - Settings → Accessibility → DeadLock Focus Guard
   - Toggle ON

2. **Overlay Permission**
   - Settings → Apps & Notifications → Special App Access → Overlay Apps
   - Enable DeadLock

3. **Notification Permission**
   - Granted automatically on Android 13+

---

## Dependency Injection (Hilt)

### How It Works
```kotlin
@AndroidEntryPoint          // Marks activity/service for DI
class MainActivity : ComponentActivity() { ... }

@HiltViewModel             // ViewModel injected dependencies
class DashboardViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val analyticsRepository: AnalyticsRepository
) { ... }

// In hiltViewModel(), Hilt automatically provides instances
val viewModel: DashboardViewModel = hiltViewModel()
```

### Modules (define how to provide dependencies)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): DeadLockDatabase = ...
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
```

---

## Database Schema

### Tables
```sql
CREATE TABLE focus_sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    startTime LONG,
    endTime LONG,
    durationMinutes INTEGER,
    mode TEXT,
    isActive BOOLEAN,
    lockedPackages TEXT,           -- JSON string of package names
    distractionAttempts INTEGER,
    wasCompleted BOOLEAN
);

CREATE TABLE analytics_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    type TEXT,                      -- EventType enum value
    packageName TEXT NULL,
    sessionId LONG NULL,
    timestamp LONG,
    metadata TEXT NULL
);
```

### Queries Used
```kotlin
// Get active session
SELECT * FROM focus_sessions WHERE isActive = 1 LIMIT 1

// Check if package is locked
val packages = session.lockedPackages.split(",")
packageName in packages && session.endTime > now

// Get analytics for week
SELECT * FROM analytics_events WHERE timestamp >= (now - 7 days)

// Count distraction attempts
SELECT COUNT(*) FROM analytics_events 
WHERE type = 'DISTRACTION_ATTEMPT' AND timestamp >= (now - 7 days)

// Get most blocked apps
SELECT packageName, COUNT(*) as count FROM analytics_events
WHERE type = 'APP_BLOCKED' AND timestamp >= (now - 30 days)
GROUP BY packageName ORDER BY count DESC LIMIT 10
```

---

## State Management (Kotlin Flows)

### Reactive Data Flow
```kotlin
// In repository
fun observeActiveSession(): Flow<FocusSession?> =
    sessionDao.observeActiveSession().map { it?.toDomain() }

// In ViewModel
init {
    viewModelScope.launch {
        sessionRepository.observeActiveSession().collect { session ->
            _uiState.update { it.copy(activeSession = session) }
        }
    }
}

// In Composable
val uiState by viewModel.uiState.collectAsState()
Text("${uiState.activeSession?.remainingMillis}")
```

---

## Debugging Tips

### Check Accessibility Service Status
```kotlin
val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
val enabled = am.getEnabledAccessibilityServiceList(...)
    .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
```

### Monitor Foreground App Changes
```kotlin
// In DeadLockAccessibilityService.onAccessibilityEvent()
Log.d("DeadLock", "Foreground: ${event.packageName}")
```

### Check Database State
```kotlin
// Android Studio's Device File Explorer
// Navigate to: data/data/com.ingray.deadlock/databases/deadlock.db
```

### View Logs
```bash
adb logcat | grep DeadLock
```

---

## Performance & Optimization

### Battery
- Accessibility service: minimal overhead (event-driven)
- Enforcement service: 5-second check (not constant)
- Analytics: lazy updates (no real-time streaming)

### Memory
- Single database instance (Singleton)
- Lazy ViewModel creation
- Recomposable only when state changes
- No memory leaks (Hilt handles scope)

### Speed
- Overlay renders instantly (<50ms)
- Database queries indexed
- Non-blocking coroutine calls
- Efficient app detection (cached lists)

---

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist
- [ ] Grant all permissions → app functions
- [ ] Start session → lock active
- [ ] Device reboot → session resumes
- [ ] Navigate settings → bypass logged
- [ ] Emergency unlock → math works
- [ ] Analytics → numbers are correct
- [ ] Dark theme → UI is readable

---

## Common Issues & Solutions

### "Accessibility Service not working"
- User didn't enable in Settings
- App crashed (check logs)
- Android version < 26 (not supported)

### "Overlay not showing"
- Overlay permission not granted
- Device doesn't support SYSTEM_ALERT_WINDOW
- Another app is blocking overlays

### "Timer extends after each bypass"
- Expected behavior ✓ (timer extension penalty is by design)
- Disable in SettingsScreen if desired

### "Session lost after app crash"
- Check database: should persist
- Verify SessionRepository.getActiveSession() returns data
- May need to restart app to resume

---

## Architecture Decision Rationale

### Why Clean Architecture?
- Domain layer is testable (no Android deps)
- Easy to swap implementations
- Supports multiple UIs if needed

### Why Hilt?
- Boilerplate-free DI
- Lifecycle-aware injection
- Works with Compose

### Why Room?
- Type-safe queries
- Automatic migration support
- Coroutine integration

### Why DataStore?
- Replaces SharedPreferences
- Encrypted by default
- Flow-based (reactive)

### Why AccessibilityService?
- API-compliant (no root needed)
- Consumer-friendly (Play Store policy)
- Real-time app detection

### Why Jetpack Compose?
- Modern, declarative UI
- Material3 support
- Animations are smooth
- Tooling is excellent

---

## Future Development

- [ ] WorkManager for scheduling
- [ ] Cloud backup of analytics
- [ ] Push notifications
- [ ] Widget support
- [ ] Custom app categories
- [ ] Advanced unlock methods
- [ ] Social leaderboards
- [ ] AI distraction prediction

---

## Support & Contact

For questions about DeadLock architecture or development:
1. Check README.md for overview
2. Review comments in code
3. Check Hilt/Compose documentation
4. Test locally with connected device

---

**DeadLock**: Strict. Minimal. Irreversible. 🔒
