package infra

import domain.TtsModel
import domain.Voice
import domain.Voices

class AppSettings(
    private val store: PreferencesStore,
    private val envReader: (String) -> String? = System::getenv,
) {
    var apiKey: String
        get() = store.get(KEY_API_KEY).ifBlank { envReader(ENV_API_KEY).orEmpty() }
        set(value) = store.put(KEY_API_KEY, value)

    var lastModel: TtsModel
        get() = TtsModel.byId(store.get(KEY_MODEL, TtsModel.default.id))
        set(value) = store.put(KEY_MODEL, value.id)

    var lastVoice: Voice
        get() = Voices.byId(store.get(KEY_VOICE, Voices.default.id))
        set(value) = store.put(KEY_VOICE, value.id)

    var lastStyle: String
        get() = store.get(KEY_STYLE)
        set(value) = store.put(KEY_STYLE, value)

    var lastInputPath: String
        get() = store.get(KEY_INPUT_PATH)
        set(value) = store.put(KEY_INPUT_PATH, value)

    var lastOutputDir: String
        get() = store.get(KEY_OUTPUT_DIR)
        set(value) = store.put(KEY_OUTPUT_DIR, value)

    fun clearApiKey() = store.remove(KEY_API_KEY)

    private companion object {
        const val ENV_API_KEY = "GEMINI_API_KEY"
        const val KEY_API_KEY = "geminiApiKey"
        const val KEY_MODEL = "lastModel"
        const val KEY_VOICE = "lastVoice"
        const val KEY_STYLE = "lastStyle"
        const val KEY_INPUT_PATH = "lastInputPath"
        const val KEY_OUTPUT_DIR = "lastOutputDir"
    }
}
