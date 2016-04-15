/**
 * 
 */
package edu.sjsu.cmpe295.gearnavi.service;

import com.squareup.otto.Produce;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import edu.sjsu.cmpe295.gearnavi.event.EventBusProvider;
import edu.sjsu.cmpe295.gearnavi.event.LocationChangedEvent;

/**
 * @author mly
 * 
 */
public class LocationServiceImpl extends Service implements LocationListener {

    private static final String TAG = "LocationServiceImpl";

    private Location mLocation;
    
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;

    private final IBinder mBinder = new LocalBinder();

    public Location getLocation() {

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        isGPSEnabled = locationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);

        isNetworkEnabled = locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGPSEnabled || isNetworkEnabled) {
            locationManager.requestLocationUpdates(
                    isGPSEnabled ? LocationManager.GPS_PROVIDER
                            : LocationManager.NETWORK_PROVIDER, 0, 0, this);
        } else {
            Log.d(TAG, " ... Location providers are disabled ... ");
            // TODO: Need to define and handle this use case scenario
        }

        mLocation = locationManager.getLastKnownLocation(
                isGPSEnabled ? LocationManager.GPS_PROVIDER
                        : LocationManager.NETWORK_PROVIDER);

        if(mLocation == null)
            // GPS might have been enabled but unable to get location
            mLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        
        if(mLocation == null) {
            Log.w(TAG, " ... Not able to get location data at the moment ...");

            mLocation = new Location("NA");
            mLocation.setLatitude(0);
            mLocation.setLongitude(0);
        }
        
        return mLocation;
    }

    public void onLocationChanged(Location location) {
        // Post to event bus
        //if(location != null)
        //   EventBusProvider.getInstance().post(new LocationChangedEvent(location));
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    public class LocalBinder extends Binder {
        public LocationServiceImpl getService() {
            return LocationServiceImpl.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }  
  
    @Override
    public void onCreate() {
        super.onCreate();
        
        EventBusProvider.getInstance().register(this);
    }

    @Produce 
    public LocationChangedEvent produceLocationEvent() {
        return new LocationChangedEvent(getLocation());
    }
}
