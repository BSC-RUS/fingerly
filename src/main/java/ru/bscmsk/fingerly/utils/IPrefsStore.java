package ru.bscmsk.fingerly.utils;

/**
 * Created by izashkalov on 12.01.2018.
 */

public interface IPrefsStore {

    void add(String key, Object value);

    <T> T get(String aesIv, Class<T> tClass, T def);

    boolean contains(String key);

    void remove(String key);
}
