package com.classrecord.app.ui.activities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.classrecord.app.data.Money
import com.classrecord.app.data.SplitPlan
import com.classrecord.app.data.SplitShare
import com.classrecord.app.ui.theme.appColors

data class SplitRowUi(
    val memberId: Long,
    val name: String,
    val studentNo: String? = null,
    val included: Boolean = true,
    val weight: Int = 1,
    val customYuan: String = ""
) {
    fun toShare(): SplitShare {
        val custom = customYuan.trim().takeIf { it.isNotEmpty() }?.let { Money.parseYuanToFen(it) }
        return SplitShare(
            memberId = memberId,
            included = included,
            weight = weight.coerceAtLeast(1),
            customDueFen = if (included) custom else null
        )
    }
}

fun syncSplitRows(current: List<SplitRowUi>, members: List<Pair<Long, Pair<String, String?>>>): List<SplitRowUi> {
    val existing = current.associateBy { it.memberId }
    return members.map { (id, info) ->
        existing[id]?.copy(name = info.first, studentNo = info.second)
            ?: SplitRowUi(memberId = id, name = info.first, studentNo = info.second)
    }
}

@Composable
fun SplitPlanEditor(
    totalYuan: String,
    rows: List<SplitRowUi>,
    onChange: (List<SplitRowUi>) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalFen = Money.parseYuanToFen(totalYuan)
    val shares = rows.map { it.toShare() }
    val dues = if (totalFen != null && rows.isNotEmpty()) {
        SplitPlan.amounts(totalFen, shares)
    } else {
        List(rows.size) { null }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "勾选参与分摊的同学。可改权重，或填写「固定金额」覆盖自动分摊。余数仍精确到分。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        rows.forEachIndexed { index, row ->
            val due = dues.getOrNull(index)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (row.included) {
                        MaterialTheme.appColors.split.container.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.name, style = MaterialTheme.typography.titleSmall)
                            val sub = buildList {
                                row.studentNo?.let { add("学号 $it") }
                                if (due != null) add("应缴 ${Money.formatYuan(due)}")
                            }.joinToString(" · ")
                            if (sub.isNotEmpty()) {
                                Text(
                                    sub,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (row.included) {
                                        MaterialTheme.appColors.split.color
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                        Text("参与", style = MaterialTheme.typography.labelMedium)
                        Switch(
                            checked = row.included,
                            onCheckedChange = { included ->
                                onChange(rows.mapIndexed { i, item ->
                                    if (i == index) item.copy(included = included) else item
                                })
                            }
                        )
                    }
                    if (row.included) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = row.weight.toString(),
                                onValueChange = { text ->
                                    val w = text.filter { it.isDigit() }.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                    onChange(rows.mapIndexed { i, item ->
                                        if (i == index) item.copy(weight = w) else item
                                    })
                                },
                                modifier = Modifier.widthIn(min = 88.dp).weight(1f),
                                label = { Text("权重") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = row.customYuan,
                                onValueChange = { text ->
                                    onChange(rows.mapIndexed { i, item ->
                                        if (i == index) item.copy(customYuan = text) else item
                                    })
                                },
                                modifier = Modifier.weight(1.4f),
                                label = { Text("固定金额（可选）") },
                                placeholder = { Text("留空则按权重") },
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }
    }
}
