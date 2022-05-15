package com.jrobot.drawview;

public interface DrawingStatusListener {
    /**
     * 通过两个参数判断是否可以撤销和重做
     *
     * @param drawCount 当前画图次数 大于0可以undo操作
     * @param redoCount 撤销的次数 大于0可以redo
     */
    void onDrawStatus(int drawCount, int redoCount);

    /**
     * 每次画完一次轨迹后都会调用
     */
    void onDrawComplete();
}
