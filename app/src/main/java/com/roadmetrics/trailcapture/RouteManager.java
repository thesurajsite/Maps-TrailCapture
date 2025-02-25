package com.roadmetrics.trailcapture;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RouteManager {
  private static final String TAG = "RouteManager";
  private static final String DIRECTIONS_API_BASE_URL = "https://maps.googleapis.com/maps/api/directions/json";
  private static final String API_KEY = "AIzaSyB5tvtFU5vK7eaeUpBNug7waDt0bU0RZyE"; // Replace with your actual API key

  private Context context;
  private GoogleMap map;
  private MapManager mapManager;
  private Polyline routePolyline;

  public RouteManager(Context context, GoogleMap map, MapManager mapManager) {
    this.context = context;
    this.map = map;
    this.mapManager = mapManager;
  }

  public void requestRouteToDestination(LatLng origin, LatLng destination) {
    String url = buildDirectionsUrl(origin, destination);
    new FetchRouteTask().execute(url);
  }

  private String buildDirectionsUrl(LatLng origin, LatLng destination) {
    // Format: https://maps.googleapis.com/maps/api/directions/json?origin=lat,lng&destination=lat,lng&mode=driving&key=YOUR_API_KEY
    return DIRECTIONS_API_BASE_URL +
           "?origin=" + origin.latitude + "," + origin.longitude +
           "&destination=" + destination.latitude + "," + destination.longitude +
           "&mode=driving" +
           "&key=" + API_KEY;
  }

  private class FetchRouteTask extends AsyncTask<String, Void, List<LatLng>> {
    @Override
    protected List<LatLng> doInBackground(String... urls) {
      String data = "";
      try {
        data = downloadUrl(urls[0]);
        return parseRoutePoints(data);
      } catch (Exception e) {
        Log.e(TAG, "Exception fetching route: " + e.getMessage());
      }
      return null;
    }

    @Override
    protected void onPostExecute(List<LatLng> routePoints) {
      if (routePoints != null && !routePoints.isEmpty()) {
        drawRoute(routePoints);
      } else {
        Toast.makeText(context, "Could not calculate route", Toast.LENGTH_SHORT).show();
      }
    }
  }

  private String downloadUrl(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setDoInput(true);
    conn.connect();

    InputStream is = conn.getInputStream();
    StringBuilder sb = new StringBuilder();
    BufferedReader br = new BufferedReader(new InputStreamReader(is));

    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }

    is.close();
    conn.disconnect();
    return sb.toString();
  }

  private List<LatLng> parseRoutePoints(String jsonData) {
    List<LatLng> routePoints = new ArrayList<>();

    try {
      JSONObject jsonObject = new JSONObject(jsonData);
      JSONArray routes = jsonObject.getJSONArray("routes");

      if (routes.length() == 0) {
        return routePoints; // Return empty list if no routes found
      }

      // Get the first route
      JSONObject route = routes.getJSONObject(0);
      JSONArray legs = route.getJSONArray("legs");

      // Loop through all legs and steps to get detailed path
      for (int i = 0; i < legs.length(); i++) {
        JSONObject leg = legs.getJSONObject(i);
        JSONArray steps = leg.getJSONArray("steps");

        for (int j = 0; j < steps.length(); j++) {
          JSONObject step = steps.getJSONObject(j);
          JSONObject polyline = step.getJSONObject("polyline");
          String points = polyline.getString("points");

          List<LatLng> decodedPoints = decodePolyline(points);
          routePoints.addAll(decodedPoints);
        }
      }
    } catch (JSONException e) {
      Log.e(TAG, "Error parsing JSON: " + e.getMessage());
    }

    return routePoints;
  }

  private List<LatLng> decodePolyline(String encoded) {
    List<LatLng> poly = new ArrayList<>();
    int index = 0, len = encoded.length();
    int lat = 0, lng = 0;

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

      LatLng p = new LatLng((double) lat / 1E5, (double) lng / 1E5);
      poly.add(p);
    }

    return poly;
  }

  private void drawRoute(List<LatLng> routePoints) {
    // Remove existing route polyline if it exists
    if (routePolyline != null) {
      routePolyline.remove();
    }

    // Create a polyline with the route points
    PolylineOptions polylineOptions = new PolylineOptions()
        .addAll(routePoints)
        .width(10f)
        .color(Color.BLUE);

    routePolyline = map.addPolyline(polylineOptions);

    // Notify the MapManager if needed
    mapManager.routeDrawn(routePoints.get(routePoints.size() - 1));
  }
}