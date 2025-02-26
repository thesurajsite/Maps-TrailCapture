package com.roadmetrics.trailcapture; // Defines the package name for this class, used for organization in Android apps

// Importing necessary Android libraries and classes
import android.Manifest; // Contains constants for permission strings
import android.content.Intent; // Used for launching activities
import android.content.pm.PackageManager; // For checking if permissions are granted
import android.os.Bundle; // For passing data between activities
import android.widget.Button; // UI element for clickable buttons
import android.widget.Toast; // For showing small popup messages to the user

// AndroidX support libraries (newer versions of Android support libraries)
import androidx.annotation.NonNull; // Annotation to indicate a parameter/method/etc. should never be null
import androidx.appcompat.app.AppCompatActivity; // Base class for activities using the support library features
import androidx.core.app.ActivityCompat; // Helper for accessing ActivityCompat features
import androidx.core.content.ContextCompat; // Helper for accessing ContextCompat features

import java.util.ArrayList; // For creating dynamic lists
import java.util.List; // Interface for list data structures

// Main activity class (the entry point of the app)
public class MainActivity extends AppCompatActivity {

    // Constant used to identify permission requests when results come back
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Required call to parent class constructor
        super.onCreate(savedInstanceState);

        // Set the content view to the layout defined in activity_main.xml
        setContentView(R.layout.activity_main);

        // Find the button in the layout by its ID
        Button openMapButton = findViewById(R.id.openMapButton);

        // Set a click listener using lambda expression (-> syntax)
        // This defines what happens when the button is clicked
        openMapButton.setOnClickListener(v -> {
            // Create a new intent to open the MapsActivity
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            // Start the MapsActivity
            startActivity(intent);
        });

        // Call method to check and request necessary permissions
        requestPermissions();
    }

    // Method to check and request required permissions
    private void requestPermissions() {
        // Array of permissions that the app needs
        String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION, // Precise location permission
            Manifest.permission.ACCESS_COARSE_LOCATION, // Approximate location permission
            Manifest.permission.CAMERA // Camera permission
        };

        // Create a list to hold permissions that need to be requested
        List<String> permissionsToRequest = new ArrayList<>();

        // Loop through each permission and check if it's already granted
        for (String permission : permissions) {
            // If permission is not granted, add it to the list to request
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        // If there are permissions that need to be requested
        if (!permissionsToRequest.isEmpty()) {
            // Request all needed permissions at once
            // Convert ArrayList to array and pass the permission request code
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    // This method is called when the user responds to permission requests
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Required call to parent method
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Check if this result is for our permission request
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Assume all permissions are granted until we find one that isn't
            boolean allGranted = true;
            // Prepare message for denied permissions
            StringBuilder deniedPermissions = new StringBuilder("Please enable the following permissions:\n");

            // Loop through each permission result
            for (int i = 0; i < grantResults.length; i++) {
                // If a permission was denied
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // Set flag to false since not all permissions were granted
                    allGranted = false;
                    // Add the denied permission to the message
                    deniedPermissions.append("- ").append(permissions[i]).append("\n");
                }
            }

            // If any permissions were denied, show a toast message
            if (!allGranted) {
                Toast.makeText(this, deniedPermissions.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }
}