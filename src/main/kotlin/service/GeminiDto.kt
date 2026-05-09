package service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

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

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ErrorResponse(val error: ErrorBody = ErrorBody())

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ErrorBody(
    val code: Int = 0,
    val message: String = "",
    val status: String = "",
    val details: List<ErrorDetail> = emptyList(),
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "@type",
    visible = true,
    defaultImpl = ErrorDetail.Other::class,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ErrorDetail.QuotaFailure::class, name = "type.googleapis.com/google.rpc.QuotaFailure"),
    JsonSubTypes.Type(value = ErrorDetail.RetryInfo::class, name = "type.googleapis.com/google.rpc.RetryInfo"),
)
@JsonIgnoreProperties(ignoreUnknown = true)
internal sealed class ErrorDetail {
    data class QuotaFailure(val violations: List<QuotaViolationDto> = emptyList()) : ErrorDetail()
    data class RetryInfo(val retryDelay: String = "") : ErrorDetail()
    data object Other : ErrorDetail()
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class QuotaViolationDto(
    val quotaMetric: String = "",
    val quotaId: String = "",
    val quotaDimensions: Map<String, String> = emptyMap(),
)
