package com.classrecord.app.ui.members

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classrecord.app.ClassRecordApp
import com.classrecord.app.data.RosterParseResult
import com.classrecord.app.data.RosterParser
import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.repo.MemberRepository
import com.classrecord.app.di.AppViewModelFactory
import com.classrecord.app.ui.components.EmptyState
import com.classrecord.app.ui.theme.appColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MembersViewModel(
    private val memberRepository: MemberRepository,
    userPrefs: com.classrecord.app.data.prefs.UserPrefs
) : ViewModel() {
    val members: StateFlow<List<Member>> = memberRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val sortByStudentNo = userPrefs.sortByStudentNo

    fun previewImport(raw: String): RosterParseResult {
        val existing = members.value.map {
            com.classrecord.app.data.ExistingRosterPerson(
                id = it.id,
                name = it.name,
                studentNo = it.studentNo,
                archived = it.archived
            )
        }
        return RosterParser.parse(raw, existing)
    }

    suspend fun confirmImport(raw: String): String {
        val preview = previewImport(raw)
        val added = memberRepository.addAll(preview.toInsert)
        val restored = memberRepository.restoreArchived(preview.toRestore)
        return when {
            added == 0 && restored == 0 -> "没有可导入的新同学"
            restored == 0 -> "已导入 $added 人"
            added == 0 -> "已恢复归档 $restored 人，历史事务记录仍保留"
            else -> "已导入 $added 人，恢复归档 $restored 人"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(onBack: () -> Unit, onEdit: (Long?) -> Unit) {
    val app = LocalContext.current.applicationContext as ClassRecordApp
    val vm: MembersViewModel = viewModel(factory = AppViewModelFactory(app.container))
    val members by vm.members.collectAsStateWithLifecycle()
    val sortByNo by vm.sortByStudentNo.collectAsStateWithLifecycle(initialValue = true)
    val sorted = remember(members, sortByNo) {
        com.classrecord.app.data.MemberSort.members(members, sortByNo)
    }
    val active = sorted.filter { !it.archived }
    val archived = sorted.filter { it.archived }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showImport by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("班级成员") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showImport = true }) {
                        Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = "批量导入")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEdit(null) },
                containerColor = MaterialTheme.appColors.classScope.container,
                contentColor = MaterialTheme.appColors.classScope.onContainer
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "添加成员")
            }
        }
    ) { padding ->
        if (members.isEmpty()) {
            EmptyState(
                title = "还没有同学",
                subtitle = "点右下角添加姓名。学号和备注可选。",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(active, key = { it.id }) { member ->
                    MemberRow(member, onClick = { onEdit(member.id) })
                }
                if (archived.isNotEmpty()) {
                    item {
                        Text(
                            "已归档",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(archived, key = { it.id }) { member ->
                        MemberRow(member, onClick = { onEdit(member.id) })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showImport) {
        ImportMembersDialog(
            onDismiss = { showImport = false },
            onPreview = vm::previewImport,
            onConfirm = { raw ->
                scope.launch {
                    runCatching { vm.confirmImport(raw) }
                        .onSuccess { message ->
                            showImport = false
                            snackbar.showSnackbar(message)
                        }
                        .onFailure { snackbar.showSnackbar(it.message ?: "导入失败") }
                }
            }
        )
    }
}

@Composable
private fun ImportMembersDialog(
    onDismiss: () -> Unit,
    onPreview: (String) -> RosterParseResult,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val preview = remember(text) { onPreview(text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量导入名单") },
        text = {
            Column {
                Text(
                    "每行一位，或用逗号、顿号、Tab 分隔。支持「姓名 学号」「学号 姓名」。同名或同学号会提示；已归档的可一并恢复，历史事务记录仍保留。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder = { Text("张三\n李四 2023002\n2023003 王五\n赵六、钱七") }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    buildString {
                        append("将导入 ${preview.insertCount} 人")
                        if (preview.restoreCount > 0) append("，恢复归档 ${preview.restoreCount} 人")
                        if (preview.skippedDuplicate > 0) append("，跳过冲突 ${preview.skippedDuplicate}")
                        if (preview.skippedBlank > 0) append("，空行 ${preview.skippedBlank}")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (preview.toInsert.isNotEmpty()) {
                    Text(
                        preview.toInsert.take(8).joinToString("、") {
                            if (it.studentNo != null) "${it.name}（${it.studentNo}）" else it.name
                        } + if (preview.toInsert.size > 8) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (preview.toRestore.isNotEmpty()) {
                    Text(
                        "将恢复：" + preview.toRestore.take(6).joinToString("、") { it.name } +
                            if (preview.toRestore.size > 6) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.appColors.success
                    )
                }
                if (preview.conflicts.isNotEmpty()) {
                    Text(
                        preview.conflicts.take(6).joinToString("\n") { it.hint } +
                            if (preview.conflicts.size > 6) "\n…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.appColors.warning
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = preview.insertCount > 0 || preview.restoreCount > 0
            ) {
                val label = when {
                    preview.restoreCount > 0 && preview.insertCount > 0 ->
                        "导入 ${preview.insertCount}、恢复 ${preview.restoreCount}"
                    preview.restoreCount > 0 -> "恢复 ${preview.restoreCount} 人"
                    else -> "导入 ${preview.insertCount} 人"
                }
                Text(label)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun MemberRow(member: Member, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (member.archived) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.appColors.memberShortcut
            }
        )
    ) {
        ListItem(
            headlineContent = { Text(member.name) },
            supportingContent = {
                val bits = buildList {
                    member.studentNo?.let { add("学号 $it") }
                    member.note?.let { add(it) }
                    if (member.archived) add("已归档")
                }
                if (bits.isNotEmpty()) {
                    Text(
                        bits.joinToString(" · "),
                        color = if (member.archived) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.appColors.classScope.color
                        }
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

class MemberEditViewModel(
    private val memberRepository: MemberRepository,
    private val memberId: Long?
) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var studentNo by mutableStateOf("")
        private set
    var note by mutableStateOf("")
        private set
    var archived by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(memberId == null)
        private set
    private var existing: Member? = null

    init {
        if (memberId != null) {
            viewModelScope.launch {
                val member = memberRepository.getById(memberId)
                existing = member
                if (member != null) {
                    name = member.name
                    studentNo = member.studentNo.orEmpty()
                    note = member.note.orEmpty()
                    archived = member.archived
                }
                loaded = true
            }
        }
    }

    fun onName(v: String) { name = v }
    fun onStudentNo(v: String) { studentNo = v }
    fun onNote(v: String) { note = v }

    suspend fun save() {
        val current = existing
        if (current == null) {
            memberRepository.add(name, studentNo, note)
        } else {
            memberRepository.update(
                current.copy(name = name, studentNo = studentNo, note = note, archived = archived)
            )
        }
    }

    suspend fun setArchived(value: Boolean) {
        val current = existing ?: return
        archived = value
        memberRepository.setArchived(current, value)
        existing = current.copy(archived = value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberEditScreen(memberId: Long?, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as ClassRecordApp
    val vm: MemberEditViewModel = viewModel(
        factory = AppViewModelFactory(app.container, memberId = memberId)
    )
    val host = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (memberId == null) "添加成员" else "编辑成员") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                runCatching { vm.save(); onDone() }
                                    .onFailure { host.showSnackbar("请填写姓名") }
                            }
                        },
                        enabled = vm.name.isNotBlank()
                    ) { Text("保存") }
                }
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(host) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(20.dp)
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = vm.name,
                onValueChange = vm::onName,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("姓名") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                value = vm.studentNo,
                onValueChange = vm::onStudentNo,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("学号（可选）") },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                value = vm.note,
                onValueChange = vm::onNote,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("备注（可选）") }
            )
            if (memberId != null) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = vm.archived,
                        onClick = { scope.launch { vm.setArchived(!vm.archived) } },
                        label = { Text(if (vm.archived) "已归档（不参与新事务）" else "归档") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.appColors.warningContainer,
                            selectedLabelColor = MaterialTheme.appColors.onWarning
                        )
                    )
                }
                Text(
                    "归档后不再进入新事务，历史点名、缴费记录会保留。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
