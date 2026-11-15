package com.vida.missions;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MissionStore {
    public static final int MAIN = 0, SIDE = 1, DAILY = 2, EVENT = 3;

    private static final String PREFS = "missions";
    private static final String KEY_DATA = "data";
    private static final String KEY_TAB = "tab";

    public static class Mission {
        public long id;
        public String title;
        public String objective;
        public int type;
        public int steps;
        public int progress;

        public boolean done() {
            return progress >= steps;
        }
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static Mission make(long id, String title, String obj, int type, int steps) {
        Mission m = new Mission();
        m.id = id;
        m.title = title;
        m.objective = obj;
        m.type = type;
        m.steps = steps;
        m.progress = 0;
        return m;
    }

    public static synchronized List<Mission> all(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.contains(KEY_DATA)) {
            // First run: a few sample missions so the widget isn't empty.
            List<Mission> seed = new ArrayList<>();
            seed.add(make(1, "Finish the weekly report", "Draft, review, send", MAIN, 3));
            seed.add(make(2, "Water the plants", "", SIDE, 1));
            seed.add(make(3, "Drink water", "8 glasses today", DAILY, 8));
            seed.add(make(4, "Weekend event prep", "Pick up supplies", EVENT, 2));
            save(c, seed);
            return seed;
        }
        List<Mission> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p.getString(KEY_DATA, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Mission m = new Mission();
                m.id = o.getLong("id");
                m.title = o.getString("title");
                m.objective = o.optString("obj", "");
                m.type = o.optInt("type", MAIN);
                m.steps = Math.max(1, o.optInt("steps", 1));
                m.progress = o.optInt("progress", 0);
                out.add(m);
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    private static void save(Context c, List<Mission> list) {
        JSONArray a = new JSONArray();
        try {
            for (Mission m : list) {
                JSONObject o = new JSONObject();
                o.put("id", m.id);
                o.put("title", m.title);
                o.put("obj", m.objective);
                o.put("type", m.type);
                o.put("steps", m.steps);
                o.put("progress", m.progress);
                a.put(o);
            }
        } catch (JSONException ignored) {
        }
        prefs(c).edit().putString(KEY_DATA, a.toString()).apply();
    }

    /** Missions for one tab: unfinished first, finished at the bottom. */
    public static List<Mission> forTab(Context c, int tab) {
        List<Mission> active = new ArrayList<>();
        List<Mission> done = new ArrayList<>();
        for (Mission m : all(c)) {
            if (m.type != tab) continue;
            if (m.done()) done.add(m);
            else active.add(m);
        }
        active.addAll(done);
        return active;
    }

    public static synchronized void add(Context c, String title, String obj, int type, int steps) {
        List<Mission> list = all(c);
        list.add(make(System.currentTimeMillis(), title, obj, type, steps));
        save(c, list);
    }

    /** One more step. Tapping an already-completed mission resets it. */
    public static synchronized void advance(Context c, long id) {
        List<Mission> list = all(c);
        for (Mission m : list) {
            if (m.id == id) {
                m.progress = m.done() ? 0 : m.progress + 1;
                break;
            }
        }
        save(c, list);
    }

    public static synchronized void delete(Context c, long id) {
        List<Mission> list = all(c);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                list.remove(i);
                break;
            }
        }
        save(c, list);
    }

    public static int getTab(Context c) {
        return prefs(c).getInt(KEY_TAB, MAIN);
    }

    public static void setTab(Context c, int tab) {
        prefs(c).edit().putInt(KEY_TAB, tab).apply();
    }
}
