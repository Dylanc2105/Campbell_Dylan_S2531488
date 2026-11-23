Traffic Scotland Android app
============================

Open the Campbell_Dylan_S2531488 folder in Android Studio and sync Gradle.
Android Studio creates local.properties for your SDK path. Add the API values below
in that same file to test the traffic feed and Google Maps:

TRAFFIC_SCOTLAND_CLIENT_ID=YOUR_ID
TRAFFIC_SCOTLAND_CLIENT_SECRET=YOUR_SECRET
GOOGLE_MAPS_API_KEY=YOUR_MAPS_KEY

Do not upload local.properties or any private credentials to GitHub.
The Google Maps key is included in the APK; restrict it in Google Cloud to the
application ID and relevant signing certificate.

Search for a road or choose a distance in the list, then tap View Map to see the
matching events. With no filter selected, the map shows all current feed events.
Reported short delays are amber and long delays are red. The displayed values
come from the Traffic Scotland XML; missing delay information is not guessed.

The app uses Lucide-based icons. Attribution and licence text are in
LICENSE_ICONS.txt. ChatGPT assisted with code and icons; disclose this in the
coursework GenAI declaration and prompt appendix.

The archive contains source code only. Build an APK in Android Studio for testing
and submit the required report, video, APK and source project separately.
