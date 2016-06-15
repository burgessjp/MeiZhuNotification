package com.example.burge.meizhunotification.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.burge.meizhunotification.notification.MeiZhuNotification;
import com.example.burge.meizhunotification.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final MeiZhuNotification notification =
                new MeiZhuNotification.Builder().setContext(MainActivity.this)
                        .setTime(System.currentTimeMillis())
                        .setImgRes(R.drawable.notify)
                        .setTitle("你收到了一条消息")
                        .setContent("人丑就要多读书").build();


        findViewById(R.id.btn_show_window).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                notification.show();

            }
        });

        findViewById(R.id.btn_hide_window).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notification.dismiss();
            }
        });

    }
}
