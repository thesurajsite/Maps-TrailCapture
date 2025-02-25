package com.roadmetrics.trailcapture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.libraries.places.api.Places;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

  private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

  private GoogleMap mMap;
  private LocationManager locationManager;
  private MapManager mapManager;
  private PlaceSearchManager placeSearchManager;

  // Variable to track whether the user is zooming manually
  private boolean isUserZooming = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);

    // Initialize Places API
    if (!Places.isInitialized()) {
      Places.initialize(getApplicationContext(), "AIzaSyB5tvtFU5vK7eaeUpBNug7waDt0bU0RZyE");
    }

    // Initialize map fragment
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    if (mapFragment != null) {
      mapFragment.getMapAsync(this);
    }

    // Initialize managers
    locationManager = new LocationManager(this);
    placeSearchManager = new PlaceSearchManager(this, getSupportFragmentManager());
  }

  @Override
  public void onMapReady(@NonNull GoogleMap googleMap) {
    mMap = googleMap;

    // Initialize map manager
    mapManager = new MapManager(mMap, this);

    // Configure map UI settings
    mMap.setOnCameraMoveListener(() -> isUserZooming = true);
    mMap.setOnCameraIdleListener(() -> isUserZooming = false);

    // Connect the managers
    locationManager.setMapManager(mapManager);
    placeSearchManager.setMapManager(mapManager);
    placeSearchManager.setGoogleMap(mMap);
    placeSearchManager.initializeAutocompleteFragment();

    if (checkPermissions()) {
      mapManager.enableUserLocation();
      locationManager.setupLocationUpdates();
    } else {
      requestPermissions();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    placeSearchManager.handleActivityResult(requestCode, resultCode, data);
  }

  private boolean checkPermissions() {
    return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
           PackageManager.PERMISSION_GRANTED &&
           ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
           PackageManager.PERMISSION_GRANTED;
  }

  private void requestPermissions() {
    ActivityCompat.requestPermissions(this, new String[]{
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    }, LOCATION_PERMISSION_REQUEST_CODE);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
      if (checkPermissions()) {
        mapManager.enableUserLocation();
        locationManager.setupLocationUpdates();
      } else {
        Toast.makeText(this, "Location permission is required!", Toast.LENGTH_SHORT).show();
      }
    }
  }

  public boolean isUserZooming() {
    return isUserZooming;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    locationManager.cleanUp();
  }
}