package com.roadmetrics.trailcapture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.FragmentManager;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.roadmetrics.trailcapture.R;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PlaceSearchManager {
  private static final int AUTOCOMPLETE_REQUEST_CODE = 100;
  private Context context;
  private FragmentManager fragmentManager;
  private MapManager mapManager;
  private RouteManager routeManager;
  private GoogleMap map;
  private SearchView searchView;

  public PlaceSearchManager(Context context, FragmentManager fragmentManager) {
    this.context = context;
    this.fragmentManager = fragmentManager;
  }

  public void setMapManager(MapManager mapManager) {
    this.mapManager = mapManager;
  }

  public void setGoogleMap(GoogleMap map) {
    this.map = map;
    // Initialize RouteManager once we have the map
    if (this.mapManager != null && this.map != null) {
      this.routeManager = new RouteManager(context, map, mapManager);
    }
  }

  public void setSearchView(SearchView searchView) {
    this.searchView = searchView;
  }

  public void initializeAutocompleteFragment() {
    AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
        fragmentManager.findFragmentById(R.id.autocomplete_fragment);

    if (autocompleteFragment != null) {
      autocompleteFragment.setPlaceFields(Arrays.asList(
          Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
      autocompleteFragment.setCountry("IN"); // Optional: Restrict search to a specific country

      // Set listener for place selection
      autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
        @Override
        public void onPlaceSelected(@NonNull Place place) {
          LatLng latLng = place.getLatLng();
          if (latLng != null) {
            // Move camera to the selected place
            mapManager.moveCameraToLocation(latLng, 15);

            // Calculate and draw route to the selected place
            LatLng currentLocation = mapManager.getCurrentLocation();
            if (currentLocation != null && routeManager != null) {
              routeManager.requestRouteToDestination(currentLocation, latLng);
            } else {
              Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show();
            }
          }
        }

        @Override
        public void onError(@NonNull Status status) {
          if (status.getStatusCode() == CommonStatusCodes.CANCELED) {
            Log.d("PlaceSearchManager", "User is still typing, no error.");
          } else {
            Log.e("PlaceSearchManager", "Error selecting place: " + status.getStatusMessage());
            Toast.makeText(context, "Error: " + status.getStatusMessage(),
                           Toast.LENGTH_SHORT).show();
          }
        }
      });
    }
  }

  public void launchPlacesSearchActivity() {
    List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
    Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
        .build((Activity) context);
    ((Activity) context).startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
  }

  public void handleActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        Place place = Autocomplete.getPlaceFromIntent(data);
        if (place != null && place.getLatLng() != null) {
          LatLng selectedLocation = place.getLatLng();

          // Move camera to the selected place
          mapManager.moveCameraToLocation(selectedLocation, 15);

          // Calculate and draw route
          LatLng currentLocation = mapManager.getCurrentLocation();
          if (currentLocation != null && routeManager != null) {
            routeManager.requestRouteToDestination(currentLocation, selectedLocation);
          }

          // Update the search bar with the place name if available
          if (searchView != null) {
            searchView.setQuery(place.getName(), false);
          }
        }
      } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
        Toast.makeText(context, "Error selecting place", Toast.LENGTH_SHORT).show();
      }
    }
  }

  public void searchLocationByName(String locationName) {
    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
    try {
      List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
      if (!addresses.isEmpty()) {
        Address address = addresses.get(0);
        LatLng destinationLatLng = new LatLng(address.getLatitude(), address.getLongitude());

        // Get current location and request route
        LatLng currentLocation = mapManager.getCurrentLocation();
        if (currentLocation != null && routeManager != null) {
          routeManager.requestRouteToDestination(currentLocation, destinationLatLng);
        } else {
          Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show();
        }
      } else {
        Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show();
      }
    } catch (IOException e) {
      e.printStackTrace();
      Toast.makeText(context, "Error finding location", Toast.LENGTH_SHORT).show();
    }
  }
}