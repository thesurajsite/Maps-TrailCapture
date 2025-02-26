package com.roadmetrics.trailcapture; // This code belongs to the "trailcapture" app

// These are tools the code will use
import android.Manifest; // Helps ask for permissions (like using GPS)
import android.content.Context; // Helps the app understand its environment
import android.content.pm.PackageManager; // Checks if the app has permissions
import android.location.Location; // Represents a physical location (latitude/longitude)
import androidx.core.app.ActivityCompat; // Helps with permission handling
import com.google.android.gms.location.FusedLocationProviderClient; // Google's tool to get location
import com.google.android.gms.location.LocationCallback; // Receives location updates
import com.google.android.gms.location.LocationRequest; // Sets up how we want location updates
import com.google.android.gms.location.LocationResult; // Contains location data
import com.google.android.gms.location.LocationServices; // Google's location services
import com.google.android.gms.maps.model.LatLng; // A point with latitude and longitude

// This class manages getting the user's location
public class LocationManager {
  private Context context; // Reference to the app environment
  private FusedLocationProviderClient fusedLocationClient; // Google's tool for getting location
  private LocationCallback locationCallback; // Handles what happens when we get a new location
  private MapManager mapManager; // Reference to the class that manages the map

  // This is called when the LocationManager is created
  public LocationManager(Context context) {
    this.context = context; // Save the app environment
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context); // Initialize Google's location tool
  }

  // This connects the LocationManager to the MapManager
  public void setMapManager(MapManager mapManager) {
    this.mapManager = mapManager; // Save the map manager
  }

  // This sets up regular location updates
  public void setupLocationUpdates() {
    LocationRequest locationRequest = LocationRequest.create() // Create a request for location updates
                                                     .setInterval(2000)  // Get updates every 2 seconds (2000 milliseconds)
                                                     .setFastestInterval(2000) // Don't get updates faster than every 2 seconds
                                                     .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // Use the most accurate location (GPS)

    // Create an object that handles what to do when we get a new location
    locationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) { // This runs when we get new locations
        if (locationResult == null) return; // If no location data, do nothing
        for (Location location : locationResult.getLocations()) { // For each new location
          LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude()); // Convert to a map point
          mapManager.updateLocationPoint(newPoint); // Tell the map manager about the new location
        }
      }
    };

    // Check if we have permission to use the phone's location
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED && // If we can use precise location (GPS)
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED) { // And we can use approximate location (network)
      fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null); // Start getting location updates
    }
  }

  // This gets the last known location of the device
  public void getLastKnownLocation(LocationCallback callback) {
    // Check if we DON'T have permission to use the phone's location
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED && // If we can't use precise location
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) { // And we can't use approximate location
      return; // Then give up and do nothing
    }

    // Try to get the last known location
    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> { // When we get the location...
      if (location != null) { // If we actually got a location
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude()); // Convert to a map point
        callback.onLocationResult(LocationResult.create(java.util.Arrays.asList(location))); // Tell the callback about it
      }
    });
  }

  // This cleans up resources when we're done with the LocationManager
  public void cleanUp() {
    if (fusedLocationClient != null && locationCallback != null) { // If we have a location client and callback
      fusedLocationClient.removeLocationUpdates(locationCallback); // Stop getting location updates
    }
  }
}