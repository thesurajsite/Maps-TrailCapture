package com.roadmetrics.trailcapture;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

  private GoogleMap mMap;
  private FusedLocationProviderClient fusedLocationClient;
  private LocationCallback locationCallback;
  private List<LatLng> pathPoints = new ArrayList<>();
  private Polyline polyline;
  private Polyline destinationPolyline;
  private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
  private SearchView searchView;

  // Variable to track whether the user is zooming manually
  private boolean isUserZooming = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);

    // Initialize Places API
    if (!Places.isInitialized()) {
      Places.initialize(getApplicationContext(), "AIzaSyB5tvtFU5vK7eaeUpBNug7waDt0bU0RZyE"); // Replace with actual API key
    }

    // Initialize AutocompleteSupportFragment
    AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
        getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

    if (autocompleteFragment != null) {
      autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
      autocompleteFragment.setCountry("IN"); // Optional: Restrict search to a specific country

      // Set listener for place selection
      autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
        @Override
        public void onPlaceSelected(@NonNull Place place) {
          LatLng latLng = place.getLatLng();
          if (latLng != null && mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
          }
        }

        @Override
        public void onError(@NonNull Status status) {
          if (status.getStatusCode() == CommonStatusCodes.CANCELED) {
            Log.d("MapsActivity", "User is still typing, no error.");
          } else {
            Log.e("MapsActivity", "Error selecting place: " + status.getStatusMessage());
            Toast.makeText(MapsActivity.this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
          }
        }
      });
    }

    // Initialize map fragment
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map);
    if (mapFragment != null) {
      mapFragment.getMapAsync(this);
    }

    // Initialize location client
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
  }



  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == 100 && resultCode == RESULT_OK) {
      Place place = Autocomplete.getPlaceFromIntent(data);
      if (place != null && place.getLatLng() != null) {
        LatLng selectedLocation = place.getLatLng();

        // Move the map to the selected place
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));

        // Update the search bar with the place name
        searchView.setQuery(place.getName(), false);
      }
    } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
      Toast.makeText(this, "Error selecting place", Toast.LENGTH_SHORT).show();
    }
  }


  @Override
  public void onMapReady(@NonNull GoogleMap googleMap) {
    mMap = googleMap;

    // Detect user zooming activity
    mMap.setOnCameraMoveListener(() -> isUserZooming = true);
    mMap.setOnCameraIdleListener(() -> isUserZooming = false);

    if (checkPermissions()) {
      enableUserLocation();
      setupLocationUpdates();
    } else {
      requestPermissions();
    }
  }

  private boolean checkPermissions() {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
           ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
  }

  private void requestPermissions() {
    ActivityCompat.requestPermissions(this, new String[]{
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    }, LOCATION_PERMISSION_REQUEST_CODE);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
      if (checkPermissions()) {
        enableUserLocation();
        setupLocationUpdates();
      } else {
        Toast.makeText(this, "Location permission is required!", Toast.LENGTH_SHORT).show();
      }
    }
  }

  private void enableUserLocation() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    mMap.setMyLocationEnabled(true);
    mMap.getUiSettings().setMyLocationButtonEnabled(true);

    // Fetch last known location and move camera initially
    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
      if (location != null) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18));
      }
    });
  }

  private void setupLocationUpdates() {
    LocationRequest locationRequest = LocationRequest.create()
                                                     .setInterval(5000)  // Request location updates every 5 seconds
                                                     .setFastestInterval(2000)
                                                     .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    locationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) return;
        for (Location location : locationResult.getLocations()) {
          updateMapWithNewLocation(location);
        }
      }
    };

    if (checkPermissions()) {
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
          ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
      } else {
        // Request permissions if not granted
        requestPermissions();
      }
    }
  }

  private void updateMapWithNewLocation(Location location) {
    if (mMap == null) return;

    LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
    pathPoints.add(newPoint);

    // Get the current zoom level before moving the camera
    float currentZoom = mMap.getCameraPosition().zoom;

    // Preserve user zoom level
    if (!isUserZooming) {
      mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPoint, currentZoom)); // Use current zoom
    } else {
      mMap.moveCamera(CameraUpdateFactory.newLatLng(newPoint));
    }

    // Update polyline path
    if (polyline != null) {
      polyline.remove();
    }
    polyline = mMap.addPolyline(new PolylineOptions()
                                    .addAll(pathPoints)
                                    .width(8f)
                                    .color(android.graphics.Color.GREEN));
  }

  private void searchLocation(String locationName) {
    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
    try {
      List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
      if (!addresses.isEmpty()) {
        Address address = addresses.get(0);
        LatLng destinationLatLng = new LatLng(address.getLatitude(), address.getLongitude());
        drawRoute(destinationLatLng);
      } else {
        Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
      }
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show();
    }
  }

  private void drawRoute(LatLng destination) {
    if (!pathPoints.isEmpty()) {
      if (destinationPolyline != null) {
        destinationPolyline.remove();
      }
      List<LatLng> routePoints = new ArrayList<>(pathPoints);
      routePoints.add(destination);

      destinationPolyline = mMap.addPolyline(new PolylineOptions()
                                                 .addAll(routePoints)
                                                 .width(10f)
                                                 .color(android.graphics.Color.BLUE));

      mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, 14));
    }
  }


  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (fusedLocationClient != null && locationCallback != null) {
      fusedLocationClient.removeLocationUpdates(locationCallback);
    }
  }
}
