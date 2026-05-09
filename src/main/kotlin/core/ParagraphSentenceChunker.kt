package core

class ParagraphSentenceChunker(
    private val maxChars: Int = DEFAULT_MAX_CHARS,
    private val paragraphSeparator: Regex = Regex("\\n\\s*\\n"),
    private val sentenceTerminator: Regex = Regex("(?<=[.!?。…]|다\\.)\\s+"),
) : TextChunker {

    override fun chunk(text: String): List<String> {
        val trimmed = text.removePrefix(BOM).trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxChars) return listOf(trimmed)

        val paragraphs = trimmed.split(paragraphSeparator)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return packParagraphs(paragraphs)
    }

    private fun packParagraphs(paragraphs: List<String>): List<String> = buildList {
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                add(current.toString().trim())
                current.clear()
            }
        }

        for (paragraph in paragraphs) {
            if (paragraph.length > maxChars) {
                flush()
                addAll(splitOversizedParagraph(paragraph))
                continue
            }
            if (current.length + paragraph.length + PARAGRAPH_GAP > maxChars) flush()
            if (current.isNotEmpty()) current.append("\n\n")
            current.append(paragraph)
        }
        flush()
    }

    private fun splitOversizedParagraph(paragraph: String): List<String> = buildList {
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                add(current.toString())
                current.clear()
            }
        }

        for (sentence in paragraph.split(sentenceTerminator)) {
            if (sentence.length > maxChars) {
                flush()
                sentence.chunked(maxChars).forEach { add(it) }
                continue
            }
            if (current.length + sentence.length + SENTENCE_GAP > maxChars) flush()
            if (current.isNotEmpty()) current.append(' ')
            current.append(sentence)
        }
        flush()
    }

    companion object {
        const val DEFAULT_MAX_CHARS = 3000
        private const val PARAGRAPH_GAP = 2
        private const val SENTENCE_GAP = 1
        private const val BOM = "\uFEFF"
    }
}
