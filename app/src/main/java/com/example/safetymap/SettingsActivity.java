package com.example.safetymap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.safetymap.R;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "AppSettingsPrefs";
    public static final String NOTIFICATIONS_ENABLED_KEY = "notifications_enabled";
    public static final String MAP_TYPE_KEY = "map_type";
    public static final String PROXIMITY_RADIUS_KEY = "proximity_radius";
    public static final String SHOW_WARNINGS_KEY = "show_warnings";
    public static final String SHOW_COMPLAINTS_KEY = "show_complaints";
    public static final String LOCATION_UPDATE_INTERVAL_KEY = "location_update_interval";
    public static final String THEME_KEY = "app_theme";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final String NOTIFICATION_SOUND_KEY = "notification_sound";
    public static final String NOTIFICATION_VIBRATION_KEY = "notification_vibration";

    private Switch notificationsSwitch;
    private Spinner mapTypeSpinner;
    private SeekBar proximityRadiusSeekBar;
    private TextView proximityRadiusValue;
    private CheckBox showWarningsCheckbox;
    private CheckBox showComplaintsCheckbox;
    private SeekBar locationUpdateIntervalSeekBar;
    private TextView locationUpdateIntervalValue;
    private RadioGroup themeRadioGroup;
    private Switch notificationSoundSwitch;
    private Switch notificationVibrationSwitch;

    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load saved theme before super.onCreate()
        settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int theme = settings.getInt(THEME_KEY, THEME_LIGHT);
        if (theme == THEME_LIGHT) {
            setTheme(R.style.AppTheme_Light);
        } else {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize components
        notificationsSwitch = findViewById(R.id.notifications_switch);
        mapTypeSpinner = findViewById(R.id.map_type_spinner);
        proximityRadiusSeekBar = findViewById(R.id.proximity_radius_seekbar);
        proximityRadiusValue = findViewById(R.id.proximity_radius_value);
        showWarningsCheckbox = findViewById(R.id.show_warnings_checkbox);
        showComplaintsCheckbox = findViewById(R.id.show_complaints_checkbox);
        locationUpdateIntervalSeekBar = findViewById(R.id.location_update_interval_seekbar);
        locationUpdateIntervalValue = findViewById(R.id.location_update_interval_value);
        themeRadioGroup = findViewById(R.id.theme_radio_group);
        notificationSoundSwitch = findViewById(R.id.notification_sound_switch);
        notificationVibrationSwitch = findViewById(R.id.notification_vibration_switch);

        // Load saved preferences
        boolean notificationsEnabled = settings.getBoolean(NOTIFICATIONS_ENABLED_KEY, true);
        notificationsSwitch.setChecked(notificationsEnabled);

        // Map Type
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.map_type_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mapTypeSpinner.setAdapter(adapter);

        int mapType = settings.getInt(MAP_TYPE_KEY, 1);
        mapTypeSpinner.setSelection(mapType - 1); // Adjust for array index

        // Proximity Radius
        int savedRadius = settings.getInt(PROXIMITY_RADIUS_KEY, 50);
        proximityRadiusSeekBar.setProgress(savedRadius);
        proximityRadiusValue.setText(savedRadius + " meters");

        // Marker Filtering
        boolean showWarnings = settings.getBoolean(SHOW_WARNINGS_KEY, true);
        boolean showComplaints = settings.getBoolean(SHOW_COMPLAINTS_KEY, true);
        showWarningsCheckbox.setChecked(showWarnings);
        showComplaintsCheckbox.setChecked(showComplaints);

        // Location Update Interval
        int savedInterval = settings.getInt(LOCATION_UPDATE_INTERVAL_KEY, 5);
        locationUpdateIntervalSeekBar.setProgress(savedInterval);
        locationUpdateIntervalValue.setText(savedInterval + " seconds");

        // Theme Selection
        if (theme == THEME_LIGHT) {
            themeRadioGroup.check(R.id.theme_light);
        } else {
            themeRadioGroup.check(R.id.theme_dark);
        }

        // Notification Sound and Vibration
        boolean soundEnabled = settings.getBoolean(NOTIFICATION_SOUND_KEY, true);
        boolean vibrationEnabled = settings.getBoolean(NOTIFICATION_VIBRATION_KEY, true);
        notificationSoundSwitch.setChecked(soundEnabled);
        notificationVibrationSwitch.setChecked(vibrationEnabled);

        // Set Listeners
        notificationsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(NOTIFICATIONS_ENABLED_KEY, isChecked);
            editor.apply();
        });

        mapTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedMapType = position + 1; // GoogleMap map types start from 1
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(MAP_TYPE_KEY, selectedMapType);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        proximityRadiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue = progress;
                proximityRadiusValue.setText(progressValue + " meters");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(PROXIMITY_RADIUS_KEY, progressValue);
                editor.apply();
            }
        });

        showWarningsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(SHOW_WARNINGS_KEY, isChecked);
            editor.apply();
        });

        showComplaintsCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(SHOW_COMPLAINTS_KEY, isChecked);
            editor.apply();
        });

        locationUpdateIntervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue = progress;
                locationUpdateIntervalValue.setText(progressValue + " seconds");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(LOCATION_UPDATE_INTERVAL_KEY, progressValue);
                editor.apply();
            }
        });

        themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int selectedTheme = (checkedId == R.id.theme_light) ? THEME_LIGHT : THEME_DARK;
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(THEME_KEY, selectedTheme);
            editor.apply();

            // Recreate activity to apply theme
            recreate();
        });

        notificationSoundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(NOTIFICATION_SOUND_KEY, isChecked);
            editor.apply();
        });

        notificationVibrationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(NOTIFICATION_VIBRATION_KEY, isChecked);
            editor.apply();
        });
    }
}
