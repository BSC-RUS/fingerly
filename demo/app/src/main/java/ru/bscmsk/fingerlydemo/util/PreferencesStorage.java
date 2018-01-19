package ru.bscmsk.fingerlydemo.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import ru.bscmsk.fingerly.utils.IPrefsStore;

/**
 * Created by izashkalov on 12.01.2018.
 */

public class PreferencesStorage implements IPrefsStore {
    private final SharedPreferences prefs;

    public PreferencesStorage(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public void add(String key, Object value) {
        if (TextUtils.isEmpty(key) || value == null)
            return;

        if (value instanceof String)
            prefs.edit().putString(key, (String) value).apply();

        if (value instanceof Integer)
            prefs.edit().putInt(key, (Integer) value).apply();

        if (value instanceof Long)
            prefs.edit().putLong(key, (Long) value).apply();

        if (value instanceof Boolean)
            prefs.edit().putBoolean(key, (Boolean) value).apply();

        if (value instanceof Float)
            prefs.edit().putFloat(key, (Float) value).apply();
    }

    @Override
    public <T> T get(String key, Class<T> tClass, T def) {
        Class<T> newClass = null;
        if(TextUtils.isEmpty(key) || tClass == null)
            return def;

        if (tClass.equals(String.class))
            return (T) prefs.getString(key, (String) def);

        if (tClass.equals(Integer.class))
            return (T) Integer.valueOf(prefs.getInt(key, (Integer) def));

        if (tClass.equals(Long.class))
            return (T) Long.valueOf(prefs.getLong(key, (Long) def));

        if (tClass.equals(Boolean.class))
            return (T) Boolean.valueOf(prefs.getBoolean(key, (Boolean) def));

        if (tClass.equals(Boolean.class))
            return (T) Float.valueOf(prefs.getFloat(key, (Float) def));

        return def;
    }

    @Override
    public boolean contains(String key) {
        return prefs.contains(key);
    }

    @Override
    public void remove(String key) {
        prefs.edit().remove(key).apply();
    }
}
