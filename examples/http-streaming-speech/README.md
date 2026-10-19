# Streaming Speech over HTTP

A complete, runnable demo of `aiSpeakStream()`: BoxLang pushes audio to a browser **while the provider is still generating it**, so the listener hears the first words in well under a second.

No plugin is needed. The server streams bytes, and the browser plays them with built-in features.

## Two modes

| Mode | Endpoint | Wire format | Browser side | Good for |
|---|---|---|---|---|
| **Simple** | `speak.bxm` | Raw mp3 bytes | A plain `<audio src="speak.bxm?...">` tag | The quickest way to add spoken output to a page |
| **Voice agent** | `speak-events.bxm` | Newline-delimited JSON events (base64 PCM chunks plus word timestamps) | `fetch()` streaming plus the Web Audio API, with word highlighting and a Stop (barge-in) button | Low latency, interruptible voice agents |

### How it works

```
Provider  ->  aiSpeakStream( text, callback )  ->  callback writes each chunk to the HTTP response  ->  browser plays it
```

- `speak.bxm` writes each `audio` event's bytes to the response and flushes, so the browser starts playing after the first chunk.
- `speak-events.bxm` writes every event (`audio`, `timestamps`, `done`) as one JSON line, exactly as the callback receives it. `index.html` decodes each line as it arrives and schedules the PCM with Web Audio.
- When the listener presses Stop or closes the tab, the next write fails, the callback returns `false`, and `aiSpeakStream()` closes the provider connection. The provider stops generating immediately.

## Run it

1. Install the module the way the other examples do (see the [examples README](../README.md)), and set a provider key. Cartesia is the default:

   ```bash
   export CARTESIA_API_KEY="..."
   ```

2. Start MiniServer from this folder:

   ```bash
   cd examples/http-streaming-speech
   bvm miniserver
   ```

   The included `miniserver.json` serves this folder on port 8085. Without BVM you can run the MiniServer jar directly: `java -jar boxlang-miniserver-<version>.jar --webroot . --port 8085`.

3. Open <http://127.0.0.1:8085/> and press **Play** or **Speak**.

Use the provider dropdown to try ElevenLabs, OpenAI, Mistral or Gemini. Each needs its own key on the server (`ELEVENLABS_API_KEY`, `OPENAI_API_KEY`, `MISTRAL_API_KEY`, `GEMINI_API_KEY`). Word highlighting needs word timestamps, which Cartesia provides.

## Try the endpoints directly

```bash
# Raw mp3 bytes, saved as they arrive
curl -N -o hello.mp3 "http://127.0.0.1:8085/speak.bxm?text=Hello%20from%20BoxLang&provider=cartesia&format=mp3"

# The event stream, one JSON object per line
curl -N "http://127.0.0.1:8085/speak-events.bxm?text=Hello%20from%20BoxLang"
```

`speak.bxm` also accepts `format=pcm` (raw 16-bit 24kHz mono) and `format=mulaw` (8kHz telephony audio) for use with your own player or phone bridge.

## Notes

- **Demo only.** These endpoints spend your provider credits for anyone who can reach them. Add authentication and rate limits before exposing them. They already cap text at 500 characters and only allow a fixed list of providers.
- **Why `getResponseChannel()`?** The BoxLang web runtime does not stream binary output through its normal output buffer. The endpoints write to the response channel and flush after each chunk, then emit nothing else, so no stray bytes reach the audio. They are plain `.bxm` scripts, so you can read the whole mechanism in one short file.
- **Cold start.** The first request after the server starts is slower (module load and TLS handshake). Later requests start in a few hundred milliseconds with Cartesia.
- **Phone calls.** For telephony, stream `mulaw` and forward each chunk to your phone provider's media WebSocket instead of a browser.
