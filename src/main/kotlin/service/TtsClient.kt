package service

import domain.AudioBuffer
import domain.TtsModel
import domain.Voice

interface TtsClient {
    suspend fun synthesize(text: String, model: TtsModel, voice: Voice): AudioBuffer
}
