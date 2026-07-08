# My Presence — Production Architecture

## Architectural Overview

**Pattern:** Clean Architecture + MVVM + Unidirectional Data Flow (UDF)
**UI Layer:** Jetpack Compose + Material 3 Expressive + Glassmorphism
**DI:** Hilt
**Async:** Kotlin Coroutines + Flow
**Networking:** Ktor (OkHttp engine) with certificate pinning
**Persistence:** Room + DataStore + EncryptedSharedPreferences (Android Keystore)
**Image Loading:** Coil 3
**State Management:** StateFlow (screen state) + UDF events (user actions)

```
┌─────────────────────────────────────────────────┐
│                  UI Layer                        │
│  Composable Screens → ViewModels (StateFlow)     │
├─────────────────────────────────────────────────┤
│                Domain Layer                      │
│  Use Cases → Repository Interfaces → Models      │
├─────────────────────────────────────────────────┤
│                Data Layer                        │
│  Repository Impl → Data Sources                  │
│  (Discord API / Gateway / Room / DataStore / ESP)│
├─────────────────────────────────────────────────┤
│            Security Layer                        │
│  Cert Pinning / Root Detect / Emulator Detect    │
│  Anti-Tamper / Screenshot Blocker / ProGuard    │
└─────────────────────────────────────────────────┘
```

## Key Design Decisions

1. **Single module for speed**, but packages mirror multi-module structure for easy modularization later.
2. **UDF** — all state changes go through ViewModels; screens never mutate state directly.
3. **No hacks** — if Discord doesn't officially support mobile Rich Presence via Gateway with Bearer tokens, the app documents the limitation rather than faking it.
4. **Glassmorphism** — build a reusable glass composable using `drawBehind` + blur + translucent colors. Used on cards, sheets, nav bar.
5. **Animations** — spring-based physics as default motion; Lottie for complex illustrations; custom SharedElementTransition via `Modifier.graphicsLayer`.
6. **3D** — subtle `rotateX`/`rotateY` on cards via pointer input; elevation shadows via `ambientShadowColor` + `spotShadowColor`.
7. **Certificate Pinning** — pin Discord's API certificate public keys via OkHttp `CertificatePinner`. Pins retrieved from Discord's actual cert on initial implementation.
8. **Theme** — forced dark (System UI matches), Black/Charcoal base, Gold accent (#D4AF37), blurple secondary.

## Security Architecture

| Feature | Implementation |
|---------|---------------|
| Cert Pinning | OkHttp CertificatePinner — pin sha256 of Discord API cert |
| Root Detection | Check for su binary, test-keys, Magisk, busybox, dangerous props |
| Emulator Detection | Check Build.* fields, QEMU drivers, known emulator files |
| Anti-Tamper | Verify APK signature matches debug/release keystore |
| Screenshot Blocker | `window.setFlags(SECURE)` on Login + Onboarding |
| Debug Log Removal | `-assumenosideeffects` in ProGuard for `Log.*` |
| R8/Obfuscation | Full R8 with `-repackageclasses`, aggressive obfuscation |
| Secure Storage | EncryptedSharedPreferences (AES-256 GCM) for tokens |
| No Secrets in APK | CLIENT_ID is the only embedded value (public per OAuth2 spec) |

## Theme System

**Base:** `#0A0A0B` (near-black) / `#121214` (dark titanium) / `#1A1A1E` (charcoal)
**Accent:** `#D4AF37` (gold) / variants
**Secondary (Discord):** `#5865F2` (blurple)
**Glass:** translucent surfaces (`<color>.copy(alpha = 0.8f)`) with `BlurEffect` overlay
**Typography:** System font (Google Sans / Roboto) + custom weight/size scale

## Performance Targets

- Recomposition: Avoid unstable types; use `@Stable`/`@Immutable`; key LazyColumn items
- Images: Coil with disk cache + memory cache + appropriate size downscaling
- Lists: LazyColumn with `key`, `contentType`, precomputed sizes
- Gateway: Reconnection with exponential backoff; heartbeat on correct interval
- Battery: WakeLock only during active presence; release on disconnect

## Known Discord Limitations

1. **Rich Presence via OAuth2 Bearer tokens on Gateway** — not an officially documented mobile use case. Presence updates may not display on the user's Discord profile as expected. The app sends valid OP 4 payloads but Discord's behavior with Bearer tokens on Gateway is undefined.
2. **OAuth2 `rpc` and `gateway` scopes** — not officially documented for mobile apps. Only `identify` is used.
3. **No BOT token** — the app uses OAuth2 Bearer tokens, which have limited Gateway capabilities compared to Bot tokens.
