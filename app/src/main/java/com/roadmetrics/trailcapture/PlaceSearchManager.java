package com.roadmetrics.trailcapture; // Defines the package where this class belongs

// Import statements for required classes and interfaces
import android.app.Activity; // For handling activity-related operations
import android.content.Context; // Provides access to application resources
import android.content.Intent; // For launching activities and handling their results
import android.location.Address; // Represents a physical address
import android.location.Geocoder; // Provides forward and reverse geocoding
import android.util.Log; // For logging messages to Logcat
import android.widget.Toast; // For displaying short notification messages
import androidx.annotation.NonNull; // Denotes that a parameter, field, or method return value can never be null
import androidx.appcompat.widget.SearchView; // UI component for searching
import androidx.fragment.app.FragmentManager; // Manages fragments in an activity
import com.google.android.gms.common.api.CommonStatusCodes; // Standard status codes for Google APIs
import com.google.android.gms.common.api.Status; // Contains status information from a Google API request
import com.google.android.gms.maps.GoogleMap; // Represents the Google Map itself
import com.google.android.gms.maps.model.LatLng; // Represents a geographical location (latitude/longitude)
import com.google.android.libraries.places.api.model.Place; // Represents a physical place in the world
import com.google.android.libraries.places.widget.Autocomplete; // Helper class for the Places Autocomplete widget
import com.google.android.libraries.places.widget.AutocompleteActivity; // Activity that shows the autocomplete UI
import com.google.android.libraries.places.widget.AutocompleteSupportFragment; // Fragment that shows the autocomplete UI
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener; // Callback for place selection events
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode; // Controls the UI mode of the autocomplete activity
import com.roadmetrics.trailcapture.R; // Resources class for accessing resource IDs
import java.io.IOException; // Signals an I/O operation failure
import java.util.Arrays; // Utility methods for working with arrays
import java.util.List; // Interface for list data structures
import java.util.Locale; // Represents a specific geographical, political, or cultural region

public class PlaceSearchManager { // Defines a class that manages place search functionality
  private static final int AUTOCOMPLETE_REQUEST_CODE = 100; // Request code for autocomplete activity result handling
  private Context context; // Reference to the app context
  private FragmentManager fragmentManager; // Manager for fragment transactions
  private MapManager mapManager; // Reference to the MapManager for map operations
  private RouteManager routeManager; // Reference to the RouteManager for route calculations
  private GoogleMap map; // Reference to the GoogleMap instance
  private SearchView searchView; // Reference to the SearchView UI component

  // Constructor that initializes the PlaceSearchManager with context and fragment manager
  public PlaceSearchManager(Context context, FragmentManager fragmentManager) {
    this.context = context; // Store the context
    this.fragmentManager = fragmentManager; // Store the fragment manager
  }

  // Method to set the MapManager
  public void setMapManager(MapManager mapManager) {
    this.mapManager = mapManager; // Store the MapManager reference
  }

  // Method to set the GoogleMap and initialize RouteManager if dependencies are available
  public void setGoogleMap(GoogleMap map) {
    this.map = map; // Store the GoogleMap reference
    // Initialize RouteManager once we have both the map and mapManager
    if (this.mapManager != null && this.map != null) {
      this.routeManager = new RouteManager(context, map, mapManager);
    }
  }

  // Method to set the SearchView
  public void setSearchView(SearchView searchView) {
    this.searchView = searchView; // Store the SearchView reference
  }

