package com.classrecord.app.data

import com.classrecord.app.i18n.AppStrings

data class ParsedMember(
    val name: String,
    val studentNo: String? = null
)

data class ExistingRosterPerson(
    val id: Long,
    val name: String,
    val studentNo: String?,
    val archived: Boolean
)

data class RosterConflict(
    val parsed: ParsedMember,
    val hint: String
)

data class RestoreCandidate(
    val memberId: Long,
    val name: String,
    val studentNo: String?
)

data class RosterParseResult(
    val toInsert: List<ParsedMember>,
    val toRestore: List<RestoreCandidate> = emptyList(),
    val conflicts: List<RosterConflict> = emptyList(),
    val skippedDuplicate: Int,
    val skippedBlank: Int
) {
    val insertCount: Int get() = toInsert.size
    val restoreCount: Int get() = toRestore.size
}

object RosterParser {
    private val studentNoPattern = Regex("""^[A-Za-z0-9][A-Za-z0-9._\-]{0,31}$""")

    fun parse(raw: String, existingActiveNames: Set<String>, strings: AppStrings): RosterParseResult {
        val existing = existingActiveNames.map { name ->
            ExistingRosterPerson(id = 0L, name = name, studentNo = null, archived = false)
        }
        return parse(raw, existing, strings)
    }

    fun parse(raw: String, existing: List<ExistingRosterPerson>, strings: AppStrings): RosterParseResult {
        var skippedBlank = 0
        val tokens = mutableListOf<ParsedMember>()
        normalize(raw).lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                skippedBlank += 1
                return@forEach
            }
            tokens += splitLine(trimmed)
        }

        val activeByName = existing.filter { !it.archived }.associateBy { it.name }
        val activeByNo = existing.filter { !it.archived && !it.studentNo.isNullOrBlank() }
            .associateBy { it.studentNo!!.lowercase() }
        val archivedByName = existing.filter { it.archived }.associateBy { it.name }
        val archivedByNo = existing.filter { it.archived && !it.studentNo.isNullOrBlank() }
            .associateBy { it.studentNo!!.lowercase() }

        val seenNames = mutableSetOf<String>()
        val seenNos = mutableSetOf<String>()
        val toInsert = mutableListOf<ParsedMember>()
        val toRestore = mutableListOf<RestoreCandidate>()
        val conflicts = mutableListOf<RosterConflict>()
        var skippedDuplicate = 0

        tokens.forEach { item ->
            if (item.name.isBlank()) {
                skippedBlank += 1
                return@forEach
            }
            val noKey = item.studentNo?.lowercase()
            when {
                item.name in seenNames || (noKey != null && noKey in seenNos) -> {
                    skippedDuplicate += 1
                    conflicts += RosterConflict(item, strings.rosterDup(label(item, strings)))
                }
                noKey != null && activeByNo.containsKey(noKey) -> {
                    val other = activeByNo.getValue(noKey)
                    skippedDuplicate += 1
                    conflicts += RosterConflict(
                        item,
                        strings.rosterStudentTaken(item.studentNo!!, other.name)
                    )
                }
                item.name in activeByName -> {
                    skippedDuplicate += 1
                    val other = activeByName.getValue(item.name)
                    val extra = other.studentNo?.let { strings.rosterStudentNoParen(it) }.orEmpty()
                    conflicts += RosterConflict(item, strings.rosterAlready(item.name, extra))
                }
                noKey != null && archivedByNo.containsKey(noKey) -> {
                    val archived = archivedByNo.getValue(noKey)
                    toRestore += RestoreCandidate(archived.id, archived.name, item.studentNo)
                    seenNames += item.name
                    seenNos += noKey
                }
                item.name in archivedByName -> {
                    val archived = archivedByName.getValue(item.name)
                    toRestore += RestoreCandidate(
                        archived.id,
                        archived.name,
                        item.studentNo ?: archived.studentNo
                    )
                    seenNames += item.name
                    item.studentNo?.lowercase()?.let { seenNos += it }
                }
                else -> {
                    toInsert += item
                    seenNames += item.name
                    if (noKey != null) seenNos += noKey
                }
            }
        }
        return RosterParseResult(
            toInsert = toInsert,
            toRestore = toRestore,
            conflicts = conflicts,
            skippedDuplicate = skippedDuplicate,
            skippedBlank = skippedBlank
        )
    }

    private fun normalize(raw: String): String {
        return raw
            .replace("\uFEFF", "")
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u3000', ' ')
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
        val separators = charArrayOf(',', '，', ';', '；', '\t')
        if (text.indexOfAny(separators) >= 0) {
            return text.split(',', '，', ';', '；', '\t').mapNotNull { piece ->
                val name = piece.trim()
                if (name.isEmpty()) null else ParsedMember(name)
            }
        }
        return listOf(ParsedMember(text.trim()))
    }

    private fun parseNameAndStudentNo(text: String): ParsedMember? {
        val parts = text.split(',', '，', ';', '；', '\t', limit = 2)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        pairFrom(parts)?.let { return it }
        val spaced = text.trim().split(Regex("""\s+"""), limit = 2)
        if (spaced.size == 2) {
            return pairFrom(spaced.map { it.trim() })
        }
        return null
    }

    private fun pairFrom(parts: List<String>): ParsedMember? {
        if (parts.size != 2) return null
        val a = parts[0]
        val b = parts[1]
        return when {
            looksLikeStudentNo(b) && !looksLikeStudentNo(a) -> ParsedMember(a, b)
            looksLikeStudentNo(a) && !looksLikeStudentNo(b) -> ParsedMember(b, a)
            looksLikeStudentNo(b) -> ParsedMember(a, b)
            else -> null
        }
    }

    private fun looksLikeStudentNo(value: String): Boolean = studentNoPattern.matches(value)

    private fun label(item: ParsedMember, strings: AppStrings): String =
        strings.rosterDisplayLabel(item.name, item.studentNo)
}
