# DeadLock — Production-Grade Anti-Distraction Android App

## Overview

**DeadLock** is an advanced, military-grade focus enforcement Android application designed to temporarily lock apps or the entire device with **extremely high resistance to bypassing**. The app enforces irreversible timers and prevents easy workarounds through multiple technical layers.

### Core Philosophy
- **Strict**: No easy outs, no mercy.
- **Futuristic**: Dark, cyberpunk UI with neon accents.
- **Minimal**: Distraction-free design.
- **Irreversible**: Once locked, commitment is enforced.

---

## Features

### 1. **App Locking**
- Select any installed apps
- Lock for specified duration
- Prevent access completely via overlay blocking screen
- Real-time app detection and interception

### 2. **Hard Focus Timer**
- Irreversible countdown timer
- No pause functionality
- No easy cancellation
- Survives device reboot

### 3. **Focus Modes**
- **Soft Focus**: Block selected apps only
- **Deep Work**: Only productivity apps allowed
- **Monk Mode**: Only calls and notes
- **Dopamine Detox**: Blocks social media, video content, short-form apps
- **Exam Mode**: Only study and reference apps

### 4. **Bypass Resistance System**
Detects and punishes:
- App uninstall attempts → timer extension
- Force stop attempts → warning + analytics
- Settings navigation abuse → bypass penalty
- Accessibility service disable → reboot recovery
- Overlay visibility attempts → app block enforcement
- Rapid app switching → distraction counter increment

### 5. **Productivity Analytics**
- Daily/weekly focus time tracking
- Distraction attempt logging
- Bypass attempt detection & logging
- Streak counting (consecutive focus days)
- Discipline score (0–100)
- Most blocked apps ranking
- Historical session data

### 6. **Emergency Unlock System**
- Math challenge to prevent accidental unlocks
- Configurable cooldown period
- Unlock attempts logged and penalized
- Accountability email notifications (future)

### 7. **Bedtime Lockdown**
- Scheduled device restrictions
- Configurable bedtime hours
- Only emergency calls allowed
- Notification suppression

---

## Technical Architecture

```
DeadLock/
├── domain/
│   ├── model/              # Domain entities (FocusSession, LockedApp, etc.)
│   └── repository/         # Repository interfaces
├── data/
│   ├── local/              # Room database
│   │   ├── entity/
│   │   ├── dao/
│   │   └── DeadLockDatabase
│   └── repository/         # Repository implementations
├── di/                     # Hilt dependency injection
├── service/                # Android services
│   ├── DeadLockAccessibilityService
│   ├── LockEnforcementService
│   └── BootReceiver
├── overlay/                # Lock overlay activity
├── ui/
│   ├── theme/              # Color, typography
│   ├── components/         # Reusable UI components
│   ├── navigation/         # Navigation routes
│   ├── dashboard/          # Dashboard screen
│   ├── lock/               # Lock configuration
│   ├── session/            # Active session view
│   ├── analytics/          # Analytics screen
│   └── settings/           # Settings screen
├── MainActivity.kt         # Nav entry point
└── DeadLockApp.kt         # Hilt application
```

### Clean Architecture Principles
- **Domain Layer**: Business logic, interfaces (pure Kotlin)
- **Data Layer**: Implementation details (Room, DataStore, repositories)
- **UI Layer**: Jetpack Compose screens, ViewModels, navigation
- **Dependency Injection**: Hilt for loose coupling

---

## Lock Engine Pipeline

1. **Foreground App Detection**
   - AccessibilityService monitors window state changes
   - Real-time package name extraction
   - Settings navigation detection

2. **Lock Verification**
   - Check if package is in active session's locked list
   - Verify timer hasn't expired
   - Check focus mode restrictions

3. **Restriction Decision**
   - Determine if app should be blocked
   - Check whitelist (productivity apps, etc.)
   - Update distraction analytics

4. **Overlay Launch**
   - Show full-screen lock overlay
   - Display motivational message
   - Show remaining time

5. **Enforcement Monitoring**
   - Detect bypass attempts (settings, uninstall, force stop)
   - Extend timer on penalties
   - Log all attempts

6. **Timer Validation**
   - Background service validates timer every 5 seconds
   - Complete session when time expires
   - Survive reboot via persistent storage

---

## Key Components

### **DeadLockAccessibilityService**
- Monitors app switching and window state changes
- Detects suspicious settings navigation
- Triggers lock overlay for blocked apps
- Records distraction attempts
- Starts LockEnforcementService on connection

### **LockEnforcementService**
- Foreground service that runs continuously
- Updates notification with remaining time
- Validates active session every 5 seconds
- Completes expired sessions
- Ensures enforcement survives background suspension

### **LockOverlayActivity**
- Full-screen, immersive overlay
- Prevents back button press (blocks bypass)
- Shows animated timer countdown
- Displays locked app name
- Shows motivational quotes
- Guards against screenshots

### **BootReceiver**
- Receives `BOOT_COMPLETED` broadcast
- Restarts enforcement service after reboot
- Maintains active sessions across device restart
- Records reboot recovery in analytics

### **Room Database**
- `FocusSessionEntity`: Active and completed sessions
- `AnalyticsEventEntity`: All events (attempts, blocks, completions)
- Persistent storage survives app process death

