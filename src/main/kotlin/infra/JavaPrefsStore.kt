package infra

import java.util.prefs.Preferences

class JavaPrefsStore(node: String) : PreferencesStore {
    private val prefs: Preferences = Preferences.userRoot().node(node)

    override fun get(key: String, default: String): String = prefs.get(key, default)
    override fun put(key: String, value: String) = prefs.put(key, value)
    override fun remove(key: String) = prefs.remove(key)
}
