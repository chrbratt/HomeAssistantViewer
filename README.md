# HA Viewer

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A lightweight Android companion app for [Home Assistant](https://www.home-assistant.io/) — monitor your sensors and control lights and switches in real time from a clean, customisable dashboard.

**Created by C Bratt · [inix.se](http://inix.se)**

---

## Features

- **Multiple connections** — manage 2+ Home Assistant installations from a single app
- **Real-time updates** — persistent WebSocket connection per server; state changes pushed instantly, no polling
- **Favorites dashboard** — pin only the entities you care about; displayed in a configurable 1/2/3-column grid
- **Drag-to-reorder** — long-press a dashboard card to rearrange; order persists across restarts
- **Dimmer support** — brightness slider for dimmable lights with optimistic UI updates
- **Smart entity picker** — browse all entities from any connection with:
  - Sticky category headers
  - Quick-filter chips (Lights, Sensors, Climate, …)
  - Multi-word search (e.g. "temperature utomhus" finds `ws2900.utomhus` with name "Utomhus Temperatur")
- **Connection health indicator** — coloured dot on the settings icon (green/amber/red) reflecting live WebSocket state
- **Secure storage** — credentials stored in `EncryptedSharedPreferences`; non-sensitive settings in `DataStore`

## Architecture

```
data/
  model/          HaEntityState, HaConnection, FavoriteEntity, …
  HaWebSocketClient.kt    one persistent WS connection per HA server
  HomeAssistantRepository.kt   REST API calls (Retrofit + Moshi)
  ConnectionPool.kt       manages N client pairs, reacts to settings changes
  SettingsRepository.kt   encrypted storage, multi-connection, favorites

viewmodel/
  DashboardViewModel.kt   parallel multi-connection fetch, WS update fan-in
  EntityPickerViewModel.kt  connection selector, category filter, smart search
  SettingsViewModel.kt
  ConnectionsViewModel.kt  CRUD + connection tester

ui/screens/
  DashboardScreen.kt
  EntityPickerScreen.kt
  ConnectionsScreen.kt
  SettingsScreen.kt
  AboutScreen.kt
```

**Tech stack:** Kotlin · Jetpack Compose · Material 3 · OkHttp WebSocket · Retrofit · Moshi · Kotlin Coroutines/Flow · EncryptedSharedPreferences

## Getting started

1. Clone the repository
2. Open in Android Studio
3. Run on a device or emulator (API 35+)
4. Go to **Settings → Connections → +** and enter your Home Assistant base URL and a Long-Lived Access Token
5. Tap the ★ icon on the dashboard to select which entities to show

## Generating a Long-Lived Access Token

In Home Assistant: **Profile → Long-Lived Access Tokens → Create token**

## Privacy

This app does not collect, transmit, or share any personal data. All credentials are stored encrypted on your device only. See [PRIVACY_POLICY.md](PRIVACY_POLICY.md) for the full policy.
