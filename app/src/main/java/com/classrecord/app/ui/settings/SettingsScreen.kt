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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.classrecord.app.BuildConfig
import com.classrecord.app.R
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.DateFormats
import com.classrecord.app.i18n.LocaleApplier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classrecord.app.ClassRecordApp
import com.classrecord.app.data.MemberSort
import com.classrecord.app.data.backup.BackupRepository
import com.classrecord.app.data.csv.Csv
import com.classrecord.app.i18n.AppStrings
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
    private val userPrefs: UserPrefs,
    private val appStrings: AppStrings
) : ViewModel() {
    val sortByStudentNo = userPrefs.sortByStudentNo
    val themeMode = userPrefs.themeMode

    private val _appliedLanguage = MutableStateFlow(LocaleApplier.readAppliedLanguage(appContext))
    val appliedLanguage: StateFlow<AppLanguage> = _appliedLanguage.asStateFlow()

    init {
        viewModelScope.launch { refreshAppliedLanguage() }
    }

    fun setSortByStudentNo(value: Boolean) {
        viewModelScope.launch { userPrefs.setSortByStudentNo(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPrefs.setThemeMode(mode) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPrefs.setAppLanguage(language)
            refreshAppliedLanguage()
        }
    }

    private suspend fun refreshAppliedLanguage() {
        val stored = userPrefs.appLanguage.first()
        when (stored) {
            AppLanguage.SYSTEM -> {
                // Follow system: never push empty locales (preserves OS per-app language).
                _appliedLanguage.value = AppLanguage.SYSTEM
            }
            else -> {
                if (LocaleApplier.readAppliedLanguage(appContext) != stored) {
                    LocaleApplier.apply(appContext, stored, allowClearToSystem = false)
                }
                _appliedLanguage.value = stored
            }
        }
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
        return Csv.withBom(Csv.members(members, appContext, appStrings))
    }

    suspend fun ledgerCsvBytes(): ByteArray {
        return Csv.withBom(Csv.ledger(ledgerRepository.observeAll().first(), appContext, appStrings))
    }

    suspend fun activitiesCsvBytes(): ByteArray {
        return Csv.withBom(Csv.allActivities(activityRepository.snapshotAll(), appContext, appStrings))
    }

    suspend fun shareCsv(name: String, bytes: ByteArray): File {
        val dir = File(appContext.cacheDir, "export").also { it.mkdirs() }
        val file = File(dir, name)
        file.writeBytes(bytes)
        return file
    }

    private fun writeBytes(uri: Uri, bytes: ByteArray) {
        appContext.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error(appStrings.errWriteFile())
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
    val appLanguage by vm.appliedLanguage.collectAsStateWithLifecycle(initialValue = AppLanguage.SYSTEM)
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmRestore by remember { mutableStateOf<Uri?>(null) }
    var pendingSave by remember { mutableStateOf<SaveKind?>(null) }
    val backupFileName = stringResource(R.string.settings_backup_file)
    val csvMembersName = stringResource(R.string.csv_members)
    val csvActivitiesName = stringResource(R.string.csv_activities)
    val csvLedgerName = stringResource(R.string.csv_ledger)
    val githubUrl = stringResource(R.string.about_github_url)

    val backupSaver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { vm.backupTo(uri) }
                    .onSuccess { snackbar.showSnackbar(context.getString(R.string.msg_backup_saved)) }
                    .onFailure { snackbar.showSnackbar(it.message ?: context.getString(R.string.msg_backup_failed)) }
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
                }.onSuccess { snackbar.showSnackbar(context.getString(R.string.msg_csv_exported)) }
                    .onFailure { snackbar.showSnackbar(it.message ?: context.getString(R.string.msg_export_failed)) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
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
            Text(
                stringResource(R.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_theme_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { vm.setThemeMode(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.settings_language_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(stringResource(R.string.settings_language_title), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.settings_language_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = appLanguage == lang,
                                onClick = { vm.setAppLanguage(lang) },
                                label = {
                                    Text(
                                        when (lang) {
                                            AppLanguage.SYSTEM -> stringResource(R.string.lang_system)
                                            AppLanguage.ZH -> stringResource(R.string.lang_zh)
                                            AppLanguage.EN -> stringResource(R.string.lang_en)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.settings_sort_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(stringResource(R.string.settings_sort_by_no), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.settings_sort_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = sortByNo, onCheckedChange = vm::setSortByStudentNo)
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.settings_backup_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_backup_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { backupSaver.launch(backupFileName) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_backup_save)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching { vm.backupShareFile() }
                            .onSuccess {
                                shareFile(
                                    context,
                                    it,
                                    "application/zip",
                                    context.getString(R.string.share_backup)
                                )
                            }
                            .onFailure {
                                snackbar.showSnackbar(it.message ?: context.getString(R.string.msg_share_failed))
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_backup_share)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    restorePicker.launch(arrayOf("application/zip", "application/json", "*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_backup_restore)) }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.settings_export_csv),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.appColors.success
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    pendingSave = SaveKind.MEMBERS
                    csvSaver.launch(csvMembersName)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_export_members)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    pendingSave = SaveKind.ACTIVITIES
                    csvSaver.launch(csvActivitiesName)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_export_activities)) }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    pendingSave = SaveKind.LEDGER
                    csvSaver.launch(csvLedgerName)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_export_ledger)) }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    scope.launch {
                        runCatching {
                            vm.shareCsv(context.getString(R.string.csv_members), vm.membersCsvBytes())
                        }
                            .onSuccess {
                                shareFile(
                                    context,
                                    it,
                                    "text/csv",
                                    context.getString(R.string.share_members_csv)
                                )
                            }
                            .onFailure {
                                snackbar.showSnackbar(it.message ?: context.getString(R.string.msg_share_failed))
                            }
                    }
                }
            ) { Text(stringResource(R.string.settings_share_members_csv)) }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "${stringResource(R.string.settings_about_version)} ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${stringResource(R.string.settings_about_updated)} ${
                            DateFormats.formatBuildTime(context, BuildConfig.BUILD_TIME_ISO)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_about_github), style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { uriHandler.openUri(githubUrl) }) {
                        Text(stringResource(R.string.settings_about_open_github))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    val pending = confirmRestore
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            title = { Text(stringResource(R.string.settings_restore_title)) },
            text = { Text(stringResource(R.string.settings_restore_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pending
                        confirmRestore = null
                        scope.launch {
                            runCatching { vm.restoreFrom(uri) }
                                .onSuccess { snackbar.showSnackbar(context.getString(R.string.msg_restored)) }
                                .onFailure {
                                    snackbar.showSnackbar(it.message ?: context.getString(R.string.msg_restore_failed))
                                }
                        }
                    }
                ) { Text(stringResource(R.string.settings_restore_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
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
