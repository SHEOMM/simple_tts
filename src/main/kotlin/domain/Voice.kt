package domain

data class Voice(val id: String, val displayName: String) {
    val label: String get() = "$id — $displayName"
}

object Voices {
    val all: List<Voice> = listOf(
        Voice("Kore", "차분, 중성"),
        Voice("Charon", "깊은 남성"),
        Voice("Puck", "밝고 활기"),
        Voice("Aoede", "부드러운 여성"),
        Voice("Leda", "또렷함"),
        Voice("Fenrir", "강렬한 남성"),
        Voice("Algieba", "매끄러움"),
        Voice("Achernar", "부드러운 여성"),
        Voice("Zephyr", "산뜻함"),
        Voice("Orus", "묵직함"),
        Voice("Callirrhoe", "여유로움"),
        Voice("Autonoe", "차분한 여성"),
        Voice("Enceladus", "낮고 안정"),
        Voice("Iapetus", "뉴트럴"),
        Voice("Umbriel", "어두운 톤"),
        Voice("Despina", "부드러움"),
        Voice("Erinome", "또렷한 여성"),
        Voice("Algenib", "굵고 안정"),
        Voice("Rasalgethi", "정보 전달형"),
        Voice("Laomedeia", "활기찬"),
        Voice("Alnilam", "단단함"),
        Voice("Schedar", "따뜻함"),
        Voice("Gacrux", "어른스러움"),
        Voice("Pulcherrima", "또박또박"),
        Voice("Achird", "친근함"),
        Voice("Zubenelgenubi", "캐주얼"),
        Voice("Vindemiatrix", "온화함"),
        Voice("Sadachbia", "활발함"),
        Voice("Sadaltager", "박식한 톤"),
        Voice("Sulafat", "따뜻한 여성"),
    )

    val default: Voice = all.first { it.id == "Kore" }

    fun byId(id: String): Voice = all.firstOrNull { it.id == id } ?: default
}
