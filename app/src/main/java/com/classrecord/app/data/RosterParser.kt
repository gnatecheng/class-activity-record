package com.classrecord.app.data

data class ParsedMember(
    val name: String,
    val studentNo: String? = null
)

data class RosterParseResult(
    val toInsert: List<ParsedMember>,
    val skippedDuplicate: Int,
    val skippedBlank: Int
) {
    val insertCount: Int get() = toInsert.size
}

object RosterParser {
    private val studentNoPattern = Regex("""^[A-Za-z0-9][A-Za-z0-9._\-]{0,31}$""")

    /**
     * Accepts multiline, comma, or Chinese顿号 lists.
     * A line like `姓名 学号` / `姓名,学号` / `姓名，学号` becomes name + studentNo
     * when the second token looks like a student number.
     * Dedupes against [existingActiveNames] and within the paste (exact name).
     */
    fun parse(raw: String, existingActiveNames: Set<String>): RosterParseResult {
        var skippedBlank = 0
        val tokens = mutableListOf<ParsedMember>()
        raw.replace("\r\n", "\n").replace('\r', '\n').lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                skippedBlank += 1
                return@forEach
            }
            tokens += splitLine(trimmed)
        }

        val seen = existingActiveNames.toMutableSet()
        val toInsert = mutableListOf<ParsedMember>()
        var skippedDuplicate = 0
        tokens.forEach { item ->
            if (item.name.isBlank()) {
                skippedBlank += 1
                return@forEach
            }
            if (item.name in seen) {
                skippedDuplicate += 1
            } else {
                seen += item.name
                toInsert += item
            }
        }
        return RosterParseResult(toInsert, skippedDuplicate, skippedBlank)
    }

    private fun splitLine(line: String): List<ParsedMember> {
        if (line.contains('、')) {
            return line.split('、').flatMap { piece ->
                val part = piece.trim()
                if (part.isEmpty()) emptyList() else splitCommaOrPair(part)
            }
        }
        return splitCommaOrPair(line)
    }

    private fun splitCommaOrPair(text: String): List<ParsedMember> {
        parseNameAndStudentNo(text)?.let { return listOf(it) }
        if (text.contains(',') || text.contains('，')) {
            return text.split(',', '，').mapNotNull { piece ->
                val name = piece.trim()
                if (name.isEmpty()) null else ParsedMember(name)
            }
        }
        return listOf(ParsedMember(text.trim()))
    }

    private fun parseNameAndStudentNo(text: String): ParsedMember? {
        val comma = text.split(',', '，', limit = 2)
        if (comma.size == 2) {
            val name = comma[0].trim()
            val no = comma[1].trim()
            if (name.isNotEmpty() && studentNoPattern.matches(no)) {
                return ParsedMember(name, no)
            }
        }
        val spaced = text.trim().split(Regex("""\s+"""), limit = 2)
        if (spaced.size == 2) {
            val name = spaced[0].trim()
            val no = spaced[1].trim()
            if (name.isNotEmpty() && studentNoPattern.matches(no)) {
                return ParsedMember(name, no)
            }
        }
        return null
    }
}
