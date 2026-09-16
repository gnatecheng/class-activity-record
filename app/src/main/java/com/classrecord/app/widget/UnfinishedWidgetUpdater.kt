package com.classrecord.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.classrecord.app.ClassRecordApp
import com.classrecord.app.MainActivity
import com.classrecord.app.R
import com.classrecord.app.data.repo.ActivityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object UnfinishedWidgetUpdater {
    fun observe(app: ClassRecordApp, scope: CoroutineScope, repository: ActivityRepository) {
        scope.launch {
            repository.observeList()
                .distinctUntilChanged()
                .collect { items ->
                    updateAll(app, UnfinishedWidgetSnapshot.from(items))
                }
        }
    }

    suspend fun refreshFromDb(context: Context) {
        val app = context.applicationContext as ClassRecordApp
        val snapshot = UnfinishedWidgetSnapshot.from(app.container.activityRepository.snapshotList())
        updateAll(context, snapshot)
    }

    fun updateAll(context: Context, snapshot: UnfinishedWidgetSnapshot) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, UnfinishedWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return
        ids.forEach { id -> bind(context, manager, id, snapshot) }
    }

    fun bind(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        snapshot: UnfinishedWidgetSnapshot
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_unfinished).apply {
            setTextViewText(R.id.widget_label, context.getString(R.string.widget_unfinished))
            setTextViewText(R.id.widget_count, snapshot.count.toString())
            setTextViewText(R.id.widget_caption, snapshot.caption)
            setTextViewText(R.id.widget_detail, snapshot.detail)
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }
        manager.updateAppWidget(appWidgetId, views)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
