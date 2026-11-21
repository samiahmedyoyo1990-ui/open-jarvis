# AI Providers — JARVIS

JARVIS routes prompts and multimodal image queries through `UniversalAiEngine` (`com.example.ai.UniversalAiEngine`) using the `UniversalAiProvider` adapter interface.

## Built-in Provider Types (`AiProviderType`)

1. **`GOOGLE_GEMINI` (`GeminiDirectProviderAdapter`)**
   - Default Base URL: `https://generativelanguage.googleapis.com`
   - Default Model: `gemini-2.5-flash`
   - Authentication: `x-goog-api-key` HTTP header (never query strings)
   - Supports connection testing, dynamic model discovery, text chat, and Base64 multimodal image analysis (`inline_data`).

2. **`OPEN_ROUTER` (`OpenRouterProviderAdapter`)**
   - Default Base URL: `https://openrouter.ai/api/v1`
   - Default Model: `deepseek/deepseek-chat`
   - Authentication: `Authorization: Bearer <key>`
   - Supports model discovery (`/models`), text chat, and `image_url` multimodal payloads.

3. **`CUSTOM_OPENAI` (`OpenAiCompatibleProviderAdapter`)**
   - Default Base URL: `https://api.openai.com/v1`
   - Default Model: `gpt-4o-mini`
   - Connects any OpenAI-compatible API endpoint (OpenAI, Groq, Together, Ollama, LM Studio, or private HTTPS/LAN servers).
   - API keys are required for public HTTPS endpoints and optional for local `http://localhost` / RFC1918 LAN servers.

4. **`FIREBASE_AI` (`FirebaseAiProviderAdapter`)**
   - Built-in server-side `Firebase.ai.generativeModel(...)` integrated with Firebase App Check.
   - Supports text chat and Bitmap multimodal requests (`content("user") { image(bitmap); text(...) }`).

## Failover & Validation Behavior

- If a BYOK provider is active with a missing API key (on non-local endpoints), JARVIS explicitly reports the missing key rather than silently sending prompts to a different provider.
- Optional **Automatic Backup Failover** activates only when explicitly enabled by the user and a retryable/server error occurs on the primary provider.
- Non-local HTTP URLs are rejected by `AiEndpointSecurity.isValidEndpointUrl(...)` to protect keys in transit.
