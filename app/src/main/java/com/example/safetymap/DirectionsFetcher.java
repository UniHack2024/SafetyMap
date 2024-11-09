package com.example.safetymap;

import android.graphics.Color;
import android.os.AsyncTask;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DirectionsFetcher extends AsyncTask<LatLng, String, String> {
    private GoogleMap mMap;
    private LatLng source;
    private LatLng destination;
    private ArrayList<LatLng> waypoints;

    public DirectionsFetcher(GoogleMap mMap, LatLng source, LatLng destination, ArrayList<LatLng> waypoints) {
        this.mMap = mMap;
        this.source = source;
        this.destination = destination;
        this.waypoints = waypoints;
    }

    @Override
    protected String doInBackground(LatLng... latLngs) {
        String url = makeURL(source, destination);
        return getJSONFromURL(url);
    }

    private String makeURL(LatLng source, LatLng destination) {
        StringBuilder urlString = new StringBuilder();
        urlString.append("https://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=").append(source.latitude).append(",").append(source.longitude);
        urlString.append("&destination=").append(destination.latitude).append(",").append(destination.longitude);
        urlString.append("&sensor=false&mode=walking&alternatives=false&region=us");
        urlString.append("&key=YOUR_GOOGLE_MAPS_API_KEY");
        return urlString.toString();
    }

    private String getJSONFromURL(String urlString) {
        StringBuilder json = new StringBuilder();
        try {
            URL url = new URL(urlString);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            try (InputStream in = new BufferedInputStream(httpURLConnection.getInputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line).append('\n');
                }
            } finally {
                httpURLConnection.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    protected void drawPath(String result) {
        try {
            JSONObject json = new JSONObject(result);
            JSONArray routeArray = json.getJSONArray("routes");
            JSONObject routes = routeArray.getJSONObject(0);
            JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
            String encodedString = overviewPolylines.getString("points");
            List<LatLng> list = decodePoly(encodedString);
            mMap.addPolyline(new PolylineOptions().addAll(list).width(12).color(Color.parseColor("#05b1fb")).geodesic(true));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

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
}
