package com.classrecord.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.classrecord.app.data.Money
import com.classrecord.app.data.entity.ClassProfile
import com.classrecord.app.data.entity.ScopeType
import com.classrecord.app.data.repo.ActivityListItem
import com.classrecord.app.data.repo.ActivityRepository
import com.classrecord.app.data.repo.ClassRepository
import com.classrecord.app.data.repo.LedgerRepository
import com.classrecord.app.data.repo.MemberRepository
import com.classrecord.app.data.repo.SubGroupRepository
import com.classrecord.app.ui.components.EmptyState
import com.classrecord.app.ui.components.label
import com.classrecord.app.ui.components.unfinishedHint
import com.classrecord.app.ui.theme.appColors
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HomeUiState(
    val loaded: Boolean = false,
    val classProfile: ClassProfile? = null,
    val memberCount: Int = 0,
    val subGroupCount: Int = 0,
    val ledgerBalanceFen: Long = 0L,
    val activities: List<ActivityListItem> = emptyList()
) {
    val needsOnboarding: Boolean
        get() = classProfile == null || classProfile.name.isBlank()
}

class HomeViewModel(
    classRepository: ClassRepository,
    memberRepository: MemberRepository,
    subGroupRepository: SubGroupRepository,
    activityRepository: ActivityRepository,
    ledgerRepository: LedgerRepository
) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        classRepository.observe(),
        memberRepository.observeActiveCount(),
        subGroupRepository.observeActiveCount(),
        activityRepository.observeListIncludingArchived(),
        ledgerRepository.observeBalanceFen()
    ) { profile, members, groups, activities, balance ->
        HomeUiState(
            loaded = true,
            classProfile = profile,
            memberCount = members,
            subGroupCount = groups,
            ledgerBalanceFen = balance,
            activities = activities
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onEditClass: () -> Unit,
    onMembers: () -> Unit,
    onSubGroups: () -> Unit,
    onNewActivity: () -> Unit,
    onOpenActivity: (Long) -> Unit,
    onLedger: () -> Unit,
    onSettings: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val className = state.classProfile?.name?.ifBlank { "未命名班级" } ?: "未命名班级"
    var menuOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var archiveFilter by remember { mutableStateOf(ActivityArchiveFilter.ACTIVE) }
    val visible = remember(state.activities, query, archiveFilter) {
        ActivityListQuery.filter(state.activities, query, archiveFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(className, fontWeight = FontWeight.SemiBold)
                        Text(
                            "在班 ${state.memberCount} 人",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEditClass) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑班级名称")
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("设置") },
                            onClick = {
                                menuOpen = false
                                onSettings()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewActivity) {
                Text("新建", modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShortcutCard(
                        modifier = Modifier.weight(1f),
                        title = "成员",
                        subtitle = "${state.memberCount} 人",
                        containerColor = MaterialTheme.appColors.memberShortcut,
                        icon = {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                tint = MaterialTheme.appColors.classScope.color
                            )
                        },
                        onClick = onMembers
                    )
                    ShortcutCard(
                        modifier = Modifier.weight(1f),
                        title = "小团体",
                        subtitle = "${state.subGroupCount} 个",
                        containerColor = MaterialTheme.appColors.groupShortcut,
                        icon = {
                            Icon(
                                Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.appColors.groupScope.color
                            )
                        },
                        onClick = onSubGroups
                    )
                }
            }
            item {
                ShortcutCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "班费账本",
                    subtitle = "余额 ${Money.formatYuan(state.ledgerBalanceFen)}",
                    containerColor = MaterialTheme.appColors.ledgerShortcut,
                    icon = {
                        Icon(
                            Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.appColors.money
                        )
                    },
                    onClick = onLedger
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "事务",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("搜索事务") },
                    placeholder = { Text("按标题搜索") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "清除")
                            }
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = archiveFilter == ActivityArchiveFilter.ACTIVE,
                        onClick = { archiveFilter = ActivityArchiveFilter.ACTIVE },
                        label = { Text("进行中") }
                    )
                    FilterChip(
                        selected = archiveFilter == ActivityArchiveFilter.ARCHIVED,
                        onClick = { archiveFilter = ActivityArchiveFilter.ARCHIVED },
                        label = { Text("已归档") }
                    )
                }
            }
            if (visible.isEmpty()) {
                item {
                    EmptyState(
                        title = emptyTitle(state.activities.isEmpty(), query, archiveFilter),
                        subtitle = emptySubtitle(state.activities.isEmpty(), query, archiveFilter)
                    )
                }
            } else {
                items(visible, key = { it.activity.id }) { item ->
                    ActivityCard(item = item, onClick = { onOpenActivity(item.activity.id) })
                }
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActivityCard(item: ActivityListItem, onClick: () -> Unit) {
    val activity = item.activity
    val typeTint = MaterialTheme.appColors.typeTint(activity.type)
    val scopeTint = MaterialTheme.appColors.scopeTint(activity.scopeType == ScopeType.CLASS)
    val progressColor = if (item.unfinishedCount == 0) {
        MaterialTheme.appColors.success
    } else {
        MaterialTheme.appColors.warning
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = typeTint.container)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    activity.type.label(),
                    style = MaterialTheme.typography.labelMedium,
                    color = typeTint.color
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.scopeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = scopeTint.color
                )
                if (activity.archived) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "已归档",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    formatDay(activity.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(activity.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "${unfinishedHint(activity.type, item.unfinishedCount)} · ${item.doneCount}/${item.totalCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = progressColor
            )
        }
    }
}

private fun emptyTitle(
    noActivities: Boolean,
    query: String,
    filter: ActivityArchiveFilter
): String {
    val needle = query.trim()
    return when {
        noActivities && filter == ActivityArchiveFilter.ACTIVE -> "还没有事务"
        needle.isNotEmpty() -> "没有匹配的事务"
        filter == ActivityArchiveFilter.ARCHIVED -> "没有已归档事务"
        else -> "还没有事务"
    }
}

private fun emptySubtitle(
    noActivities: Boolean,
    query: String,
    filter: ActivityArchiveFilter
): String {
    val needle = query.trim()
    return when {
        needle.isNotEmpty() -> "试试其他标题，或切换「进行中 / 已归档」。"
        filter == ActivityArchiveFilter.ARCHIVED -> "在事务详情菜单中可将进行中的事务归档。"
        noActivities -> "点击右下角「新建」，为全班或小团体记录出勤、缴费、分摊或清单。"
        else -> "点击右下角「新建」，为全班或小团体记录出勤、缴费、分摊或清单。"
    }
}

private val dayFmt = DateTimeFormatter.ofPattern("M月d日")

fun formatDay(millis: Long): String {
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(dayFmt)
}

fun formatDate(millis: Long): String {
    val fmt = DateTimeFormatter.ofPattern("yyyy年M月d日")
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(fmt)
}
