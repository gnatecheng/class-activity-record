package com.classrecord.app.data

import com.classrecord.app.data.entity.ActivityType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ActivityTemplates {
    private val dayFmt = DateTimeFormatter.ofPattern("M月d日")

    fun titles(type: ActivityType, nowMillis: Long = System.currentTimeMillis()): List<String> {
        val local = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val day = local.format(dayFmt)
        val month = "${local.monthValue}月"
        return when (type) {
            ActivityType.ATTENDANCE -> listOf(
                "今日点名（$day）",
                "早读出勤（$day）",
                "晚自习点名（$day）"
            )
            ActivityType.PAYMENT -> listOf(
                "班费（$month）",
                "活动缴费（$day）"
            )
            ActivityType.SPLIT -> listOf(
                "寝室水电分摊",
                "聚餐分摊（$day）"
            )
            ActivityType.CHECKLIST -> listOf(
                "作业提交（$day）",
                "值日检查（$day）"
            )
        }
    }
}
