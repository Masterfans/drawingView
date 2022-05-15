package com.jrobot.drawview;

public interface DrawAction {

    boolean redo();

    boolean undo();

    void clean();

    boolean isEmpty();
}
