package com.example.datahopdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import datahop.Datahop;

public class MainActivity extends AppCompatActivity {

    private static final String root = ".datahop";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("-----Version :", Datahop.version());
        try {
            Datahop.init(getApplicationContext().getCacheDir()+"/"+root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("Node Status onCreate", String.valueOf(Datahop.isNodeOnline()));
        if (!Datahop.isNodeOnline()) {
            try {
                Datahop.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onStart() {
        super.onStart();
        Log.d("Node Status onStart", String.valueOf(Datahop.isNodeOnline()));
        try {
            String Id = Datahop.getID();
            final TextView textViewID = this.findViewById(R.id.textview_id);
            textViewID.setText(Id);

            String addrs = Datahop.getAddress();
            final TextView textViewAddrs = this.findViewById(R.id.textview_address);
            textViewAddrs.setText(addrs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}