package core

import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavEncoder {

    fun encode(
        pcm: ByteArray,
        sampleRate: Int,
        channels: Int = 1,
        bitsPerSample: Int = 16,
    ): ByteArray {
        val header = buildHeader(pcm.size, sampleRate, channels, bitsPerSample)
        return header + pcm
    }

    private fun buildHeader(pcmSize: Int, sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8

        return ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toAscii())
            putInt(pcmSize + HEADER_SIZE - 8)
            put("WAVE".toAscii())
            put("fmt ".toAscii())
            putInt(FMT_CHUNK_SIZE)
            putShort(PCM_FORMAT)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toAscii())
            putInt(pcmSize)
        }.array()
    }

    private fun String.toAscii(): ByteArray = toByteArray(Charsets.US_ASCII)

    private const val HEADER_SIZE = 44
    private const val FMT_CHUNK_SIZE = 16
    private const val PCM_FORMAT: Short = 1
}
