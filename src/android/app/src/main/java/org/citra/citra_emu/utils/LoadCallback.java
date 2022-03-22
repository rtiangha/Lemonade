package org.citra.citra_emu.utils;

import androidx.annotation.Keep;

public interface LoadCallback<T> {
    @Keep
    void onLoad(T data);

    @Keep
    void onLoadError();
}