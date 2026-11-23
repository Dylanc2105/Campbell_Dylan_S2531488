package com.example.campbell_dylan_s2531488;

import android.util.Base64;
import android.util.Log;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrafficRepository {

    private static final Pattern ROAD_PATTERN = Pattern.compile(
            "\\b(?:M|A|B)\\d{1,4}(?:\\(M\\))?\\b",
            Pattern.CASE_INSENSITIVE
    );

    public ArrayList<TrafficEvent> downloadAndParse(
            String feedUrl, String clientId, String clientSecret
    ) throws IOException, XmlPullParserException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(feedUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);

            String credentials = clientId + ":" + clientSecret;
            String encoded = Base64.encodeToString(
                    credentials.getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
            );
            connection.setRequestProperty("Authorization", "Basic " + encoded);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned HTTP " + responseCode);
            }

            StringBuilder downloadedText = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    downloadedText.append(line).append('\n');
                }
            }

            String xmlData = downloadedText.toString();
            return parseTrafficEvents(xmlData);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private ArrayList<TrafficEvent> parseTrafficEvents(
            String xmlData
    ) throws XmlPullParserException, IOException {

        ArrayList<TrafficEvent> events =
                new ArrayList<>();

        XmlPullParserFactory factory =
                XmlPullParserFactory.newInstance();

        factory.setNamespaceAware(true);

        XmlPullParser parser =
                factory.newPullParser();

        parser.setInput(
                new StringReader(xmlData)
        );

        TrafficEvent currentEvent = null;

        int recordDepth = -1;
        int commentDepth = -1;
        int validityDepth = -1;

        ArrayList<String> tagPath =
                new ArrayList<>();

        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {

                String tagName = parser.getName();

                tagPath.add(tagName);

                if (tagName.equals("situationRecord")) {

                    currentEvent = new TrafficEvent();

                    recordDepth = parser.getDepth();

                    currentEvent.setId(
                            parser.getAttributeValue(null, "id")
                    );

                    String type = null;

                    for (int i = 0;
                         i < parser.getAttributeCount();
                         i++) {

                        if (parser.getAttributeName(i)
                                .equals("type")) {

                            type = parser.getAttributeValue(i);
                            break;
                        }
                    }

                    currentEvent.setEventType(type);

                } else if (currentEvent != null) {

                    if (tagName.equals("generalPublicComment")) {
                        commentDepth = parser.getDepth();
                    }

                    if (tagName.equals("validity")) {
                        validityDepth = parser.getDepth();
                    }
                }

            } else if (eventType == XmlPullParser.TEXT) {

                if (currentEvent != null
                        && !tagPath.isEmpty()) {

                    String value = parser.getText().trim();

                    String currentTag =
                            tagPath.get(tagPath.size() - 1);

                    if (!value.isEmpty()) {

                        if (currentTag.equals("roadNumber")
                                || currentTag.equals("roadName")) {

                            if (currentEvent.getRoadName() == null) {
                                currentEvent.setRoadName(value);
                            }

                        } else if (
                                currentTag.equals("value")
                                        && commentDepth >= 0
                                        && parser.getDepth()
                                        > commentDepth
                        ) {

                            String oldDescription =
                                    currentEvent.getDescription();

                            if (oldDescription == null
                                    || oldDescription.isEmpty()) {

                                currentEvent.setDescription(value);
                            }

                        } else if (
                                currentTag.equals(
                                        "situationRecordCreationTime"
                                )
                        ) {

                            if (currentEvent.getStartTime() == null) {
                                currentEvent.setStartTime(value);
                            }

                        } else if (
                                currentTag.equals("overallStartTime")
                                        && validityDepth >= 0
                        ) {

                            currentEvent.setStartTime(value);

                        } else if (
                                currentTag.equals("overallEndTime")
                                        && validityDepth >= 0
                        ) {

                            currentEvent.setEndTime(value);

                        } else if (
                                currentTag.equals("delaysType")
                        ) {

                            currentEvent.setSeverity(value);

                        } else if (
                                currentTag.equals("latitude")
                                        || currentTag.equals("longitude")
                        ) {

                            saveCoordinate(
                                    currentEvent,
                                    tagPath,
                                    currentTag,
                                    value
                            );
                        }
                    }
                }

            } else if (eventType == XmlPullParser.END_TAG) {

                String tagName = parser.getName();

                if (tagName.equals("generalPublicComment")) {
                    commentDepth = -1;
                }

                if (tagName.equals("validity")) {
                    validityDepth = -1;
                }

                if (tagName.equals("situationRecord")
                        && currentEvent != null
                        && parser.getDepth() == recordDepth) {

                    if (currentEvent.getRoadName() == null
                            || currentEvent.getRoadName()
                            .trim().isEmpty()) {

                        String roadName =
                                extractRoadFromDescription(
                                        currentEvent.getDescription()
                                );

                        if (roadName != null) {
                            currentEvent.setRoadName(roadName);
                        }
                    }

                    events.add(currentEvent);

                    currentEvent = null;
                    recordDepth = -1;
                    commentDepth = -1;
                    validityDepth = -1;
                }

                if (!tagPath.isEmpty()) {
                    tagPath.remove(tagPath.size() - 1);
                }
            }

            eventType = parser.next();
        }

        return events;
    }

    private String extractRoadFromDescription(
            String description
    ) {

        if (description == null
                || description.trim().isEmpty()) {

            return null;
        }

        Matcher matcher =
                ROAD_PATTERN.matcher(description);

        if (matcher.find()) {
            return matcher.group().toUpperCase(Locale.UK);
        }

        return null;
    }

    private void saveCoordinate(
            TrafficEvent event,
            ArrayList<String> tagPath,
            String coordinateTag,
            String value
    ) {

        boolean insideLocation =
                tagPath.contains("groupOfLocations")
                        || tagPath.contains("locationContainedInGroup")
                        || tagPath.contains("pointCoordinates")
                        || tagPath.contains("pointByCoordinates")
                        || tagPath.contains("tpegLinearLocation");

        if (!insideLocation) {
            return;
        }

        try {

            double coordinate =
                    Double.parseDouble(value);

            if (coordinateTag.equals("latitude")
                    && (coordinate < -90 || coordinate > 90)) {
                return;
            }

            if (coordinateTag.equals("longitude")
                    && (coordinate < -180 || coordinate > 180)) {
                return;
            }

            boolean isFrom = tagPath.contains("from");
            boolean isTo = tagPath.contains("to");

            if (isFrom && !isTo) {

                if (coordinateTag.equals("latitude")) {
                    event.setFromLatitude(coordinate);
                } else {
                    event.setFromLongitude(coordinate);
                }

            } else if (isTo && !isFrom) {

                if (coordinateTag.equals("latitude")) {
                    event.setToLatitude(coordinate);
                } else {
                    event.setToLongitude(coordinate);
                }

            } else {

                if (coordinateTag.equals("latitude")
                        && event.getLatitude() == null) {

                    event.setLatitude(coordinate);

                } else if (
                        coordinateTag.equals("longitude")
                                && event.getLongitude() == null
                ) {

                    event.setLongitude(coordinate);
                }
            }

        } catch (NumberFormatException e) {

            Log.w(
                    "TrafficScotland",
                    "Invalid coordinate: " + value
            );
        }
    }

}
