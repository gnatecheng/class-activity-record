package com.classrecord.app.data.demo

internal data class DemoMemberSpec(val name: String, val studentNo: String?)

internal data class DemoActivitySpec(
    val title: String,
    val type: com.classrecord.app.data.entity.ActivityType,
    val note: String?,
    val scopeSubgroup: Boolean = false,
    val perPersonDueFen: Long? = null,
    val totalSplitFen: Long? = null,
    val paidCount: Int? = null,
    /** Members marked done (present / checked off) for attendance or checklist demos. */
    val doneCount: Int? = null,
)

internal object DemoContent {
    data class Pack(
        val className: String,
        val members: List<DemoMemberSpec>,
        val subgroupName: String,
        val subgroupMemberIndices: List<Int>,
        val activities: List<DemoActivitySpec>,
        val ledgerIncomeTitle: String,
        val ledgerExpenseTitle: String,
    )

    val english = Pack(
        className = "Weekend Badminton Club",
        members = listOf(
            DemoMemberSpec("Alice Chen", "2026001"),
            DemoMemberSpec("Bob Liu", "2026002"),
            DemoMemberSpec("Carol Wang", "2026003"),
            DemoMemberSpec("David Zhang", "2026004"),
            DemoMemberSpec("Emma Li", "2026005"),
            DemoMemberSpec("Frank Zhao", "2026006"),
            DemoMemberSpec("Grace Wu", "2026007"),
            DemoMemberSpec("Henry Xu", "2026008"),
            DemoMemberSpec("Ivy Sun", "2026009"),
            DemoMemberSpec("James Tan", "2026010"),
            DemoMemberSpec("Kelly Zhou", "2026011"),
            DemoMemberSpec("Leo Yang", "2026012"),
        ),
        subgroupName = "Squad A",
        subgroupMemberIndices = listOf(0, 1, 2, 3),
        activities = listOf(
            DemoActivitySpec(
                title = "Saturday session check-in",
                type = com.classrecord.app.data.entity.ActivityType.ATTENDANCE,
                note = "Weekly court booking check-in.",
                doneCount = 8,
            ),
            DemoActivitySpec(
                title = "Tournament sign-up",
                type = com.classrecord.app.data.entity.ActivityType.CHECKLIST,
                note = "Sign up for singles and doubles.",
                doneCount = 9,
            ),
            DemoActivitySpec(
                title = "Autumn outing fee",
                type = com.classrecord.app.data.entity.ActivityType.PAYMENT,
                note = "Includes transport and lunch.",
                perPersonDueFen = 20_000L,
                paidCount = 7,
            ),
            DemoActivitySpec(
                title = "Court fee split",
                type = com.classrecord.app.data.entity.ActivityType.SPLIT,
                note = "September court rental for Squad A.",
                scopeSubgroup = true,
                totalSplitFen = 12_000L,
            ),
        ),
        ledgerIncomeTitle = "Group fund top-up",
        ledgerExpenseTitle = "Shuttlecocks",
    )

    val chinese = Pack(
        className = "周末羽毛球群",
        members = listOf(
            DemoMemberSpec("张三", "2026001"),
            DemoMemberSpec("李四", "2026002"),
            DemoMemberSpec("王五", "2026003"),
            DemoMemberSpec("赵六", "2026004"),
            DemoMemberSpec("钱七", "2026005"),
            DemoMemberSpec("孙八", "2026006"),
            DemoMemberSpec("周九", "2026007"),
            DemoMemberSpec("吴十", "2026008"),
            DemoMemberSpec("郑十一", "2026009"),
            DemoMemberSpec("王十二", "2026010"),
            DemoMemberSpec("冯十三", "2026011"),
            DemoMemberSpec("陈十四", "2026012"),
        ),
        subgroupName = "A 组",
        subgroupMemberIndices = listOf(0, 1, 2, 3),
        activities = listOf(
            DemoActivitySpec(
                title = "周六活动点名",
                type = com.classrecord.app.data.entity.ActivityType.ATTENDANCE,
                note = "每周场地签到。",
                doneCount = 8,
            ),
            DemoActivitySpec(
                title = "比赛报名",
                type = com.classrecord.app.data.entity.ActivityType.CHECKLIST,
                note = "单打、双打项目报名。",
                doneCount = 9,
            ),
            DemoActivitySpec(
                title = "秋季出游费用",
                type = com.classrecord.app.data.entity.ActivityType.PAYMENT,
                note = "含交通与午餐。",
                perPersonDueFen = 20_000L,
                paidCount = 7,
            ),
            DemoActivitySpec(
                title = "场地费分摊",
                type = com.classrecord.app.data.entity.ActivityType.SPLIT,
                note = "A 组 9 月场地费。",
                scopeSubgroup = true,
                totalSplitFen = 12_000L,
            ),
        ),
        ledgerIncomeTitle = "团费补充",
        ledgerExpenseTitle = "球费购置",
    )
}
