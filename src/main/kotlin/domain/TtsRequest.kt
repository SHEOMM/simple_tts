package domain

data class TtsRequest(
    val text: String,
    val model: TtsModel,
    val voice: Voice,
    val styleInstruction: String = "",
)
