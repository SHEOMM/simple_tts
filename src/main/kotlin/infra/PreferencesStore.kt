package infra

interface PreferencesStore {
    fun get(key: String, default: String = ""): String
    fun put(key: String, value: String)
    fun remove(key: String)
}
