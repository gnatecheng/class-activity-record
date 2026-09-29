package com.classrecord.app.ui.activities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classrecord.app.ClassRecordApp
import com.classrecord.app.data.Money
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.ScopeType
import com.classrecord.app.data.repo.ActivityRepository
import com.classrecord.app.data.repo.MemberRepository
import com.classrecord.app.data.repo.SubGroupRepository
import com.classrecord.app.data.repo.SubGroupWithMembers
import com.classrecord.app.di.AppViewModelFactory
import com.classrecord.app.ui.components.label
import com.classrecord.app.R
import com.classrecord.app.i18n.DateFormats
import androidx.compose.ui.res.stringResource
import com.classrecord.app.ui.theme.appColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WizardState(
    val step: Int = 0,
    val scopeType: ScopeType? = null,
    val subGroupId: Long? = null,
    val type: ActivityType? = null,
    val title: String = "",
    val note: String = "",
    val deadline: Long? = null,
    val paymentYuan: String = "",
    val splitYuan: String = "",
    val splitRows: List<SplitRowUi> = emptyList()
)

data class WizardReady(
    val activeMemberCount: Int = 0,
    val groups: List<SubGroupWithMembers> = emptyList(),
    val activeMembers: List<com.classrecord.app.data.entity.Member> = emptyList()
)

