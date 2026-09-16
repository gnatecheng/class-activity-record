package com.classrecord.app.ui.subgroups

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.entity.SubGroup
import com.classrecord.app.data.repo.MemberRepository
import com.classrecord.app.data.repo.SubGroupRepository
import com.classrecord.app.data.repo.SubGroupWithMembers
import com.classrecord.app.di.AppViewModelFactory
import com.classrecord.app.ui.components.EmptyState
import com.classrecord.app.ui.theme.appColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SubGroupsViewModel(subGroupRepository: SubGroupRepository) : ViewModel() {
    val groups: StateFlow<List<SubGroupWithMembers>> = subGroupRepository.observeWithMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubGroupsScreen(onBack: () -> Unit, onEdit: (Long?) -> Unit) {
    val app = LocalContext.current.applicationContext as ClassRecordApp
    val vm: SubGroupsViewModel = viewModel(factory = AppViewModelFactory(app.container))
    val groups by vm.groups.collectAsStateWithLifecycle()
    val active = groups.filter { !it.group.archived }
    val archived = groups.filter { it.group.archived }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("班内小团体") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEdit(null) },
                containerColor = MaterialTheme.appColors.groupScope.container,
                contentColor = MaterialTheme.appColors.groupScope.onContainer
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "添加小团体")
            }
        }
    ) { padding ->
        if (groups.isEmpty()) {
            EmptyState(
                title = "还没有小团体",
                subtitle = "宿舍、值日组都可以。空的小团体不能作为事务范围。",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(active, key = { it.group.id }) { item ->
                    GroupRow(item, onClick = { onEdit(item.group.id) })
                }
                if (archived.isNotEmpty()) {
                    item {
                        Text(
                            "已归档",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(archived, key = { it.group.id }) { item ->
                        GroupRow(item, onClick = { onEdit(item.group.id) })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun GroupRow(item: SubGroupWithMembers, onClick: () -> Unit) {
    val preview = item.memberNames.take(6).joinToString("、")
    val empty = item.memberNames.isEmpty()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                item.group.archived -> MaterialTheme.colorScheme.surfaceVariant
                empty -> MaterialTheme.appColors.warningContainer
                else -> MaterialTheme.appColors.groupShortcut
            }
        )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    item.group.name,
                    color = MaterialTheme.appColors.groupScope.onContainer
                )
            },
            supportingContent = {
                val extra = if (empty) {
                    "暂无成员，不能作为事务范围"
                } else {
                    "${item.memberIds.size} 人" + if (preview.isNotBlank()) " · $preview" else ""
                }
                Text(
                    extra,
                    color = if (empty) {
                        MaterialTheme.appColors.onWarning
                    } else {
                        MaterialTheme.appColors.groupScope.color
                    }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

class SubGroupEditViewModel(
    private val subGroupRepository: SubGroupRepository,
    memberRepository: MemberRepository,
    private val groupId: Long?
) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var note by mutableStateOf("")
        private set
    var archived by mutableStateOf(false)
        private set
    var selected by mutableStateOf(setOf<Long>())
        private set
    private var existing: SubGroup? = null

    val members: StateFlow<List<Member>> = memberRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (groupId != null) {
            viewModelScope.launch {
                val group = subGroupRepository.getById(groupId)
                existing = group
                if (group != null) {
                    name = group.name
                    note = group.note.orEmpty()
                    archived = group.archived
                    selected = subGroupRepository.getMemberIds(groupId).toSet()
                }
            }
        }
    }

    fun onName(v: String) { name = v }
    fun onNote(v: String) { note = v }

    fun toggle(id: Long) {
        selected = selected.toMutableSet().also {
            if (!it.add(id)) it.remove(id)
        }
    }

    suspend fun save() {
        val current = existing
        if (current == null) {
            subGroupRepository.create(name, note, selected)
        } else {
            subGroupRepository.update(
                current.copy(name = name, note = note, archived = archived),
                selected
            )
        }
    }

    suspend fun setArchived(value: Boolean) {
        val current = existing ?: return
        archived = value
        subGroupRepository.setArchived(current, value)
        existing = current.copy(archived = value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubGroupEditScreen(groupId: Long?, onDone: () -> Unit) {
    val app = LocalContext.current.applicationContext as ClassRecordApp
    val vm: SubGroupEditViewModel = viewModel(
        factory = AppViewModelFactory(app.container, groupId = groupId)
    )
    val members by vm.members.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (groupId == null) "新建小团体" else "编辑小团体") },
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
                                    .onFailure { snackbar.showSnackbar("请填写名称") }
                            }
                        },
                        enabled = vm.name.isNotBlank()
                    ) { Text("保存") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = vm.name,
                        onValueChange = vm::onName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("名称") },
                        placeholder = { Text("例如：3号宿舍") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = vm.note,
                        onValueChange = vm::onNote,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("备注（可选）") }
                    )
                    if (groupId != null) {
                        Spacer(Modifier.height(12.dp))
                        Row {
                            FilterChip(
                                selected = vm.archived,
                                onClick = { scope.launch { vm.setArchived(!vm.archived) } },
                                label = { Text(if (vm.archived) "已归档" else "归档") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.appColors.warningContainer,
                                    selectedLabelColor = MaterialTheme.appColors.onWarning
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("选择成员（可多选，可为空）", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "修改成员不会影响已经创建的历史事务。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(members, key = { it.id }) { member ->
                ListItem(
                    headlineContent = { Text(member.name) },
                    supportingContent = member.studentNo?.let { { Text("学号 $it") } },
                    leadingContent = {
                        Checkbox(
                            checked = member.id in vm.selected,
                            onCheckedChange = { vm.toggle(member.id) }
                        )
                    },
                    modifier = Modifier.clickable { vm.toggle(member.id) }
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
