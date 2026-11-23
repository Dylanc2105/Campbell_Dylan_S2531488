package com.example.campbell_dylan_s2531488;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Locale;

public class EventListFragment extends Fragment {

    private TextView listHeading;
    private TextView listStatus;
    private ListView eventListView;
    private EditText roadSearchInput;
    private TextView rangeButton;
    private TrafficViewModel trafficViewModel;

    private final ArrayList<TrafficEvent> trafficEvents =
            new ArrayList<>();

    private final ArrayList<TrafficEvent> filteredEvents =
            new ArrayList<>();

    private String currentTitle = "Traffic Events";

    private OnEventSelectedListener listener;

    private static final double REFERENCE_LATITUDE = 55.7644;
    private static final double REFERENCE_LONGITUDE = -4.1770;

    private Double selectedDistanceKm = null;

    private static final String STATE_DISTANCE =
            "selectedDistanceKm";

    public interface OnEventSelectedListener {
        void onEventSelected(TrafficEvent event);
    }

    public EventListFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(
                R.layout.fragment_event_list,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        listHeading = view.findViewById(R.id.listHeading);
        listStatus = view.findViewById(R.id.listStatus);
        eventListView = view.findViewById(R.id.eventListView);
        roadSearchInput = view.findViewById(R.id.roadSearchInput);
        rangeButton = view.findViewById(R.id.rangeButton);
        trafficViewModel = new ViewModelProvider(requireActivity())
                .get(TrafficViewModel.class);

        if (savedInstanceState != null) {

            double savedDistance =
                    savedInstanceState.getDouble(
                            STATE_DISTANCE,
                            -1.0
                    );

            selectedDistanceKm =
                    savedDistance > 0 ? savedDistance : null;
        }

        roadSearchInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {

                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        trafficViewModel.setSearchText(s.toString());
                        filterEvents(s.toString());
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                }
        );

        rangeButton.setOnClickListener(
                v -> showDistanceOptions()
        );

        listStatus.setClickable(false);

