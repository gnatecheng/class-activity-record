package com.classrecord.app.ui.activities

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import android.net.Uri
import com.classrecord.app.ClassRecordApp
import com.classrecord.app.data.ActivityCopyText
import com.classrecord.app.data.AttachmentStore
import com.classrecord.app.data.MemberSort
import com.classrecord.app.data.Money
import com.classrecord.app.data.csv.Csv
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.prefs.UserPrefs
import com.classrecord.app.data.repo.ActivityDetail
import com.classrecord.app.data.repo.ActivityMemberRow
import com.classrecord.app.data.repo.ActivityRepository
import com.classrecord.app.data.repo.LedgerRepository
import com.classrecord.app.di.AppViewModelFactory
import com.classrecord.app.ui.components.ScopeChip
import com.classrecord.app.ui.components.StatusBadge
import com.classrecord.app.ui.components.TypeChip
import com.classrecord.app.ui.home.formatDate
import com.classrecord.app.ui.home.formatDay
import com.classrecord.app.ui.theme.appColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActivityDetailViewModel(
    private val activityRepository: ActivityRepository,
    private val ledgerRepository: LedgerRepository,
    val attachmentStore: AttachmentStore,
    userPrefs: UserPrefs,
    private val appContext: Context,
    private val activityId: Long
) : ViewModel() {
    private val _onlyUnfinished = MutableStateFlow(true)
    val onlyUnfinished: StateFlow<Boolean> = _onlyUnfinished.asStateFlow()

    val sortByStudentNo = userPrefs.sortByStudentNo

    val detail: StateFlow<ActivityDetail?> = activityRepository.observeDetail(activityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val alreadyInLedger: StateFlow<Boolean> = ledgerRepository.observeHasIncome(activityId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setOnlyUnfinished(value: Boolean) {
        _onlyUnfinished.value = value
    }

    suspend fun onTap(row: ActivityMemberRow) {
        val type = detail.value?.activity?.type ?: return
        when (type) {
            ActivityType.ATTENDANCE -> activityRepository.toggleAttendance(row.member)
            ActivityType.CHECKLIST -> activityRepository.toggleChecklist(row.member)
            ActivityType.PAYMENT, ActivityType.SPLIT -> activityRepository.togglePaid(row.member)
        }
    }

    suspend fun onLongPress(row: ActivityMemberRow) {
        val type = detail.value?.activity?.type ?: return
        if (type == ActivityType.ATTENDANCE) {
            activityRepository.markExcused(row.member)
        }
    }

    suspend fun saveAmounts(
        row: ActivityMemberRow,
        dueYuan: String,
        paidYuan: String,
        note: String,
        markPaid: Boolean?
    ) {
        val due = dueYuan.trim().takeIf { it.isNotEmpty() }?.let {
            Money.parseYuanToFen(it) ?: error("应缴金额格式不对")
        }
        val paid = paidYuan.trim().takeIf { it.isNotEmpty() }?.let {
            Money.parseYuanToFen(it) ?: error("已缴金额格式不对")
        }
        activityRepository.updateAmounts(row.member, due, paid, note, markPaid)
    }

    fun defaultReuseTitle(): String {
        val title = detail.value?.activity?.title.orEmpty()
        return ActivityCopyText.nextPeriodTitle(title, formatDay(System.currentTimeMillis()))
    }

    suspend fun reuse(title: String): Long = activityRepository.reuse(activityId, title)

    suspend fun recordPaidToLedger() {
        val current = detail.value ?: error("事务不存在")
        ledgerRepository.recordPaidTotal(current)
    }

    suspend fun setAttendance(row: ActivityMemberRow, status: MemberStatus) {
        activityRepository.setStatus(row.member, status)
    }

    suspend fun saveAttachment(row: ActivityMemberRow, uri: Uri) {
        val old = row.member.attachmentPath
        val (path, mime) = attachmentStore.saveFromUri(uri, row.member.activityId, row.member.memberId)
        activityRepository.setAttachment(row.member, path, mime)
        if (old != null && old != path) attachmentStore.delete(old)
    }

    suspend fun deleteAttachment(row: ActivityMemberRow) {
        attachmentStore.delete(row.member.attachmentPath)
        activityRepository.setAttachment(row.member, null, null)
    }

    suspend fun applySplit(totalYuan: String, rows: List<SplitRowUi>) {
        val total = Money.parseYuanToFen(totalYuan) ?: error("请填写有效总额")
        activityRepository.applySplitPlan(activityId, total, rows.map { it.toShare() })
    }

    suspend fun progressCsvFile(): java.io.File {
        val current = activityRepository.snapshot(activityId) ?: error("事务不存在")
        val dir = java.io.File(appContext.cacheDir, "export").also { it.mkdirs() }
        val file = java.io.File(dir, "${current.activity.title}-进度.csv")
        file.writeBytes(Csv.withBom(Csv.activityProgress(current)))
        return file
    }

    suspend fun setArchived(archived: Boolean) {
        activityRepository.setArchived(activityId, archived)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityDetailScreen(
    activityId: Long,
    onBack: () -> Unit,
    onOpenedNew: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as ClassRecordApp
    val vm: ActivityDetailViewModel = viewModel(
        factory = AppViewModelFactory(app.container, activityId = activityId)
    )
    val detail by vm.detail.collectAsStateWithLifecycle()
    val onlyUnfinished by vm.onlyUnfinished.collectAsStateWithLifecycle()
    val alreadyInLedger by vm.alreadyInLedger.collectAsStateWithLifecycle()
    val sortByNo by vm.sortByStudentNo.collectAsStateWithLifecycle(initialValue = true)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var editing by remember { mutableStateOf<ActivityMemberRow?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var reuseTitle by remember { mutableStateOf<String?>(null) }
    var rollCall by remember { mutableStateOf(false) }
    var rollIndex by remember { mutableStateOf(0) }
    var splitEditor by remember { mutableStateOf(false) }
    var viewingPath by remember { mutableStateOf<String?>(null) }
    var photoRow by remember { mutableStateOf<ActivityMemberRow?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val row = photoRow
        photoRow = null
        if (uri != null && row != null) {
            scope.launch {
                runCatching { vm.saveAttachment(row, uri) }
                    .onSuccess { snackbar.showSnackbar("已保存凭证") }
                    .onFailure { snackbar.showSnackbar(it.message ?: "无法保存图片") }
            }
        }
    }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val row = photoRow
        val uri = cameraUri
        photoRow = null
        cameraUri = null
        if (ok && row != null && uri != null) {
            scope.launch {
                runCatching { vm.saveAttachment(row, uri) }
                    .onSuccess { snackbar.showSnackbar("已保存凭证") }
                    .onFailure { snackbar.showSnackbar(it.message ?: "无法保存图片") }
            }
        }
    }

    val current = detail
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current?.activity?.title ?: "事务详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("复制摘要") },
                            onClick = {
                                menuOpen = false
                                current?.let {
                                    clipboard.setText(AnnotatedString(ActivityCopyText.summary(it)))
                                    scope.launch { snackbar.showSnackbar("已复制") }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("复制催缴文案") },
                            onClick = {
                                menuOpen = false
                                current?.let {
                                    clipboard.setText(AnnotatedString(ActivityCopyText.reminder(it)))
                                    scope.launch { snackbar.showSnackbar("已复制") }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("分享催缴") },
                            onClick = {
                                menuOpen = false
                                current?.let {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, ActivityCopyText.reminder(it))
                                    }
                                    context.startActivity(Intent.createChooser(send, "分享催缴文案"))
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("再开一期") },
                            onClick = {
                                menuOpen = false
                                reuseTitle = vm.defaultReuseTitle()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导出本事务 CSV") },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    runCatching { vm.progressCsvFile() }
                                        .onSuccess {
                                            com.classrecord.app.ui.settings.shareFile(
                                                context,
                                                it,
                                                "text/csv",
                                                "分享进度 CSV"
                                            )
                                        }
                                        .onFailure { snackbar.showSnackbar(it.message ?: "无法导出") }
                                }
                            }
                        )
                        if (current?.activity?.type == ActivityType.SPLIT) {
                            DropdownMenuItem(
                                text = { Text("调整分摊") },
                                onClick = {
                                    menuOpen = false
                                    splitEditor = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(if (current?.activity?.archived == true) "取消归档" else "归档")
                            },
                            onClick = {
                                menuOpen = false
                                val archived = current?.activity?.archived == true
                                scope.launch {
                                    runCatching { vm.setArchived(!archived) }
                                        .onSuccess {
                                            snackbar.showSnackbar(
                                                if (archived) "已取消归档" else "已归档，可在首页筛选查看"
                                            )
                                        }
                                        .onFailure { snackbar.showSnackbar(it.message ?: "无法更新归档") }
                                }
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (current == null) {
            Column(Modifier.padding(padding).padding(24.dp)) {
                Text("事务不存在或已删除")
            }
            return@Scaffold
        }
        val type = current.activity.type
        val sorted = MemberSort.activityRows(current.rows, sortByNo, pendingFirst = true)
        val visible = if (onlyUnfinished) {
            sorted.filter { it.member.status == MemberStatus.PENDING && it.member.included }
        } else {
            sorted
        }
        val rollList = if (onlyUnfinished) {
            sorted.filter { it.member.status == MemberStatus.PENDING && it.member.included }
        } else {
            sorted.filter { it.member.included }
        }
        val safeRollIndex = if (rollList.isEmpty()) 0 else rollIndex.coerceIn(0, rollList.lastIndex)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TypeChip(type)
                        ScopeChip(current.scopeLabel, current.activity.scopeType)
                        if (current.activity.archived) {
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text("已归档") }
                            )
                        }
                    }
                    current.activity.deadline?.let {
                        Text("截止 ${formatDate(it)}", style = MaterialTheme.typography.bodySmall)
                    }
                    current.activity.note?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (type == ActivityType.SPLIT && current.activity.totalAmount != null) {
                        Text(
                            "总额 ${Money.formatYuan(current.activity.totalAmount!!)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "进度 ${current.doneCount}/${current.totalCount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (current.unfinishedCount == 0) {
                            MaterialTheme.appColors.success
                        } else {
                            MaterialTheme.appColors.warning
                        }
                    )
                    val hint = when (type) {
                        ActivityType.ATTENDANCE -> "点按切换未到/已到，长按请假"
                        ActivityType.PAYMENT, ActivityType.SPLIT -> "点按切换已缴/未缴；点编辑可改金额"
                        ActivityType.CHECKLIST -> "点按切换完成状态"
                    }
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = onlyUnfinished,
                            onClick = { vm.setOnlyUnfinished(!onlyUnfinished) },
                            label = { Text("仅未完成") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.appColors.warningContainer,
                                selectedLabelColor = MaterialTheme.appColors.onWarning
                            )
                        )
                        if (type == ActivityType.ATTENDANCE) {
                            FilterChip(
                                selected = rollCall,
                                onClick = {
                                    rollCall = !rollCall
                                    rollIndex = 0
                                },
                                label = { Text("连续点名") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.appColors.attendance.container,
                                    selectedLabelColor = MaterialTheme.appColors.attendance.onContainer
                                )
                            )
                        }
                    }
                    if (type == ActivityType.PAYMENT || type == ActivityType.SPLIT) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    runCatching { vm.recordPaidToLedger() }
                                        .onSuccess { snackbar.showSnackbar("已记入班费") }
                                        .onFailure { snackbar.showSnackbar(it.message ?: "无法记入") }
                                }
                            },
                            enabled = !alreadyInLedger
                        ) {
                            Text(if (alreadyInLedger) "已收合计已记入班费" else "将已收合计记入班费")
                        }
                    }
                }
            }
            if (rollCall && type == ActivityType.ATTENDANCE) {
                item {
                    val row = rollList.getOrNull(safeRollIndex)
                    RollCallPane(
                        index = safeRollIndex,
                        total = rollList.size,
                        row = row,
                        onPresent = {
                            row?.let {
                                scope.launch { vm.setAttendance(it, MemberStatus.DONE) }
                            }
                        },
                        onAbsent = {
                            row?.let {
                                scope.launch { vm.setAttendance(it, MemberStatus.PENDING) }
                                rollIndex = (rollIndex + 1).coerceAtMost((rollList.size - 1).coerceAtLeast(0))
                            }
                        },
                        onExcused = {
                            row?.let {
                                scope.launch { vm.setAttendance(it, MemberStatus.EXCUSED) }
                            }
                        },
                        onBack = { rollIndex = (rollIndex - 1).coerceAtLeast(0) },
                        onSkip = { rollIndex = (rollIndex + 1).coerceAtMost((rollList.size - 1).coerceAtLeast(0)) },
                        onExit = { rollCall = false }
                    )
                }
            } else if (visible.isEmpty()) {
                item {
                    Text(
                        if (onlyUnfinished) "没有未完成的同学" else "没有成员快照",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(visible, key = { "${it.member.activityId}-${it.member.memberId}" }) { row ->
                    val statusTint = MaterialTheme.appColors.statusTint(row.member.status)
                    ListItem(
                        headlineContent = { Text(row.name) },
                        supportingContent = {
                            Text(buildSupport(type, row))
                        },
                        leadingContent = if (
                            (type == ActivityType.PAYMENT || type == ActivityType.SPLIT) &&
                            row.member.attachmentPath != null
                        ) {
                            {
                                val bmp = remember(row.member.attachmentPath) {
                                    vm.attachmentStore.loadThumb(row.member.attachmentPath)
                                }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "缴费凭证",
                                        modifier = Modifier
                                            .size(48.dp)
                                            .combinedClickable(onClick = {
                                                viewingPath = row.member.attachmentPath
                                            }),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        } else null,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (type == ActivityType.PAYMENT || type == ActivityType.SPLIT) {
                                    IconButton(onClick = { editing = row }) {
                                        Icon(Icons.Outlined.Edit, contentDescription = "编辑金额")
                                    }
                                }
                                StatusBadge(type, row.member.status)
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = statusTint.container.copy(alpha = 0.55f)),
                        modifier = Modifier.combinedClickable(
                            onClick = {
                                scope.launch {
                                    vm.onTap(row)
                                    if (type == ActivityType.ATTENDANCE && row.member.status == MemberStatus.PENDING) {
                                        // no-op; snackbar optional
                                    }
                                }
                            },
                            onLongClick = {
                                if (type == ActivityType.ATTENDANCE) {
                                    scope.launch {
                                        vm.onLongPress(row)
                                        snackbar.showSnackbar("已更新请假状态")
                                    }
                                }
                            }
                        )
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    val pendingReuse = reuseTitle
    if (pendingReuse != null) {
        var editable by remember(pendingReuse) { mutableStateOf(pendingReuse) }
        AlertDialog(
            onDismissRequest = { reuseTitle = null },
            title = { Text("再开一期") },
            text = {
                Column {
                    Text(
                        "将按当前名单重新快照，不复制完成状态。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editable,
                        onValueChange = { editable = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("新事务标题") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            runCatching { vm.reuse(editable) }
                                .onSuccess {
                                    reuseTitle = null
                                    onOpenedNew(it)
                                }
                                .onFailure { snackbar.showSnackbar(it.message ?: "无法创建") }
                        }
                    },
                    enabled = editable.isNotBlank()
                ) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { reuseTitle = null }) { Text("取消") }
            }
        )
    }

    val editRow = editing
    if (editRow != null) {
        AmountDialog(
            row = editRow,
            thumb = remember(editRow.member.attachmentPath) {
                vm.attachmentStore.loadThumb(editRow.member.attachmentPath)
            },
            onDismiss = { editing = null },
            onSave = { due, paid, note, markPaid ->
                scope.launch {
                    runCatching { vm.saveAmounts(editRow, due, paid, note, markPaid) }
                        .onSuccess { editing = null }
                        .onFailure { snackbar.showSnackbar(it.message ?: "保存失败") }
                }
            },
            onPickPhoto = {
                photoRow = editRow
                pickImage.launch("image/*")
            },
            onTakePhoto = {
                photoRow = editRow
                val target = vm.attachmentStore.createCameraTarget(
                    editRow.member.activityId,
                    editRow.member.memberId
                )
                cameraUri = target.second
                takePicture.launch(target.second)
            },
            onViewPhoto = { viewingPath = editRow.member.attachmentPath },
            onDeletePhoto = {
                scope.launch {
                    runCatching { vm.deleteAttachment(editRow) }
                        .onSuccess { snackbar.showSnackbar("已删除凭证") }
                        .onFailure { snackbar.showSnackbar(it.message ?: "无法删除") }
                }
            }
        )
    }

    if (splitEditor && current != null) {
        var totalYuan by remember(current.activity.totalAmount) {
            mutableStateOf(current.activity.totalAmount?.let { Money.formatFen(it) }.orEmpty())
        }
        var rows by remember(current.rows) {
            mutableStateOf(
                current.rows.map {
                    SplitRowUi(
                        memberId = it.member.memberId,
                        name = it.name,
                        studentNo = it.studentNo,
                        included = it.member.included,
                        weight = it.member.weight.coerceAtLeast(1),
                        customYuan = ""
                    )
                }
            )
        }
        AlertDialog(
            onDismissRequest = { splitEditor = false },
            title = { Text("调整分摊") },
            text = {
                Column {
                    OutlinedTextField(
                        value = totalYuan,
                        onValueChange = { totalYuan = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("分摊总额（元）") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    SplitPlanEditor(
                        totalYuan = totalYuan,
                        rows = rows,
                        onChange = { rows = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            runCatching { vm.applySplit(totalYuan, rows) }
                                .onSuccess { splitEditor = false }
                                .onFailure { snackbar.showSnackbar(it.message ?: "无法保存") }
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { splitEditor = false }) { Text("取消") }
            }
        )
    }

    val preview = viewingPath
    if (preview != null) {
        val bmp = remember(preview) { vm.attachmentStore.loadThumb(preview, 1600) }
        AlertDialog(
            onDismissRequest = { viewingPath = null },
            title = { Text("缴费凭证") },
            text = {
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "缴费凭证",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text("无法显示图片")
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingPath = null }) { Text("关闭") }
            }
        )
    }
}

private fun buildSupport(type: ActivityType, row: ActivityMemberRow): String {
    val bits = mutableListOf<String>()
    row.studentNo?.let { bits += "学号 $it" }
    if (!row.member.included) bits += "不参与分摊"
    if (type == ActivityType.PAYMENT || type == ActivityType.SPLIT) {
        bits += "应缴 ${row.member.amountDue?.let { Money.formatYuan(it) } ?: "—"}"
        bits += "已缴 ${row.member.amountPaid?.let { Money.formatYuan(it) } ?: "—"}"
    }
    row.member.note?.let { bits += it }
    return bits.joinToString(" · ")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AmountDialog(
    row: ActivityMemberRow,
    thumb: android.graphics.Bitmap?,
    onDismiss: () -> Unit,
    onSave: (due: String, paid: String, note: String, markPaid: Boolean?) -> Unit,
    onPickPhoto: () -> Unit,
    onTakePhoto: () -> Unit,
    onViewPhoto: () -> Unit,
    onDeletePhoto: () -> Unit
) {
    var due by remember { mutableStateOf(row.member.amountDue?.let { Money.formatFen(it) }.orEmpty()) }
    var paid by remember { mutableStateOf(row.member.amountPaid?.let { Money.formatFen(it) }.orEmpty()) }
    var note by remember { mutableStateOf(row.member.note.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑 ${row.name}") },
        text = {
            Column {
                OutlinedTextField(
                    value = due,
                    onValueChange = { due = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("应缴（元）") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = paid,
                    onValueChange = { paid = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("已缴（元）") },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注") },
                    minLines = 2
                )
                Spacer(Modifier.height(12.dp))
                Text("缴费凭证", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                if (thumb != null) {
                    Image(
                        bitmap = thumb.asImageBitmap(),
                        contentDescription = "缴费凭证",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .combinedClickable(onClick = onViewPhoto),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPickPhoto) { Text("相册") }
                    OutlinedButton(onClick = onTakePhoto) { Text("拍照") }
                    if (row.member.attachmentPath != null) {
                        OutlinedButton(onClick = onDeletePhoto) { Text("删除图片") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onSave(due, paid, note, true) }) { Text("标记已缴") }
                    OutlinedButton(onClick = { onSave(due, paid, note, false) }) { Text("标记未缴") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(due, paid, note, null) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
