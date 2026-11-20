package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.service.DeckLinkQuickTileService

/**
 * Home-screen remote control widget for one-tap Play/Pause, Mute, Hotspot Listener, and Lock PC.
 */
class DeckLinkRemoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_decklink_remote)
            views.setOnClickPendingIntent(
                R.id.widget_btn_open,
                buildActionIntent(context, 5001, null)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                buildActionIntent(context, 5002, DeckLinkQuickTileService.QUICK_ACTION_PLAY_PAUSE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_mute,
                buildActionIntent(context, 5003, DeckLinkQuickTileService.QUICK_ACTION_MUTE)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_hotspot,
                buildActionIntent(context, 5004, DeckLinkQuickTileService.QUICK_ACTION_HOTSPOT)
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_lock,
                buildActionIntent(context, 5005, DeckLinkQuickTileService.QUICK_ACTION_LOCK_PC)
            )
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun buildActionIntent(
        context: Context,
        requestCode: Int,
        actionName: String?
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (actionName != null) {
                putExtra(DeckLinkQuickTileService.EXTRA_QUICK_ACTION, actionName)
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
