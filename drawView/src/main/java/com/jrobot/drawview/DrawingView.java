package com.jrobot.drawview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.util.Pools;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Deque;
import java.util.LinkedList;

public class DrawingView extends View implements DrawAction, DrawActionLiveData {

    //颜色
    private int mDrawColor = Color.BLACK;
    private Paint mPaint;
    private int lineWidth = 5;
    //绘画路径
    private Path path;
    private float downX;
    private float downY;
    private int minLength = 2;
    private Deque<DrawData> mQueue;
    private Deque<DrawData> mRedoQueue;
    private Pools.Pool<DrawData> mPool;
    private DrawingStatusListener mDrawingStatusListener;
    public static final int MAX_SIZE = 50;

    private MutableLiveData<Integer> mRedoLiveData;
    private MutableLiveData<Integer> mUndoLiveData;

    public DrawingView(Context context) {
        super(context);
        init(null, 0);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public DrawingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr);
    }

    private void init(AttributeSet attrs, int defStyleAttr) {
        mPaint = new Paint();
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.DrawingView, 0, defStyleAttr);

        lineWidth = a.getDimensionPixelOffset(R.styleable.DrawingView_lineWidth, lineWidth);
        minLength = a.getInteger(R.styleable.DrawingView_minLength, minLength);
        mDrawColor = a.getColor(R.styleable.DrawingView_lineColor, mDrawColor);

        initPaint();
        mQueue = new LinkedList<>();
        mRedoQueue = new LinkedList<>();
        mPool = new Pools.SimplePool<>(MAX_SIZE);
        path = new Path();
    }

    private void initPaint() {
        mPaint.setColor(mDrawColor);
        mPaint.setStrokeWidth(lineWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);

    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float currentX = event.getX();
        float currentY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = currentX;
                downY = currentY;
                path.moveTo(downX, downY);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = currentX - downX;
                float dy = currentY - downY;
                if (Math.abs(dx) > minLength || Math.abs(dy) > minLength) {
                    path.quadTo((downX + currentX) / 2, (downY + currentY) / 2, currentX, currentY);
                    downX = currentX;
                    downY = currentY;
                }
                break;
            case MotionEvent.ACTION_UP:
                saveCache();
                if (mDrawingStatusListener != null) {
                    mDrawingStatusListener.onDrawComplete();
                }
                notifyStatus();
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (DrawData data : mQueue) {
            if (data != null && !data.path.isEmpty()) {
                canvas.drawPath(data.path, data.paint);
            }
        }
        canvas.drawPath(path, mPaint);
    }

    public void saveToFile(String path) throws IOException {
        saveToFile(path, false, 0);
    }

    public void saveToFile(String path, boolean cleanBlank, int blank) throws IOException {
        Bitmap bitmap = saveToBitmap(cleanBlank, blank);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] buffer = bos.toByteArray();
        if (buffer != null) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
            OutputStream outputStream = new FileOutputStream(file);
            outputStream.write(buffer);
            outputStream.close();
        }
    }

    /**
     * 清空空白边界
     *
     * @param cleanBlank
     * @param blank      边距
     */
    public Bitmap saveToBitmap(boolean cleanBlank, int blank) {
        Bitmap bitmap = getDrawingBitmap();
        if (cleanBlank) {
            bitmap = cleanBlank(bitmap, blank);
        }
        return bitmap;
    }

    public Bitmap getDrawingBitmap() {
        setDrawingCacheEnabled(true);
        buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(getDrawingCache());
        setDrawingCacheEnabled(false);
        return bitmap;
    }

    private Bitmap cleanBlank(Bitmap bitmap, int blank) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int top = 0, left = 0, right = 0, bottom = 0;
        int[] pixels = new int[width];
        Drawable drawable = getBackground();
        int color = Color.WHITE;
        if (drawable instanceof ColorDrawable) {
            color = ((ColorDrawable) drawable).getColor();
        }
        boolean isStop;
        //扫描上边距第一个不等于背景的点
        for (int y = 0; y < height; y++) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1);
            isStop = false;
            for (int pix : pixels) {
                if (pix != color) {
                    isStop = true;
                    top = y;
                    break;
                }
            }
            if (isStop) {
                break;
            }
        }

        //下边距
        for (int y = height - 1; y >= 0; y--) {
            bitmap.getPixels(pixels, 0, width, 0, y, width, 1);
            isStop = false;
            for (int pix : pixels) {
                if (pix != color) {
                    isStop = true;
                    bottom = y;
                    break;
                }
            }
            if (isStop) {
                break;
            }
        }

        pixels = new int[height];
        //左边距
        for (int x = 0; x < width; x++) {
            bitmap.getPixels(pixels, 0, 1, x, 0, 1, height);
            isStop = false;
            for (int pix : pixels) {
                if (pix != color) {
                    isStop = true;
                    left = x;
                    break;
                }
            }
            if (isStop) {
                break;
            }
        }
        //右边距
        for (int x = width - 1; x >= 0; x--) {
            bitmap.getPixels(pixels, 0, 1, x, 0, 1, height);
            isStop = false;
            for (int pix : pixels) {
                if (pix != color) {
                    right = x;
                    isStop = true;
                    break;
                }
            }
            if (isStop) {
                break;
            }
        }

        left = left - blank > 0 ? left - blank : 0;
        top = top - blank > 0 ? top - blank : 0;
        right = right + blank > width - 1 ? width - 1 : right + blank;
        bottom = bottom + blank > height - 1 ? height - 1 : bottom + blank;

        if (left == 0 && top == 0 && right == 0 && bottom == 0) {
            right = 10;
            bottom = 10;
        }
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    }


    public boolean isEmpty() {
        return mQueue.isEmpty();
    }

    @Override
    public void clean() {
        path.reset();
        freeData();
        notifyStatus();
        invalidate();
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public int getDrawColor() {
        return mDrawColor;
    }

    public void setLineWidth(int lineWidth) {
//        saveCache();
        this.lineWidth = lineWidth;
        mPaint.setStrokeWidth(lineWidth);
        invalidate();
    }

    private void saveCache() {
        DrawData data = getDrawData();
        data.path.addPath(path);
        data.paint.set(mPaint);
        mQueue.add(data);
        path.reset();
    }

    public void setLineColor(int color) {
//        saveCache();
        mDrawColor = color;
        mPaint.setColor(color);
        invalidate();
    }

    private void freeData() {
        mQueue.clear();
        mRedoQueue.clear();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        freeData();
    }

    private DrawData getDrawData() {
        DrawData data = mPool.acquire();
        if (data == null) {
            data = new DrawData();
            data.paint = new Paint();
            data.path = new Path();
        } else {
            data.paint.reset();
            data.path.reset();
        }
        return data;
    }

    @Override
    public boolean undo() {
        if (mQueue.size() > 0) {
            //LIFO
            DrawData poll = mQueue.pollLast();
            boolean add = mRedoQueue.add(poll);
            invalidate();
            notifyStatus();
            return add;
        }
        return false;
    }

    @Override
    public boolean redo() {
        if (mRedoQueue.size() > 0) {
            DrawData poll = mRedoQueue.pollLast();
            boolean add = mQueue.add(poll);
            invalidate();
            notifyStatus();
            return add;
        }
        return false;
    }

    public void setDrawingStatusListener(DrawingStatusListener drawingStatusListener) {
        mDrawingStatusListener = drawingStatusListener;
        notifyStatus();
    }

    private void notifyStatus() {
        if (mDrawingStatusListener != null) {
            mDrawingStatusListener.onDrawStatus(mQueue.size(), mRedoQueue.size());
        }
    }

    @Override
    public void observeUndo(LifecycleOwner owner, Observer<Integer> count) {
        mUndoLiveData.observe(owner, count);
    }

    @Override
    public void observerRedo(LifecycleOwner owner, Observer<Integer> count) {
        mRedoLiveData.observe(owner, count);
    }

    class DrawData {
        Path path;
        Paint paint;
    }
}
