package core

fun interface TextChunker {
    fun chunk(text: String): List<String>
}
