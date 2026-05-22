# DeadLock — Quick Reference Card

## 🚀 Build & Run
```bash
./gradlew build              # Build APK
./gradlew installDebug       # Install to device
./gradlew runDebug           # Run in emulator
./gradlew test               # Run unit tests
```

## 📱 Key Screens & Routes
```kotlin
Screen.Dashboard     → Home, active session, stats
Screen.LockConfig    → App selection, duration, mode
Screen.FocusSession  → Active countdown, math challenge
Screen.Analytics     → Discipline score, charts
Screen.Settings      → Permissions, toggles
```

## 🏗️ Project Paths
```
domain/model/           → Data models (AppInfo, FocusSession, etc.)
domain/repository/      → Interfaces (SessionRepository, etc.)
data/local/entity/      → Room entities
data/local/dao/         → Database queries
data/repository/        → Repository implementations
service/                → Accessibility, Foreground, Boot
overlay/                → Lock overlay activity
ui/theme/               → Colors, Typography
ui/components/          → Reusable UI parts
ui/*/Screen.kt          → Compose screens
ui/*/ViewModel.kt       → State management
```

## 🔑 Core Classes

### Domain Models
```kotlin
FocusSession        → Active lock session with timer
FocusMode           → Enum: Soft, DeepWork, Monk, Detox, Exam
AppInfo             → Installed app with selection state
AnalyticsEvent      → Logged event (started, blocked, etc.)
UserSettings        → Preferences (cooldown, features)
```

### Repositories (Interfaces)
```kotlin
SessionRepository       → startSession, cancelSession, isPackageLocked
AnalyticsRepository     → recordEvent, getSummary
SettingsRepository      → saveSettings, observeSettings
```

### ViewModels
```kotlin
DashboardViewModel      → Active session + analytics
LockConfigViewModel     → App list, duration, mode selection
FocusSessionViewModel   → Timer, math challenge
AnalyticsViewModel      → Load & refresh stats
SettingsViewModel       → Permissions, settings
```

### Services
```kotlin
DeadLockAccessibilityService  → Monitors foreground app
LockEnforcementService        → Validates timer every 5s
BootReceiver                  → Restarts on reboot
```

## 💾 Database

### Tables
```sql
focus_sessions (id, startTime, endTime, mode, lockedPackages, ...)
analytics_events (id, type, packageName, timestamp, ...)
```

### Key Queries
```kotlin
// Get active session
sessionDao.observeActiveSession()

// Check if package locked
sessionRepository.isPackageLocked(packageName)

// Get analytics for week
analyticsRepository.getSummary()
```

## 🎨 UI Components
```kotlin
GlassCard(glowColor: Color)              → Card with neon border
NeonLabel(text: String, color: Color)    → Uppercase label
StatItem(label, value, color)            → Stat display
DisciplineScoreRing(score: Int)          → Animated ring
MiniBarChart(data, maxValue)             → Bar chart
```

## 🎯 State Flow Pattern
```kotlin
// In ViewModel
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

fun updateState() {
    _uiState.update { it.copy(field = newValue) }
}

// In Composable
val state by viewModel.uiState.collectAsState()
Text(state.value)
```

## 🔐 Permissions Required
```xml
android.permission.BIND_ACCESSIBILITY_SERVICE
android.permission.SYSTEM_ALERT_WINDOW
android.permission.POST_NOTIFICATIONS
android.permission.QUERY_ALL_PACKAGES
android.permission.RECEIVE_BOOT_COMPLETED
```

## 🧪 Debug Commands
```bash
adb logcat | grep DeadLock          # View logs
adb shell dumpsys accessibility     # Check service status
adb shell am force-stop com.ingray.deadlock  # Force kill
adb shell pm grant com.ingray.deadlock [perm]  # Grant permission
```

## 📊 Event Types
```kotlin
SESSION_STARTED         → User began focus
SESSION_COMPLETED       → Timer expired
DISTRACTION_ATTEMPT     → Locked app opened
BYPASS_ATTEMPT          → Settings manipulation
APP_BLOCKED             → Overlay shown
UNLOCK_ATTEMPT          → Emergency unlock
REBOOT_RECOVERY         → Recovered from reboot
```

## 🎨 Color Palette
```kotlin
NeonCyan    = Color(0xFF00D4FF)     // Primary action
NeonRed     = Color(0xFFFF3864)     // Danger, blocked
NeonGreen   = Color(0xFF00FF87)     // Success, active
NeonPurple  = Color(0xFF7B2FBE)     // Secondary
NeonAmber   = Color(0xFFFFB800)     // Warning, info
BackgroundDeep = Color(0xFF050508)  // Base black
SurfaceDark = Color(0xFF0F0F1A)     // Card background
```

