package com.classrecord.app.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CLASS_EDIT = "class_edit"
    const val MEMBERS = "members"
    const val MEMBER_EDIT = "member_edit?memberId={memberId}"
    const val SUBGROUPS = "subgroups"
    const val SUBGROUP_EDIT = "subgroup_edit?groupId={groupId}"
    const val WIZARD = "wizard"
    const val DETAIL = "detail/{activityId}"
    const val LEDGER = "ledger"
    const val SETTINGS = "settings"

    fun memberEdit(memberId: Long? = null): String =
        "member_edit?memberId=${memberId ?: -1L}"

    fun subgroupEdit(groupId: Long? = null): String =
        "subgroup_edit?groupId=${groupId ?: -1L}"

    fun detail(activityId: Long): String = "detail/$activityId"
}
