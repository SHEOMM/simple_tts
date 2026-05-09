package domain

data class AudioBuffer(val pcm: ByteArray, val sampleRate: Int) {
    override fun equals(other: Any?): Boolean = this === other ||
        (other is AudioBuffer && sampleRate == other.sampleRate && pcm.contentEquals(other.pcm))

    override fun hashCode(): Int = 31 * pcm.contentHashCode() + sampleRate
}
