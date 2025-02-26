package com.roadmetrics.trailcapture; // This line says this code belongs to the "trailcapture" app

// These are like importing tools that the code will use
import android.content.Context; // Helps the app understand its environment
import android.graphics.Color; // Lets us use colors like blue
import android.os.AsyncTask; // Helps do work in the background without freezing the app
import android.util.Log; // Allows writing messages for debugging
import android.widget.Toast; // Shows small popup messages to the user

import com.google.android.gms.maps.GoogleMap; // The main Google Maps tool
import com.google.android.gms.maps.model.LatLng; // Represents a location with latitude and longitude
import com.google.android.gms.maps.model.Polyline; // Represents a line on the map (for our route)
import com.google.android.gms.maps.model.PolylineOptions; // Helps customize how the line looks

// These help work with data coming from Google's servers
import org.json.JSONArray; // Handles lists of data
import org.json.JSONException; // Helps deal with errors in the data
import org.json.JSONObject; // Handles structured data

// These help with internet communication and data handling
import java.io.BufferedReader; // Helps read data efficiently
import java.io.IOException; // Helps handle errors when reading/writing data
import java.io.InputStream; // Represents data coming in
import java.io.InputStreamReader; // Helps read incoming data
import java.net.HttpURLConnection; // Makes internet connections
import java.net.URL; // Represents a web address
import java.util.ArrayList; // A type of list that can grow in size
import java.util.List; // The basic concept of a list

// This is the main class - it manages the route shown on the map
public class RouteManager {
  private static final String TAG = "RouteManager"; // Tag for log messages
  private static final String DIRECTIONS_API_BASE_URL = "https://maps.googleapis.com/maps/api/directions/json"; // The Google web address to get directions
  private static final String API_KEY = "AIzaSyB5tvtFU5vK7eaeUpBNug7waDt0bU0RZyE"; // The password to use Google's services

  private Context context; // Reference to the app environment
  private GoogleMap map; // Reference to the map being shown
  private MapManager mapManager; // Reference to another class that manages the map

  // This will hold the blue line showing the route
  private Polyline routePolyline;

  // This list stores all the points that make up the route
  private List<LatLng> fullRoutePoints = new ArrayList<>();

  // This keeps track of whether we're currently navigating
  private boolean isNavigating = false;

  // This is called when the RouteManager is created
  public RouteManager(Context context, GoogleMap map, MapManager mapManager) {
    this.context = context; // Save the app environment
    this.map = map; // Save the map
    this.mapManager = mapManager; // Save the map manager
  }

  // This function starts navigation to a destination
  public void requestRouteToDestination(LatLng origin, LatLng destination) {
    isNavigating = true; // Turn on navigation mode
    String url = buildDirectionsUrl(origin, destination); // Create the web address to get directions
    new FetchRouteTask().execute(url); // Start a background task to get the route
  }

  // This function removes the route line from the map
  private void clearRoute() {
    if (routePolyline != null) { // If there is a route line
      routePolyline.remove(); // Remove it from the map
      routePolyline = null; // Forget about it
    }
    fullRoutePoints.clear(); // Empty the list of route points
  }

  // This creates the web address to ask Google for directions
  private String buildDirectionsUrl(LatLng origin, LatLng destination) {
    // Format: https://maps.googleapis.com/maps/api/directions/json?origin=lat,lng&destination=lat,lng&mode=driving&key=YOUR_API_KEY
    return DIRECTIONS_API_BASE_URL +
           "?origin=" + origin.latitude + "," + origin.longitude + // Starting point
           "&destination=" + destination.latitude + "," + destination.longitude + // Ending point
           "&mode=driving" + // Use driving directions
           "&key=" + API_KEY; // Include the password for Google's services
  }

  // This is a special class that works in the background to get route data
  private class FetchRouteTask extends AsyncTask<String, Void, List<LatLng>> {
    @Override
    protected List<LatLng> doInBackground(String... urls) { // This runs in the background
      String data = "";
      try {
        data = downloadUrl(urls[0]); // Download the route data from Google
        return parseRoutePoints(data); // Convert the data into map points
      } catch (Exception e) {
        Log.e(TAG, "Exception fetching route: " + e.getMessage()); // Write error in the log
      }
      return null; // Return nothing if there's an error
    }

    @Override
    protected void onPostExecute(List<LatLng> routePoints) { // This runs after background work is done
      if (routePoints != null && !routePoints.isEmpty()) { // If we got route points
        fullRoutePoints = routePoints; // Save all the route points
        // Draw the route with user's current location
        updateRouteDisplay(mapManager.getCurrentLocation());
      } else {
        Toast.makeText(context, "Could not calculate route", Toast.LENGTH_SHORT).show(); // Show error message
      }
    }
  }

  // This function downloads data from a web address
  private String downloadUrl(String urlStr) throws IOException {
    URL url = new URL(urlStr); // Create a URL object from the string
    HttpURLConnection conn = (HttpURLConnection) url.openConnection(); // Open a connection
    conn.setRequestMethod("GET"); // Set the request type to GET (just fetching data)
    conn.setDoInput(true); // We want to receive data
    conn.connect(); // Start the connection

    InputStream is = conn.getInputStream(); // Get the incoming data stream
    StringBuilder sb = new StringBuilder(); // Create a string builder to store the data
    BufferedReader br = new BufferedReader(new InputStreamReader(is)); // Create a reader for the data

    String line;
    while ((line = br.readLine()) != null) { // Read each line of data
      sb.append(line); // Add it to our string builder
    }

    is.close(); // Close the data stream
    conn.disconnect(); // End the connection
    return sb.toString(); // Return all the data as a string
  }

