package com.roadmetrics.trailcapture;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.roadmetrics.trailcapture.MapsActivity;
import java.util.ArrayList;
import java.util.List;

public class MapManager {
  private GoogleMap mMap;
  private Context context;
  private List<LatLng> pathPoints = new ArrayList<>();
  private Polyline polyline;
  private LatLng currentLocation; // Track current location

  public MapManager(GoogleMap map, Context context) {
    this.mMap = map;
    this.context = context;
  }

  public void enableUserLocation() {
    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED) {
      return;
    }

    mMap.setMyLocationEnabled(true);
    mMap.getUiSettings().setMyLocationButtonEnabled(true);
  }

  public void updateLocationPoint(LatLng newPoint) {
    if (mMap == null) return;

    // Update current location
    this.currentLocation = newPoint;

    pathPoints.add(newPoint);

    // Get the current zoom level before moving the camera
    float currentZoom = mMap.getCameraPosition().zoom;

    // Preserve user zoom level
    boolean isUserZooming = ((MapsActivity)context).isUserZooming();
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

  public void moveCameraToLocation(LatLng location, float zoomLevel) {
    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel));
  }

  public LatLng getCurrentLocation() {
    return currentLocation;
  }

  public void routeDrawn(LatLng destination) {
    // Move camera to show the destination
    // You could potentially implement additional logic here
    // such as adjusting bounds to show the entire route
    moveCameraToLocation(destination, 14);
  }

  public List<LatLng> getPathPoints() {
    return pathPoints;
  }
}