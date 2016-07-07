package com.example.kamel.easyobddiag;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import static com.example.kamel.easyobddiag.R.xml.preferences;

public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {


    public static final String ENABLE_BT = "enable_bluetooth";
    public static final String BLUETOOTH_LIST = "bluetooth_list";

    public String deviceAddress = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(preferences);

        ArrayList<CharSequence> pairedDeviceStrings = new ArrayList<>();
        ArrayList<CharSequence> vals = new ArrayList<>();
        ListPreference listBtDevices = (ListPreference) getPreferenceScreen()
                .findPreference(BLUETOOTH_LIST);

        /*
     * Rozpoczecie przeszukania protokołu Bluetooth w celu uzupełniania listy
     * sparowanych urządzeń i późniejszej możliwości wybrania odpowiedniego
     * interfejsu ODB II.
     */
        final BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            listBtDevices.setEntries(pairedDeviceStrings.toArray(new CharSequence[0]));
            listBtDevices.setEntryValues(vals.toArray(new CharSequence[0]));

            // Tylko dla urządzen bez Bluetooth
            Toast.makeText(this, "Twoje urządzenie nie obsługuje protokółu Bluetooth",
                    Toast.LENGTH_LONG).show();

            return;
        }

        /*
     * Przeszukanie protokołu i dodanie urządzeń sparowanych do listy
     */
        final Activity thisActivity = this;
        listBtDevices.setEntries(new CharSequence[1]);
        listBtDevices.setEntryValues(new CharSequence[1]);
        listBtDevices.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                if (mBtAdapter == null || !mBtAdapter.isEnabled()) {
                    Toast.makeText(thisActivity,
                            "Twoje urządzenie nie obsługuje protokółu Bluetooth lub jest on wyłączony",
                            Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });

    /*
     * Pobieranie sparowanych urządzeń i zaprezentowanie ich w postaci listy.
     */

        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceStrings.add(device.getName() + "\n" + device.getAddress());
                vals.add(device.getAddress());

                deviceAddress = device.getAddress();
            }
        }


        listBtDevices.setEntries(pairedDeviceStrings.toArray(new CharSequence[0]));
        listBtDevices.setEntryValues(vals.toArray(new CharSequence[0]));
        Log.e(deviceAddress, "Adresik: ");


    }




    //Funkcja obsługująca włączenie/wyłączenie Bluetooth
    public static boolean setBluetooth(boolean enable) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean isEnabled = bluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            return bluetoothAdapter.enable();
        }
        else if(!enable && isEnabled) {
            return bluetoothAdapter.disable();
        }
        return true;
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}
