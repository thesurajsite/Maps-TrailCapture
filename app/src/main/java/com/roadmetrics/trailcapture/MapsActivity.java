package com.roadmetrics.trailcapture; // Defines the package name for this class

// Importing necessary Android libraries and classes
import android.Manifest; // Contains constants for permission strings
import android.content.Intent; // Used for launching activities and handling results
import android.content.pm.PackageManager; // For checking if permissions are granted
import android.os.Bundle; // For passing data between activities
import android.widget.Toast; // For showing small popup messages to the user
import androidx.annotation.NonNull; // Annotation to indicate a parameter/method/etc. should never be null
import androidx.appcompat.app.AppCompatActivity; // Base class for activities using the support library features
import androidx.core.app.ActivityCompat; // Helper for accessing ActivityCompat features
import com.google.android.gms.maps.GoogleMap; // Main class for Google Maps integration
import com.google.android.gms.maps.OnMapReadyCallback; // Interface to be notified when the map is ready to be used
import com.google.android.gms.maps.SupportMapFragment; // Fragment that displays a Google Map
import com.google.android.libraries.places.api.Places; // Google Places API for location search


// Main activity class for the map functionality, implements OnMapReadyCallback to handle map initialization
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

  // Constant used to identify location permission requests when results come back
  private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

  // Instance of Google Map that will be displayed
  private GoogleMap mMap;

  // Custom manager for handling user location updates
  private LocationManager locationManager;

  // Custom manager for handling map-related operations
  private MapManager mapManager;

  // Custom manager for handling place search functionality
  private PlaceSearchManager placeSearchManager;

  // Flag to track whether the user is currently manually zooming on the map
  private boolean isUserZooming = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Required call to parent class constructor
    super.onCreate(savedInstanceState);

    // Set the content view to the layout defined in activity_maps.xml
    setContentView(R.layout.activity_maps);

    // Initialize the Google Places API with your API key if not already initialized
    if (!Places.isInitialized()) {
      Places.initialize(getApplicationContext(), "AIzaSyB5tvtFU5vK7eaeUpBNug7waDt0bU0RZyE");
    }

    // Find the map fragment in the layout
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

    // Request the map to be ready asynchronously
    // When ready, onMapReady() will be called
    if (mapFragment != null) {
      mapFragment.getMapAsync(this);
    }

    // Initialize the location manager with this activity context
    locationManager = new LocationManager(this);

    // Initialize the place search manager with this activity context and fragment manager
    placeSearchManager = new PlaceSearchManager(this, getSupportFragmentManager());
  }

  // Called when the GoogleMap instance is ready to be used
  @Override
  public void onMapReady(@NonNull GoogleMap googleMap) {
    // Store the GoogleMap instance
    mMap = googleMap;

    // Initialize the map manager with the map and activity context
    mapManager = new MapManager(mMap, this);

    // Set listeners to detect when the user is interacting with the map view
    // This listener is triggered when the map camera starts moving (e.g., during zoom, pan)
    mMap.setOnCameraMoveListener(() -> isUserZooming = true);

    // This listener is triggered when the map camera stops moving
    mMap.setOnCameraIdleListener(() -> isUserZooming = false);

    // Connect all the manager classes to share information between them
    locationManager.setMapManager(mapManager); // Let location manager update the map manager
    placeSearchManager.setMapManager(mapManager); // Let place search manager update the map manager
    placeSearchManager.setGoogleMap(mMap); // Give place search manager access to the map
    placeSearchManager.initializeAutocompleteFragment(); // Set up the search UI components

    // Check if we have the necessary location permissions
    if (checkPermissions()) {
      // If we have permissions, enable showing user location on map
      mapManager.enableUserLocation();
      // And start receiving location updates
      locationManager.setupLocationUpdates();
    } else {
      // If we don't have permissions, request them
      requestPermissions();
    }
  }

  // Called when an activity launched by this activity returns a result
  // Used for handling results from place search activities
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Required call to parent method
    super.onActivityResult(requestCode, resultCode, data);
    // Delegate handling of the result to the place search manager
    placeSearchManager.handleActivityResult(requestCode, resultCode, data);
  }

  // Helper method to check if we have the location permissions needed
  private boolean checkPermissions() {
    // Check if both fine and coarse location permissions are granted
    return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
           PackageManager.PERMISSION_GRANTED &&
           ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
           PackageManager.PERMISSION_GRANTED;
  }

  // Helper method to request location permissions
  private void requestPermissions() {
    // Request both fine and coarse location permissions simultaneously
    ActivityCompat.requestPermissions(this, new String[]{
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    }, LOCATION_PERMISSION_REQUEST_CODE);
  }

  // Called when the user responds to permission requests
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    // Required call to parent method
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    // Check if this result is for our location permission request
    if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
      // Check if permissions are now granted
      if (checkPermissions()) {
        // If permissions are granted, enable showing user location on map
        mapManager.enableUserLocation();
        // And start receiving location updates
        locationManager.setupLocationUpdates();
      } else {
        // If permissions are still not granted, show a message to the user
        Toast.makeText(this, "Location permission is required!", Toast.LENGTH_SHORT).show();
      }
    }
  }

  // Public method to check if user is currently zooming
  // This can be called by other classes to check the zooming state
  public boolean isUserZooming() {
    return isUserZooming;
  }

  // Called when the activity is being destroyed
  @Override
  protected void onDestroy() {
    // Required call to parent method
    super.onDestroy();

    // Clean up resources used by the location manager
    // This likely includes unregistering location listeners to prevent memory leaks
    locationManager.cleanUp();
  }
}