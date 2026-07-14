<div align="center">

  <img src="app/src/main/res/drawable/ic_app_logo.png" width="112" height="112" alt="My Presence Logo">

  # My Presence

  <p>
    <strong>Turn your Android activity into a sleek, living Discord presence.</strong>
  </p>

  <p>
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
    <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Compose">
    <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
    <img src="https://img.shields.io/badge/Hilt-18181B?style=for-the-badge&logo=hilt&logoColor=white" alt="Hilt">
    <img src="https://img.shields.io/badge/Ktor-FF5722?style=for-the-badge&logo=ktor&logoColor=white" alt="Ktor">
    <img src="https://img.shields.io/badge/Discord%20Gateway-v10-5865F2?style=for-the-badge" alt="Discord Gateway">
  </p>

  <p>
    <a href="#features">Features</a> •
    <a href="#how-it-works">How It Works</a> •
    <a href="#build-and-run">Build</a> •
    <a href="#tech-stack">Tech Stack</a> •
    <a href="#built-by">Built By</a>
  </p>

</div>

---

My Presence is a premium Android experience that detects what you're doing on your phone and turns it into a polished Discord Rich Presence. Built with a modern, dark-first UI and a clean architecture, it feels less like a side project and more like a product you would actually want to live with every day.

## Why this project feels special

- A beautiful, premium interface designed to feel intentional, smooth, and modern.
- Deep Discord integration with a gateway-based presence engine.
- Smart app detection, persistent background behavior, and polished onboarding.
- Built with a production-minded architecture that is easy to extend.

## Features

- ✨ Beautiful dashboard with a premium glassy, modern aesthetic
- 🔍 Automatic app detection using Android usage data
- 🎮 Rich Presence customization with status, details, images, and more
- 🧠 Persistent background presence handling for reliable updates
- 🔐 Secure auth flow and modern app architecture
- 🧩 Clean, modular structure designed for future growth
- 🌙 Dark-first UI crafted to feel polished and immersive

## How it works

1. The app detects your current activity on Android.
2. It builds a presence payload using your selected state and metadata.
3. That payload is sent over Discord Gateway to appear as your live status.

## Screenshots

> Screenshots will be added as the app evolves. The experience is already designed to feel polished and premium on-device.

## Build and run

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17+
- A Discord application with a client ID
- Optional Firebase setup for Google sign-in and analytics

### Setup

```bash
git clone https://github.com/Legendscene/mypresence.git
cd mypresence

cat > secrets.properties << EOF
DISCORD_CLIENT_ID=your_discord_client_id
GOOGLE_WEB_CLIENT_ID=your_google_client_id
DISCORD_BOT_TOKEN=your_bot_token
IMGUR_CLIENT_ID=your_imgur_client_id
EOF
```

Then open the project in Android Studio and run it on a device with API 26+.

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- Hilt
- Room + DataStore
- Ktor + OkHttp
- Coroutines + Flow
- Coil for image loading
- Firebase for auth and analytics
- Discord Gateway v10

## Project structure

```text
com.kyrx.mypresence
├── core/       - DI, gateway, auth, analytics
├── data/       - API clients, repositories, gateway impl
├── domain/     - models, use cases, repository contracts
├── feature/    - screens and UI flows
├── service/    - foreground presence service
└── ui/         - shared components, theme, navigation
```

## Built by

### Pranay
- Portfolio: [https://pranayprajapati.netlify.app/](https://pranayprajapati.netlify.app/)
- Discord: [https://discord.com/users/1189590421307924562](https://discord.com/users/1189590421307924562)
- GitHub: [https://github.com/Legendscene](https://github.com/Legendscene)
- Dev Community: [https://dev.to/legendscene](https://dev.to/legendscene)

## Contributors

### Arnav
- Portfolio: [https://arnavbadola.vercel.app](https://arnavbadola.vercel.app)
- GitHub: [https://github.com/theycallmearnav](https://github.com/theycallmearnav)
- Discord: [https://discord.com/users/1374981345880707122](https://discord.com/users/1374981345880707122)
- Dev Community: [https://dev.to/theycallmearnav](https://dev.to/theycallmearnav)

## License

MIT License — built with care for the Android and Discord communities.

---

<div align="center">
  <sub>Built with passion using Kotlin, Compose, and Discord APIs.</sub>
</div>
