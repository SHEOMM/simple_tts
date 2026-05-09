package cli

import domain.TtsError
import domain.TtsRequest
import infra.AppModule
import kotlinx.coroutines.runBlocking
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
            model = module.settings.lastModel,
            voice = module.settings.lastVoice,
            styleInstruction = module.settings.lastStyle,
        )

        runBlocking {
            try {
                module.pipeline.synthesizeToFile(
                    request = request,
                    outputPath = output,
                    onProgress = ProgressCallback { c, t, n -> println("[$c/$t] 합성 중... ($n 자)") },
                )
                println("완료: $output")
            } catch (e: TtsError) {
                fail(e.message ?: "변환 실패")
            }
        }
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
