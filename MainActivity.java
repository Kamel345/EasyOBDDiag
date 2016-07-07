package com.example.kamel.easyobddiag;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.EngineCoolantTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final int START_DATA = 0;
    private static final int STOP_DATA = 1;
    private static final int SETTINGS = 2;
    private static boolean bluetoothDefaultIsEnable = false;

    private SensorManager sensorManager;
    private Sensor orientSensor = null;
    private TextView compass;
    private TextView speed;
    private TextView rpm;
    private TextView cooltemp;

    private BluetoothDevice dev = null;
    private BluetoothSocket sock = null;

    private boolean stan = true;



    GPSControl gps;

    SharedPreferences prefs;



    /* Funkcja obsługująca prace żyroskopu. Pobieramy dane i przetwarzamy
     * je na kierunki świata po czym zostają przekazane do TextView w
     * celu prezentacji.
     */
    private final SensorEventListener orientListener = new SensorEventListener() {

        public void onSensorChanged(SensorEvent event) {
            compass = (TextView) findViewById(R.id.tvCompass);
            float x = event.values[0];
            String symbol = "";
            if (x >= 337.5 || x < 22.5) {
                symbol = "N";
            } else if (x >= 22.5 && x < 67.5) {
                symbol = "NE";
            } else if (x >= 67.5 && x < 112.5) {
                symbol = "E";
            } else if (x >= 112.5 && x < 157.5) {
                symbol = "SE";
            } else if (x >= 157.5 && x < 202.5) {
                symbol = "S";
            } else if (x >= 202.5 && x < 247.5) {
                symbol = "SW";
            } else if (x >= 247.5 && x < 292.5) {
                symbol = "W";
            } else if (x >= 292.5 && x < 337.5) {
                symbol = "NW";
            }
            compass.setText(symbol);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // do nothing
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Switch setBt = (Switch) findViewById(R.id.swBluetooth);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();

        if(isEnabled)
        {
            setBt.setChecked(true);
        }

        setBt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    setBluetooth(true);
                } else {
                    setBluetooth(false);
                }
            }
        });

        gps = new GPSControl(MainActivity.this);

        if (gps.canGetLocation()) {

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            DecimalFormat df = new DecimalFormat();

            df.setMaximumFractionDigits(4);
            df.setMinimumFractionDigits(4);

            // \n is for new line
            Toast.makeText(getApplicationContext(), "Twoja lokalizacja - \nSzerokość: " + df.format(latitude) + "\nDługość: " + df.format(longitude), Toast.LENGTH_LONG).show();
        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }


        //Aktywacja żyroskopu
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        if (sensors.size() > 0)
            orientSensor = sensors.get(0);

    }

    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resuming..");
        //Aktualizowanie "kompasu"
        sensorManager.registerListener(orientListener, orientSensor,
                SensorManager.SENSOR_DELAY_UI);

    }

    protected void onDestroy() {
        super.onDestroy();

        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null && btAdapter.isEnabled() && !bluetoothDefaultIsEnable)
            btAdapter.disable();
    }

    //Tworzenie menu akcji na pasku aplikacji
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, START_DATA, 0, getString(R.string.menu_start_data));
        menu.add(0, STOP_DATA, 0, getString(R.string.menu_stop_data));
        menu.add(0, SETTINGS, 0, getString(R.string.menu_settings));
        return true;
    }

    //Dodawianie funkcjonalnosci do opcji w menu akcji
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case START_DATA:
                startData();
                return true;
            case STOP_DATA:
                stopData();
                return true;
            case SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return false;
    }

    //Funkcja obsługująca włączenie/wyłączenie Bluetooth
    public static boolean setBluetooth(boolean enable) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            return bluetoothAdapter.enable();
        } else if (!enable && isEnabled) {
            return bluetoothAdapter.disable();
        }
        return true;
    }


    public void startData()
    {
        Log.d(TAG, "Laczenie...");

        stan = true;

        prefs = getSharedPreferences("pref", Activity.MODE_PRIVATE);

        // get the remote Bluetooth device
        final String remoteDevice = "00:0D:18:3A:67:89";//prefs.getString(SettingsActivity.BLUETOOTH_LIST, null);
        if (remoteDevice == ""){ //|| "".equals(remoteDevice)) {
            Toast.makeText(getApplicationContext(), getString(R.string.text_bluetooth_nodevice), Toast.LENGTH_LONG).show();

            // log error
            Log.e(TAG, "Nie wybrano urządzenie Bluetooth");
            Log.e(remoteDevice, "Urządzenie :");

        }
        else
        {
            final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            dev = btAdapter.getRemoteDevice(remoteDevice);
            Log.d(TAG, "Stopping Bluetooth discovery.");
            btAdapter.cancelDiscovery();

            try
            {
                sock = BluetoothManager.connect(dev);
                Log.e(TAG, "Połączono");

                new EchoOffCommand().run(sock.getInputStream(), sock.getOutputStream());

                new LineFeedOffCommand().run(sock.getInputStream(), sock.getOutputStream());

                new TimeoutCommand(125).run(sock.getInputStream(), sock.getOutputStream());

                new SelectProtocolCommand(ObdProtocols.AUTO).run(sock.getInputStream(), sock.getOutputStream());

                RPMCommand engineRPM = new RPMCommand();
                SpeedCommand speedValue = new SpeedCommand();
                EngineCoolantTemperatureCommand tempWater = new EngineCoolantTemperatureCommand();


                   speed = (TextView)findViewById(R.id.tvSpeed);
                   rpm = (TextView)findViewById(R.id.tvRPM);
                   cooltemp = (TextView)findViewById(R.id.tvCoolTemp);

                   engineRPM.run(sock.getInputStream(), sock.getOutputStream());
                   speedValue.run(sock.getInputStream(), sock.getOutputStream());
                   tempWater.run(sock.getInputStream(), sock.getOutputStream());

                   rpm.setText(engineRPM.getFormattedResult());
                   speed.setText(speedValue.getFormattedResult());
                   cooltemp.setText(tempWater.getFormattedResult());


                /*
                while (stan)
                {
                    speed = (TextView)findViewById(R.id.tvSpeed);
                    rpm = (TextView)findViewById(R.id.tvRPM);
                    cooltemp = (TextView)findViewById(R.id.tvCoolTemp);

                    engineRPM.run(sock.getInputStream(), sock.getOutputStream());
                    speedValue.run(sock.getInputStream(), sock.getOutputStream());
                    tempWater.run(sock.getInputStream(), sock.getOutputStream());

                    rpm.setText(engineRPM.getFormattedResult());
                    speed.setText(speedValue.getFormattedResult());
                    cooltemp.setText(tempWater.getFormattedResult());
                }
                */
            }
            catch (Exception e2)
            {
                Log.e(TAG, "Błąd podczas połaczenia Bluetooth. Zatrzymano..", e2);
            }

        }


    }

    public void stopData()
    {
        stan = false;

        if (sock != null)
            try
            {
                sock.close();
            }
            catch (IOException e)
            {
                Log.e(TAG, e.getMessage());
            }
    }


}
