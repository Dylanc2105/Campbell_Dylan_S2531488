package com.example.campbell_dylan_s2531488;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener {

    private TextView rawDataDisplay;
    private Button startButton;
    private Button roadworksButton;
    private Button mapButton;

    private View rawDataScroll;
    private View eventListContainer;
    private View mapContainer;

    private EventListFragment eventListFragment;
    private MapFragment mapFragment;
    private TrafficViewModel trafficViewModel;

    private boolean showingMap = false;
    private View landscapeDetailsPanel;

    private static final String STATE_SHOWING_MAP = "showingMap";

    private static final String UNPLANNED = "UNPLANNED EVENTS";
    private static final String ROADWORKS = "CURRENT ROADWORKS";

    private static final int ACTIVE_BLUE = Color.rgb(36, 85, 154);
    private static final int INACTIVE_BLUE = Color.rgb(230, 239, 250);
    private static final int INACTIVE_TEXT = Color.rgb(36, 85, 154);

    private String selectedFeedTitle = UNPLANNED;

    private final String clientId =
            BuildConfig.TRAFFIC_SCOTLAND_CLIENT_ID;

    private final String clientKey =
            BuildConfig.TRAFFIC_SCOTLAND_CLIENT_SECRET;

    private final String unplannedEventsURL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";

    private final String currentRoadworksURL =
            "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private static final long REFRESH_INTERVAL_MS =
            5 * 60 * 1000L;

    private final TrafficViewModel.LoadListener loadListener =
            new TrafficViewModel.LoadListener() {
                @Override
                public void onLoaded(String feedTitle, boolean automatic) {
                    if (!isFinishing() && !isDestroyed()) {
                        trafficLoaded(feedTitle, automatic);
                    }
                }

                @Override
                public void onFailed(String feedTitle, boolean automatic, String message) {
                    if (!isFinishing() && !isDestroyed()) {
                        trafficLoadFailed(feedTitle, automatic, message);
                    }
                }
            };

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {

            if (trafficViewModel != null
                    && trafficViewModel.isDataLoaded()) {

                String feedTitle =
                        trafficViewModel.getCurrentFeedTitle();

                if (ROADWORKS.equals(feedTitle)) {
                    startProgress(
                            currentRoadworksURL,
                            ROADWORKS,
                            true
                    );
                } else if (UNPLANNED.equals(feedTitle)) {
                    startProgress(
                            unplannedEventsURL,
                            UNPLANNED,
                            true
                    );
                }
            }

            mainHandler.postDelayed(this, REFRESH_INTERVAL_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        rawDataDisplay = findViewById(R.id.rawDataDisplay);
        startButton = findViewById(R.id.startButton);
        roadworksButton = findViewById(R.id.roadworksButton);
        mapButton = findViewById(R.id.mapButton);

        rawDataScroll = findViewById(R.id.rawDataScroll);
        eventListContainer = findViewById(R.id.eventListContainer);
        mapContainer = findViewById(R.id.mapContainer);

        landscapeDetailsPanel = findViewById(R.id.landscapeDetailsPanel);
        configureLandscapeDetails();

        startButton.setOnClickListener(this);
        roadworksButton.setOnClickListener(this);
        mapButton.setOnClickListener(this);

        trafficViewModel = new ViewModelProvider(this)
                .get(TrafficViewModel.class);
        trafficViewModel.setLoadListener(loadListener);

        eventListFragment =
                (EventListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.eventListContainer);

        if (eventListFragment == null) {
            eventListFragment = new EventListFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.eventListContainer,
                            eventListFragment
                    )
                    .commitNow();
        }

        eventListFragment.setOnEventSelectedListener(
                this::showEventDetails
        );

        mapFragment =
                (MapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapContainer);
        if (mapFragment != null) {
            mapFragment.setOnMapEventSelectedListener(this::showEventDetails);
        }

        if (savedInstanceState != null) {
            showingMap = savedInstanceState.getBoolean(
                    STATE_SHOWING_MAP,
                    false
            );
        }

        if (trafficViewModel.isDataLoaded()) {

            selectedFeedTitle =
                    trafficViewModel.getCurrentFeedTitle();

            displayEventList(selectedFeedTitle, true);
            updateMapMarkers();

        } else if (trafficViewModel.isLoading()) {
            showMessage("Downloading traffic information...");
            startButton.setEnabled(false);
            roadworksButton.setEnabled(false);

        } else {

            selectedFeedTitle = UNPLANNED;

            startProgress(
                    unplannedEventsURL,
                    UNPLANNED,
                    false
            );

        }

        updateCategoryButtons();

        if (showingMap) {
            showMap();
        } else {
            mapContainer.setVisibility(View.GONE);
            mapButton.setText("View Map");
        }
    }

    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState
    ) {
        outState.putBoolean(
                STATE_SHOWING_MAP,
                showingMap
        );

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mainHandler.removeCallbacks(refreshRunnable);
        mainHandler.postDelayed(
                refreshRunnable,
                REFRESH_INTERVAL_MS
        );
    }

    @Override
    protected void onStop() {
        mainHandler.removeCallbacks(refreshRunnable);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (trafficViewModel != null) {
            trafficViewModel.clearLoadListener(loadListener);
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.startButton) {

            changeFeed(
                    unplannedEventsURL,
                    UNPLANNED
            );

        } else if (view.getId() == R.id.roadworksButton) {

            changeFeed(
                    currentRoadworksURL,
                    ROADWORKS
            );

        } else if (view.getId() == R.id.mapButton) {

            if (showingMap) {
                showEventList();
            } else {
                showMap();
            }
        }
    }

    private void changeFeed(String feedUrl, String feedTitle) {

        if (trafficViewModel.isLoading()) {
            return;
        }

        selectedFeedTitle = feedTitle;
        updateCategoryButtons();

        startProgress(feedUrl, feedTitle, false);
    }

    private void updateCategoryButtons() {

        boolean isUnplanned = UNPLANNED.equals(selectedFeedTitle);

        styleCategoryButton(startButton, isUnplanned);
        styleCategoryButton(roadworksButton, !isUnplanned);
    }

    private void styleCategoryButton(
            Button button,
            boolean selected
    ) {

        int backgroundColour =
                selected ? ACTIVE_BLUE : INACTIVE_BLUE;

        int textColour =
                selected ? Color.WHITE : INACTIVE_TEXT;

        button.setBackgroundTintList(
                ColorStateList.valueOf(backgroundColour)
        );

        button.setTextColor(textColour);

        button.setSelected(selected);
    }

    private void showMap() {

        showingMap = true;

        if (mapFragment == null) {
            mapFragment = new MapFragment();

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.mapContainer,
                            mapFragment
                    )
                    .commitNow();
        }

        mapFragment.setOnMapEventSelectedListener(this::showEventDetails);
        updateMapMarkers();

        rawDataScroll.setVisibility(View.GONE);
        eventListContainer.setVisibility(View.GONE);
        mapContainer.setVisibility(View.VISIBLE);

        mapButton.setText("View List");
    }

    private void showEventList() {

        showingMap = false;

        mapContainer.setVisibility(View.GONE);
        mapButton.setText("View Map");

        if (trafficViewModel.isDataLoaded()) {

            displayEventList(
                    trafficViewModel.getCurrentFeedTitle(),
                    true
            );

        } else {

            showMessage(
                    "Traffic information is still loading."
            );
        }
    }

    private void updateMapMarkers() {

        if (mapFragment != null
                && trafficViewModel != null
                && trafficViewModel.isDataLoaded()) {

            if (eventListFragment != null
                    && eventListFragment.hasActiveFilter()) {
                mapFragment.setEvents(eventListFragment.getFilteredEvents());
            } else {
                mapFragment.setEvents(trafficViewModel.getTrafficEvents());
            }
        }
    }

    private void showEventOnMap(TrafficEvent event) {

        if (!hasValidLocation(event)) {

            Toast.makeText(
                    this,
                    "No valid map location is available.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        showMap();

        if (mapFragment != null) {
            mapFragment.focusOnEvent(event);
        }
    }

    private boolean hasValidLocation(TrafficEvent event) {

        if (event == null) {
            return false;
        }

        if (isValidCoordinatePair(
                event.getLatitude(),
                event.getLongitude()
        )) {
            return true;
        }

        if (isValidCoordinatePair(
                event.getFromLatitude(),
                event.getFromLongitude()
        )) {
            return true;
        }

        return isValidCoordinatePair(
                event.getToLatitude(),
                event.getToLongitude()
        );
    }

    private boolean isValidCoordinatePair(
            Double latitude,
            Double longitude
    ) {

        if (latitude == null || longitude == null) {
            return false;
        }

        return !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && !Double.isInfinite(latitude)
                && !Double.isInfinite(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    private void startProgress(
            String feedUrl,
            String feedTitle,
            boolean isAutomaticRefresh
    ) {
        if (trafficViewModel.isLoading()) {
            return;
        }

        if (clientId.isEmpty() || clientKey.isEmpty()) {
            if (!isAutomaticRefresh) {
                showMessage("Traffic Scotland API credentials are missing.");
            }
            return;
        }

        boolean started = trafficViewModel.loadTrafficData(
                feedUrl, feedTitle, clientId, clientKey, isAutomaticRefresh
        );
        if (!started) {
            return;
        }

        if (!isAutomaticRefresh) {
            if (!showingMap) {
                showMessage("Downloading and parsing " + feedTitle + "...");
            } else {
                rawDataScroll.setVisibility(View.GONE);
                eventListContainer.setVisibility(View.GONE);
                mapContainer.setVisibility(View.VISIBLE);
            }
            startButton.setEnabled(false);
            roadworksButton.setEnabled(false);
        }
    }

    private void trafficLoaded(String feedTitle, boolean automatic) {
        selectedFeedTitle = feedTitle;
        updateCategoryButtons();
        displayEventList(feedTitle, automatic);
        updateMapMarkers();
        if (showingMap) {
            showMap();
        }
        startButton.setEnabled(true);
        roadworksButton.setEnabled(true);
    }

    private void trafficLoadFailed(
            String feedTitle, boolean automatic, String message
    ) {
        if (trafficViewModel.isDataLoaded()) {
            selectedFeedTitle = trafficViewModel.getCurrentFeedTitle();
            updateCategoryButtons();
        }

        if (automatic) {
            Toast.makeText(this, "Automatic refresh failed", Toast.LENGTH_SHORT).show();
        } else if (showingMap && trafficViewModel.isDataLoaded()) {
            Toast.makeText(
                    this,
                    "Unable to update traffic data. Showing previous results.",
                    Toast.LENGTH_LONG
            ).show();
        } else {
            showMessage("Error downloading " + feedTitle + ":\n" + message);
        }

        startButton.setEnabled(true);
        roadworksButton.setEnabled(true);
    }

    private void showMessage(String message) {

        rawDataScroll.setVisibility(View.VISIBLE);
        eventListContainer.setVisibility(View.GONE);
        mapContainer.setVisibility(View.GONE);

        rawDataDisplay.setText(message);
    }

    private void displayEventList(String feedTitle, boolean preserveSearch) {
        if (!preserveSearch) {
            trafficViewModel.setSearchText("");
        }
        eventListFragment.setEvents(
                trafficViewModel.getTrafficEvents(), feedTitle
        );

        if (!showingMap) {
            rawDataScroll.setVisibility(View.GONE);
            eventListContainer.setVisibility(View.VISIBLE);
            mapContainer.setVisibility(View.GONE);
        }
    }

    private void showEventDetails(TrafficEvent event) {
        if (event == null) {
            return;
        }

        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE
                && landscapeDetailsPanel != null) {
            showLandscapeDetails(event);
            return;
        }

        Dialog sheet = new Dialog(this);
        sheet.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = LayoutInflater.from(this).inflate(
                R.layout.bottom_sheet_event_details, null, false);

        TextView category = content.findViewById(R.id.detailsCategory);
        TextView road = content.findViewById(R.id.detailsRoadName);
        TextView description = content.findViewById(R.id.detailsDescription);
        TextView severity = content.findViewById(R.id.detailsSeverity);
        TextView type = content.findViewById(R.id.detailsEventType);
        TextView start = content.findViewById(R.id.detailsStart);
        TextView end = content.findViewById(R.id.detailsEnd);
        TextView locationToggle = content.findViewById(R.id.detailsLocationToggle);
        TextView coordinates = content.findViewById(R.id.detailsCoordinates);
        TextView showOnMap = content.findViewById(R.id.detailsShowOnMap);

        category.setText(selectedFeedTitle);
        road.setText(showValue(event.getRoadName()));
        description.setText(showValue(event.getDescription()));
        severity.setText(formatEventLabel(event.getSeverity()));
        type.setText(formatEventLabel(event.getEventType()));
        start.setText(formatEventDate(event.getStartTime()));
        end.setText(formatEventDate(event.getEndTime()));

        String coordinateText =
                "Latitude: " + showCoordinate(event.getLatitude())
                        + "\nLongitude: " + showCoordinate(event.getLongitude())
                        + "\n\nFrom: " + showCoordinate(event.getFromLatitude())
                        + ", " + showCoordinate(event.getFromLongitude())
                        + "\nTo: " + showCoordinate(event.getToLatitude())
                        + ", " + showCoordinate(event.getToLongitude());
        coordinates.setText(coordinateText);
        coordinates.setVisibility(View.GONE);
        locationToggle.setText("Location details  ▾");
        locationToggle.setOnClickListener(v -> {
            boolean expanding = coordinates.getVisibility() != View.VISIBLE;
            coordinates.setVisibility(expanding ? View.VISIBLE : View.GONE);
            locationToggle.setText(expanding
                    ? "Location details  ▴" : "Location details  ▾");
        });

        if (hasValidLocation(event)) {
            showOnMap.setVisibility(View.VISIBLE);
            showOnMap.setText(showingMap ? "Centre on Map" : "Show on Map");
            showOnMap.setOnClickListener(v -> {
                sheet.dismiss();
                showEventOnMap(event);
            });
        } else {
            showOnMap.setVisibility(View.GONE);
        }

        GradientDrawable panelBackground = new GradientDrawable();
        panelBackground.setColor(Color.WHITE);
        panelBackground.setCornerRadii(new float[]{
                dp(20), dp(20), dp(20), dp(20),
                0, 0, 0, 0
        });
        content.setBackground(panelBackground);
        content.setClipToOutline(true);

        View.OnTouchListener swipeToDismiss = new View.OnTouchListener() {
            private float startingY;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getActionMasked();

                if (action == MotionEvent.ACTION_DOWN) {
                    startingY = motionEvent.getRawY();
                    return true;
                }

                if (action == MotionEvent.ACTION_MOVE) {
                    float distance = Math.max(
                            0f, motionEvent.getRawY() - startingY);
                    content.setTranslationY(distance);
                    return true;
                }

                if (action == MotionEvent.ACTION_UP) {
                    float distance = motionEvent.getRawY() - startingY;
                    if (distance > dp(65)) {
                        sheet.dismiss();
                    } else {
                        content.animate()
                                .translationY(0f)
                                .setDuration(150)
                                .start();
                    }
                    return true;
                }

                if (action == MotionEvent.ACTION_CANCEL) {
                    content.animate()
                            .translationY(0f)
                            .setDuration(150)
                            .start();
                    return true;
                }

                return false;
            }
        };

        if (content instanceof ViewGroup) {
            View handle = ((ViewGroup) content).getChildAt(0);
            if (handle != null) {
                handle.setOnTouchListener(swipeToDismiss);
            }
        }
        category.setOnTouchListener(swipeToDismiss);

        sheet.setContentView(content);
        sheet.setCancelable(true);
        sheet.setCanceledOnTouchOutside(true);
        sheet.show();

        Window window = sheet.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.35f;
            window.setAttributes(params);
        }

        content.setTranslationY(dp(320));
        content.animate().translationY(0f).setDuration(220).start();
    }

    private void configureLandscapeDetails() {
        if (landscapeDetailsPanel == null) {
            return;
        }

        View handle = findViewById(R.id.landscapeDetailsHandle);
        if (handle != null) {

            handle.setOnClickListener(v -> hideLandscapeDetails());
            handle.setOnTouchListener(new View.OnTouchListener() {
                private float downY;
                private boolean dragged;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            downY = event.getRawY();
                            dragged = false;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            float distance = event.getRawY() - downY;
                            if (distance > dp(12)) {
                                dragged = true;
                                landscapeDetailsPanel.setTranslationY(distance);
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                            float travelled = event.getRawY() - downY;
                            if (travelled > dp(55)) {
                                hideLandscapeDetails();
                            } else if (!dragged) {
                                hideLandscapeDetails();
                            } else {
                                landscapeDetailsPanel.animate()
                                        .translationY(0).setDuration(170).start();
                            }
                            return true;
                        case MotionEvent.ACTION_CANCEL:
                            landscapeDetailsPanel.animate()
                                    .translationY(0).setDuration(170).start();
                            return true;
                        default:
                            return false;
                    }
                }
            });
        }
    }

    private int dp(float size) {
        return (int) (size * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void showLandscapeDetails(TrafficEvent event) {
        if (landscapeDetailsPanel == null) {
            return;
        }

        ((TextView) findViewById(R.id.landscapeDetailsCategory))
                .setText(selectedFeedTitle);
        ((TextView) findViewById(R.id.landscapeDetailsRoad))
                .setText(showValue(event.getRoadName()));
        ((TextView) findViewById(R.id.landscapeDetailsDescription))
                .setText(showValue(event.getDescription()));
        ((TextView) findViewById(R.id.landscapeDetailsSeverity))
                .setText(formatEventLabel(event.getSeverity()));
        ((TextView) findViewById(R.id.landscapeDetailsType))
                .setText(formatEventLabel(event.getEventType()));
        ((TextView) findViewById(R.id.landscapeDetailsStart))
                .setText(formatEventDate(event.getStartTime()));
        ((TextView) findViewById(R.id.landscapeDetailsEnd))
                .setText(formatEventDate(event.getEndTime()));

        TextView coordinates = findViewById(R.id.landscapeDetailsCoordinates);
        TextView locationToggle = findViewById(R.id.landscapeDetailsLocationToggle);
        coordinates.setText(
                "Latitude: " + showCoordinate(event.getLatitude())
                        + "\nLongitude: " + showCoordinate(event.getLongitude())
                        + "\nFrom: " + showCoordinate(event.getFromLatitude())
                        + ", " + showCoordinate(event.getFromLongitude())
                        + "\nTo: " + showCoordinate(event.getToLatitude())
                        + ", " + showCoordinate(event.getToLongitude())
        );
        coordinates.setVisibility(View.GONE);
        locationToggle.setText("Location details  ▾");
        locationToggle.setOnClickListener(v -> {
            boolean expand = coordinates.getVisibility() != View.VISIBLE;
            coordinates.setVisibility(expand ? View.VISIBLE : View.GONE);
            locationToggle.setText(expand
                    ? "Location details  ▴" : "Location details  ▾");
        });

        Button mapAction = findViewById(R.id.landscapeDetailsMapButton);
        if (hasValidLocation(event)) {
            mapAction.setVisibility(View.VISIBLE);
            mapAction.setText(showingMap ? "Centre on Map" : "Show on Map");
            mapAction.setOnClickListener(v -> {
                hideLandscapeDetails();
                showEventOnMap(event);
            });
        } else {
            mapAction.setVisibility(View.GONE);
        }

        landscapeDetailsPanel.animate().cancel();
        if (landscapeDetailsPanel.getVisibility() != View.VISIBLE) {
            landscapeDetailsPanel.setVisibility(View.VISIBLE);
            landscapeDetailsPanel.post(() -> {
                if (landscapeDetailsPanel.getVisibility() == View.VISIBLE) {
                    landscapeDetailsPanel.setTranslationY(landscapeDetailsPanel.getHeight());
                    landscapeDetailsPanel.animate().translationY(0)
                            .setDuration(250).start();
                }
            });
        } else {
            landscapeDetailsPanel.animate().translationY(0)
                    .setDuration(180).start();
        }
    }

    private void hideLandscapeDetails() {
        if (landscapeDetailsPanel == null
                || landscapeDetailsPanel.getVisibility() != View.VISIBLE) {
            return;
        }
        landscapeDetailsPanel.animate().cancel();
        landscapeDetailsPanel.animate()
                .translationY(landscapeDetailsPanel.getHeight())
                .setDuration(220)
                .withEndAction(() -> {
                    landscapeDetailsPanel.setVisibility(View.GONE);
                    landscapeDetailsPanel.setTranslationY(0);
                }).start();
    }

    private String formatEventLabel(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Not provided";
        }
        String readable = value.trim()
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");
        return readable.substring(0, 1).toUpperCase(Locale.UK)
                + readable.substring(1);
    }

    private String formatEventDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Not provided";
        }
        DateTimeFormatter display = DateTimeFormatter.ofPattern(
                "d MMM yyyy, HH:mm", Locale.UK);
        try {

            return OffsetDateTime.parse(value).format(display);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value).format(display);
            } catch (DateTimeParseException ignoredAgain) {

                return value;
            }
        }
    }

    private String showValue(String value) {

        if (value == null || value.trim().isEmpty()) {
            return "Not provided";
        }

        return value;
    }

    private String showCoordinate(Double coordinate) {

        if (coordinate == null) {
            return "Not provided";
        }

        return coordinate.toString();
    }
}
