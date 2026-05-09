import java.util.prefs.Preferences

object Settings {
    private val prefs = Preferences.userRoot().node("dev.tts.gemini")
    private const val API_KEY = "geminiApiKey"
    private const val LAST_INPUT = "lastInputPath"
    private const val LAST_OUTPUT_DIR = "lastOutputDir"
    private const val LAST_VOICE = "lastVoice"
    private const val LAST_MODEL = "lastModel"
    private const val LAST_STYLE = "lastStyle"

    fun loadApiKey(): String =
        prefs.get(API_KEY, "").ifBlank { System.getenv("GEMINI_API_KEY") ?: "" }

    fun saveApiKey(key: String) { prefs.put(API_KEY, key) }
    fun clearApiKey() { prefs.remove(API_KEY) }

    fun lastInputPath(): String = prefs.get(LAST_INPUT, "")
    fun setLastInputPath(p: String) { prefs.put(LAST_INPUT, p) }

    fun lastOutputDir(): String = prefs.get(LAST_OUTPUT_DIR, "")
    fun setLastOutputDir(p: String) { prefs.put(LAST_OUTPUT_DIR, p) }

    fun lastVoice(): String = prefs.get(LAST_VOICE, DEFAULT_VOICE)
    fun setLastVoice(v: String) { prefs.put(LAST_VOICE, v) }

    fun lastModel(): String = prefs.get(LAST_MODEL, MODEL_FLASH)
    fun setLastModel(m: String) { prefs.put(LAST_MODEL, m) }

    fun lastStyle(): String = prefs.get(LAST_STYLE, "")
    fun setLastStyle(s: String) { prefs.put(LAST_STYLE, s) }
}
