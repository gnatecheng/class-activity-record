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
        className = "Class 2, Grade 12",
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
            DemoMemberSpec("Jack Ma", "2026010"),
            DemoMemberSpec("Kelly Zhou", "2026011"),
            DemoMemberSpec("Leo Yang", "2026012"),
        ),
        subgroupName = "Dorm 301",
        subgroupMemberIndices = listOf(0, 1, 2, 3),
        activities = listOf(
            DemoActivitySpec(
                title = "Morning roll call",
                type = com.classrecord.app.data.entity.ActivityType.ATTENDANCE,
                note = "First period homeroom check-in.",
                doneCount = 8,
            ),
            DemoActivitySpec(
                title = "Sports day sign-up",
                type = com.classrecord.app.data.entity.ActivityType.CHECKLIST,
                note = "Sign up for relay and field events.",
                doneCount = 9,
            ),
            DemoActivitySpec(
                title = "Autumn trip fee",
                type = com.classrecord.app.data.entity.ActivityType.PAYMENT,
                note = "Includes bus and lunch.",
                perPersonDueFen = 20_000L,
                paidCount = 7,
            ),
            DemoActivitySpec(
                title = "Dorm water split",
                type = com.classrecord.app.data.entity.ActivityType.SPLIT,
                note = "September water bill for Dorm 301.",
                scopeSubgroup = true,
                totalSplitFen = 12_000L,
            ),
        ),
        ledgerIncomeTitle = "Class fund top-up",
        ledgerExpenseTitle = "Class supplies",
    )

    val chinese = Pack(
        className = "高三（2）班",
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
        subgroupName = "301 寝室",
        subgroupMemberIndices = listOf(0, 1, 2, 3),
        activities = listOf(
            DemoActivitySpec(
                title = "早自习点名",
                type = com.classrecord.app.data.entity.ActivityType.ATTENDANCE,
                note = "第一节课前签到。",
                doneCount = 8,
            ),
            DemoActivitySpec(
                title = "运动会报名",
                type = com.classrecord.app.data.entity.ActivityType.CHECKLIST,
                note = "接力、田赛项目报名。",
                doneCount = 9,
            ),
            DemoActivitySpec(
                title = "秋游费用",
                type = com.classrecord.app.data.entity.ActivityType.PAYMENT,
                note = "含大巴与午餐。",
                perPersonDueFen = 20_000L,
                paidCount = 7,
            ),
            DemoActivitySpec(
                title = "寝室水电分摊",
                type = com.classrecord.app.data.entity.ActivityType.SPLIT,
                note = "301 寝室 9 月水费。",
                scopeSubgroup = true,
                totalSplitFen = 12_000L,
            ),
        ),
        ledgerIncomeTitle = "班费补充",
        ledgerExpenseTitle = "班级用品",
    )
}
