package domain

data class TtsRequest(
    val text: String,
    val apiKey: String,
    val model: TtsModel,
    val voice: Voice,
    val styleInstruction: String = "",
)
