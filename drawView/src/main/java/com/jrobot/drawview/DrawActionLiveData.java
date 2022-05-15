package com.jrobot.drawview;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

public interface DrawActionLiveData {

    /**
     * @param owner
     * @param count 撤销次数
     */
    void observeUndo(LifecycleOwner owner, Observer<Integer> count);

    /**
     * @param owner
     * @param count 重做次数
     */
    void observerRedo(LifecycleOwner owner, Observer<Integer> count);
}
