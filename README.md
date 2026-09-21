# G-Star FF

Android app for viewing and updating the daily G-Star FF results chart.

Package: `com.bidding.gstar`  
Current version: `1.2` (`versionCode` 3)

## Features

- Public chart of daily patti and single values
- Tap a day to open slot-level detail
- Admin login to add up to 8 entries for the current day
- Admin password change
- Data stored in Cloud Firestore (`entry`, `loginData`)

## Requirements

- Android Studio with Android SDK 36
- JDK 17 or later
- Android 5.0+ (API 21)
- Firebase project `gstarff-android` with Firestore enabled

## Build

```bash
./gradlew :app:assembleDebug
```

Release upload for Google Play must be a signed Android App Bundle with a new `versionCode`.

## Firebase setup

1. Place `app/google-services.json` from the Firebase Android app `com.bidding.gstar`.
2. Open [Firestore Rules](https://console.firebase.google.com/project/gstarff-android/firestore/rules).
3. Publish the rules in `firestore.rules`.
4. Create collection `loginData` with document ID `login` and fields:
   - `username` (string)
   - `password` (string)
5. Chart rows live in collection `entry`, one document per date (`dateString`).

If the app logs `PERMISSION_DENIED`, the published Firestore rules are still blocking reads.

## Play Console

- Target API: 36
- This app does not use Google Play Billing. Do not declare the Billing permission unless you add real in-app purchases.
- Host [PRIVACY_POLICY.md](PRIVACY_POLICY.md) at a public URL and paste that URL into Play Console → App content → Privacy policy.

## Contact

G-StarFF@gmail.com
