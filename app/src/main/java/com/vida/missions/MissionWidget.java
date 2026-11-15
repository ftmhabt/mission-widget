package com.vida.missions;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

public class MissionWidget extends AppWidgetProvider {
    static final String ACTION_TAB = "com.vida.missions.TAB";
    static final String ACTION_ITEM = "com.vida.missions.ITEM";
    static final String EXTRA_TAB = "tab";
    static final String EXTRA_ID = "id";
    static final String EXTRA_OP = "op";
    static final int OP_ADVANCE = 0;
    static final int OP_DELETE = 1;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for (int id : ids) {
            m.updateAppWidget(id, build(c, id));
        }
        m.notifyAppWidgetViewDataChanged(ids, R.id.list);
    }

    @Override
    public void onReceive(Context c, Intent i) {
        String a = i.getAction();
        if (ACTION_TAB.equals(a)) {
            MissionStore.setTab(c, i.getIntExtra(EXTRA_TAB, 0));
            refresh(c);
        } else if (ACTION_ITEM.equals(a)) {
            long id = i.getLongExtra(EXTRA_ID, -1);
            if (i.getIntExtra(EXTRA_OP, OP_ADVANCE) == OP_DELETE) {
                MissionStore.delete(c, id);
            } else {
                MissionStore.advance(c, id);
            }
            refresh(c);
        } else {
            super.onReceive(c, i);
        }
    }

    /** Redraw every placed widget. Also called from AddMissionActivity. */
    public static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, MissionWidget.class));
        for (int id : ids) {
            m.updateAppWidget(id, build(c, id));
        }
        m.notifyAppWidgetViewDataChanged(ids, R.id.list);
    }

    private static RemoteViews build(Context c, int widgetId) {
        RemoteViews rv = new RemoteViews(c.getPackageName(), R.layout.widget);
        int tab = MissionStore.getTab(c);

        // Tabs. The selected tab is "disabled" so its selector shows the gold pill.
        int[] tabIds = {R.id.tab_main, R.id.tab_side, R.id.tab_daily, R.id.tab_event};
        for (int t = 0; t < tabIds.length; t++) {
            rv.setBoolean(tabIds[t], "setEnabled", t != tab);
            Intent i = new Intent(c, MissionWidget.class)
                    .setAction(ACTION_TAB)
                    .putExtra(EXTRA_TAB, t);
            rv.setOnClickPendingIntent(tabIds[t], PendingIntent.getBroadcast(
                    c, 100 + t, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }

        // Done / total for the current tab.
        List<MissionStore.Mission> list = MissionStore.forTab(c, tab);
        int done = 0;
        for (MissionStore.Mission m : list) {
            if (m.done()) done++;
        }
        rv.setTextViewText(R.id.count, list.isEmpty() ? "" : done + "/" + list.size());

        // "+" opens the add-mission dialog.
        Intent add = new Intent(c, AddMissionActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        rv.setOnClickPendingIntent(R.id.add, PendingIntent.getActivity(
                c, 200, add, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        // The list.
        Intent svc = new Intent(c, MissionListService.class);
        svc.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        svc.setData(Uri.parse(svc.toUri(Intent.URI_INTENT_SCHEME)));
        rv.setRemoteAdapter(R.id.list, svc);
        rv.setEmptyView(R.id.list, R.id.empty);

        // Template for taps on list rows. Each row fills in the mission id and operation.
        Intent tpl = new Intent(c, MissionWidget.class).setAction(ACTION_ITEM);
        rv.setPendingIntentTemplate(R.id.list, PendingIntent.getBroadcast(
                c, 300, tpl, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE));

        rv.setViewVisibility(R.id.empty, list.isEmpty() ? View.VISIBLE : View.GONE);
        return rv;
    }
}