class ActivityWizardViewModel(
    memberRepository: MemberRepository,
    private val subGroupRepository: SubGroupRepository,
    private val activityRepository: ActivityRepository,
    private val appStrings: com.classrecord.app.i18n.AppStrings
) : ViewModel() {
    private val _state = MutableStateFlow(WizardState())
    val state: StateFlow<WizardState> = _state.asStateFlow()

    val ready: StateFlow<WizardReady> = combine(
        memberRepository.observeActive(),
        subGroupRepository.observeWithMembers()
    ) { members, groups ->
        WizardReady(
            activeMemberCount = members.size,
            groups = groups.filter { !it.group.archived },
            activeMembers = members
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WizardReady())

    fun update(block: (WizardState) -> WizardState) = _state.update(block)

    fun next() {
        val members = membersForScope(_state.value, ready.value)
        _state.update {
            val nextStep = (it.step + 1).coerceAtMost(2)
            it.copy(
                step = nextStep,
                splitRows = if (nextStep == 2) {
                    syncSplitRows(
                        it.splitRows,
                        members.map { m -> m.id to (m.name to m.studentNo) }
                    )
                } else {
                    it.splitRows
                }
            )
        }
    }

    fun back() = _state.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }

    suspend fun create(): Long {
        val s = _state.value
        val type = requireNotNull(s.type)
        val scope = requireNotNull(s.scopeType)
        val payment = if (type == ActivityType.PAYMENT) {
            Money.parseYuanToFen(s.paymentYuan) ?: error(appStrings.errInvalidPerPerson())
        } else null
        val split = if (type == ActivityType.SPLIT) {
            Money.parseYuanToFen(s.splitYuan) ?: error(appStrings.errInvalidSplitTotal())
        } else null
        val plan = if (type == ActivityType.SPLIT) s.splitRows.map { it.toShare() } else null
        return activityRepository.create(
            scopeType = scope,
            subGroupId = s.subGroupId,
            type = type,
            title = s.title,
            note = s.note,
            totalAmount = split,
            perPersonDue = payment,
            deadline = s.deadline,
            splitPlan = plan
        )
    }

    fun selectableGroups(ready: WizardReady): List<SubGroupWithMembers> {
        return ready.groups.filter { it.memberIds.isNotEmpty() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityWizardScreen(onCancel: () -> Unit, onCreated: (Long) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ClassRecordApp
    val vm: ActivityWizardViewModel = viewModel(factory = AppViewModelFactory(app.container))
    val state by vm.state.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wizard_title)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 20.dp)
        ) {
            LinearProgressIndicator(
                progress = { (state.step + 1) / 3f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when (state.step) {
                    0 -> stringResource(R.string.wizard_step_pick_scope)
                    1 -> stringResource(R.string.wizard_step_pick_type)
                    else -> stringResource(R.string.wizard_step_fill_details)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            when (state.step) {
                0 -> ScopeStep(
                    state = state,
                    ready = ready,
                    onClass = {
                        vm.update { it.copy(scopeType = ScopeType.CLASS, subGroupId = null) }
                    },
                    onGroup = { id ->
                        vm.update { it.copy(scopeType = ScopeType.SUBGROUP, subGroupId = id) }
                    }
                )
                1 -> TypeStep(
                    selected = state.type,
                    onSelect = { type -> vm.update { it.copy(type = type) } }
                )
                else -> DetailsStep(
                    state = state,
                    memberCount = currentHeadcount(state, ready),
                    onTitle = { v -> vm.update { it.copy(title = v) } },
                    onNote = { v -> vm.update { it.copy(note = v) } },
                    onPayment = { v -> vm.update { it.copy(paymentYuan = v) } },
                    onSplit = { v -> vm.update { it.copy(splitYuan = v) } },
                    onSplitRows = { rows -> vm.update { it.copy(splitRows = rows) } },
                    onPickDate = { showDate = true },
                    onClearDate = { vm.update { it.copy(deadline = null) } }
                )
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (state.step == 0) {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
                } else {
                    TextButton(onClick = { vm.back() }) { Text(stringResource(R.string.action_back_step)) }
                }
                val canNext = when (state.step) {
                    0 -> (state.scopeType == ScopeType.CLASS && ready.activeMemberCount > 0) ||
                        (state.scopeType == ScopeType.SUBGROUP && state.subGroupId != null)
                    1 -> state.type != null
                    else -> state.title.isNotBlank() && amountsOk(state)
                }
                Button(
                    onClick = {
                        if (state.step < 2) {
                            vm.next()
                        } else {
                            scope.launch {
                                runCatching { vm.create() }
                                    .onSuccess { onCreated(it) }
                                    .onFailure { snackbar.showSnackbar(it.message ?: context.getString(R.string.err_create_failed)) }
                            }
                        }
                    },
                    enabled = canNext
                ) {
                    Text(
                        stringResource(
                            if (state.step < 2) R.string.action_next else R.string.action_create
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = state.deadline)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.update { it.copy(deadline = picker.selectedDateMillis) }
                    showDate = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        ) {
            DatePicker(state = picker)
        }
    }
}

private fun membersForScope(state: WizardState, ready: WizardReady): List<com.classrecord.app.data.entity.Member> {
    return when (state.scopeType) {
        ScopeType.CLASS -> ready.activeMembers
        ScopeType.SUBGROUP -> {
            val ids = ready.groups.firstOrNull { it.group.id == state.subGroupId }?.memberIds.orEmpty()
            ready.activeMembers.filter { it.id in ids }
        }
        null -> emptyList()
    }
}

private fun currentHeadcount(state: WizardState, ready: WizardReady): Int {
    return membersForScope(state, ready).size
}

private fun amountsOk(state: WizardState): Boolean {
    return when (state.type) {
        ActivityType.PAYMENT -> Money.parseYuanToFen(state.paymentYuan) != null
        ActivityType.SPLIT -> Money.parseYuanToFen(state.splitYuan) != null &&
            state.splitRows.any { it.included }
        else -> true
    }
}

@Composable
private fun ScopeStep(
    state: WizardState,
    ready: WizardReady,
    onClass: () -> Unit,
    onGroup: (Long) -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(stringResource(R.string.wizard_step_scope), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        val classEnabled = ready.activeMemberCount > 0
        val classTint = MaterialTheme.appColors.classScope
        val classSelected = state.scopeType == ScopeType.CLASS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(selected = classSelected, enabled = classEnabled, onClick = onClass),
            colors = CardDefaults.cardColors(
                containerColor = if (classSelected) classTint.container else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = classSelected,
                    onClick = onClass,
                    enabled = classEnabled
                )
                Column {
                    Text(stringResource(R.string.wizard_whole_class), color = classTint.color)
                    Text(
                        if (classEnabled) {
                            stringResource(R.string.wizard_class_members, ready.activeMemberCount)
                        } else {
                            stringResource(R.string.wizard_add_members_first)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.wizard_subgroups), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.appColors.groupScope.color)
        val selectable = ready.groups.filter { it.memberIds.isNotEmpty() }
        val emptyGroups = ready.groups.filter { it.memberIds.isEmpty() }
        if (selectable.isEmpty() && emptyGroups.isEmpty()) {
            Text(stringResource(R.string.wizard_no_subgroups), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val groupTint = MaterialTheme.appColors.groupScope
        selectable.forEach { group ->
            val chosen = state.scopeType == ScopeType.SUBGROUP && state.subGroupId == group.group.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = chosen, onClick = { onGroup(group.group.id) }),
                colors = CardDefaults.cardColors(
                    containerColor = if (chosen) groupTint.container else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = chosen,
                        onClick = { onGroup(group.group.id) }
                    )
                    Column {
                        Text(group.group.name, color = groupTint.color)
                        Text(
                            stringResource(R.string.count_people, group.memberIds.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        emptyGroups.forEach { group ->
            Text(
                stringResource(R.string.wizard_group_empty_scope, group.group.name),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.appColors.onWarning,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun TypeStep(selected: ActivityType?, onSelect: (ActivityType) -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TypeCard(ActivityType.ATTENDANCE, stringResource(R.string.wizard_type_attendance_desc), selected, onSelect)
        TypeCard(ActivityType.PAYMENT, stringResource(R.string.wizard_type_payment_desc), selected, onSelect)
        TypeCard(ActivityType.SPLIT, stringResource(R.string.wizard_type_split_desc), selected, onSelect)
        TypeCard(ActivityType.CHECKLIST, stringResource(R.string.wizard_type_checklist_desc), selected, onSelect)
    }
}

@Composable
private fun TypeCard(
    type: ActivityType,
    desc: String,
    selected: ActivityType?,
    onSelect: (ActivityType) -> Unit
) {
    val tint = MaterialTheme.appColors.typeTint(type)
    val chosen = selected == type
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(type) },
        colors = CardDefaults.cardColors(
            containerColor = if (chosen) tint.container else tint.container.copy(alpha = 0.38f)
        )
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = chosen, onClick = { onSelect(type) })
            Column {
                Text(type.label(), style = MaterialTheme.typography.titleMedium, color = tint.color)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsStep(
    state: WizardState,
    memberCount: Int,
    onTitle: (String) -> Unit,
    onNote: (String) -> Unit,
    onPayment: (String) -> Unit,
    onSplit: (String) -> Unit,
    onSplitRows: (List<SplitRowUi>) -> Unit,
    onPickDate: () -> Unit,
    onClearDate: () -> Unit
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = state.title,
            onValueChange = onTitle,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.wizard_title_label)) },
            placeholder = {
                Text(
                    when (state.type) {
                        ActivityType.ATTENDANCE -> stringResource(R.string.wizard_title_attendance)
                        ActivityType.PAYMENT -> stringResource(R.string.wizard_title_payment)
                        ActivityType.SPLIT -> stringResource(R.string.wizard_title_split)
                        ActivityType.CHECKLIST -> stringResource(R.string.wizard_title_checklist)
                        null -> stringResource(R.string.wizard_title_label)
                    }
                )
            },
            singleLine = true
        )
        state.type?.let { type ->
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.wizard_templates), style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.classrecord.app.data.ActivityTemplates.titles(type, LocalContext.current).forEach { preset ->
                    FilterChip(
                        selected = state.title == preset,
                        onClick = { onTitle(preset) },
                        label = { Text(preset) }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.note,
            onValueChange = onNote,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.member_note)) }
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onPickDate) {
                Text(
                    state.deadline?.let {
                        stringResource(
                            R.string.deadline_prefix,
                            DateFormats.formatDate(LocalContext.current, it)
                        )
                    } ?: stringResource(R.string.wizard_deadline_set)
                )
            }
            if (state.deadline != null) {
                TextButton(onClick = onClearDate) { Text(stringResource(R.string.wizard_deadline_clear)) }
            }
        }
        when (state.type) {
            ActivityType.PAYMENT -> {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.paymentYuan,
                    onValueChange = onPayment,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.wizard_per_person)) },
                    placeholder = { Text(stringResource(R.string.wizard_per_person_hint)) },
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.wizard_per_person_support)) }
                )
            }
            ActivityType.SPLIT -> {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.splitYuan,
                    onValueChange = onSplit,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.split_edit_total)) },
                    placeholder = { Text(stringResource(R.string.wizard_split_total_hint)) },
                    singleLine = true,
                    supportingText = {
                        Text(stringResource(R.string.wizard_split_scope, memberCount))
                    }
                )
                Spacer(Modifier.height(12.dp))
                SplitPlanEditor(
                    totalYuan = state.splitYuan,
                    rows = state.splitRows,
                    onChange = onSplitRows
                )
            }
            else -> Unit
        }
    }
}
