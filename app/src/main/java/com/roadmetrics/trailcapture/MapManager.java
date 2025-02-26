package com.roadmetrics.trailcapture; // Defines the package where this class belongs

// Import statements for required classes and interfaces
import android.Manifest; // For permission constants
import android.content.Context; // Provides access to application-specific resources
import android.content.pm.PackageManager; // Used to check if permissions are granted
import androidx.core.app.ActivityCompat; // Helper for checking and requesting permissions
import com.google.android.gms.maps.CameraUpdateFactory; // Factory for creating camera update objects
import com.google.android.gms.maps.GoogleMap; // Represents the Google Map itself
import com.google.android.gms.maps.model.LatLng; // Represents a geographical location (latitude/longitude)
import com.roadmetrics.trailcapture.MapsActivity; // Imports the MapsActivity from the same package
import java.util.ArrayList; // Dynamic array implementation in Java
import java.util.List; // Interface for list data structures

public class MapManager { // Defines a MapManager class that handles map operations
  private GoogleMap mMap; // Instance of GoogleMap that this class will manage
  private Context context; // Reference to the app context for accessing system services
  private LatLng currentLocation; // Tracks the most recent location

  // Constructor that initializes the MapManager with a GoogleMap instance and context
  public MapManager(GoogleMap map, Context context) {
    this.mMap = map; // Store the GoogleMap reference
    this.context = context; // Store the Context reference
  }

  // Method to enable showing the user's location on the map
  public void enableUserLocation() {
    // Check if the app has been granted location permissions
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) {
      return; // Exit the method if neither fine nor coarse location permissions are granted
    }

    mMap.setMyLocationEnabled(true); // Enable the blue dot showing user's location on the map
    mMap.getUiSettings().setMyLocationButtonEnabled(true); // Enable the button that centers the map on user's location
  }

  // Method to update the map with a new location point
  public void updateLocationPoint(LatLng newPoint) {
    if (mMap == null) return; // Safety check: exit if the map hasn't been initialized

    // Update the stored current location with the new point
    this.currentLocation = newPoint;

    // Get the current zoom level before moving the camera
    float currentZoom = mMap.getCameraPosition().zoom;

    // Determine if the user is currently manually zooming the map
    boolean isUserZooming = ((MapsActivity)context).isUserZooming();
    if (!isUserZooming) {
      // If user is not zooming, move camera to new location while preserving the current zoom level
      mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPoint, currentZoom));
    } else {
      // If user is zooming, only move the camera position without changing zoom
      mMap.moveCamera(CameraUpdateFactory.newLatLng(newPoint));
    }
  }

  // Method to move the camera to a specific location with specified zoom level
  public void moveCameraToLocation(LatLng location, float zoomLevel) {
    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel));
  }

  // Getter method to retrieve the most recent location
  public LatLng getCurrentLocation() {
    return currentLocation;
  }

  // Method called when a route to a destination has been drawn
  public void routeDrawn(LatLng destination) {
    // Move camera to show the destination
    // You could potentially implement additional logic here
    // such as adjusting bounds to show the entire route
    moveCameraToLocation(destination, 14); // Move camera to destination with zoom level 14
  }
}
