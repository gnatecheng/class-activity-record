package com.classrecord.app.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.classrecord.app.data.AttachmentStore
import com.classrecord.app.data.backup.BackupRepository
import com.classrecord.app.data.db.AppDatabase
import com.classrecord.app.data.prefs.UserPrefs
import com.classrecord.app.data.repo.ActivityRepository
import com.classrecord.app.data.repo.ClassRepository
import com.classrecord.app.data.repo.LedgerRepository
import com.classrecord.app.data.repo.MemberRepository
import com.classrecord.app.data.repo.SubGroupRepository
import com.classrecord.app.ui.activities.ActivityDetailViewModel
import com.classrecord.app.ui.activities.ActivityWizardViewModel
import com.classrecord.app.ui.classprofile.ClassEditViewModel
import com.classrecord.app.ui.home.HomeViewModel
import com.classrecord.app.ui.ledger.LedgerViewModel
import com.classrecord.app.ui.members.MemberEditViewModel
import com.classrecord.app.ui.members.MembersViewModel
import com.classrecord.app.ui.onboarding.OnboardingViewModel
import com.classrecord.app.ui.settings.SettingsViewModel
import com.classrecord.app.ui.subgroups.SubGroupEditViewModel
import com.classrecord.app.ui.subgroups.SubGroupsViewModel

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: AppDatabase = AppDatabase.create(appContext)
    val userPrefs = UserPrefs(appContext)
    val attachmentStore = AttachmentStore(appContext)
    val classRepository = ClassRepository(database)
    val memberRepository = MemberRepository(database)
    val subGroupRepository = SubGroupRepository(database)
    val activityRepository = ActivityRepository(database)
    val ledgerRepository = LedgerRepository(database)
    val backupRepository = BackupRepository(appContext, database, attachmentStore)
}

class AppViewModelFactory(
    private val container: AppContainer,
    private val memberId: Long? = null,
    private val groupId: Long? = null,
    private val activityId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val vm: ViewModel = when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) ->
                HomeViewModel(
                    container.classRepository,
                    container.memberRepository,
                    container.subGroupRepository,
                    container.activityRepository,
                    container.ledgerRepository
                )
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) ->
                OnboardingViewModel(
                    container.classRepository,
                    container.memberRepository,
                    container.subGroupRepository
                )
            modelClass.isAssignableFrom(ClassEditViewModel::class.java) ->
                ClassEditViewModel(container.classRepository)
            modelClass.isAssignableFrom(MembersViewModel::class.java) ->
                MembersViewModel(container.memberRepository, container.userPrefs)
            modelClass.isAssignableFrom(MemberEditViewModel::class.java) ->
                MemberEditViewModel(container.memberRepository, memberId)
            modelClass.isAssignableFrom(SubGroupsViewModel::class.java) ->
                SubGroupsViewModel(container.subGroupRepository)
            modelClass.isAssignableFrom(SubGroupEditViewModel::class.java) ->
                SubGroupEditViewModel(
                    container.subGroupRepository,
                    container.memberRepository,
                    groupId
                )
            modelClass.isAssignableFrom(ActivityWizardViewModel::class.java) ->
                ActivityWizardViewModel(
                    container.memberRepository,
                    container.subGroupRepository,
                    container.activityRepository
                )
            modelClass.isAssignableFrom(ActivityDetailViewModel::class.java) ->
                ActivityDetailViewModel(
                    container.activityRepository,
                    container.ledgerRepository,
                    container.attachmentStore,
                    container.userPrefs,
                    container.appContext,
                    requireNotNull(activityId)
                )
            modelClass.isAssignableFrom(LedgerViewModel::class.java) ->
                LedgerViewModel(container.ledgerRepository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    container.appContext,
                    container.backupRepository,
                    container.memberRepository,
                    container.activityRepository,
                    container.ledgerRepository,
                    container.userPrefs
                )
            else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
        }
        return vm as T
    }
}
