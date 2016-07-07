package com.example.kamel.easyobddiag;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * Created by Kamel on 2016-06-02.
 */
public class GPSControl extends Service implements LocationListener {

    private final Context mContext;

    //zmienna statusu GPS
    boolean gpsEnabled = false;

    //zmienna statusu internetu
    boolean networkEnabled = false;
    boolean canGetLocation = false;

    Location location;
    double latitude; //opóźnienie
    double longitude;

    //Minimalny dystans do zmiany pozycji w metrach
    private  static  final long MIN_DISTANCE_FOR_UPDATES = 10;

    //Minimalny czas pomiędzy aktualizacjami
    private static final long MIN_TIME_UPDATES = 1000*60 *1;

    //Deklaracja Location Managera
    protected LocationManager locationManager;

    //Konstruktor klasy
    public  GPSControl(Context context)
    {
        this.mContext = context;
        getLocation();
    }

    public  Location getLocation()
    {
        try{
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            //Pobieranie statusu GPS
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            //Pobieranie statusu internetu
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if(!gpsEnabled && !networkEnabled)
            {

            }
            else
            {
                this.canGetLocation = true;
                //Najpierw pobieramy lokalizacje z Network Provider
                if (networkEnabled)
                {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_UPDATES,MIN_DISTANCE_FOR_UPDATES,this);
                    Log.d("network","network");
                    if (locationManager != null)
                    {
                        location = locationManager.
                                getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if(location != null)
                        {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        return location ;

                    }

                }
                //Jeśli GPS jest dostępny pobierz lat/long przez Usługe GPS
                if (gpsEnabled)
                {
                    if(location == null)
                    {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                MIN_TIME_UPDATES,MIN_DISTANCE_FOR_UPDATES,this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if(locationManager != null)
                        {
                            location = locationManager.
                                    getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if(location != null)
                            {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return  location;
    }


    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
            * Function to get latitude
    * */
    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     * */
    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }

        // return longitude
        return longitude;
    }

    /**
     * Function to check if best network provider
     * @return boolean
     * */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function to show settings alert dialog
     * */
    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // Setting Dialog Title
        alertDialog.setTitle("Ustawiena GPS");

        // Setting Dialog Message
        alertDialog.setMessage("GPS jest wyłączony. Czy chcesz przejść do ustawień w celu aktywacji?");

        // Setting Icon to Dialog
        //alertDialog.setIcon(R.drawable.delete);

        // On pressing Settings button
        alertDialog.setPositiveButton("Ustawienia", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Anuluj", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }
}