        updateRangeButtonText();
        roadSearchInput.setText(trafficViewModel.getSearchText());
        filterEvents(trafficViewModel.getSearchText());
    }

    @Override
    public void onSaveInstanceState(
            @NonNull Bundle outState
    ) {
        outState.putDouble(
                STATE_DISTANCE,
                selectedDistanceKm == null
                        ? -1.0
                        : selectedDistanceKm
        );

        super.onSaveInstanceState(outState);
    }

    public void setEvents(
            ArrayList<TrafficEvent> events,
            String title
    ) {
        trafficEvents.clear();

        if (events != null) {
            trafficEvents.addAll(events);
        }

        currentTitle = title;

        if (getView() != null) {

            String search = trafficViewModel.getSearchText();
            if (!roadSearchInput.getText().toString().equals(search)) {
                roadSearchInput.setText(search);
            }
            filterEvents(search);
        }
    }

    public boolean hasActiveFilter() {
        return selectedDistanceKm != null
                || (roadSearchInput != null
                && !roadSearchInput.getText().toString().trim().isEmpty());
    }

    public ArrayList<TrafficEvent> getFilteredEvents() {
        return new ArrayList<>(filteredEvents);
    }

    public void setOnEventSelectedListener(
            OnEventSelectedListener listener
    ) {
        this.listener = listener;
    }

    private void updateRangeButtonText() {

        if (rangeButton == null) {
            return;
        }

        if (selectedDistanceKm == null) {

            rangeButton.setText("Range");

            rangeButton.setContentDescription(
                    "Filter by distance. No range selected."
            );

        } else {

            String rangeText =
                    selectedDistanceKm.intValue() + " km";

            rangeButton.setText(rangeText);

            rangeButton.setContentDescription(
                    "Distance filter: " + rangeText
            );
        }
    }

    private void showDistanceOptions() {

        String[] options = {
                "All Events",
                "Within 5 km",
                "Within 10 km",
                "Within 25 km",
                "Within 50 km"
        };

        int selectedOption = 0;

        if (selectedDistanceKm != null) {

            if (selectedDistanceKm == 5.0) {
                selectedOption = 1;

            } else if (selectedDistanceKm == 10.0) {
                selectedOption = 2;

            } else if (selectedDistanceKm == 25.0) {
                selectedOption = 3;

            } else if (selectedDistanceKm == 50.0) {
                selectedOption = 4;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Nearby traffic - East Kilbride")
                .setSingleChoiceItems(
                        options,
                        selectedOption,
                        (dialog, which) -> {

                            switch (which) {

                                case 1:
                                    selectedDistanceKm = 5.0;
                                    break;

                                case 2:
                                    selectedDistanceKm = 10.0;
                                    break;

                                case 3:
                                    selectedDistanceKm = 25.0;
                                    break;

                                case 4:
                                    selectedDistanceKm = 50.0;
                                    break;

                                default:
                                    selectedDistanceKm = null;
                                    break;
                            }

                            updateRangeButtonText();

                            filterEvents(
                                    roadSearchInput.getText().toString()
                            );

                            dialog.dismiss();
                        }
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterEvents(String searchText) {

        filteredEvents.clear();

        String query =
                searchText.trim().toUpperCase(Locale.UK);

        for (TrafficEvent event : trafficEvents) {

            String roadName = event.getRoadName();

            if (roadName == null) {
                roadName = "";
            }

            boolean matchesRoad =
                    query.isEmpty()
                            || roadName.toUpperCase(Locale.UK)
                            .contains(query);

            if (!matchesRoad) {
                continue;
            }

            if (selectedDistanceKm == null) {

                filteredEvents.add(event);
                continue;
            }

            Double distance = event.getDistanceKmFrom(
                    REFERENCE_LATITUDE,
                    REFERENCE_LONGITUDE
            );

            if (distance != null
                    && distance <= selectedDistanceKm) {

                filteredEvents.add(event);
            }
        }

        displayEvents();
    }

    private void displayEvents() {

        if (getView() == null) {
            return;
        }

        listHeading.setText(currentTitle);

        String status =
                "Showing " + filteredEvents.size()
                        + " of " + trafficEvents.size()
                        + " events";

        if (filteredEvents.isEmpty()
                && !trafficEvents.isEmpty()) {

            status += "\nNo matching traffic events found.";
        }

        if (hasActiveFilter()) {
            status += "\nView Map shows matching events";
        }
        listStatus.setText(status);

        updateRangeButtonText();

        eventListView.setDivider(null);
        eventListView.setDividerHeight(0);

        ArrayAdapter<TrafficEvent> adapter =
                new ArrayAdapter<TrafficEvent>(
                        requireContext(),
                        R.layout.item_traffic_event,
                        filteredEvents
                ) {

                    @NonNull
                    @Override
                    public View getView(
                            int position,
                            @Nullable View convertView,
                            @NonNull ViewGroup parent
                    ) {

                        View row = convertView;

                        if (row == null) {

                            row = LayoutInflater.from(
                                    requireContext()
                            ).inflate(
                                    R.layout.item_traffic_event,
                                    parent,
                                    false
                            );
                        }

                        TrafficEvent event =
                                getItem(position);

                        if (event == null) {
                            return row;
                        }

                        View eventCard =
                                row.findViewById(R.id.eventCard);

                        ImageView eventIcon =
                                row.findViewById(R.id.eventIcon);

                        TextView eventRoadName =
                                row.findViewById(R.id.eventRoadName);

                        TextView eventDistance =
                                row.findViewById(R.id.eventDistance);

                        TextView eventDescription =
                                row.findViewById(R.id.eventDescription);

                        TextView eventSubDescription =
                                row.findViewById(
                                        R.id.eventSubDescription
                                );

                        TextView eventSeverity =
                                row.findViewById(R.id.eventSeverity);

                        TextView eventDetailsLink =
                                row.findViewById(
                                        R.id.eventDetailsLink
                                );

                        eventIcon.setImageResource(
                                getEventIcon(event)
                        );

                        String roadName = event.getRoadName();

                        if (roadName == null
                                || roadName.trim().isEmpty()) {

                            roadName = "Road not specified";
                        }

                        eventRoadName.setText(roadName);

                        Double distance = event.getDistanceKmFrom(
                                REFERENCE_LATITUDE,
                                REFERENCE_LONGITUDE
                        );

                        if (distance != null) {

                            eventDistance.setText(
                                    String.format(
                                            Locale.UK,
                                            "%.1f km",
                                            distance
                                    )
                            );

                            eventDistance.setVisibility(
                                    View.VISIBLE
                            );

                        } else {

                            eventDistance.setText("");
                            eventDistance.setVisibility(
                                    View.GONE
                            );
                        }

                        String description =
                                event.getDescription();

                        if (description == null
                                || description.trim().isEmpty()) {

                            description =
                                    "No description available";
                        }

                        int sentenceBreak =
                                description.indexOf(". ");

                        if (sentenceBreak > 0
                                && sentenceBreak
                                < description.length() - 2) {

                            String headline =
                                    description.substring(
                                            0,
                                            sentenceBreak + 1
                                    );

                            String extraInformation =
                                    description.substring(
                                            sentenceBreak + 2
                                    ).trim();

                            eventDescription.setText(headline);

                            eventSubDescription.setText(
                                    extraInformation
                            );

                            eventSubDescription.setVisibility(
                                    View.VISIBLE
                            );

                        } else {

                            eventDescription.setText(
                                    description
                            );

                            eventSubDescription.setText("");

                            eventSubDescription.setVisibility(
                                    View.GONE
                            );
                        }

                        String delayLabel =
                                getDelayLabel(
                                        event.getSeverity()
                                );

                        if (delayLabel.startsWith("● ")) {

                            delayLabel =
                                    delayLabel.substring(2);
                        }

                        eventSeverity.setText(delayLabel);

                        eventSeverity.setTextColor(
                                getDelayColour(
                                        event.getSeverity()
                                )
                        );

                        eventCard.setOnClickListener(
                                v -> openEventDetails(event)
                        );

                        eventDetailsLink.setOnClickListener(
                                v -> openEventDetails(event)
                        );

                        eventCard.setContentDescription(
                                roadName
                                        + ". "
                                        + description
                                        + ". View details"
                        );

                        return row;
                    }
                };

        eventListView.setAdapter(adapter);

        eventListView.setOnItemClickListener(
                (parent, selectedView, position, id) -> {

                    if (position >= 0
                            && position < filteredEvents.size()) {

                        openEventDetails(
                                filteredEvents.get(position)
                        );
                    }
                }
        );
    }

    private void openEventDetails(TrafficEvent event) {

        if (listener != null && event != null) {

            listener.onEventSelected(event);
        }
    }

    private int getEventIcon(TrafficEvent event) {

        String type = event.getEventType() == null
                ? ""
                : event.getEventType();

        String description = event.getDescription() == null
                ? ""
                : event.getDescription();

        String text = (type + " " + description)
                .toLowerCase(Locale.UK)
                .replaceAll("[^a-z0-9]", "");

        if (text.contains("accident")
                || text.contains("collision")
                || text.contains("crash")) {

            return R.drawable.ic_event_accident;
        }

        if (text.contains("laneclosure")
                || text.contains("laneclosed")
                || text.contains("lanesclosed")
                || text.contains("lanerestriction")
                || text.contains("contraflow")) {

            return R.drawable.ic_event_lane_restriction;
        }

        if (text.contains("roadclosed")
                || text.contains("roadclosure")
                || text.contains("carriagewayclosed")
                || text.contains("carriagewayclosure")
                || text.contains("fullclosure")) {

            return R.drawable.ic_event_road_closure;
        }

        if (text.contains("roadwork")
                || text.contains("resurfacing")
                || text.contains("maintenance")
                || text.contains("construction")) {

            return R.drawable.ic_event_roadworks;
        }

        if (text.contains("congestion")
                || text.contains("queuing")
                || text.contains("queueing")
                || text.contains("slowtraffic")
                || text.contains("heavytraffic")) {

            return R.drawable.ic_event_congestion;
        }

        if (currentTitle != null
                && currentTitle.toLowerCase(Locale.UK)
                .replaceAll("[^a-z]", "")
                .contains("roadwork")) {

            return R.drawable.ic_event_roadworks;
        }

        return R.drawable.ic_event_warning;
    }

    private String getDelayLabel(String severity) {

        if (severity == null || severity.trim().isEmpty()) {
            return "● Delay information unavailable";
        }

        String value = severity.toLowerCase(Locale.UK)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");

        if (value.equals("nodelays")
                || value.equals("nodelay")) {

            return "● No delay reported";
        }

        if (value.equals("unknown")
                || value.equals("undefined")) {

            return "● Delay information unknown";
        }

        if (value.contains("longdelay")) {
            return "● Long delay reported";
        }

        if (value.contains("shortdelay") || value.contains("minordelay")) {
            return "● Short delay reported";
        }

        if (value.contains("severe") || value.contains("major")) {
            return "● Major delay reported";
        }

        if (value.contains("delay")) {
            return "● Delay reported";
        }

        return "● Delay information: " + severity;
    }

    private int getDelayColour(String severity) {

        if (severity == null || severity.trim().isEmpty()) {
            return Color.GRAY;
        }

        String value = severity.toLowerCase(Locale.UK)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");

        if (value.equals("nodelays")
                || value.equals("nodelay")) {

            return Color.rgb(0, 130, 60);
        }

        if (value.equals("unknown")
                || value.equals("undefined")) {

            return Color.GRAY;
        }

        if (value.contains("longdelay")
                || value.contains("severe")
                || value.contains("major")) {

            return Color.rgb(190, 35, 35);
        }

        if (value.contains("delay")) {

            return Color.rgb(180, 105, 0);
        }

        return Color.GRAY;
    }
}
