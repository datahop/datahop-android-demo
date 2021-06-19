package network.datahop.datahopdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import datahop.Datahop;
import datahop.ConnectionManager;
import datahop.types.Types;
import network.datahop.blediscovery.BLEAdvertising;
import network.datahop.blediscovery.BLEServiceDiscovery;
import network.datahop.wifidirect.WifiDirectHotSpot;
import network.datahop.wifidirect.WifiLink;

public class MainActivity extends AppCompatActivity implements ConnectionManager {

    private static final String root = ".datahop";
    ArrayList<String> activePeers = new ArrayList<>();
    TextView textViewPeers;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("-----Version :", Datahop.version());
        textViewPeers = this.findViewById(R.id.textview_peers);
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
                    Log.d("Online : ", String.valueOf(Datahop.isNodeOnline()));
                    try {
                        Types.StringSlice addrs = Types.StringSlice.parseFrom(Datahop.addrs());
                        Log.d("Addrs : ", addrs.getOutputList().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Log.d("State : ", Datahop.state().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Types.StringSlice ifaceAddrs = Types.StringSlice.parseFrom(Datahop.interfaceAddrs());
                        Log.d("IfaceAddrs : ", ifaceAddrs.getOutputList().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Types.StringSlice peers = Types.StringSlice.parseFrom(Datahop.peers());
                        Log.d("Peers : ", peers.getOutputList().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Types.StringSlice tags = Types.StringSlice.parseFrom(Datahop.getTags());
                        Log.d("Tags : ", tags.getOutputList().toString());

                        if (tags.getOutputList().size() > 0) {
                            String latestTag = tags.getOutput(tags.getOutputList().size()-1);
                            Log.d("Latest Tag ", latestTag);
                            byte[] value = Datahop.get(latestTag);
                            Log.d("Latest Tag value ",  new String(value, StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        Log.d("Size : ", String.valueOf(Datahop.diskUsage()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String unixTime = String.valueOf(System.currentTimeMillis() / 1000L);
                    String value = "Stats called at "+unixTime;
                    try {
                        Datahop.add(unixTime, value.getBytes());
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Datahop.stop();
        Datahop.close();
    }

    @Override
    public void peerConnected(String s) {
        Log.d("*** Peer Connected ***", s);
        activePeers.add(s);
        this.runOnUiThread(new Runnable() {
            public void run() {
                textViewPeers.setText(activePeers.toString());
            }
        });
    }

    @Override
    public void peerDisconnected(String s) {
        Log.d("* Peer Disconnected *", s);
        activePeers.remove(s);
        this.runOnUiThread(new Runnable() {
            public void run() {
                textViewPeers.setText(activePeers.toString());
            }
        });
    }
}