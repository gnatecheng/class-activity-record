package com.classrecord.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.entity.ScopeType
import com.classrecord.app.ui.theme.appColors

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

fun ActivityType.label(): String = when (this) {
    ActivityType.ATTENDANCE -> "出勤"
    ActivityType.PAYMENT -> "缴费"
    ActivityType.SPLIT -> "费用分摊"
    ActivityType.CHECKLIST -> "清单"
}

fun MemberStatus.label(type: ActivityType): String = when (type) {
    ActivityType.ATTENDANCE -> when (this) {
        MemberStatus.PENDING -> "未到"
        MemberStatus.DONE -> "已到"
        MemberStatus.EXCUSED -> "请假"
    }
    ActivityType.PAYMENT, ActivityType.SPLIT -> when (this) {
        MemberStatus.DONE -> "已缴"
        else -> "未缴"
    }
    ActivityType.CHECKLIST -> when (this) {
        MemberStatus.DONE -> "已完成"
        else -> "未完成"
    }
}

fun unfinishedHint(type: ActivityType, count: Int): String {
    if (count <= 0) return "已完成"
    return when (type) {
        ActivityType.ATTENDANCE -> "${count}人未到"
        ActivityType.PAYMENT, ActivityType.SPLIT -> "${count}人未缴"
        ActivityType.CHECKLIST -> "${count}项未完成"
    }
}

@Composable
fun TypeChip(type: ActivityType, modifier: Modifier = Modifier) {
    val tint = MaterialTheme.appColors.typeTint(type)
    AssistChip(
        onClick = {},
        modifier = modifier,
        label = { Text(type.label()) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = tint.container,
            labelColor = tint.onContainer
        )
    )
}

@Composable
fun ScopeChip(label: String, scopeType: ScopeType, modifier: Modifier = Modifier) {
    val tint = MaterialTheme.appColors.scopeTint(scopeType == ScopeType.CLASS)
    AssistChip(
        onClick = {},
        modifier = modifier,
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = tint.container,
            labelColor = tint.onContainer
        )
    )
}

@Composable
fun StatusBadge(type: ActivityType, status: MemberStatus) {
    val tint = MaterialTheme.appColors.statusTint(status)
    Surface(
        color = tint.container,
        contentColor = tint.onContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            status.label(type),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
