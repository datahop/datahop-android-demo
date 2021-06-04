package network.datahop.datahopdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.ArrayList;

import datahop.Datahop;
import datahop.ConnectionManager;
import network.datahop.blediscovery.BLEAdvertising;
import network.datahop.blediscovery.BLEServiceDiscovery;
import network.datahop.wifidirect.WifiDirectHotSpot;
import network.datahop.wifidirect.WifiLink;

public class MainActivity extends AppCompatActivity implements ConnectionManager {

    private static final String root = ".datahop";
    ArrayList<String> activePeers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("-----Version :", Datahop.version());
        try {
            BLEServiceDiscovery bleDiscoveryDriver = BLEServiceDiscovery.getInstance(getApplicationContext());
            BLEAdvertising bleAdvertisingDriver = BLEAdvertising.getInstance(getApplicationContext());

            WifiDirectHotSpot hotspot = WifiDirectHotSpot.getInstance(getApplicationContext());
            WifiLink connection = WifiLink.getInstance(getApplicationContext());

            Datahop.init(
                    getApplicationContext().getCacheDir() + "/" + root,
                    this,
                    bleDiscoveryDriver,
                    bleAdvertisingDriver,
                    hotspot,
                    connection
            );

            bleAdvertisingDriver.setNotifier(Datahop.getAdvertisementNotifier());
            bleDiscoveryDriver.setNotifier(Datahop.getDiscoveryNotifier());
            hotspot.setNotifier(Datahop.getWifiHotspotNotifier());
            connection.setNotifier(Datahop.getWifiConnectionNotifier());
            Datahop.startDiscovery();
            Datahop.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("Node Id", Datahop.id());
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
            String Id = Datahop.id();
            final TextView textViewID = this.findViewById(R.id.textview_id);
            textViewID.setText(Id);

            final Button button = findViewById(R.id.button);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Datahop.peers();
                    Log.d("Online : ", String.valueOf(Datahop.isNodeOnline()));
                    Log.d("Addrs : ", Datahop.addrs());
                    Log.d("interface Addrs : ", Datahop.interfaceAddrs());
                    Log.d("peers : ", Datahop.peers());
                    try {
                        Log.d("Size : ", String.valueOf(Datahop.diskUsage()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Datahop.stop();
        Datahop.close();
    }

    @Override
    public void peerConnected(String s) {
        Log.d("*** Peer Connected ***", s);
        activePeers.add(s);
    }

    @Override
    public void peerDisconnected(String s) {
        Log.d("* Peer Disconnected *", s);
        activePeers.remove(s);
    }
}