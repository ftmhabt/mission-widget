package com.vida.missions;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

public class MissionListService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext());
    }

    private static int diamond(int type) {
        switch (type) {
            case MissionStore.SIDE:
                return R.drawable.ic_diamond_side;
            case MissionStore.DAILY:
                return R.drawable.ic_diamond_daily;
            case MissionStore.EVENT:
                return R.drawable.ic_diamond_event;
            default:
                return R.drawable.ic_diamond_main;
        }
    }

    private static class Factory implements RemoteViewsFactory {
        private final Context ctx;
        private List<MissionStore.Mission> items = new ArrayList<>();

        Factory(Context ctx) {
            this.ctx = ctx;
        }

        @Override
        public void onCreate() {
        }

        @Override
        public void onDataSetChanged() {
            items = MissionStore.forTab(ctx, MissionStore.getTab(ctx));
        }

        @Override
        public void onDestroy() {
            items.clear();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= items.size()) return null;
            MissionStore.Mission m = items.get(position);
            boolean done = m.done();
            boolean multi = !done && m.steps > 1;
            boolean hasObj = !done && m.objective != null && !m.objective.isEmpty();

            RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_item);

            rv.setImageViewResource(R.id.icon, done ? R.drawable.ic_diamond_done : diamond(m.type));

            rv.setViewVisibility(R.id.title, done ? View.GONE : View.VISIBLE);
            rv.setViewVisibility(R.id.title_done, done ? View.VISIBLE : View.GONE);
            rv.setViewVisibility(R.id.obj, hasObj ? View.VISIBLE : View.GONE);
            rv.setViewVisibility(R.id.obj_done, done ? View.VISIBLE : View.GONE);
            rv.setViewVisibility(R.id.bar, multi ? View.VISIBLE : View.GONE);
            rv.setViewVisibility(R.id.prog, multi ? View.VISIBLE : View.GONE);

            if (done) {
                SpannableString s = new SpannableString(m.title);
                s.setSpan(new StrikethroughSpan(), 0, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                rv.setTextViewText(R.id.title_done, s);
            } else {
                rv.setTextViewText(R.id.title, m.title);
                if (hasObj) rv.setTextViewText(R.id.obj, m.objective);
                if (multi) {
                    rv.setProgressBar(R.id.bar, m.steps, m.progress, false);
                    rv.setTextViewText(R.id.prog, m.progress + "/" + m.steps);
                }
            }

            // Tap the row: add a step (or reset if complete). Tap the x: delete.
            Intent advance = new Intent()
                    .putExtra(MissionWidget.EXTRA_ID, m.id)
                    .putExtra(MissionWidget.EXTRA_OP, MissionWidget.OP_ADVANCE);
            rv.setOnClickFillInIntent(R.id.row, advance);

            Intent delete = new Intent()
                    .putExtra(MissionWidget.EXTRA_ID, m.id)
                    .putExtra(MissionWidget.EXTRA_OP, MissionWidget.OP_DELETE);
            rv.setOnClickFillInIntent(R.id.del, delete);

            return rv;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return items.get(position).id;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