  // Method to initialize the autocomplete fragment for place search
  public void initializeAutocompleteFragment() {
    // Find the autocomplete fragment in the layout using its ID
    AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
        fragmentManager.findFragmentById(R.id.autocomplete_fragment);

    if (autocompleteFragment != null) { // Check if fragment was found
      // Specify which place data fields to return (ID, name, and coordinates)
      autocompleteFragment.setPlaceFields(Arrays.asList(
          Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
      autocompleteFragment.setCountry("IN"); // Optional: Restrict search to India

      // Set listener for place selection events
      autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
        @Override
        public void onPlaceSelected(@NonNull Place place) {
          LatLng latLng = place.getLatLng(); // Get the coordinates of the selected place
          if (latLng != null) {
            // Move camera to the selected place with zoom level 15
            mapManager.moveCameraToLocation(latLng, 15);

            // Calculate and draw route to the selected place
            LatLng currentLocation = mapManager.getCurrentLocation(); // Get user's current location
            if (currentLocation != null && routeManager != null) {
              // Request a route from current location to the selected place
              routeManager.requestRouteToDestination(currentLocation, latLng);
            } else {
              // Show error message if current location is not available
              Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show();
            }
          }
        }

        @Override
        public void onError(@NonNull Status status) {
          if (status.getStatusCode() == CommonStatusCodes.CANCELED) {
            // Log if user is still typing (not an actual error)
            Log.d("PlaceSearchManager", "User is still typing, no error.");
          } else {
            // Log and show error message for genuine errors
            Log.e("PlaceSearchManager", "Error selecting place: " + status.getStatusMessage());
            Toast.makeText(context, "Error: " + status.getStatusMessage(),
                           Toast.LENGTH_SHORT).show();
          }
        }
      });
    }
  }

  // Method to launch the full-screen places search activity
  public void launchPlacesSearchActivity() {
    // Specify which place data fields to return
    List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
    // Create intent for the autocomplete activity in overlay mode
    Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
        .build((Activity) context);
    // Start the activity and wait for result
    ((Activity) context).startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
  }

  // Method to handle the result returned from the places search activity
  public void handleActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == AUTOCOMPLETE_REQUEST_CODE) { // Check if this is the result we're expecting
      if (resultCode == Activity.RESULT_OK) { // Check if the operation completed successfully
        // Extract the selected place from the result
        Place place = Autocomplete.getPlaceFromIntent(data);
        if (place != null && place.getLatLng() != null) {
          LatLng selectedLocation = place.getLatLng(); // Get the coordinates

          // Move camera to the selected place with zoom level 15
          mapManager.moveCameraToLocation(selectedLocation, 15);

          // Calculate and draw route from current location to selected place
          LatLng currentLocation = mapManager.getCurrentLocation();
          if (currentLocation != null && routeManager != null) {
            routeManager.requestRouteToDestination(currentLocation, selectedLocation);
          }

          // Update the search view text with the selected place name
          if (searchView != null) {
            searchView.setQuery(place.getName(), false); // Set text but don't submit the query
          }
        }
      } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
        // Show error message if there was an error in the autocomplete activity
        Toast.makeText(context, "Error selecting place", Toast.LENGTH_SHORT).show();
      }
    }
  }

  // Method to search for a location by its name using Geocoder
  public void searchLocationByName(String locationName) {
    // Create a Geocoder with the device's current locale
    Geocoder geocoder = new Geocoder(context, Locale.getDefault());
    try {
      // Try to get location coordinates from the location name (limit to 1 result)
      List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
      if (!addresses.isEmpty()) { // Check if any locations were found
        // Get the first (and only) address
        Address address = addresses.get(0);
        // Create LatLng object from the address coordinates
        LatLng destinationLatLng = new LatLng(address.getLatitude(), address.getLongitude());

        // Get current location and request route
        LatLng currentLocation = mapManager.getCurrentLocation();
        if (currentLocation != null && routeManager != null) {
          // Calculate and draw route from current location to the geocoded destination
          routeManager.requestRouteToDestination(currentLocation, destinationLatLng);
        } else {
          // Show error message if current location is not available
          Toast.makeText(context, "Current location not available", Toast.LENGTH_SHORT).show();
        }
      } else {
        // Show message if no locations matching the name were found
        Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show();
      }
    } catch (IOException e) {
      // Handle exceptions that might occur during geocoding
      e.printStackTrace(); // Print stack trace for debugging
      Toast.makeText(context, "Error finding location", Toast.LENGTH_SHORT).show();
    }
  }
}