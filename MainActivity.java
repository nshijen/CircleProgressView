package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.myapplication.circleprogress.CircleProgressView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final CircleProgressView progressView = findViewById(R.id.cpv);
        TextView tvHelloWorld = findViewById(R.id.tvHelloWorld);
        progressView.setBarWidth(10);
        progressView.setRimWidth(10);
        progressView.setRoundToBlock(false);
        progressView.setRimColor(getResources().getColor(android.R.color.white));
        int colorFrom = getResources().getColor(android.R.color.white);
        int colorTo = getResources().getColor(android.R.color.holo_orange_dark);
        int color[] = {colorFrom,colorTo};
        progressView.setBarColor(color);
        progressView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        progressView.setAnimationFinishListener(new CircleProgressView.AnimationFinishListener() {
            @Override
            public void onAnimationUpdateListener() {
                Log.d(" Shijen ", "onAnimationUpdateListener: ");
            }
        });

        tvHelloWorld.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             progressView.setValueAnimated(0,100,6000);
            }
        });
    }




}