### **DataStore**
- User settings (emergency unlock, bedtime, feature toggles)
- Encrypted preferences
- Reactive (Flow-based) updates

---

## Security & Bypass Resistance

### **Access Control**
- Accessibility Service reads app activity (API-compliant)
- Device Admin APIs avoided (not consumer-friendly)
- Overlay via SYSTEM_ALERT_WINDOW permission

### **Persistence**
- Timers stored in database with millisecond precision
- Reboot detection via broadcast receiver
- WorkManager backup task (future)

### **Tamper Detection**
- Accessibility service disable → recorded event
- Settings navigation abuse → distraction penalty
- App uninstall attempts → overlay enforcement
- Rapid app switching → analytics logging

### **Graceful Degradation**
- Works on Android 26+
- Handles accessibility service denial gracefully
- Falls back to overlay-only on unsupported devices
- Warns users of missing permissions

---

## UI Design Language

### **Color Palette**
- **Neon Cyan** (`#00D4FF`): Primary action, focus state
- **Neon Red** (`#FF3864`): Danger, bypass attempts
- **Neon Green** (`#00FF87`): Success, active sessions
- **Neon Purple** (`#7B2FBE`): Secondary actions
- **Neon Amber** (`#FFB800`): Warnings, info
- **Background Deep** (`#050508`): Base layer
- **Surface Dark** (`#0F0F1A`): Card backgrounds

### **Components**
- **GlassCard**: Frosted glass morphism with neon borders
- **NeonLabel**: Uppercase, wide-letterspaced labels
- **StatItem**: Focus stat displays
- **DisciplineScoreRing**: Animated circular progress
- **MiniBarChart**: Weekly data visualization

### **Typography**
- Bold, extralight weights for contrast
- Wide letter spacing for authority
- Sans-serif for technical feel

---

## Permissions Required

### **Mandatory**
- `BIND_ACCESSIBILITY_SERVICE` — app monitoring
- `SYSTEM_ALERT_WINDOW` — overlay window
- `POST_NOTIFICATIONS` — foreground service notification
- `PACKAGE_USAGE_STATS` — app detection (future)
- `RECEIVE_BOOT_COMPLETED` — reboot recovery

### **Optional** (Graceful degradation)
- Device Admin (not implemented, consumer-unfriendly)
- Modify system settings

---

## Development Setup

### **Requirements**
- Android Studio Giraffe+
- Kotlin 2.2.10+
- Android SDK 26+ (minSdk)
- Gradle 8.x

### **Dependencies**
- **Compose**: Material3, Navigation, Lifecycle
- **Hilt**: Dependency injection
- **Room**: Local persistence
- **DataStore**: Encrypted settings
- **WorkManager**: Background tasks (future)
- **Coroutines**: Async operations

### **Building**
```bash
./gradlew build
./gradlew installDebug
```

### **Running**
```bash
./gradlew runDebug
```

### **Testing**
```bash
./gradlew test          # Unit tests
./gradlew connectedAndroidTest  # Instrumentation tests
```

---

## Usage Flow

### **First Launch**
1. Grant Accessibility Service permission
2. Grant Overlay permission
3. Configure focus preferences

### **Starting a Session**
1. Dashboard → "Start Focus Session"
2. Select apps to lock (or choose focus mode)
3. Pick duration (15, 25, 45, 60, 90, 120 minutes)
4. Tap "INITIATE LOCKDOWN"
5. Session begins — enforcement service starts

### **During Session**
- Locked apps show full-screen overlay
- Real-time timer countdown
- Distraction attempt counter
- Motivational quotes rotate

### **Emergency Unlock** (if enabled)
- Tap "Emergency Unlock"
- Confirm intent warning
- Solve math challenge
- Unlock granted (logged and penalized)

### **After Session**
- Dashboard updates with completion
- Analytics recorded
- Streak updated
- Discipline score adjusted

---

## Performance Optimizations

### **Battery**
- 5-second enforcement check (not constant)
- Minimal foreground service memory
- Efficient database queries
- Lazy analytics updates

### **Memory**
- Single database instance (Singleton)
- ViewModel caching
- Lazy composable recomposition
- Garbage collection friendly

### **Speed**
- Instant overlay rendering
- Non-blocking database calls (coroutines)
- Efficient app detection (cached package lists)
- Animated transitions (not janky)

---

## Future Enhancements

- [ ] Push notifications for accountability partner
- [ ] AI distraction prediction engine
- [ ] Custom app category whitelisting
- [ ] Focus music integration
- [ ] Habit tracking calendar
- [ ] Social accountability leaderboards
- [ ] Device Admin mode (opt-in, rooted)
- [ ] Cloud backup of analytics
- [ ] Advanced biometrics (face recognition unlock)
- [ ] Widget support
- [ ] Wear OS integration

---

## License & Attribution

**DeadLock** is a production-ready focus enforcement application. Built with Kotlin, Jetpack Compose, Hilt, and Room.

---

## Support

For issues, feature requests, or bypass discoveries (security),contact the development team.

**DeadLock**: Where focus isn't a preference—it's the law.
