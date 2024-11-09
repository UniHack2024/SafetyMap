package com.example.safetymap;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

//import com.example.safetmapapp.R;

import com.example.safetymap.SettingsActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String DIRECTIONS_API_KEY = "AIzaSyA8yicEvne7a6Q9Cn6zi8WrOfoVSmAazH0";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private LatLng currentLatLng;
    private LatLng destinationLatLng;
    private Polyline currentRoute;

    private Circle locationCircle;
    private Marker currentWaypoint;
    private Marker selectedDestinationMarker;
    private FloatingActionButton warningFab;
    private View removeRouteButton;

    // Use HashMap to keep track of all markers and additional info
    private final HashMap<Marker, MarkerInfo> warningMarkers = new HashMap<>();
    private final HashMap<Marker, MarkerInfo> complaintMarkers = new HashMap<>();
    private boolean isAddingWarning = false;
    private boolean isAddingComplaint = false;

    // For handling route fetching
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // For managing warnings and complaints
    private String currentCategory;
    private String currentDescription;
    private String currentRating;
    private Uri currentImageUri;

    // UI State
    private enum Mode {
        NORMAL,
        COMPLAINTS,
        SETTINGS
    }

    private Mode currentMode = Mode.NORMAL;

    // Constants for camera and gallery
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int GALLERY_REQUEST_CODE = 200;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 201;

    private Uri tempImageUri;
    private Uri[] selectedImageUri = new Uri[1];
    private ImageView photoPreview;

    // Notification and Proximity
    private static final String CHANNEL_ID = "ProximityAlerts";
    private static final int NOTIFICATION_ID = 1;

    private SharedPreferences settings;

    private int proximityRadius = 50; // Default value, will be updated from settings

    // FusedLocationProviderClient and LocationCallback
    private LocationCallback locationCallback;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load saved theme before super.onCreate
        settings = getSharedPreferences(SettingsActivity.PREFS_NAME, MODE_PRIVATE);
        int theme = settings.getInt(SettingsActivity.THEME_KEY, SettingsActivity.THEME_LIGHT);
        if (theme == SettingsActivity.THEME_LIGHT) {
            setTheme(R.style.AppTheme_Light);
        } else {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Create notification channel
        createNotificationChannel();

        // Initialize location client and callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                // ... Existing code
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    checkProximityToMarkers(currentLatLng);
                }
            }
        };

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), DIRECTIONS_API_KEY);
        }

        // Setup AutocompleteSupportFragment for location search
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NotNull Place place) {
                    if (place.getLatLng() != null) {
                        destinationLatLng = place.getLatLng();
                        if (selectedDestinationMarker != null) {
                            selectedDestinationMarker.remove();
                        }
                        selectedDestinationMarker = mMap.addMarker(new MarkerOptions()
                                .position(destinationLatLng)
                                .title(place.getName()));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 18));
                    }
                }

                @Override
                public void onError(@NotNull com.google.android.gms.common.api.Status status) {
                    Log.e("MapsActivity", "An error occurred: " + status);
                    Toast.makeText(MapsActivity.this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Warning Floating Action Button
        warningFab = findViewById(R.id.warning_fab);
        warningFab.setOnClickListener(v -> {
            if (currentMode == Mode.COMPLAINTS) {
                showComplaintCategoryDialog();
            } else {
                showWarningCategoryDialog();
            }
        });

        // Remove Route Button
        removeRouteButton = findViewById(R.id.remove_route_button);
        removeRouteButton.setVisibility(View.GONE);
        removeRouteButton.setOnClickListener(v -> {
            if (currentRoute != null) {
                currentRoute.remove();
                currentRoute = null;
                removeRouteButton.setVisibility(View.GONE);
            }
        });

        // Bottom Navigation View
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                // Handle Home navigation
                currentMode = Mode.NORMAL;
                Toast.makeText(MapsActivity.this, "Home selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.navigation_complaints) {
                // Handle Complaints navigation
                currentMode = Mode.COMPLAINTS;
                Toast.makeText(MapsActivity.this, "Complaints selected", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.navigation_settings) {
                // Handle Settings navigation
                currentMode = Mode.SETTINGS;
                // Start SettingsActivity
                Intent intent = new Intent(MapsActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            } else {
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Set map type from settings
        int mapType = settings.getInt(SettingsActivity.MAP_TYPE_KEY, GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMapType(mapType);

        // Set a single OnMapClickListener
        mMap.setOnMapClickListener(latLng -> {
            if (isAddingWarning) {
                addWarningMarker(latLng, currentCategory, currentDescription, currentRating, currentImageUri);
                isAddingWarning = false;
            } else if (isAddingComplaint) {
                addComplaintMarker(latLng, currentCategory, currentDescription, currentRating, currentImageUri);
                isAddingComplaint = false;
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableUserLocation();
        }

        // Long Press to Add Waypoint Marker
        mMap.setOnMapLongClickListener(latLng -> {
            if (currentWaypoint != null) {
                currentWaypoint.remove();
            }
            currentWaypoint = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Waypoint")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)));
            destinationLatLng = latLng;
            Toast.makeText(MapsActivity.this, "Waypoint added", Toast.LENGTH_SHORT).show();
        });

        // Click on Marker to View Options
        mMap.setOnMarkerClickListener(marker -> {
            if (warningMarkers.containsKey(marker)) {
                centerMarker(marker, 18);
                marker.showInfoWindow(); // Ensure the marker's info window remains visible
                showMarkerOptionsDialog(marker, warningMarkers);
            } else if (complaintMarkers.containsKey(marker)) {
                centerMarker(marker, 18);
                marker.showInfoWindow();
                showMarkerOptionsDialog(marker, complaintMarkers);
            } else if (marker.equals(currentWaypoint) || marker.equals(selectedDestinationMarker)) {
                centerMarker(marker, 18);
                marker.showInfoWindow();
                showMarkerOptionsDialog(marker, null);
            }
            return true;
        });

        // Click on POI to Set as Destination
        mMap.setOnPoiClickListener(poi -> {
            if (selectedDestinationMarker != null) {
                selectedDestinationMarker.remove();
            }
            destinationLatLng = poi.latLng;
            selectedDestinationMarker = mMap.addMarker(new MarkerOptions()
                    .position(destinationLatLng)
                    .title(poi.name));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 18));
            showMarkerOptionsDialog(selectedDestinationMarker, null);
        });

        updateMarkerVisibility();
    }

    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Disable the default location indicator
            mMap.setMyLocationEnabled(false);

            // Get the user's current location and add a custom circle as the location indicator
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18));

                            // Create a custom circle overlay with light purple color at the user's location
                            if (locationCircle != null) {
                                locationCircle.remove();
                            }
                            locationCircle = mMap.addCircle(new CircleOptions()
                                    .center(currentLatLng)
                                    .radius(4) // Adjust radius as needed
                                    .fillColor(Color.rgb(174, 81, 152)) // Light purple color
                                    .strokeColor(Color.argb(255, 180, 140, 255)) // Slightly darker purple for the border
                                    .strokeWidth(12));
                        }
                    });

            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // Get interval from settings
            int intervalSeconds = settings.getInt(SettingsActivity.LOCATION_UPDATE_INTERVAL_KEY, 5);
            long intervalMillis = intervalSeconds * 1000L;

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(intervalMillis);
            locationRequest.setFastestInterval(intervalMillis / 2);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Proximity Alerts";
            String description = "Notifications when near warnings or complaints";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void checkProximityToMarkers(LatLng currentLocation) {
        boolean notificationsEnabled = settings.getBoolean(SettingsActivity.NOTIFICATIONS_ENABLED_KEY, true);
        if (!notificationsEnabled) {
            return;
        }

        proximityRadius = settings.getInt(SettingsActivity.PROXIMITY_RADIUS_KEY, 50);

        // Check warnings
        for (Marker marker : warningMarkers.keySet()) {
            if (!marker.isVisible()) continue;
            float[] results = new float[1];
            Location.distanceBetween(currentLocation.latitude, currentLocation.longitude,
                    marker.getPosition().latitude, marker.getPosition().longitude, results);
            if (results[0] < proximityRadius) {
                sendProximityNotification(marker, "Warning");
                break; // Notify once per interval
            }
        }

        // Check complaints
        for (Marker marker : complaintMarkers.keySet()) {
            if (!marker.isVisible()) continue;
            float[] results = new float[1];
            Location.distanceBetween(currentLocation.latitude, currentLocation.longitude,
                    marker.getPosition().latitude, marker.getPosition().longitude, results);
            if (results[0] < proximityRadius) {
                sendProximityNotification(marker, "Complaint");
                break; // Notify once per interval
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void sendProximityNotification(Marker marker, String type) {
        String title = "Nearby " + type;
        String content = "You are near a " + type.toLowerCase() + ": " + marker.getTitle();

        boolean soundEnabled = settings.getBoolean(SettingsActivity.NOTIFICATION_SOUND_KEY, true);
        boolean vibrationEnabled = settings.getBoolean(SettingsActivity.NOTIFICATION_VIBRATION_KEY, true);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Ensure you have this icon
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        if (soundEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
        }

        if (vibrationEnabled) {
            builder.setVibrate(new long[]{0, 500, 500, 500});
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();

        // Update map type and marker visibility
        if (mMap != null) {
            int mapType = settings.getInt(SettingsActivity.MAP_TYPE_KEY, GoogleMap.MAP_TYPE_NORMAL);
            mMap.setMapType(mapType);

            updateMarkerVisibility();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void updateMarkerVisibility() {
        boolean showWarnings = settings.getBoolean(SettingsActivity.SHOW_WARNINGS_KEY, true);
        boolean showComplaints = settings.getBoolean(SettingsActivity.SHOW_COMPLAINTS_KEY, true);

        for (Marker marker : warningMarkers.keySet()) {
            marker.setVisible(showWarnings);
        }

        for (Marker marker : complaintMarkers.keySet()) {
            marker.setVisible(showComplaints);
        }
    }

    private void showWarningCategoryDialog() {
        // Warning categories and their corresponding icons
        final String[] warningCategories = {"Theft", "Fire", "Dangerous Street", "Road Hole", "Falling Plaster"};
        final int[] icons = {
                R.drawable.ic_theft,
                R.drawable.ic_fire,
                R.drawable.ic_dangerous_street,
                R.drawable.ic_road_hole,
                R.drawable.ic_falling_plaster
        };

        // Create a custom adapter for icons and centered text
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Select Warning Category");
        builder.setAdapter(new CategoryAdapter(this, warningCategories, icons), (dialog, which) -> {
            showDescriptionDialog(warningCategories[which], true);
        });
        builder.show();
    }

    private void showComplaintCategoryDialog() {
        // Complaint categories and their corresponding icons
        final String[] complaintCategories = {"Water and Sewage", "Sites", "Streets and Sidewalks", "Road Traffic and Traffic Signs", "Agricultural Markets", "Public Order"};
        final int[] icons = {
                R.drawable.ic_water,
                R.drawable.ic_sites,
                R.drawable.ic_streets,
                R.drawable.ic_traffic_signs,
                R.drawable.ic_markets,
                R.drawable.ic_public_order
        };

        // Create a custom adapter for icons and centered text
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Select Complaint Category");
        builder.setAdapter(new CategoryAdapter(this, complaintCategories, icons), (dialog, which) -> {
            showDescriptionDialog(complaintCategories[which], false);
        });
        builder.show();
    }

    // Custom adapter for categories with icons
    private static class CategoryAdapter extends ArrayAdapter<String> {
        Context context;
        String[] categories;
        int[] icons;

        CategoryAdapter(Context context, String[] categories, int[] icons) {
            super(context, R.layout.dialog_warning_category, categories);
            this.context = context;
            this.categories = categories;
            this.icons = icons;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.dialog_warning_category, parent, false);
            }

            TextView textView = convertView.findViewById(R.id.warning_category_text);
            ImageView iconView = convertView.findViewById(R.id.warning_category_icon);

            textView.setText(categories[position]);
            textView.setGravity(Gravity.CENTER); // Center the text
            iconView.setImageResource(icons[position]);

            return convertView;
        }
    }

    private void showDescriptionDialog(final String category, boolean isWarning) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog);

        View dialogView = getLayoutInflater().inflate(R.layout.custom_description_dialog, null);
        builder.setView(dialogView);

        TextView titleView = dialogView.findViewById(R.id.dialog_title);
        titleView.setText("Add Description");

        EditText input = dialogView.findViewById(R.id.dialog_description_input);
        View okButton = dialogView.findViewById(R.id.dialog_ok_button);

        // ImageView and Button for photo
        photoPreview = dialogView.findViewById(R.id.photo_preview);
        Button addPhotoButton = dialogView.findViewById(R.id.add_photo_button);

        // Rating prompt and buttons
        TextView ratingPrompt = dialogView.findViewById(R.id.rating_prompt);
        Button ratingLow = dialogView.findViewById(R.id.rating_low);
        Button ratingIntermediary = dialogView.findViewById(R.id.rating_intermediary);
        Button ratingDangerous = dialogView.findViewById(R.id.rating_dangerous);

        final String[] selectedRating = {null};
        selectedImageUri[0] = null;
        currentImageUri = null;

        if (isWarning) {
            ratingPrompt.setText("Rate this warning:");
            // Set text for warning ratings
            ratingLow.setText("Low");
            ratingIntermediary.setText("Moderate");
            ratingDangerous.setText("High");

            // Set up rating button listeners
            ratingLow.setOnClickListener(v -> selectedRating[0] = "Low");
            ratingIntermediary.setOnClickListener(v -> selectedRating[0] = "Moderate");
            ratingDangerous.setOnClickListener(v -> selectedRating[0] = "High");
        } else {
            ratingPrompt.setText("Set urgency level:");
            // Set text for complaint urgency ratings
            ratingLow.setText("Non-Urgent");
            ratingIntermediary.setText("Moderate");
            ratingDangerous.setText("Urgent");

            // Set up rating button listeners
            ratingLow.setOnClickListener(v -> selectedRating[0] = "Non-Urgent");
            ratingIntermediary.setOnClickListener(v -> selectedRating[0] = "Moderate");
            ratingDangerous.setOnClickListener(v -> selectedRating[0] = "Urgent");
        }

        // Add photo button click listener
        addPhotoButton.setOnClickListener(v -> {
            // Open dialog to choose between taking a photo or selecting from gallery
            showImagePickerOptions();
        });

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle OK button click
        okButton.setOnClickListener(v -> {
            currentDescription = input.getText().toString();
            currentCategory = category;
            Uri imageUri = selectedImageUri[0];
            currentRating = selectedRating[0];  // Assign the selected rating/urgency

            if (currentRating == null) {
                Toast.makeText(MapsActivity.this, "Please select a rating.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isWarning) {
                isAddingWarning = true;
                currentImageUri = imageUri;
                Toast.makeText(MapsActivity.this, "Click on the map to add a " + category + " warning", Toast.LENGTH_SHORT).show();
            } else {
                isAddingComplaint = true;
                currentImageUri = imageUri;
                Toast.makeText(MapsActivity.this, "Click on the map to add a complaint about " + category, Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss(); // Close the dialog after "OK" is clicked
        });
    }

    private void showImagePickerOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Take photo
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                        } else {
                            openCamera();
                        }
                    } else {
                        // Choose from gallery
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
                        } else {
                            openGallery();
                        }
                    }
                })
                .show();
    }


    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create a file to store the image
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                tempImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }


    private File createImageFile() throws IOException {
        // Create an image file name with a timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // Use getExternalFilesDir to get the directory for your app's private images
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Ensure the directory exists
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }

        // Create the image file
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return imageFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                // Image captured by camera
                if (tempImageUri != null) {
                    photoPreview.setVisibility(View.VISIBLE);
                    photoPreview.setImageURI(tempImageUri);
                    selectedImageUri[0] = tempImageUri;
                }
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                // Image selected from gallery
                Uri selectedUri = data.getData();
                photoPreview.setVisibility(View.VISIBLE);
                photoPreview.setImageURI(selectedUri);
                selectedImageUri[0] = selectedUri;
            }
        }
    }

    private void addWarningMarker(LatLng latLng, String category, String description, String rating, Uri imageUri) {
        String currentDate = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date());

        // Define radius and color intensity based on rating
        int radius;
        int fillColor;

        switch (rating) {
            case "Low":
                radius = 10;  // Smaller radius for low warnings
                fillColor = Color.argb(50, 0, 255, 0);  // Light green with more transparency
                break;
            case "Moderate":
                radius = 25;  // Medium radius for intermediary warnings
                fillColor = Color.argb(100, 255, 165, 0);  // Orange with moderate transparency
                break;
            case "High":
                radius = 40;  // Larger radius for dangerous warnings
                fillColor = Color.argb(150, 255, 0, 0);  // Red with less transparency
                break;
            default:
                radius = 50;  // Default radius
                fillColor = Color.argb(100, 0, 0, 255);  // Default to blue color
                break;
        }

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(category + " - " + currentDate)
                .snippet("Rating: " + rating + "\nNumber of Cases: 1")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        if (marker != null) {
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(latLng)
                    .radius(radius)  // Dynamic radius based on rating
                    .strokeColor(Color.RED)
                    .fillColor(fillColor));  // Dynamic color intensity based on rating

            // Save the marker and its info
            List<Uri> imageUris = new ArrayList<>();
            if (imageUri != null) {
                imageUris.add(imageUri);
            }
            warningMarkers.put(marker, new MarkerInfo(category, description, new Date(), circle, 1, rating, imageUris));
            marker.showInfoWindow();
        }
    }

    private void addComplaintMarker(LatLng latLng, String category, String description, String urgency, Uri imageUri) {
        String currentDate = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date());

        // Define color and icon based on urgency
        float markerColor;
        String snippetText = "Urgency: " + urgency + "\nNumber of Cases: 1";

        switch (urgency) {
            case "Non-Urgent":
                markerColor = BitmapDescriptorFactory.HUE_GREEN;
                break;
            case "Moderate":
                markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                break;
            case "Urgent":
                markerColor = BitmapDescriptorFactory.HUE_RED;
                break;
            default:
                markerColor = BitmapDescriptorFactory.HUE_ORANGE;
                break;
        }

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(category + " - " + currentDate)
                .snippet(snippetText)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

        if (marker != null) {
            // Save the marker and its info
            List<Uri> imageUris = new ArrayList<>();
            if (imageUri != null) {
                imageUris.add(imageUri);
            }
            complaintMarkers.put(marker, new MarkerInfo(category, description, new Date(), null, 1, urgency, imageUris));
            marker.showInfoWindow();
        }
    }

    private void showMarkerOptionsDialog(Marker marker, HashMap<Marker, MarkerInfo> markerMap) {
        MarkerInfo info;
        if (markerMap != null) {
            info = markerMap.get(marker);
        } else {
            info = null;
        }
        if (info != null) {
            // Update snippet to display urgency or rating
            String snippetText;
            if (warningMarkers.containsKey(marker)) {
                snippetText = "Rating: " + info.ratingOrUrgency + "\nNumber of Cases: " + info.numberOfCases;
            } else if (complaintMarkers.containsKey(marker)) {
                snippetText = "Urgency: " + info.ratingOrUrgency + "\nNumber of Cases: " + info.numberOfCases;
            } else {
                snippetText = "Number of Cases: " + info.numberOfCases;
            }
            marker.setSnippet(snippetText);
            marker.showInfoWindow();

            // Inflate the custom layout for marker options
            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.custom_marker_options_dialog, null);

            // Set up the custom options
            View readDescriptionOption = dialogView.findViewById(R.id.read_description_option);
            View addNewCaseOption = dialogView.findViewById(R.id.add_new_case_option);
            View listOfCasesOption = dialogView.findViewById(R.id.list_of_cases_option);
            View seePhotosOption = dialogView.findViewById(R.id.see_photos_option);

            // Hide "See Photos" option if there are no photos
            if (info.imageUris == null || info.imageUris.isEmpty()) {
                seePhotosOption.setVisibility(View.GONE);
            }

            // Create the AlertDialog using the custom layout
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            // Set up click listeners for each option
            readDescriptionOption.setOnClickListener(v -> {
                dialog.dismiss();

                // Inflate the custom layout
                View descriptionView = LayoutInflater.from(this).inflate(R.layout.custom_description_viewer, null);

                // Find the TextView and Button in the custom layout
                TextView descriptionText = descriptionView.findViewById(R.id.description_text);
                View okButton = descriptionView.findViewById(R.id.description_ok_button);

                // Set the description text
                descriptionText.setText(info.description);

                // Create and show the AlertDialog using the custom layout
                AlertDialog descriptionDialog = new AlertDialog.Builder(this)
                        .setView(descriptionView)
                        .create();

                // Set up the OK button to dismiss the dialog
                okButton.setOnClickListener(v1 -> descriptionDialog.dismiss());

                // Show the description dialog
                descriptionDialog.show();
            });

            addNewCaseOption.setOnClickListener(v -> {
                dialog.dismiss();
                showAddCaseDialog(marker, info, markerMap);
            });

            listOfCasesOption.setOnClickListener(v -> {
                dialog.dismiss();
                showListOfCasesDialog(info);
            });

            seePhotosOption.setOnClickListener(v -> {
                dialog.dismiss();
                showPhotosDialog(info.imageUris);
            });

            // Show the dialog
            dialog.show();
        } else {
            // If the marker is not in the markers map, show default options
            new AlertDialog.Builder(this)
                    .setItems(new CharSequence[]{"Navigate", "Remove"}, (dialogInterface, which) -> {
                        switch (which) {
                            case 0: // Navigate
                                destinationLatLng = marker.getPosition();
                                fetchRoute(currentLatLng, destinationLatLng);
                                break;
                            case 1: // Remove
                                if (marker.equals(currentWaypoint)) {
                                    currentWaypoint = null;
                                } else if (marker.equals(selectedDestinationMarker)) {
                                    selectedDestinationMarker = null;
                                    destinationLatLng = null;
                                }

                                if (currentRoute != null && marker.getPosition().equals(destinationLatLng)) {
                                    currentRoute.remove();
                                    currentRoute = null;
                                    removeRouteButton.setVisibility(View.GONE);
                                }

                                marker.remove();
                                if (warningMarkers.containsKey(marker)) {
                                    MarkerInfo markerInfo = warningMarkers.get(marker);
                                    if (markerInfo.circle != null) {
                                        markerInfo.circle.remove(); // Remove associated circle
                                    }
                                    warningMarkers.remove(marker);
                                }
                                if (complaintMarkers.containsKey(marker)) {
                                    complaintMarkers.remove(marker);
                                }
                                Toast.makeText(MapsActivity.this, "Marker removed", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }).show();
        }
    }

    private void showAddCaseDialog(Marker marker, MarkerInfo info, HashMap<Marker, MarkerInfo> markerMap) {
        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.custom_add_case_dialog, null);

        EditText input = dialogView.findViewById(R.id.new_case_description_input);
        Button addPhotoButton = dialogView.findViewById(R.id.add_photo_button);
        ImageView photoPreview = dialogView.findViewById(R.id.photo_preview);

        final Uri[] newCaseImageUri = {null};

        // Add photo button click listener
        addPhotoButton.setOnClickListener(v -> {
            showImagePickerOptionsForNewCase(newCaseImageUri, photoPreview);
        });

        new AlertDialog.Builder(this)
                .setTitle("Add New Case")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    String newCaseDescription = input.getText().toString();
                    String currentDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());
                    info.caseDescriptions.add(new CaseInfo(currentDate, newCaseDescription, newCaseImageUri[0]));
                    info.numberOfCases++;

                    // Add the new image to the list
                    if (newCaseImageUri[0] != null) {
                        info.imageUris.add(newCaseImageUri[0]);
                    }

                    // Update the marker's snippet
                    if (warningMarkers.containsKey(marker)) {
                        marker.setSnippet("Rating: " + info.ratingOrUrgency + "\nNumber of Cases: " + info.numberOfCases);
                    } else if (complaintMarkers.containsKey(marker)) {
                        marker.setSnippet("Urgency: " + info.ratingOrUrgency + "\nNumber of Cases: " + info.numberOfCases);
                    } else {
                        marker.setSnippet("Number of Cases: " + info.numberOfCases);
                    }
                    marker.showInfoWindow();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showImagePickerOptionsForNewCase(Uri[] imageUriHolder, ImageView photoPreview) {
        String[] options = {"Take Photo", "Choose from Gallery"};

        new AlertDialog.Builder(this)
                .setTitle("Add Photo")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Take photo
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                        } else {
                            openCameraForNewCase(imageUriHolder, photoPreview);
                        }
                    } else {
                        // Choose from gallery
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
                        } else {
                            openGalleryForNewCase(imageUriHolder, photoPreview);
                        }
                    }
                })
                .show();
    }

    private void openCameraForNewCase(Uri[] imageUriHolder, ImageView photoPreview) {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            // Create a file to store the image
            File photoFile = null;
            try {
                photoFile = createImageFile(); // Reuse the method to create a file
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri imageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);

                // Set the image URI to the holder
                imageUriHolder[0] = imageUri;

                // Update the photo preview
                photoPreview.setVisibility(View.VISIBLE);
                photoPreview.setImageURI(imageUri);
            }
        }
    }

    private void openGalleryForNewCase(Uri[] imageUriHolder, ImageView photoPreview) {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE);

        // Handle the result in onActivityResult
        // Set the image URI to the holder in onActivityResult
        // Update the photo preview
    }

    private void showListOfCasesDialog(MarkerInfo info) {
        // Inflate the custom layout for the list of cases
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_list_of_cases, null);

        // Set up the ListView and adapter
        ListView caseListView = dialogView.findViewById(R.id.case_list_view);
        ArrayAdapter<CaseInfo> adapter = new ArrayAdapter<CaseInfo>(this, R.layout.item_case, info.caseDescriptions) {
            @NonNull
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_case, parent, false);
                }

                CaseInfo caseInfo = getItem(position);

                TextView caseDate = convertView.findViewById(R.id.case_date);
                TextView caseDescription = convertView.findViewById(R.id.case_description);
                ImageView casePhoto = convertView.findViewById(R.id.case_photo);

                caseDate.setText("Date: " + caseInfo.date);
                caseDescription.setText(caseInfo.description);

                if (caseInfo.imageUri != null) {
                    casePhoto.setVisibility(View.VISIBLE);
                    casePhoto.setImageURI(caseInfo.imageUri);
                } else {
                    casePhoto.setVisibility(View.GONE);
                }

                return convertView;
            }
        };
        caseListView.setAdapter(adapter);

        // Create and show the dialog
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showPhotosDialog(List<Uri> imageUris) {
        // Inflate the custom layout for viewing photos
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_photos_viewer, null);

        ListView photosListView = dialogView.findViewById(R.id.photos_list_view);

        ArrayAdapter<Uri> adapter = new ArrayAdapter<Uri>(this, R.layout.item_photo, imageUris) {
            @NonNull
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_photo, parent, false);
                }

                Uri imageUri = getItem(position);
                ImageView photoView = convertView.findViewById(R.id.photo_view);

                if (imageUri != null) {
                    photoView.setImageURI(imageUri);
                }

                return convertView;
            }
        };
        photosListView.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    private void centerMarker(Marker marker, float zoomLevel) {
        if (mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), zoomLevel));
        }
    }

    private void fetchRoute(LatLng origin, LatLng destination) {
        executorService.execute(() -> {
            String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=" + origin.latitude + "," + origin.longitude +
                    "&destination=" + destination.latitude + "," + destination.longitude +
                    "&mode=walking&key=" + DIRECTIONS_API_KEY;
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String result = stringBuilder.toString();
                // Now parse the result and update the UI on the main thread
                mainHandler.post(() -> handleRouteResult(result));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void handleRouteResult(String result) {
        if (result != null) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray routes = jsonObject.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String points = overviewPolyline.getString("points");
                    List<LatLng> decodedPath = decodePoly(points);
                    if (currentRoute != null) {
                        currentRoute.remove();
                    }
                    int customPurple = ContextCompat.getColor(MapsActivity.this, R.color.deep_purple);
                    currentRoute = mMap.addPolyline(new PolylineOptions().addAll(decodedPath).color(customPurple).width(10));
                    removeRouteButton.setVisibility(View.VISIBLE);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // Method to decode polyline points
    private List<LatLng> decodePoly(String encoded) {
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

            poly.add(new LatLng((double) lat / 1E5, (double) lng / 1E5));
        }

        return poly;
    }

    public static class MarkerInfo {
        String category;
        String description;
        Date reportedDate;
        Circle circle;
        public int numberOfCases;
        String ratingOrUrgency; // For warnings and complaints
        List<CaseInfo> caseDescriptions;
        List<Uri> imageUris;

        MarkerInfo(String category, String description, Date reportedDate, Circle circle, int numberOfCases, String ratingOrUrgency, List<Uri> imageUris) {
            this.category = category;
            this.description = description;
            this.reportedDate = reportedDate;
            this.circle = circle;
            this.numberOfCases = numberOfCases;
            this.ratingOrUrgency = ratingOrUrgency;
            this.imageUris = imageUris != null ? imageUris : new ArrayList<>();
            this.caseDescriptions = new ArrayList<>();
            this.caseDescriptions.add(new CaseInfo(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(reportedDate), description, imageUris != null && !imageUris.isEmpty() ? imageUris.get(0) : null));
        }
    }

    private static class CaseInfo {
        String date;
        String description;
        Uri imageUri;

        CaseInfo(String date, String description, Uri imageUri) {
            this.date = date;
            this.description = description;
            this.imageUri = imageUri;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted for camera
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted for storage
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission is required to select photos.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Handle location permissions
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
