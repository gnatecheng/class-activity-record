package com.classrecord.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classrecord.app.ClassRecordApp
import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.repo.ClassRepository
import com.classrecord.app.data.repo.MemberRepository
import com.classrecord.app.data.repo.SubGroupRepository
import com.classrecord.app.di.AppViewModelFactory
import com.classrecord.app.ui.theme.appColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.classrecord.app.R

data class OnboardingUiState(
    val step: Int = 0,
    val className: String = "",
    val memberName: String = "",
    val studentNo: String = "",
    val groupName: String = "",
    val selectedMemberIds: Set<Long> = emptySet()
)

class OnboardingViewModel(
    private val classRepository: ClassRepository,
    private val memberRepository: MemberRepository,
    private val subGroupRepository: SubGroupRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(OnboardingUiState())
    val ui: StateFlow<OnboardingUiState> = _ui.asStateFlow()
    val members: StateFlow<List<Member>> = memberRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun update(block: (OnboardingUiState) -> OnboardingUiState) {
        _ui.update(block)
    }

    fun next() {
        _ui.update { it.copy(step = (it.step + 1).coerceAtMost(3)) }
    }

    fun back() {
        _ui.update { it.copy(step = (it.step - 1).coerceAtLeast(0)) }
    }

    suspend fun saveClassName() {
        classRepository.saveName(_ui.value.className)
        next()
    }

    suspend fun addMember() {
        val name = _ui.value.memberName
        memberRepository.add(name, _ui.value.studentNo.ifBlank { null }, null)
        _ui.update { it.copy(memberName = "", studentNo = "") }
    }

    suspend fun saveOptionalGroup() {
        val name = _ui.value.groupName.trim()
        if (name.isNotEmpty() && _ui.value.selectedMemberIds.isNotEmpty()) {
            subGroupRepository.create(name, null, _ui.value.selectedMemberIds)
        }
        next()
    }

    fun toggleMember(id: Long) {
        _ui.update {
            val next = it.selectedMemberIds.toMutableSet()
            if (!next.add(id)) next.remove(id)
            it.copy(selectedMemberIds = next)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinished: (goToWizard: Boolean) -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ClassRecordApp
    val vm: OnboardingViewModel = viewModel(factory = AppViewModelFactory(app.container))
    val ui by vm.ui.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.onboarding_title)) }) },
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
                progress = { (ui.step + 1) / 4f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(Modifier.height(20.dp))
            when (ui.step) {
                0 -> StepClassName(
                    name = ui.className,
                    onName = { vm.update { s -> s.copy(className = it) } },
                    onNext = {
                        scope.launch {
                            runCatching { vm.saveClassName() }
                                .onFailure { snackbar.showSnackbar(context.getString(R.string.err_class_name_required)) }
                        }
                    }
                )
                1 -> StepMembers(
                    members = members,
                    name = ui.memberName,
                    studentNo = ui.studentNo,
                    onName = { vm.update { s -> s.copy(memberName = it) } },
                    onStudentNo = { vm.update { s -> s.copy(studentNo = it) } },
                    onAdd = {
                        scope.launch {
                            runCatching { vm.addMember() }
                                .onFailure { snackbar.showSnackbar(context.getString(R.string.err_member_name_required)) }
                        }
                    },
                    onBack = { vm.back() },
                    onNext = {
                        if (members.isEmpty()) {
                            scope.launch { snackbar.showSnackbar(context.getString(R.string.err_at_least_one_member)) }
                        } else {
                            vm.next()
                        }
                    }
                )
                2 -> StepGroup(
                    members = members,
                    groupName = ui.groupName,
                    selected = ui.selectedMemberIds,
                    onName = { vm.update { s -> s.copy(groupName = it) } },
                    onToggle = vm::toggleMember,
                    onBack = { vm.back() },
                    onSkip = { vm.next() },
                    onNext = {
                        scope.launch {
                            runCatching { vm.saveOptionalGroup() }
                                .onFailure { snackbar.showSnackbar(it.message ?: context.getString(R.string.err_create_subgroup)) }
                        }
                    }
                )
                else -> StepDone(
                    onHome = { onFinished(false) },
                    onWizard = { onFinished(true) }
                )
            }
        }
    }
}

@Composable
private fun StepClassName(name: String, onName: (String) -> Unit, onNext: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text(
            stringResource(R.string.onboarding_class_headline),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_class_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.class_edit_title)) },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.onboarding_class_hint)) }
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
            Text(stringResource(R.string.action_next))
        }
    }
}

@Composable
private fun StepMembers(
    members: List<Member>,
    name: String,
    studentNo: String,
    onName: (String) -> Unit,
    onStudentNo: (String) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.onboarding_members_headline),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.appColors.classScope.color
        )
        Text(
            stringResource(R.string.onboarding_members_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.csv_col_name)) },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = studentNo,
            onValueChange = onStudentNo,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.member_student_no)) },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth(), enabled = name.isNotBlank()) {
            Text(stringResource(R.string.onboarding_add_to_list))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.onboarding_added_count, members.size), style = MaterialTheme.typography.labelLarge)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(members, key = { it.id }) { member ->
                ListItem(
                    headlineContent = { Text(member.name) },
                    supportingContent = member.studentNo?.let {
                        { Text(stringResource(R.string.student_no_label, it)) }
                    }
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back_step)) }
            Button(onClick = onNext) { Text(stringResource(R.string.action_next)) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StepGroup(
    members: List<Member>,
    groupName: String,
    selected: Set<Long>,
    onName: (String) -> Unit,
    onToggle: (Long) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.onboarding_group_headline),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.appColors.groupScope.color
        )
        Text(
            stringResource(R.string.onboarding_group_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = groupName,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.onboarding_subgroup_name)) },
            placeholder = { Text(stringResource(R.string.subgroup_name_hint)) },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.onboarding_select_members), style = MaterialTheme.typography.labelLarge)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(members, key = { it.id }) { member ->
                ListItem(
                    headlineContent = { Text(member.name) },
                    leadingContent = {
                        Checkbox(
                            checked = member.id in selected,
                            onCheckedChange = { onToggle(member.id) }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.action_back_step)) }
            Row {
                TextButton(onClick = onSkip) { Text(stringResource(R.string.onboarding_skip)) }
                Button(
                    onClick = onNext,
                    enabled = groupName.isBlank() || selected.isNotEmpty()
                ) { Text(stringResource(R.string.action_next)) }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StepDone(onHome: () -> Unit, onWizard: () -> Unit) {
    Column {
        Text(
            stringResource(R.string.onboarding_done_headline),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.appColors.success
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_done_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onWizard, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_first_activity))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_go_home))
        }
    }
}
