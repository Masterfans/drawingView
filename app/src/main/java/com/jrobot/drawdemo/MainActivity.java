package com.jrobot.drawdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jrobot.drawview.DrawingStatusListener;
import com.jrobot.drawview.DrawingView;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    DrawingView mDrawingView;
    SeekBar mSeekBar;
    Button mCleanBtn, undoBtn, redoBtn, btnShow,
            btnSave, btnRead;
    SeekBar mSeekBar1;
    String savePath;
    CheckBox checkCap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        savePath = getCacheDir().getPath() + "/drawing.png";

        mDrawingView = findViewById(R.id.drawView);
        mSeekBar = findViewById(R.id.seekBar);
        mSeekBar1 = findViewById(R.id.seekBar1);
        mCleanBtn = findViewById(R.id.btn_clean);
        undoBtn = findViewById(R.id.btn_undo);
        redoBtn = findViewById(R.id.btn_redo);
        btnShow = findViewById(R.id.btn_show);
        btnSave = findViewById(R.id.btn_save);
        btnRead = findViewById(R.id.btn_read);
        checkCap = findViewById(R.id.check_cap);

        mSeekBar.setProgress(mDrawingView.getLineWidth());
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mDrawingView.setLineWidth(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSeekBar1.setProgress(mDrawingView.getMinLength());

        mSeekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mDrawingView.setMinLength(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mCleanBtn.setOnClickListener(v -> {
            mDrawingView.clean();
        });

        mDrawingView.setDrawingStatusListener(new DrawingStatusListener() {
            @Override
            public void onDrawStatus(int drawCount, int redoCount) {
                undoBtn.setEnabled(drawCount != 0);
                redoBtn.setEnabled(redoCount != 0);
            }

            @Override
            public void onDrawComplete() {

            }
        });

        undoBtn.setOnClickListener(v -> {
            mDrawingView.undo();
        });
        redoBtn.setOnClickListener(v -> {
            mDrawingView.redo();
        });

        btnShow.setOnClickListener(v -> {
            showBitmap(mDrawingView.saveToBitmap(checkCap.isChecked(), 10));
        });

        btnSave.setOnClickListener(v -> {
            if (checkSelfPermission(Manifest.permission_group.STORAGE) == PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
            try {
                mDrawingView.saveToFile(savePath, checkCap.isChecked(), 10);
                Log.i(TAG, "onCreate: save path: " + savePath);
                Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        });
        btnRead.setOnClickListener(v -> {
            File file = new File(savePath);
            if (!file.exists()) {
                Toast.makeText(this, "清先保存", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
            showBitmap(bitmap);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {

        }
    }

    private void showBitmap(Bitmap bitmap) {
        View view = getLayoutInflater().inflate(R.layout.layout_dialog, null);
        ImageView imageView = view.findViewById(R.id.image);
        imageView.setImageBitmap(bitmap);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("轨迹图")
                .setView(view)
                .create();
        dialog.show();
    }
}