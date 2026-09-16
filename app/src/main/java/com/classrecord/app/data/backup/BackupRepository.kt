package com.classrecord.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.classrecord.app.data.AttachmentStore
import com.classrecord.app.data.db.AppDatabase
import com.classrecord.app.data.entity.ActivityEntity
import com.classrecord.app.data.entity.ActivityMember
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.ClassProfile
import com.classrecord.app.data.entity.LedgerEntry
import com.classrecord.app.data.entity.LedgerType
import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.entity.ScopeType
import com.classrecord.app.data.entity.SubGroup
import com.classrecord.app.data.entity.SubGroupMember
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRepository(
    context: Context,
    private val db: AppDatabase,
    private val attachments: AttachmentStore
) {
    private val appContext = context.applicationContext

    suspend fun writeZipTo(uri: Uri) {
        val bytes = buildZipBytes()
        appContext.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error("无法写入备份文件")
    }

    suspend fun shareableZipFile(): File {
        val dir = File(appContext.cacheDir, "export").also { it.mkdirs() }
        val file = File(dir, "班级事务记录-备份.zip")
        file.writeBytes(buildZipBytes())
        return file
    }

    suspend fun restoreFrom(uri: Uri) {
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("无法读取备份文件")
        restoreBytes(bytes)
    }

    private suspend fun buildZipBytes(): ByteArray {
        val json = snapshotJson()
        val out = ByteArrayOutputStream()
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_NAME))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            attachments.listRelativeFiles().forEach { relative ->
                val file = attachments.fileFor(relative)
                if (!file.isFile) return@forEach
                zip.putNextEntry(ZipEntry(relative))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private suspend fun snapshotJson(): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", 3)
        root.put("exportedAt", System.currentTimeMillis())
        val profile = db.classProfileDao().get()
        if (profile != null) {
            root.put(
                "classProfile",
                JSONObject()
                    .put("id", profile.id)
                    .put("name", profile.name)
                    .put("updatedAt", profile.updatedAt)
            )
        } else {
            root.put("classProfile", JSONObject.NULL)
        }
        root.put(
            "members",
            JSONArray().also { arr ->
                db.memberDao().getAll().forEach { m ->
                    arr.put(
                        JSONObject()
                            .put("id", m.id)
                            .put("name", m.name)
                            .put("studentNo", m.studentNo)
                            .put("note", m.note)
                            .put("archived", m.archived)
                    )
                }
            }
        )
        root.put(
            "subgroups",
            JSONArray().also { arr ->
                db.subGroupDao().getAll().forEach { g ->
                    arr.put(
                        JSONObject()
                            .put("id", g.id)
                            .put("name", g.name)
                            .put("note", g.note)
                            .put("archived", g.archived)
                            .put("createdAt", g.createdAt)
                    )
                }
            }
        )
        root.put(
            "subgroupMembers",
            JSONArray().also { arr ->
                db.subGroupMemberDao().getAll().forEach { row ->
                    arr.put(
                        JSONObject()
                            .put("subGroupId", row.subGroupId)
                            .put("memberId", row.memberId)
                    )
                }
            }
        )
        root.put(
            "activities",
            JSONArray().also { arr ->
                db.activityDao().getAll().forEach { a ->
                    arr.put(
                        JSONObject()
                            .put("id", a.id)
                            .put("scopeType", a.scopeType.name)
                            .put("subGroupId", a.subGroupId)
                            .put("type", a.type.name)
                            .put("title", a.title)
                            .put("note", a.note)
                            .put("totalAmount", a.totalAmount)
                            .put("deadline", a.deadline)
                            .put("archived", a.archived)
                            .put("createdAt", a.createdAt)
                            .put("updatedAt", a.updatedAt)
                    )
                }
            }
        )
        root.put(
            "activityMembers",
            JSONArray().also { arr ->
                db.activityMemberDao().getAll().forEach { row ->
                    arr.put(
                        JSONObject()
                            .put("activityId", row.activityId)
                            .put("memberId", row.memberId)
                            .put("status", row.status.name)
                            .put("amountDue", row.amountDue)
                            .put("amountPaid", row.amountPaid)
                            .put("note", row.note)
                            .put("included", row.included)
                            .put("weight", row.weight)
                            .put("attachmentPath", row.attachmentPath)
                            .put("attachmentMime", row.attachmentMime)
                            .put("updatedAt", row.updatedAt)
                    )
                }
            }
        )
        root.put(
            "ledgerEntries",
            JSONArray().also { arr ->
                db.ledgerDao().getAll().forEach { e ->
                    arr.put(
                        JSONObject()
                            .put("id", e.id)
                            .put("type", e.type.name)
                            .put("amountFen", e.amountFen)
                            .put("title", e.title)
                            .put("note", e.note)
                            .put("relatedActivityId", e.relatedActivityId)
                            .put("createdAt", e.createdAt)
                    )
                }
            }
        )
        return root.toString()
    }

    private suspend fun restoreBytes(bytes: ByteArray) {
        val (jsonText, files) = unpack(bytes)
        val root = JSONObject(jsonText)
        if (root.optString("format") != FORMAT) {
            error("不是班级事务记录的备份文件")
        }
        db.withTransaction {
            db.ledgerDao().deleteAll()
            db.activityMemberDao().deleteAll()
            db.activityDao().deleteAll()
            db.subGroupMemberDao().deleteAll()
            db.subGroupDao().deleteAll()
            db.memberDao().deleteAll()
            db.classProfileDao().deleteAll()
            attachments.deleteAll()

            val profile = root.optJSONObject("classProfile")
            if (profile != null) {
                db.classProfileDao().upsert(
                    ClassProfile(
                        id = profile.optLong("id", 1L),
                        name = profile.getString("name"),
                        updatedAt = profile.getLong("updatedAt")
                    )
                )
            }
            val members = root.optJSONArray("members") ?: JSONArray()
            for (i in 0 until members.length()) {
                val o = members.getJSONObject(i)
                db.memberDao().insertPreserveId(
                    Member(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        studentNo = o.optNullableString("studentNo"),
                        note = o.optNullableString("note"),
                        archived = o.optBoolean("archived", false)
                    )
                )
            }
            val groups = root.optJSONArray("subgroups") ?: JSONArray()
            for (i in 0 until groups.length()) {
                val o = groups.getJSONObject(i)
                db.subGroupDao().insertPreserveId(
                    SubGroup(
                        id = o.getLong("id"),
                        name = o.getString("name"),
                        note = o.optNullableString("note"),
                        archived = o.optBoolean("archived", false),
                        createdAt = o.getLong("createdAt")
                    )
                )
            }
            val links = root.optJSONArray("subgroupMembers") ?: JSONArray()
            val linkRows = buildList {
                for (i in 0 until links.length()) {
                    val o = links.getJSONObject(i)
                    add(SubGroupMember(o.getLong("subGroupId"), o.getLong("memberId")))
                }
            }
            if (linkRows.isNotEmpty()) db.subGroupMemberDao().insertAll(linkRows)

            val activities = root.optJSONArray("activities") ?: JSONArray()
            for (i in 0 until activities.length()) {
                val o = activities.getJSONObject(i)
                db.activityDao().insertPreserveId(
                    ActivityEntity(
                        id = o.getLong("id"),
                        scopeType = ScopeType.valueOf(o.getString("scopeType")),
                        subGroupId = o.optNullableLong("subGroupId"),
                        type = ActivityType.valueOf(o.getString("type")),
                        title = o.getString("title"),
                        note = o.optNullableString("note"),
                        totalAmount = o.optNullableLong("totalAmount"),
                        deadline = o.optNullableLong("deadline"),
                        archived = o.optBoolean("archived", false),
                        createdAt = o.getLong("createdAt"),
                        updatedAt = o.getLong("updatedAt")
                    )
                )
            }
            val rows = root.optJSONArray("activityMembers") ?: JSONArray()
            val amRows = buildList {
                for (i in 0 until rows.length()) {
                    val o = rows.getJSONObject(i)
                    val path = o.optNullableString("attachmentPath")
                    val hasFile = path != null && files.containsKey(path)
                    if (path != null && hasFile) {
                        attachments.copyIntoRoot(path, files.getValue(path))
                    }
                    add(
                        ActivityMember(
                            activityId = o.getLong("activityId"),
                            memberId = o.getLong("memberId"),
                            status = MemberStatus.valueOf(o.getString("status")),
                            amountDue = o.optNullableLong("amountDue"),
                            amountPaid = o.optNullableLong("amountPaid"),
                            note = o.optNullableString("note"),
                            included = o.optBoolean("included", true),
                            weight = o.optInt("weight", 1).coerceAtLeast(1),
                            attachmentPath = if (hasFile) path else null,
                            attachmentMime = if (hasFile) o.optNullableString("attachmentMime") else null,
                            updatedAt = o.getLong("updatedAt")
                        )
                    )
                }
            }
            if (amRows.isNotEmpty()) db.activityMemberDao().insertAll(amRows)

            val ledger = root.optJSONArray("ledgerEntries") ?: JSONArray()
            for (i in 0 until ledger.length()) {
                val o = ledger.getJSONObject(i)
                db.ledgerDao().insertPreserveId(
                    LedgerEntry(
                        id = o.getLong("id"),
                        type = LedgerType.valueOf(o.getString("type")),
                        amountFen = o.getLong("amountFen"),
                        title = o.getString("title"),
                        note = o.optNullableString("note"),
                        relatedActivityId = o.optNullableLong("relatedActivityId"),
                        createdAt = o.getLong("createdAt")
                    )
                )
            }
        }
    }

    private fun unpack(bytes: ByteArray): Pair<String, Map<String, ByteArray>> {
        if (bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            var json: String? = null
            val files = mutableMapOf<String, ByteArray>()
            ZipInputStream(BufferedInputStream(bytes.inputStream())).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val data = zip.readBytes()
                        when {
                            entry.name == JSON_NAME || entry.name.endsWith(".json") ->
                                json = data.toString(Charsets.UTF_8)
                            entry.name.startsWith("${AttachmentStore.DIR}/") ->
                                files[entry.name] = data
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            return (json ?: error("备份包里没有数据文件")) to files
        }
        return bytes.toString(Charsets.UTF_8) to emptyMap()
    }

    companion object {
        const val FORMAT = "classrecord-backup"
        const val JSON_NAME = "classrecord-backup.json"
    }
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).ifBlank { null }
}

private fun JSONObject.optNullableLong(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return optLong(key)
}
