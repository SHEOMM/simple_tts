package cli

import domain.QuotaScope
import domain.TtsError
import domain.TtsRequest
import infra.AppModule
import kotlinx.coroutines.runBlocking
import service.ChunkEvent
import service.ProgressCallback
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.exitProcess

class CliEntry(private val module: AppModule) {

    fun run(args: Array<String>) {
        if (args.isEmpty()) failUsage()
        val input = Path.of(args[0]).also { if (!Files.exists(it)) fail("입력 파일이 없습니다: $it") }
        val output = if (args.size > 1) Path.of(args[1]) else deriveOutput(input)
        val apiKey = module.settings.apiKey
        if (apiKey.isBlank()) fail("API 키가 없습니다. GUI에서 저장하거나 GEMINI_API_KEY 환경변수를 설정하세요.")

        val request = TtsRequest(
            text = Files.readString(input),
            apiKey = apiKey,
            model = module.settings.lastModel,
            voice = module.settings.lastVoice,
            styleInstruction = module.settings.lastStyle,
        )

        val callback = ProgressCallback { event ->
            when (event) {
                is ChunkEvent.Started ->
                    println("[${event.current}/${event.total}] 합성 중... (${event.chars}자)")
                is ChunkEvent.Waiting -> {
                    if (event.isFirstTick) {
                        val advice = event.reason.advice.takeIf { it.isNotBlank() }?.let { " — $it" }.orEmpty()
                        println("  ${event.reason.displayName} · 약 ${event.totalSeconds}초 대기$advice")
                    }
                    print("\r  ⏱  ${event.remainingSeconds}s 남음     ")
                    if (event.remainingSeconds <= 0) println()
                }
            }
        }
        module.progressCallback = callback

        runBlocking {
            try {
                module.pipeline.synthesizeToFile(
                    request = request,
                    outputPath = output,
                    onProgress = callback,
                )
                println("완료: $output")
            } catch (e: TtsError.RateLimited) {
                fail(formatRateLimit(e))
            } catch (e: TtsError) {
                fail(e.message ?: "변환 실패")
            } finally {
                module.progressCallback = ProgressCallback.Noop
            }
        }
    }

    private fun formatRateLimit(e: TtsError.RateLimited): String {
        val violation = e.mostSevere
        val head = violation?.let { "${it.displayName} 초과" } ?: "호출 한도 초과"
        val tail = when (violation?.scope) {
            QuotaScope.RequestsPerDay -> "내일 다시 시도하거나 결제를 활성화하세요"
            QuotaScope.InputTokensPerMinute -> "더 짧은 청크로 재시도하세요"
            QuotaScope.RequestsPerMinute -> e.retryDelay?.let { "${it.toSeconds()}초 후 재시도하세요" }
                ?: "잠시 후 다시 시도하세요"
            is QuotaScope.Unknown -> "원본 ID: ${violation.scope.rawId}"
            null -> e.rawMessage.ifBlank { "응답에 상세 정보 없음" }
        }
        return "$head — $tail"
    }

    private fun deriveOutput(input: Path): Path {
        val name = input.fileName.toString()
        val base = name.substringBeforeLast('.', name)
        return input.resolveSibling("$base.wav")
    }

    private fun failUsage(): Nothing = fail("Usage: tts <input.txt> [output.wav]")

    private fun fail(message: String): Nothing {
        System.err.println(message)
        exitProcess(1)
    }
}
