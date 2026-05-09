package service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

internal data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig,
)

internal data class Content(val parts: List<Part>)
internal data class Part(val text: String)

internal data class GenerationConfig(
    val responseModalities: List<String>,
    val speechConfig: SpeechConfig,
)

internal data class SpeechConfig(val voiceConfig: VoiceConfig)
internal data class VoiceConfig(val prebuiltVoiceConfig: PrebuiltVoiceConfig)
internal data class PrebuiltVoiceConfig(val voiceName: String)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class GenerateContentResponse(val candidates: List<Candidate> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class Candidate(val content: ResponseContent? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ResponseContent(val parts: List<ResponsePart> = emptyList())

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ResponsePart(val inlineData: InlineData? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class InlineData(val mimeType: String = "", val data: String = "")
