# 📍 Trip Tracker

**Trip Tracker** is a simple, lightweight, and **100% offline** Android application designed to acquire device GPS coordinates and share them via SMS or record them in a local log history exportable as a `.gpx` file.

The application operates without requiring an internet connection or mobile data, relying solely on the device's GPS chip and basic cellular network (GSM).

---

## 💡 Motivation & Inspiration

The inspiration behind **Trip Tracker** stems from two real-world needs:

1. **Safety During Solo Road Trips and Hiking Trails**:
   When traveling alone or hiking on mountain trails, mobile data coverage (3G/4G/5G/Internet) is frequently weak or unavailable. However, offline GPS signals and basic GSM cellular networks (SMS) often remain operational, allowing users to continuously share their position with loved ones for safety and emergency assurance.

2. **Supporting Family Members with Dementia or Alzheimer's**:
   Family members with memory loss or cognitive conditions often require safety monitoring. The automatic background tracking mode enables periodic location updates to be sent automatically to a trusted family member without requiring any action or interaction from the user.

---

## ✨ Key Features

- 📱 **3 Operational Modes**:
  - **Single SMS**: Sends a single SMS with your current location upon pressing the button.
  - **Loop SMS**: Runs a background service that retrieves GPS coordinates and automatically sends an SMS every $X$ minutes (configurable interval).
  - **Just Log**: Functions like *Loop SMS*, but only records GPS locations into local history every $X$ minutes without sending SMS messages.

- 📴 **100% Offline Capability**:
  - Direct coordinate capture via device GPS hardware.
  - Direct SMS sending via traditional cellular network (GSM).

- 🗺️ **Multiple Message Formats / Links**:
  - **Google Maps**: `https://maps.google.com/?q={lat},{lng}`
  - **OpenStreetMap**: `https://www.openstreetmap.org/?mlat={lat}&mlon={lng}#map=16/{lat}/{lng}`
  - **Geo URI**: `geo:{lat},{lng}`
  - **Simple Coordinates**: `Lat: {lat}, Lng: {lng}`
  - **Custom Template**: Craft custom messages using `{lat}` and `{lng}` placeholders.
  - Live message preview in the UI.

- 📇 **Phonebook Integration**:
  - Easily choose recipient contacts directly from your phone contacts list.

- 📜 **Location History & GPX Export**:
  - Automatically stores logged locations in a local `Room` database.
  - View recorded history points directly in the app.
  - **Export to `.gpx` file** for easy importing into mapping platforms (OpenStreetMap, Strava, Wikiloc, Google Earth, etc.).

- ⚙️ **Background Foreground Service**:
  - Persistent notification displaying active tracking status and allowing a 1-tap stop at any time.

---

## 🚀 How to Install

1. Go to the **[Releases](https://github.com/drrcastro/TripTracker/releases)** section of this GitHub repository.
3. Open the downloaded file on your Android device and confirm installation (allow installation from unknown sources if prompted).
4. Launch **Trip Tracker**, grant permissions, and you're good to go!

---

## 🔒 Permissions Used

- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: Required to retrieve precise GPS coordinates.
- `SEND_SMS`: Required to send location messages via SMS.
- `READ_CONTACTS`: Used to allow selecting recipient phone numbers from your device contact list.
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION`: Required to keep periodic background tracking active when screen is off.
- `POST_NOTIFICATIONS`: Required on Android 13+ to display the active service notification.

---

## 📄 License

This project is licensed under the MIT License. Feel free to use, modify, and contribute!
