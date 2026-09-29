package com.classrecord.app.ui.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.repo.ActivityMemberRow
import com.classrecord.app.ui.components.StatusBadge
import com.classrecord.app.ui.theme.appColors
import androidx.compose.ui.res.stringResource
import com.classrecord.app.R

@Composable
fun RollCallPane(
    index: Int,
    total: Int,
    row: ActivityMemberRow?,
    onPresent: () -> Unit,
    onAbsent: () -> Unit,
    onExcused: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onExit: () -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            if (total == 0) {
                stringResource(R.string.roll_none)
            } else {
                stringResource(R.string.roll_progress, index + 1, total)
            },
            style = MaterialTheme.typography.titleMedium,
            color = if (total == 0) {
                MaterialTheme.appColors.success
            } else {
                MaterialTheme.appColors.warning
            }
        )
        Spacer(Modifier.height(12.dp))
        if (row == null) {
            Text(stringResource(R.string.roll_done), color = MaterialTheme.appColors.success)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.roll_back_list)) }
            return
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.appColors.attendance.container)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    row.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.appColors.attendance.onContainer
                )
                row.studentNo?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.student_no_label, it), color = MaterialTheme.appColors.attendance.color)
                }
                Spacer(Modifier.height(12.dp))
                StatusBadge(com.classrecord.app.data.entity.ActivityType.ATTENDANCE, row.member.status)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onPresent,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.roll_present)) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onAbsent,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.roll_absent)) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onExcused,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.roll_excused)) }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, enabled = index > 0) { Text(stringResource(R.string.roll_prev)) }
            TextButton(onClick = onSkip, enabled = index < total - 1) { Text(stringResource(R.string.roll_skip)) }
            TextButton(onClick = onExit) { Text(stringResource(R.string.roll_exit)) }
        }
    }
}

fun nextRollCallIndex(current: Int, size: Int, stayIfShrunk: Boolean = true): Int {
    if (size <= 0) return 0
    if (stayIfShrunk) return current.coerceIn(0, size - 1)
    return (current + 1).coerceAtMost(size - 1)
}