  // This function takes the JSON data from Google and extracts the route points
  private List<LatLng> parseRoutePoints(String jsonData) {
    List<LatLng> routePoints = new ArrayList<>(); // Create an empty list for the points

    try {
      JSONObject jsonObject = new JSONObject(jsonData); // Parse the JSON data
      JSONArray routes = jsonObject.getJSONArray("routes"); // Get the routes array

      if (routes.length() == 0) {
        return routePoints; // Return empty list if no routes found
      }

      // Get the first route
      JSONObject route = routes.getJSONObject(0);
      JSONArray legs = route.getJSONArray("legs"); // Get the legs of the route (sections between stops)

      // Loop through all legs and steps to get detailed path
      for (int i = 0; i < legs.length(); i++) {
        JSONObject leg = legs.getJSONObject(i);
        JSONArray steps = leg.getJSONArray("steps"); // Get the steps in this leg

        for (int j = 0; j < steps.length(); j++) {
          JSONObject step = steps.getJSONObject(j);
          JSONObject polyline = step.getJSONObject("polyline"); // Get the path for this step
          String points = polyline.getString("points"); // Get the encoded path points

          List<LatLng> decodedPoints = decodePolyline(points); // Decode the points
          routePoints.addAll(decodedPoints); // Add all these points to our route
        }
      }
    } catch (JSONException e) {
      Log.e(TAG, "Error parsing JSON: " + e.getMessage()); // Log any errors
    }

    return routePoints; // Return all the route points
  }

  // This function decodes Google's special format for route points
  private List<LatLng> decodePolyline(String encoded) {
    List<LatLng> poly = new ArrayList<>(); // Create an empty list for points
    int index = 0, len = encoded.length();
    int lat = 0, lng = 0;

    // This is a complex algorithm to decode Google's format
    while (index < len) {
      int b, shift = 0, result = 0;
      do {
        b = encoded.charAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lat += dlat;

      shift = 0;
      result = 0;
      do {
        b = encoded.charAt(index++) - 63;
        result |= (b & 0x1f) << shift;
        shift += 5;
      } while (b >= 0x20);
      int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
      lng += dlng;

      // Create a location and add it to our list
      LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
      poly.add(p);
    }

    return poly; // Return the list of points
  }

  // This is called frequently to update the route as the user moves
  public void updateRouteDisplay(LatLng currentLocation) {
    if (!isNavigating || fullRoutePoints.isEmpty() || currentLocation == null) {
      return; // Don't do anything if we're not navigating or missing information
    }

    // Remove the old route line
    if (routePolyline != null) {
      routePolyline.remove();
    }

    // Find the point on the route closest to user's current location
    int closestPointIndex = findClosestPointIndex(currentLocation, fullRoutePoints);
    if (closestPointIndex < 0) return; // Give up if something went wrong

    // Create a new path from current location to destination
    List<LatLng> routePath = new ArrayList<>();
    routePath.add(currentLocation); // Start at current location

    // Add all the remaining points on the route
    for (int i = closestPointIndex + 1; i < fullRoutePoints.size(); i++) {
      routePath.add(fullRoutePoints.get(i));
    }

    // Draw the route in blue
    if (!routePath.isEmpty() && routePath.size() > 1) {
      PolylineOptions routeOptions = new PolylineOptions()
          .addAll(routePath) // Add all our points
          .width(10f) // Make the line 10 pixels wide
          .color(Color.BLUE); // Make the line blue
      routePolyline = map.addPolyline(routeOptions); // Add the line to the map
    }

    // Tell the MapManager what our destination is
    if (!fullRoutePoints.isEmpty()) {
      mapManager.routeDrawn(fullRoutePoints.get(fullRoutePoints.size() - 1));
    }
  }

  // This finds the point on the route closest to the user
  private int findClosestPointIndex(LatLng currentLocation, List<LatLng> route) {
    if (route.isEmpty()) {
      return -1; // Return error code if route is empty
    }

    int closestPointIndex = 0; // Start with the first point
    double minDistance = calculateDistance(currentLocation, route.get(0)); // Calculate distance to first point

    // Check each point to find the closest one
    for (int i = 1; i < route.size(); i++) {
      double distance = calculateDistance(currentLocation, route.get(i));
      if (distance < minDistance) { // If this point is closer
        minDistance = distance; // Remember the distance
        closestPointIndex = i; // Remember the point
      }
    }

    return closestPointIndex; // Return the closest point
  }

  // This calculates the real-world distance between two points
  private double calculateDistance(LatLng point1, LatLng point2) {
    double earthRadius = 6371000; // Earth's radius in meters
    double dLat = Math.toRadians(point2.latitude - point1.latitude); // Latitude difference in radians
    double dLng = Math.toRadians(point2.longitude - point1.longitude); // Longitude difference in radians
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
               Math.cos(Math.toRadians(point1.latitude)) * Math.cos(Math.toRadians(point2.latitude)) *
               Math.sin(dLng / 2) * Math.sin(dLng / 2); // Complex math for Earth's curvature
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)); // More complex math
    return earthRadius * c; // Return the distance in meters
  }

  // Method to access the full route points
  public List<LatLng> getFullRoutePoints() {
    return fullRoutePoints; // Return the list of all route points
  }
}