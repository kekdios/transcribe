# VoiceNote Transcriber

Android app that transcribes WhatsApp voice notes. Long-press a voice note in
WhatsApp, tap **Share**, pick **VoiceNote Transcriber**, and the app uploads the
audio to your backend, which calls the ElevenLabs Scribe endpoint and returns
the transcript. The transcript can be copied or shared back into WhatsApp.

**No API keys live in the client.** The ElevenLabs key only exists on the
backend via the `ELEVENLABS_API_KEY` environment variable.

## Structure

- `app/` — Android app (Kotlin, Jetpack Compose, Retrofit, Coroutines)
- `backend/` — minimal FastAPI proxy to ElevenLabs Scribe

## Android app

Open the project root in Android Studio and build/run `app`, or:

```sh
./gradlew assembleDebug
```

Requires JDK 17 (`gradle.properties` points at Homebrew's `openjdk@17` on this
machine — repoint or delete that line if yours lives elsewhere) and Android SDK
(`local.properties` has `sdk.dir`).

- **minSdk 26, targetSdk 35**, `compileSdk 35`.
- `MainActivity` registers an `ACTION_SEND` intent filter for `audio/*` plus
  `audio/ogg`, `audio/mpeg`, `audio/mp4`, `audio/aac` (and
  `application/octet-stream`, which some share sheets use) with `BROWSABLE` and
  `DEFAULT` categories so it appears in WhatsApp's share sheet.
- On a share intent the app copies the incoming `content://` URI into
  app-private storage via `ContentResolver`, shows the filename and a
  **Transcribe** button, then POSTs the file as multipart form field `audio`
  to `{backendUrl}/transcribe`. The JSON `transcript` field is shown in a
  scrollable view with **Copy** and **Send back** (re-share) buttons.
- The gear icon opens **Settings**, where the backend URL is stored in
  `SharedPreferences`. Default is `http://10.0.2.2:8000` (host machine from
  the Android emulator). On a physical phone, use your computer's LAN IP, e.g.
  `http://192.168.1.20:8000`.
- **Debug builds log full request/response bodies** via OkHttp's
  `HttpLoggingInterceptor` (gated on `BuildConfig.DEBUG` in `ApiClient`).
  Release builds log nothing and are minified.
- Cleartext HTTP is allowed (`res/xml/network_security_config.xml`) so a local
  dev backend works. Tighten that before shipping publicly.

## Backend

```sh
cd backend
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
ELEVENLABS_API_KEY=sk_... uvicorn main:app --host 0.0.0.0 --port 8000
```

`POST /transcribe` accepts multipart field `audio`, forwards it to
`https://api.elevenlabs.io/v1/speech-to-text` with `model_id=scribe_v1`, and
returns `{"transcript": "..."}`.

## The loop

1. WhatsApp → long-press voice note → Share → **VoiceNote Transcriber**
2. App copies the file, tap **Transcribe**
3. Backend attaches `xi-api-key`, calls Scribe, returns text
4. App shows transcript → **Copy** or **Send back** → WhatsApp share sheet
