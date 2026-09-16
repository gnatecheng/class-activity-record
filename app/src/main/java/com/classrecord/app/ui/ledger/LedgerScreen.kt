package com.classrecord.app.ui.ledger

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Delete
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classrecord.app.ClassRecordApp
import com.classrecord.app.data.LedgerMath
import com.classrecord.app.data.Money
import com.classrecord.app.data.entity.LedgerEntry
import com.classrecord.app.data.entity.LedgerType
import com.classrecord.app.data.repo.LedgerRepository
import com.classrecord.app.di.AppViewModelFactory
import com.classrecord.app.ui.components.EmptyState
import com.classrecord.app.ui.home.formatDate
import com.classrecord.app.ui.theme.appColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LedgerUiState(
    val entries: List<LedgerEntry> = emptyList(),
    val balanceFen: Long = 0L
)

class LedgerViewModel(private val ledgerRepository: LedgerRepository) : ViewModel() {
    val state: StateFlow<LedgerUiState> = ledgerRepository.observeAll()
        .map { LedgerUiState(it, LedgerMath.balanceFen(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LedgerUiState())

    suspend fun add(type: LedgerType, yuan: String, title: String, note: String) {
        val fen = Money.parseYuanToFen(yuan) ?: error("请填写有效金额")
        ledgerRepository.add(type, fen, title, note)
    }

    suspend fun delete(id: Long) {
        ledgerRepository.delete(id)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as ClassRecordApp
    val vm: LedgerViewModel = viewModel(factory = AppViewModelFactory(app.container))
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("班费账本") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.appColors.moneyContainer,
                contentColor = MaterialTheme.appColors.onMoney
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "记一笔")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.appColors.ledgerShortcut
                    )
                ) {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text("当前余额", style = MaterialTheme.typography.labelLarge)
                        Text(
                            Money.formatYuan(state.balanceFen),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (state.balanceFen >= 0) {
                                MaterialTheme.appColors.success
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            "收入减支出。缴费事务可在详情页「将已收合计记入班费」。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (state.entries.isEmpty()) {
                item {
                    EmptyState(
                        title = "还没有流水",
                        subtitle = "点右下角记一笔收入或支出，或从缴费/分摊事务记入已收合计。"
                    )
                }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.title) },
                        supportingContent = {
                            val kind = if (entry.type == LedgerType.INCOME) "收入" else "支出"
                            val extra = buildList {
                                add(kind)
                                add(formatDate(entry.createdAt))
                                entry.note?.let { add(it) }
                            }
                            Text(extra.joinToString(" · "))
                        },
                        trailingContent = {
                            Row {
                                Text(
                                    (if (entry.type == LedgerType.INCOME) "+" else "−") +
                                        Money.formatFen(entry.amountFen),
                                    color = if (entry.type == LedgerType.INCOME) {
                                        MaterialTheme.appColors.success
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                                IconButton(onClick = {
                                    scope.launch { vm.delete(entry.id) }
                                }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "删除")
                                }
                            }
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showAdd) {
        LedgerAddDialog(
            onDismiss = { showAdd = false },
            onSave = { type, yuan, title, note ->
                scope.launch {
                    runCatching { vm.add(type, yuan, title, note) }
                        .onSuccess { showAdd = false }
                        .onFailure { snackbar.showSnackbar(it.message ?: "无法保存") }
                }
            }
        )
    }
}

@Composable
private fun LedgerAddDialog(
    onDismiss: () -> Unit,
    onSave: (LedgerType, String, String, String) -> Unit
) {
    var type by remember { mutableStateOf(LedgerType.INCOME) }
    var yuan by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记一笔") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == LedgerType.INCOME,
                        onClick = { type = LedgerType.INCOME },
                        label = { Text("收入") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.appColors.successContainer,
                            selectedLabelColor = MaterialTheme.appColors.onSuccess
                        )
                    )
                    FilterChip(
                        selected = type == LedgerType.EXPENSE,
                        onClick = { type = LedgerType.EXPENSE },
                        label = { Text("支出") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("摘要") },
                    singleLine = true,
                    placeholder = { Text(if (type == LedgerType.INCOME) "例如：班费收入" else "例如：买扫把") }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = yuan,
                    onValueChange = { yuan = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("金额（元）") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注（可选）") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(type, yuan, title, note) },
                enabled = title.isNotBlank() && yuan.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
