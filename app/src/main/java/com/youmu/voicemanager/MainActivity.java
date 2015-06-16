package com.youmu.voicemanager;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;


public class MainActivity extends ActionBarActivity {

    private LinearLayout mLayoutRecord;
    private RelativeLayout mLayoutPlay;
    private Button mBtRec;
    private Button mBtPlay;
    private VoiceManager voiceManager;
    private String mRecPath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mLayoutRecord = (LinearLayout) findViewById(R.id.layout_record);
        mLayoutPlay = (RelativeLayout) findViewById(R.id.layout_play);
        mBtRec = (Button) findViewById(R.id.button_rec);
        mBtPlay = (Button) findViewById(R.id.button_play);

        voiceManager = new VoiceManager(MainActivity.this, "/com.youmu.voicemanager/audio");

        voiceManager.setVoiceListener(new VoiceCallBack() {
            @Override
            public void voicePath(String path) {
                mRecPath = path;
            }

            @Override
            public void recFinish() {
                mBtPlay.setVisibility(View.VISIBLE);
            }
        });


        mBtRec.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mLayoutRecord.setVisibility(View.VISIBLE);
                mLayoutPlay.setVisibility(View.GONE);
                mBtPlay.setVisibility(View.INVISIBLE);

                voiceManager.sessionRecord(true);
            }
        });

        mBtPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLayoutRecord.setVisibility(View.GONE);
                mLayoutPlay.setVisibility(View.VISIBLE);

                voiceManager.sessionPlay(true, mRecPath);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
