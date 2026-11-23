package com.example.campbell_dylan_s2531488;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Locale;

public class MapFragment extends Fragment
        implements OnMapReadyCallback {

    private GoogleMap googleMap;

    private final ArrayList<TrafficEvent> trafficEvents =
            new ArrayList<>();

    private TrafficEvent selectedEvent;

    private Marker selectedMarker;

    public interface OnMapEventSelectedListener {
        void onMapEventSelected(TrafficEvent event);
    }

    private OnMapEventSelectedListener eventSelectedListener;

    public void setOnMapEventSelectedListener(OnMapEventSelectedListener listener) {
        eventSelectedListener = listener;
    }

    public MapFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        FrameLayout root =
                new FrameLayout(requireContext());

        root.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        FrameLayout mapFrame =
                new FrameLayout(requireContext());

        mapFrame.setId(View.generateViewId());

        root.addView(
                mapFrame,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        View legend = createTrafficLegend();

        FrameLayout.LayoutParams legendParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP | Gravity.START
                );

        legendParams.setMargins(
                dp(12),
                dp(12),
                dp(12),
                0
        );

        root.addView(legend, legendParams);

        return root;
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        FrameLayout mapFrame =
                (FrameLayout) ((FrameLayout) view).getChildAt(0);

        SupportMapFragment googleMapFragment =
                (SupportMapFragment) getChildFragmentManager()
                        .findFragmentById(mapFrame.getId());

        if (googleMapFragment == null) {

            googleMapFragment =
                    SupportMapFragment.newInstance();

            getChildFragmentManager()
                    .beginTransaction()
                    .replace(
                            mapFrame.getId(),
                            googleMapFragment
                    )
                    .commitNow();
        }

        googleMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        googleMap = map;

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        googleMap.getUiSettings()
                .setCompassEnabled(true);

        googleMap.getUiSettings()
                .setZoomControlsEnabled(false);

        googleMap.getUiSettings()
                .setMapToolbarEnabled(false);

        googleMap.getUiSettings()
                .setZoomGesturesEnabled(true);

        googleMap.getUiSettings()
                .setScrollGesturesEnabled(true);

        googleMap.getUiSettings()
                .setRotateGesturesEnabled(true);

        googleMap.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(92)
        );

        googleMap.setOnMarkerClickListener(marker -> {
            Object associatedEvent = marker.getTag();

            if (!(associatedEvent instanceof TrafficEvent)) {
                return false;
            }

            TrafficEvent event = (TrafficEvent) associatedEvent;
            selectedEvent = event;
            selectedMarker = marker;

            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLng(marker.getPosition())
            );

            if (eventSelectedListener != null) {
                eventSelectedListener.onMapEventSelected(event);
            } else {

                showMarkerBottomSheet(event);
            }
            return true;
        });

        LatLng scotland =
                new LatLng(56.4907, -4.2026);

        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        scotland,
                        6.5f
                )
        );

        displayTrafficMarkers();

        zoomToSelectedEvent();
    }

    public void setEvents(
            ArrayList<TrafficEvent> events
    ) {

        trafficEvents.clear();

        if (events != null) {
            trafficEvents.addAll(events);
        }

        displayTrafficMarkers();
    }

    public void focusOnEvent(TrafficEvent event) {

        selectedEvent = event;

        zoomToSelectedEvent();
    }

    private void zoomToSelectedEvent() {

        if (googleMap == null || selectedEvent == null) {
            return;
        }

        LatLng location =
                getEventLocation(selectedEvent);

        if (location == null) {
            return;
        }

        googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        location,
                        14.0f
                )
        );

        if (selectedMarker != null) {
            selectedMarker.showInfoWindow();
        }
    }

    private void displayTrafficMarkers() {

        if (googleMap == null) {
            return;
        }

        googleMap.clear();

        selectedMarker = null;

        for (TrafficEvent event : trafficEvents) {

            LatLng location =
                    getEventLocation(event);

            if (location == null) {
                continue;
            }

            Marker marker = googleMap.addMarker(
                    new MarkerOptions()
                            .position(location)
                            .title(getRoadName(event))
                            .snippet(getDescription(event))
                            .icon(
                                    BitmapDescriptorFactory
                                            .defaultMarker(
                                                    getMarkerColour(
                                                            event.getSeverity()
                                                    )
                                            )
                            )
            );

            if (marker != null) {
                marker.setTag(event);
            }

            if (isSameEvent(event, selectedEvent)) {
                selectedMarker = marker;
                selectedEvent = event;
            }
        }
    }

    private void showMarkerBottomSheet(TrafficEvent event) {

        if (!isAdded() || event == null) {
            return;
        }

        Dialog sheet = new Dialog(requireContext());
        sheet.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View content = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottom_sheet_event_details,
                null,
                false
        );

        TextView category = content.findViewById(R.id.detailsCategory);
        TextView road = content.findViewById(R.id.detailsRoadName);
        TextView description = content.findViewById(R.id.detailsDescription);
        TextView severity = content.findViewById(R.id.detailsSeverity);
        TextView type = content.findViewById(R.id.detailsEventType);
        TextView start = content.findViewById(R.id.detailsStart);
        TextView end = content.findViewById(R.id.detailsEnd);
        TextView locationToggle =
                content.findViewById(R.id.detailsLocationToggle);
        TextView coordinates =
                content.findViewById(R.id.detailsCoordinates);
        TextView mapAction = content.findViewById(R.id.detailsShowOnMap);

        category.setText("MAP EVENT");
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
            locationToggle.setText(
                    expanding ? "Location details  ▴" : "Location details  ▾"
            );
        });

        mapAction.setText("Centre on Map");
        mapAction.setOnClickListener(v -> {
            sheet.dismiss();

            LatLng position = getEventLocation(event);
            if (googleMap != null && position != null) {
                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(position, 14f)
                );
            }
        });

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
                            0f, motionEvent.getRawY() - startingY
                    );
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
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = 0.35f;
            window.setAttributes(params);
        }

        content.setTranslationY(dp(320));
        content.animate().translationY(0f).setDuration(220).start();
    }

    private String showValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Not provided";
        }
        return value;
    }

    private String showCoordinate(Double value) {
        return value == null ? "Not provided" : value.toString();
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
                "d MMM yyyy, HH:mm", Locale.UK
        );

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

    private boolean isSameEvent(
            TrafficEvent first,
            TrafficEvent second
    ) {

        if (first == null || second == null) {
            return false;
        }

        if (first == second) {
            return true;
        }

        String firstId = first.getId();
        String secondId = second.getId();

        return firstId != null
                && secondId != null
                && firstId.equals(secondId);
    }

    private LatLng getEventLocation(TrafficEvent event) {

        if (event == null) {
            return null;
        }

        Double latitude = event.getLatitude();
        Double longitude = event.getLongitude();

        if (!isValidCoordinatePair(latitude, longitude)) {

            latitude = event.getFromLatitude();
            longitude = event.getFromLongitude();
        }

        if (!isValidCoordinatePair(latitude, longitude)) {

            latitude = event.getToLatitude();
            longitude = event.getToLongitude();
        }

        if (!isValidCoordinatePair(latitude, longitude)) {
            return null;
        }

        return new LatLng(latitude, longitude);
    }

    private boolean isValidCoordinatePair(
            Double latitude,
            Double longitude
    ) {

        return latitude != null
                && longitude != null
                && !Double.isNaN(latitude)
                && !Double.isNaN(longitude)
                && !Double.isInfinite(latitude)
                && !Double.isInfinite(longitude)
                && latitude >= -90
                && latitude <= 90
                && longitude >= -180
                && longitude <= 180;
    }

    private String getRoadName(TrafficEvent event) {

        String roadName = event.getRoadName();

        if (roadName == null
                || roadName.trim().isEmpty()) {

            return "Traffic event";
        }

        return roadName;
    }

    private String getDescription(TrafficEvent event) {

        String description = event.getDescription();

        if (description == null
                || description.trim().isEmpty()) {

            return "No description available";
        }

        if (description.length() > 120) {

            description =
                    description.substring(0, 120) + "...";
        }

        return description;
    }

    private float getMarkerColour(String severity) {

        if (severity == null
                || severity.trim().isEmpty()) {

            return BitmapDescriptorFactory.HUE_AZURE;
        }

        String value =
                severity.toLowerCase(Locale.UK)
                        .replace("_", "")
                        .replace("-", "")
                        .replace(" ", "");

        if (value.equals("nodelays")
                || value.equals("nodelay")) {

            return BitmapDescriptorFactory.HUE_GREEN;
        }

        if (value.equals("unknown")
                || value.equals("undefined")) {

            return BitmapDescriptorFactory.HUE_AZURE;
        }

        if (value.contains("severe")
                || value.contains("major")
                || value.contains("longdelay")) {

            return BitmapDescriptorFactory.HUE_RED;
        }

        if (value.contains("delay")) {

            return BitmapDescriptorFactory.HUE_ORANGE;
        }

        return BitmapDescriptorFactory.HUE_AZURE;
    }

    private View createTrafficLegend() {

        LinearLayout legend =
                new LinearLayout(requireContext());

        legend.setOrientation(LinearLayout.VERTICAL);

        legend.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(14));
        background.setStroke(
                dp(1),
                Color.rgb(226, 232, 240)
        );

        legend.setBackground(background);
        legend.setElevation(dp(4));

        TextView heading =
                new TextView(requireContext());

        heading.setText("TRAFFIC CONDITIONS");
        heading.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                10
        );
        heading.setTextColor(
                Color.rgb(20, 43, 73)
        );
        heading.setTypeface(
                null,
                Typeface.BOLD
        );

        legend.addView(heading);

        LinearLayout firstRow =
                new LinearLayout(requireContext());

        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        firstRow.setPadding(0, dp(7), 0, 0);

        addLegendItem(
                firstRow,
                "● Long / major",
                Color.rgb(190, 35, 35)
        );

        addLegendItem(
                firstRow,
                "● Short / other",
                Color.rgb(220, 125, 10)
        );

        legend.addView(firstRow);

        LinearLayout secondRow =
                new LinearLayout(requireContext());

        secondRow.setOrientation(LinearLayout.HORIZONTAL);
        secondRow.setPadding(0, dp(4), 0, 0);

        addLegendItem(
                secondRow,
                "● No delay",
                Color.rgb(0, 130, 60)
        );

        addLegendItem(
                secondRow,
                "● Unknown",
                Color.rgb(36, 131, 197)
        );

        legend.addView(secondRow);

        return legend;
    }

    private void addLegendItem(
            LinearLayout parent,
            String text,
            int colour
    ) {

        TextView item =
                new TextView(requireContext());

        item.setText(text);

        item.setTextColor(colour);

        item.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                11
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        dp(112),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        parent.addView(item, params);
    }

    private int dp(float value) {

        return (int) (
                value * getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }
}
