package com.banico.dmt;

import android.app.DownloadManager;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    Button button;
    TextView downloadPercent;
    long notificationId;
    MutableLiveData<String> DownloadStatus = new MutableLiveData<>();
    MutableLiveData<Integer> DownloadPercent = new MutableLiveData<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.download);
        downloadPercent = findViewById(R.id.download_percent);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download();
            }
        });

        registerReceiver(downloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        DownloadStatus.observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String status) {
                button.setText(status);
            }
        });

        DownloadPercent.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer percent) {
                String message = percent + "% downloaded so far";
                downloadPercent.setText(message);
            }
        });
    }

    private void download() {
        File destination = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Dummy");
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("http://speedtest.ftp.otenet.gr/files/test10Mb.db"));
        request.setTitle("Dummy File")
                .setDescription("Downloading")
                .setDestinationUri(Uri.fromFile(destination))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            notificationId = downloadManager.enqueue(request);
            ObserveDownlandStatus(notificationId);
        }
    }


    private BroadcastReceiver downloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (notificationId == id) {
                Toast.makeText(context, "Download Complete", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void ObserveDownlandStatus(final long notificationId) {

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                String s = getStatus(notificationId);
                int p = getPercent(notificationId);
                DownloadStatus.setValue(s);
                DownloadPercent.setValue(p);
                Log.d("DownloadStatus", "Status: " + s + " : " + p);
                if (!s.equals("download completed")) {
                    new Handler().postDelayed(this, 1000);
                }
            }
        };

        new Handler().postDelayed(runnable, 1000);
    }

    private String getStatus(long id) {
        String Status = "Unknown Status";
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        Cursor cursor = null;
        if (downloadManager != null) {
            cursor = downloadManager.query(query);
        }
        if (cursor != null && cursor.moveToFirst()) {
            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = cursor.getInt(statusIndex);
            switch (status) {
                case DownloadManager.STATUS_FAILED:
                    Status = "download failed";
                    break;
                case DownloadManager.STATUS_PAUSED:
                    Status = "download paused";
                    break;
                case DownloadManager.STATUS_PENDING:
                    Status = "download pending";
                    break;
                case DownloadManager.STATUS_RUNNING:
                    Status = "downloading...";
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    Status = "download completed";
                    break;
            }
        }
        return Status;
    }

    private int getPercent(Long id) {
        float p = 0;
        DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(id);
        Cursor cursor = null;
        if (downloadManager != null) {
            cursor = downloadManager.query(query);
        }
        if (cursor != null && cursor.moveToFirst()) {
            int downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            int sizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int downloaded = cursor.getInt(downloadedIndex);
            int size = cursor.getInt(sizeIndex);
            p = ((float) downloaded / (float) size);
            Log.d("downloadInfo", "getPercent: " + downloaded + " : " + size);
        }
        return (int) (p * 100);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(downloadComplete);
    }
}
