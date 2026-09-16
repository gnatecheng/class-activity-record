package com.classrecord.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classrecord.app.ClassRecordApp
import com.classrecord.app.data.MemberSort
import com.classrecord.app.data.backup.BackupRepository
import com.classrecord.app.data.csv.Csv
import com.classrecord.app.data.prefs.ThemeMode
import com.classrecord.app.data.prefs.UserPrefs
import com.classrecord.app.data.repo.ActivityRepository
import com.classrecord.app.data.repo.LedgerRepository
import com.classrecord.app.data.repo.MemberRepository
import com.classrecord.app.di.AppViewModelFactory
import com.classrecord.app.ui.theme.appColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val appContext: Context,
    private val backupRepository: BackupRepository,
    private val memberRepository: MemberRepository,
    private val activityRepository: ActivityRepository,
    private val ledgerRepository: LedgerRepository,
    private val userPrefs: UserPrefs
) : ViewModel() {
    val sortByStudentNo = userPrefs.sortByStudentNo
    val themeMode = userPrefs.themeMode

    fun setSortByStudentNo(value: Boolean) {
        viewModelScope.launch { userPrefs.setSortByStudentNo(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPrefs.setThemeMode(mode) }
    }

    suspend fun backupTo(uri: Uri) = backupRepository.writeZipTo(uri)

    suspend fun backupShareFile(): File = backupRepository.shareableZipFile()

    suspend fun restoreFrom(uri: Uri) = backupRepository.restoreFrom(uri)

    suspend fun writeMembersCsv(uri: Uri) {
        writeBytes(uri, membersCsvBytes())
    }

    suspend fun writeLedgerCsv(uri: Uri) {
        writeBytes(uri, ledgerCsvBytes())
    }

    suspend fun writeActivitiesCsv(uri: Uri) {
        writeBytes(uri, activitiesCsvBytes())
    }

    suspend fun membersCsvBytes(): ByteArray {
        val members = MemberSort.members(
            memberRepository.observeAll().first(),
            sortByStudentNo.first()
        )
        return Csv.withBom(Csv.members(members))
    }

    suspend fun ledgerCsvBytes(): ByteArray {
        return Csv.withBom(Csv.ledger(ledgerRepository.observeAll().first()))
    }

    suspend fun activitiesCsvBytes(): ByteArray {
        return Csv.withBom(Csv.allActivities(activityRepository.snapshotAll()))
    }

    suspend fun shareCsv(name: String, bytes: ByteArray): File {
        val dir = File(appContext.cacheDir, "export").also { it.mkdirs() }
        val file = File(dir, name)
        file.writeBytes(bytes)
        return file
    }

    private fun writeBytes(uri: Uri, bytes: ByteArray) {
        appContext.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error("无法写入文件")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as ClassRecordApp
    val vm: SettingsViewModel = viewModel(factory = AppViewModelFactory(app.container))
    val sortByNo by vm.sortByStudentNo.collectAsStateWithLifecycle(initialValue = true)
    val themeMode by vm.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }
    var pendingSave by remember { mutableStateOf<SaveKind?>(null) }

    val backupSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { vm.backupTo(uri) }
                    .onSuccess { snackbar.showSnackbar("已保存备份") }
                    .onFailure { snackbar.showSnackbar(it.message ?: "备份失败") }
            }
        }
    }
    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) confirmRestore = uri
    }
    val csvSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val kind = pendingSave
        pendingSave = null
        if (uri != null && kind != null) {
            scope.launch {
                runCatching {
                    when (kind) {
                        SaveKind.MEMBERS -> vm.writeMembersCsv(uri)
                        SaveKind.LEDGER -> vm.writeLedgerCsv(uri)
                        SaveKind.ACTIVITIES -> vm.writeActivitiesCsv(uri)
                    }
                }.onSuccess { snackbar.showSnackbar("已导出 CSV") }
                    .onFailure { snackbar.showSnackbar(it.message ?: "导出失败") }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("外观", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("深色主题", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "可跟随系统，或在应用内固定浅色 / 深色。深色模式使用 Material 3 语义色，状态与金额对比清晰。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { vm.setThemeMode(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("名单排序", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("按学号排序", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "学号为空的排在后面，再按姓名。成员名单和事务详情都会使用。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = sortByNo, onCheckedChange = vm::setSortByStudentNo)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("备份与恢复", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                "备份为 zip（含 JSON 与缴费凭证图片）。恢复会覆盖本机全部班级数据。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { backupSaver.launch("班级事务记录-备份.zip") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("备份到文件") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching { vm.backupShareFile() }
                            .onSuccess { shareFile(context, it, "application/zip", "分享备份") }
                            .onFailure { snackbar.showSnackbar(it.message ?: "无法分享") }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("分享备份") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    restorePicker.launch(arrayOf("application/zip", "application/json", "*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("从备份恢复") }

            Spacer(Modifier.height(20.dp))
            Text("导出 CSV", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.appColors.success)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    pendingSave = SaveKind.MEMBERS
                    csvSaver.launch("班级成员.csv")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("导出成员名单") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    pendingSave = SaveKind.ACTIVITIES
                    csvSaver.launch("事务进度.csv")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("导出全部事务进度") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    pendingSave = SaveKind.LEDGER
                    csvSaver.launch("班费账本.csv")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("导出班费账本") }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    scope.launch {
                        runCatching { vm.shareCsv("班级成员.csv", vm.membersCsvBytes()) }
                            .onSuccess { shareFile(context, it, "text/csv", "分享成员 CSV") }
                            .onFailure { snackbar.showSnackbar(it.message ?: "无法分享") }
                    }
                }
            ) { Text("分享成员 CSV") }
            Spacer(Modifier.height(24.dp))
        }
    }

    val pending = confirmRestore
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text("恢复备份？") },
            text = {
                Text("将覆盖本机全部班级数据（成员、小团体、事务、账本和缴费凭证），且无法撤销。确定继续？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pending
                        confirmRestore = null
                        scope.launch {
                            runCatching { vm.restoreFrom(uri) }
                                .onSuccess { snackbar.showSnackbar("已恢复备份") }
                                .onFailure { snackbar.showSnackbar(it.message ?: "恢复失败") }
                        }
                    }
                ) { Text("覆盖并恢复") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = null }) { Text("取消") }
            }
        )
    }
}

private enum class SaveKind { MEMBERS, LEDGER, ACTIVITIES }

fun shareFile(context: Context, file: File, mime: String, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
