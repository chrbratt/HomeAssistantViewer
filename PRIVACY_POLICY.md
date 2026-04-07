# Privacy Policy — Home Assistant Viewer

**Effective date:** 2025-01-01
**Developer:** C Bratt — [inix.se](http://inix.se)

---

## Overview

Home Assistant Viewer is a companion app for Home Assistant. This policy explains what data the app handles, where it is stored, and what is never collected.

The short version: **all data stays on your device and your own server. Nothing is sent anywhere else.**

---

## Data the app stores on your device

### Connection credentials
When you add a Home Assistant connection, the app stores:
- A display name you choose
- The base URL of your Home Assistant instance
- A Long-Lived Access Token

These are stored exclusively in **Android's EncryptedSharedPreferences**, which encrypts the data using AES-256 keys backed by the Android Keystore. No one — including the developer — can read them. They never leave your device.

### App preferences
The following non-sensitive settings are stored locally in **Android DataStore**:
- Your list of favorited entities and their display order
- Dashboard column count (1, 2, or 3)
- Theme preference (System / Light / Dark)

---

## Network usage

The app communicates exclusively with the Home Assistant instance(s) you configure. Specifically:
- A **REST API call** to fetch entity states
- A **WebSocket connection** for real-time state change events
- **Service calls** (e.g., toggle a light) sent to your Home Assistant

All network traffic goes directly between your device and your server. The app does not route traffic through any proxy, cloud service, or developer-controlled infrastructure.

---

## What the app does NOT do

- Does not collect personal data
- Does not send analytics or usage statistics
- Does not show advertisements
- Does not use third-party SDKs that collect data (no Firebase, no Crashlytics, no tracking)
- Does not share any data with any third party
- Does not access your location, contacts, camera, or microphone

---

## Third-party libraries

The app uses open-source libraries (OkHttp, Retrofit, Moshi, Jetpack Compose, etc.) solely to provide its core functionality. None of these libraries are configured to collect or transmit data.

---

## Data deletion

Uninstalling the app removes all locally stored data, including connection credentials and preferences. There is no account, no server-side storage, and nothing to delete remotely.

---

## Children

The app does not target children and does not knowingly collect data from anyone.

---

## Changes to this policy

If this policy changes, the updated version will be published in the app's GitHub repository with a new effective date.

---

## Contact

Questions about this policy? Reach out at [inix.se](http://inix.se).
