package domain

import java.time.Duration

sealed class TtsError(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class MissingApiKey : TtsError("API 키가 설정되지 않았습니다. https://aistudio.google.com/apikey 에서 발급하세요.")

    class EmptyText : TtsError("입력 텍스트가 비어 있습니다.")

    class InputFileNotFound(val path: String) : TtsError("입력 파일이 없습니다: $path")

    class ApiError(val status: Int, val body: String) :
        TtsError("Gemini API 오류 $status: ${body.take(500)}")

    class RateLimited(val retryAfter: Duration?) : TtsError(
        "호출 한도 초과" + (retryAfter?.let { " · ${it.toSeconds()}초 후 재시도 권장" } ?: "")
    )

    class MalformedResponse(val snippet: String, cause: Throwable? = null) :
        TtsError("응답 파싱 실패. 본문 일부: $snippet", cause)
}
