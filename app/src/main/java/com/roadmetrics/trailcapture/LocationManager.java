package com.roadmetrics.trailcapture;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

public class LocationManager {
  private Context context;
  private FusedLocationProviderClient fusedLocationClient;
  private LocationCallback locationCallback;
  private MapManager mapManager;

  public LocationManager(Context context) {
    this.context = context;
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
  }

  public void setMapManager(MapManager mapManager) {
    this.mapManager = mapManager;
  }

  public void setupLocationUpdates() {
    LocationRequest locationRequest = LocationRequest.create()
                                                     .setInterval(5000)  // Request location updates every 5 seconds
                                                     .setFastestInterval(2000)
                                                     .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    locationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) return;
        for (Location location : locationResult.getLocations()) {
          LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
          mapManager.updateLocationPoint(newPoint);
        }
      }
    };

    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED) {
      fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
  }

  public void getLastKnownLocation(LocationCallback callback) {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) {
      return;
    }

    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
      if (location != null) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        callback.onLocationResult(LocationResult.create(java.util.Arrays.asList(location)));
      }
    });
  }

  public void cleanUp() {
    if (fusedLocationClient != null && locationCallback != null) {
      fusedLocationClient.removeLocationUpdates(locationCallback);
    }
  }
}