package com.example.campbell_dylan_s2531488;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;

public class TrafficViewModel extends ViewModel {

    public interface LoadListener {
        void onLoaded(String feedTitle, boolean automatic);
        void onFailed(String feedTitle, boolean automatic, String message);
    }

    private final ArrayList<TrafficEvent> trafficEvents = new ArrayList<>();
    private final TrafficRepository repository = new TrafficRepository();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String currentFeedTitle = "";
    private String searchText = "";
    private boolean dataLoaded = false;
    private boolean loading = false;
    private LoadListener loadListener;

    public void setLoadListener(LoadListener listener) {
        loadListener = listener;
    }

    public void clearLoadListener(LoadListener listener) {
        if (loadListener == listener) {
            loadListener = null;
        }
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean loadTrafficData(
            String feedUrl,
            String feedTitle,
            String clientId,
            String clientSecret,
            boolean automatic
    ) {
        if (loading) {
            return false;
        }
        loading = true;

        new Thread(() -> {
            try {
                ArrayList<TrafficEvent> events = repository.downloadAndParse(
                        feedUrl, clientId, clientSecret
                );
                mainHandler.post(() -> {
                    setTrafficEvents(events);
                    setCurrentFeedTitle(feedTitle);
                    loading = false;
                    if (loadListener != null) {
                        loadListener.onLoaded(feedTitle, automatic);
                    }
                });
            } catch (Exception error) {
                mainHandler.post(() -> {
                    loading = false;
                    if (loadListener != null) {
                        loadListener.onFailed(
                                feedTitle, automatic, error.getMessage()
                        );
                    }
                });
            }
        }).start();
        return true;
    }

    public ArrayList<TrafficEvent> getTrafficEvents() {
        return trafficEvents;
    }

    public void setTrafficEvents(ArrayList<TrafficEvent> events) {
        trafficEvents.clear();
        if (events != null) {
            trafficEvents.addAll(events);
        }
        dataLoaded = true;
    }

    public String getCurrentFeedTitle() {
        return currentFeedTitle;
    }

    public void setCurrentFeedTitle(String title) {
        currentFeedTitle = title;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String text) {
        searchText = text == null ? "" : text;
    }

    public boolean isDataLoaded() {
        return dataLoaded;
    }
}