## ⚙️ Hilt Injection Patterns
```kotlin
// In service/activity
@AndroidEntryPoint
class MainActivity : ComponentActivity() { }

// In ViewModel
@HiltViewModel
class MyViewModel @Inject constructor(
    private val repo: MyRepository
) : ViewModel() { }

// In Composable
val vm: MyViewModel = hiltViewModel()

// In Module
@Module
@InstallIn(SingletonComponent::class)
object MyModule {
    @Provides
    fun provideMyClass(): MyClass = MyClass()
}
```

## 🔄 Reactive Flow Patterns
```kotlin
// Repository (emit changes)
fun observeActiveSession(): Flow<FocusSession?> =
    dao.observeActiveSession().map { it?.toDomain() }

// ViewModel (collect)
init {
    viewModelScope.launch {
        repo.observeActiveSession().collect { session ->
            _uiState.update { it.copy(session = session) }
        }
    }
}

// Composable (consume)
val state by viewModel.uiState.collectAsState()
```

## 📝 Common Navigation Patterns
```kotlin
// Navigate to screen
navController.navigate(Screen.Dashboard.route)

// Navigate with popUp
navController.navigate(Screen.Analytics.route) {
    popUpTo(Screen.Dashboard.route) { inclusive = false }
}

// Navigate & replace (back stack)
navController.navigate(Screen.FocusSession.route) {
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
    }
    lazyRestoreState = true
}
```

## ⏱️ Timer Math
```kotlin
val durationMs = durationMinutes * 60_000L
val endTime = System.currentTimeMillis() + durationMs
val remainingMs = maxOf(0L, endTime - System.currentTimeMillis())

val hours = remainingMs / 3_600_000
val minutes = (remainingMs % 3_600_000) / 60_000
val seconds = (remainingMs % 60_000) / 1_000
```

## 📊 Discipline Score Formula
```kotlin
val sessionScore = (sessionsWeek * 5).coerceAtMost(40)
val streakScore = (streak * 3).coerceAtMost(30)
val distractionPenalty = (distractionsWeek * 2).coerceAtMost(30)
val score = (sessionScore + streakScore + 30 - distractionPenalty)
    .coerceIn(0, 100)
```

## 🐛 Troubleshooting

### Accessibility Service Not Working
- Check: Settings → Accessibility → DeadLock Focus Guard enabled?
- Check: `DeadLockAccessibilityService.isRunning` property
- Logs: `adb logcat | grep Accessibility`

### Overlay Not Showing
- Check: Overlay permission granted in Settings?
- Check: `Settings.canDrawOverlays(context)` returns true
- Try: Restart app, grant permission again

### Database Query Slow
- Check: No N+1 queries (use @Relation for joins)
- Check: Indexes on frequently queried columns
- Use: `@Query` instead of LazyColumn queries

### Memory Leak Detected
- Check: `viewModelScope.launch` (not `GlobalScope`)
- Check: No static references to Context
- Check: Hilt scope lifecycle (SingletonComponent, etc.)

### Timer Not Persisting After Reboot
- Check: `BootReceiver` declared in manifest
- Check: `RECEIVE_BOOT_COMPLETED` permission granted
- Check: Database query returns session correctly

## 📚 Documentation Files
- `README.md` — Full overview
- `DEVELOPER_GUIDE.md` — Setup & architecture
- `PROJECT_SUMMARY.md` — Comprehensive spec
- `IMPLEMENTATION_CHECKLIST.md` — Component list
- This file — Quick reference

## 🔗 Key Dependencies
```gradle
androidx.compose.ui:ui
androidx.compose.material3:material3
androidx.navigation:navigation-compose
com.google.dagger:hilt-android
androidx.room:room-runtime
androidx.datastore:datastore-preferences
org.jetbrains.kotlinx:kotlinx-coroutines-android
```

## 🎯 Development Workflow
1. Make change in source file
2. Build: `./gradlew build`
3. Install: `./gradlew installDebug`
4. Test on device
5. Check logs: `adb logcat`
6. Debug in Android Studio (Run → Debug)

## 🚨 Common Errors & Fixes

| Error | Cause | Fix |
|-------|-------|-----|
| `Unresolved ref` | Missing import | Import class/function |
| `No accessible constructor` | Hilt injection missing | Add @Inject constructor |
| `Flow collector not available` | Wrong scope | Use viewModelScope.launch |
| `RecreationException` | State not serializable | Use parcelable for navigation args |
| `Accessibility not working` | Service not enabled | User must enable in Settings |

---

**DeadLock**: Discipline through design. 🔒⚡
