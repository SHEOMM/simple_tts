package domain

sealed class TtsModel(
    val id: String,
    val displayName: String,
    val tier: Tier,
) {
    data object Flash3_1 : TtsModel(
        id = "gemini-3.1-flash-tts-preview",
        displayName = "3.1 Flash (최신, 표현 태그)",
        tier = Tier.PreviewFree,
    )

    data object Flash25 : TtsModel(
        id = "gemini-2.5-flash-preview-tts",
        displayName = "2.5 Flash (무료 티어)",
        tier = Tier.Free,
    )

    data object Pro25 : TtsModel(
        id = "gemini-2.5-pro-preview-tts",
        displayName = "2.5 Pro (결제 필요)",
        tier = Tier.Paid,
    )

    enum class Tier { Free, PreviewFree, Paid }

    companion object {
        val all: List<TtsModel> = listOf(Flash3_1, Flash25, Pro25)
        val default: TtsModel = Flash3_1
        fun byId(id: String): TtsModel = all.firstOrNull { it.id == id } ?: default
    }
}
