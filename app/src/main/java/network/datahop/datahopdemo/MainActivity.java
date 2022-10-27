package network.datahop.datahopdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import datahop.ConnectionManager;
import datahop.Datahop;
import datahop.types.Types;
import network.datahop.blediscovery.BLEAdvertising;
import network.datahop.blediscovery.BLEServiceDiscovery;
import network.datahop.wifidirect.WifiDirectHotSpot;
import network.datahop.wifidirect.WifiLink;

public class MainActivity extends AppCompatActivity implements ConnectionManager {

    private static final String root = ".datahop";
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 2;
    private static final int REQUEST_CHECK_SETTINGS = 111;
    private static final int EXTERNAL_STORAGE_PERMISSION_CODE = 23;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static LocationManager locationManager;

    ArrayList<String> activePeers = new ArrayList<>();
    TextView textViewPeers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("-----Version :", Datahop.version());
        textViewPeers = this.findViewById(R.id.textview_peers);

        // Wifi State enable
        WifiManager wifi = getSystemService(WifiManager.class);
        if (wifi.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
            wifi.setWifiEnabled(true);
        }

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                EXTERNAL_STORAGE_PERMISSION_CODE);

        // Ask Location Permission
        Log.d("checkSelfPermission ", String.valueOf(ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)));
        Log.d("shouldShowRequest ", String.valueOf(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)));
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Activity self = this;
        if (!isGpsEnabled()) {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            builder.setAlwaysShow(true); //this displays dialog box like Google Maps with two buttons - OK and NO,THANKS

            Task<LocationSettingsResponse> task =
                    LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

            task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
                @Override
                public void onComplete(Task<LocationSettingsResponse> task) {
                    try {
                        LocationSettingsResponse response = task.getResult(ApiException.class);
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                    } catch (ApiException exception) {
                        switch (exception.getStatusCode()) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                // Location settings are not satisfied. But could be fixed by showing the
                                // user a dialog.
                                try {
                                    // Cast to a resolvable exception.
                                    ResolvableApiException resolvable = (ResolvableApiException) exception;
                                    // Show the dialog by calling startResolutionForResult(),
                                    // and check the result in onActivityResult().
                                    resolvable.startResolutionForResult(
                                            self,
                                            REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException e) {
                                    // Ignore the error.
                                } catch (ClassCastException e) {
                                    // Ignore, should be an impossible error.
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                // Location settings are not satisfied. However, we have no way to fix the
                                // settings so we won't show the dialog.
                                break;
                        }
                    }
                }
            });
        } else {
            try {
                start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Intent intent = getIntent();
        handleIntent(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);

        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        Toast.makeText(getApplicationContext(), "User has clicked on OK - So GPS is on", Toast.LENGTH_SHORT).show();
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(getApplicationContext(), "User has clicked on NO, THANKS - So GPS is still off.", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    public void onStart() {
        super.onStart();
        Log.d("Node Status onStart", String.valueOf(Datahop.isNodeOnline()));

        if (Datahop.isNodeOnline()) {
            loadData();
        }

        final Button logButton = findViewById(R.id.refresh);
        logButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("Online : ", String.valueOf(Datahop.isNodeOnline()));
                try {
                    Types.StringSlice tags = Types.StringSlice.parseFrom(Datahop.getTags());
                    Log.d("Tags : ", tags.getOutputList().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Log.d("Size : ", String.valueOf(Datahop.diskUsage()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!Datahop.isNodeOnline()) {
                    try {
                        Datahop.start(false, true);
                        Datahop.startDiscovery(true, true, true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        final Button stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Datahop.isNodeOnline()) {
                    try {
                        Datahop.stopDiscovery();
                        Datahop.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "Permissions " + requestCode + " " + permissions + " " + grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d(TAG, "Location accepted");
                    //timers.setLocationPermission(true);
                    //if(timers.getStoragePermission())startService();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d(TAG, "Location not accepted");

                }
                break;
            }
            case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE:

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d(TAG, "Storage accepted");
                    //timers.setStoragePermission(true);
                    //new CreateWallet(getApplicationContext()).execute();
                    //if(timers.getLocationPermission())startService();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    //G.Log(TAG,"Storage not accepted");

                }
        }

        // other 'case' lines to check for other
        // permissions this app might request.

    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Datahop.close();
        if (Datahop.isNodeOnline()) {
            Datahop.stop();
            try {
                Datahop.stopDiscovery();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

    @Override
    public void peerConnected(String s) {
        Log.d("*** Peer Connected ***", s);
        activePeers.add(s);
        this.runOnUiThread(new Runnable() {
            public void run() {
                textViewPeers.setText("Peers :" + activePeers.toString());
            }
        });
    }

    @Override
    public void peerDisconnected(String s) {
        Log.d("* Peer Disconnected *", s);
        activePeers.remove(s);
        this.runOnUiThread(new Runnable() {
            public void run() {
                textViewPeers.setText("Peers :" + activePeers.toString());
            }
        });
    }

    public static boolean isGpsEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void start() throws Exception {
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
            Datahop.start(true, true);
            Datahop.startDiscovery(true, true, true);
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("Node Id", Datahop.id());
        Log.d("Node Status onCreate", String.valueOf(Datahop.isNodeOnline()));
    }

    public void loadData() {
        try {
            String Id = Datahop.id();
            final TextView textViewID = this.findViewById(R.id.textview_id);
            textViewID.setText("Node ID : " + Id);

            final TextView textViewTags = this.findViewById(R.id.textview_tags);
            final TextView textViewDU = this.findViewById(R.id.textview_disk_usage);
            final Handler handler = new Handler();
            Runnable task = new Runnable() {
                @Override
                public void run() {
                    try {
                        Types.StringSlice tags = Types.StringSlice.parseFrom(Datahop.getTags());
                        Log.d("Tags : ", tags.getOutputList().toString());
                        textViewTags.setText("Content List: " + tags.getOutputList().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        String du = String.valueOf(Datahop.diskUsage());
                        Log.d("Tags : ", du);
                        textViewDU.setText("Disk usage: " + calculateProperFileSize(Datahop.diskUsage()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(this, 5000);
                }
            };
            handler.post(task);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String[] fileSizeUnits = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

    public String calculateProperFileSize(double bytes) {
        DecimalFormat df = new DecimalFormat("0.00");

        String sizeToReturn = "";
        int index = 0;
        for (index = 0; index < fileSizeUnits.length; index++) {
            if (bytes < 1024) {
                break;
            }
            bytes = bytes / 1024;
        }

        sizeToReturn = String.valueOf(df.format(bytes)) + " " + fileSizeUnits[index];
        return sizeToReturn;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.handleIntent(intent);
    }


    void handleIntent(Intent intent) {
        if (intent!= null) {
            String action = intent.getAction();
            String type = intent.getType();
            if (Intent.ACTION_SEND.equals(action) && type != null) {
//                CircularProgressIndicator progress = findViewById(R.id.progress);
                if (type.equalsIgnoreCase("text/plain")) {
                    // TODO do something
                } else {
                    Uri selectedUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    if (selectedUri != null) {

                        String[] projection = {MediaStore.MediaColumns.DATA};
                        CursorLoader cursorLoader = new CursorLoader(this, selectedUri, projection, null, null, null);
                        Cursor cursor = cursorLoader.loadInBackground();
                        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
                        cursor.moveToFirst();
                        File file = new File(cursor.getString(column_index));
                        int size = (int) file.length();
                        byte[] bytes = new byte[size];
                        try {
                            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                            buf.read(bytes, 0, bytes.length);
                            buf.close();

                            Datahop.add("/"+file.getName(), bytes, "");
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
//                progress.show();
            }
        }
    }
}