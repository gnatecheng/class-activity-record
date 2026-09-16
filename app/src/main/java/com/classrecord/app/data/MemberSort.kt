package com.classrecord.app.data

import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.repo.ActivityMemberRow

object MemberSort {
    fun members(list: List<Member>, byStudentNo: Boolean): List<Member> {
        return if (byStudentNo) {
            list.sortedWith(
                compareBy<Member> { it.archived }
                    .thenBy { it.studentNo.isNullOrBlank() }
                    .thenBy { it.studentNo.orEmpty() }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        } else {
            list.sortedWith(
                compareBy<Member> { it.archived }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        }
    }

    fun activityRows(
        list: List<ActivityMemberRow>,
        byStudentNo: Boolean,
        pendingFirst: Boolean = true
    ): List<ActivityMemberRow> {
        return list.sortedWith(
            compareBy<ActivityMemberRow> {
                if (pendingFirst) it.member.status != MemberStatus.PENDING else false
            }.thenBy {
                if (byStudentNo) it.studentNo.isNullOrBlank() else false
            }.thenBy {
                if (byStudentNo) it.studentNo.orEmpty() else ""
            }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )
    }
}
