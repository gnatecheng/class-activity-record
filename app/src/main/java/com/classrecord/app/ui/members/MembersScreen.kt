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
import androidx.compose.ui.res.stringResource
import com.classrecord.app.R

class MembersViewModel(
    private val memberRepository: MemberRepository,
    userPrefs: com.classrecord.app.data.prefs.UserPrefs,
    private val appStrings: com.classrecord.app.i18n.AppStrings
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
        return RosterParser.parse(raw, existing, appStrings)
    }

    suspend fun confirmImport(raw: String): String {
        val preview = previewImport(raw)
        val added = memberRepository.addAll(preview.toInsert)
        val restored = memberRepository.restoreArchived(preview.toRestore)
        return appStrings.importResultMessage(added, restored)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(onBack: () -> Unit, onEdit: (Long?) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ClassRecordApp
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
                title = { Text(stringResource(R.string.members_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showImport = true }) {
                        Icon(Icons.AutoMirrored.Outlined.PlaylistAdd, contentDescription = stringResource(R.string.cd_bulk_import))
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
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.member_edit_add))
            }
        }
    ) { padding ->
        if (members.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.members_empty_title),
                subtitle = stringResource(R.string.members_empty_sub),
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
                            stringResource(R.string.members_section_archived),
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
                        .onFailure { snackbar.showSnackbar(it.message ?: context.getString(R.string.import_failed)) }
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
    val context = LocalContext.current
    val app = context.applicationContext as ClassRecordApp
    val strings = app.container.appStrings
    var text by remember { mutableStateOf("") }
    val preview = remember(text) { onPreview(text) }
    val ellipsis = stringResource(R.string.import_preview_ellipsis)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.import_help),
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
                    placeholder = { Text(stringResource(R.string.import_placeholder)) }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    buildString {
                        append(stringResource(R.string.import_will_add, preview.insertCount))
                        if (preview.restoreCount > 0) {
                            append(stringResource(R.string.import_preview_restore_suffix, preview.restoreCount))
                        }
                        if (preview.skippedDuplicate > 0) {
                            append(stringResource(R.string.import_preview_skip_conflict, preview.skippedDuplicate))
                        }
                        if (preview.skippedBlank > 0) {
                            append(stringResource(R.string.import_preview_skip_blank, preview.skippedBlank))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (preview.toInsert.isNotEmpty()) {
                    Text(
                        preview.toInsert.take(8).joinToString("、") {
                            strings.rosterDisplayLabel(it.name, it.studentNo)
                        } + if (preview.toInsert.size > 8) ellipsis else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (preview.toRestore.isNotEmpty()) {
                    Text(
                        stringResource(
                            R.string.import_will_restore,
                            preview.toRestore.take(6).joinToString("、") { it.name } +
                                if (preview.toRestore.size > 6) ellipsis else ""
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.appColors.success
                    )
                }
                if (preview.conflicts.isNotEmpty()) {
                    Text(
                        preview.conflicts.take(6).joinToString("\n") { it.hint } +
                            if (preview.conflicts.size > 6) "\n$ellipsis" else "",
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
                        stringResource(R.string.import_confirm_both, preview.insertCount, preview.restoreCount)
                    preview.restoreCount > 0 ->
                        stringResource(R.string.import_confirm_restore, preview.restoreCount)
                    else -> stringResource(R.string.import_confirm_import, preview.insertCount)
                }
                Text(label)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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
                    member.studentNo?.let { add(stringResource(R.string.student_no_label, it)) }
                    member.note?.let { add(it) }
                    if (member.archived) add(stringResource(R.string.member_archived))
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
    val context = LocalContext.current
    val app = context.applicationContext as ClassRecordApp
    val vm: MemberEditViewModel = viewModel(
        factory = AppViewModelFactory(app.container, memberId = memberId)
    )
    val host = remember { androidx.compose.material3.SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (memberId == null) R.string.member_edit_add else R.string.member_edit_edit
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                runCatching { vm.save(); onDone() }
                                    .onFailure { host.showSnackbar(context.getString(R.string.err_name_required)) }
                            }
                        },
                        enabled = vm.name.isNotBlank()
                    ) { Text(stringResource(R.string.action_save)) }
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
                label = { Text(stringResource(R.string.csv_col_name)) },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                value = vm.studentNo,
                onValueChange = vm::onStudentNo,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.member_student_no)) },
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.OutlinedTextField(
                value = vm.note,
                onValueChange = vm::onNote,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.member_note)) }
            )
            if (memberId != null) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = vm.archived,
                        onClick = { scope.launch { vm.setArchived(!vm.archived) } },
                        label = {
                            Text(
                                stringResource(
                                    if (vm.archived) R.string.member_archived_chip else R.string.member_archive_chip
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.appColors.warningContainer,
                            selectedLabelColor = MaterialTheme.appColors.onWarning
                        )
                    )
                }
                Text(
                    stringResource(R.string.member_archive_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
