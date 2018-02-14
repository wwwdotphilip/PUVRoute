package college.paul.john.puvroute.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.ButtCap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

import college.paul.john.puvroute.Icon;
import college.paul.john.puvroute.model.Mode;
import college.paul.john.puvroute.model.Route;

/*
    This class handles all Google map view activities
    Code for manipulating google maps should be done here.
 */
public class Map {
    private static final String TAG = "Map";
    private static volatile Map instance;
    private GoogleMap mMap;
    private ArrayList<LatLng> markerPoints;
    private int currentMode = 0;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private OnMapListener mMapListener;
    private Location currentLocation;

    public interface OnMapListener{
        void onChangeMode(int mode);
    }

    // Always call this method before calling other methods to make sure that map is working properly.
    public static void init(Context context, GoogleMap map) {
        getInstance().mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        getInstance().markerPoints = new ArrayList<>();
        getInstance().mMap = map;
        getInstance().mMap.setOnMarkerClickListener(new MarkerClickListener(context));
        getLastLocation(context);

         /*
          Call permission dialog for user to allow or dennie.
          Note that you will not be able to identify your current location if you dennie
          this permission.
         */
        ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                1);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        getInstance().mMap.setMyLocationEnabled(true);
        getInstance().mMap.getUiSettings().setZoomControlsEnabled(true);
        getInstance().mMap.getUiSettings().setCompassEnabled(true);
        getInstance().mMap.getUiSettings().setRotateGesturesEnabled(true);
        clearMap();

        getInstance().mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (getInstance().currentMode == Mode.MAP_MAKER){
                    getInstance().markerPoints.add(latLng);
                    redrawMap();
                }
            }
        });
    }

    static Map getInstance() {
        synchronized (Map.class) {
            if (instance == null) {
                instance = new Map();
            }
        }
        return instance;
    }

    // This doesn't do anything much but this is useful when you have UI that
    // changes behavior when the map changes mode.
    public static void setMode(int mode) {
        getInstance().currentMode = mode;
        if (getInstance().mMapListener != null){
            getInstance().mMapListener.onChangeMode(mode);
        }
    }

    // Get current map mode.
    public static int getMode(){
        return getInstance().currentMode;
    }

    // Update your map and display your current location.
    private static void getLastLocation(final Context context) {
        if (getInstance().mFusedLocationProviderClient != null) {
         /*
          Call permission dialog for user to allow or dennie.
          Note that you will not be able to identify your current location if you dennie
          this permission.
         */
            ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (getInstance().mMap != null) {
                // Check for last know location
                getInstance().mFusedLocationProviderClient.getLastLocation()
                        .addOnCompleteListener((Activity) context, new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                try {
                                    if (task.isSuccessful()) {
                                        getInstance().currentLocation = task.getResult();
                                        moveCamera(new LatLng(task.getResult().getLatitude(), task.getResult().getLongitude()));
                                    }
                                } catch (Exception e) {
                                    Activity activity = (Activity) context;
                                    Snackbar.make(activity.getWindow().getDecorView().findViewById(android.R.id.content),
                                            "Cannot find your location. Enable Location and Internet service", Snackbar.LENGTH_LONG).show();
                                }
                            }
                        });
            } else {
                Log.e(TAG, "Map has not been initialize. Call Map.setMap(GoogleMap)");
            }
        }
    }

    static void moveCamera(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)   // Sets the center of the map to location user
                .zoom(17)   // Sets the zoom
                .build();
        getInstance().mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        Log.e(TAG, "Move camera.");
    }

    // Redraw the entire map. All old data are replaced.
    private static void redrawMap() {
        if (getInstance().mMap != null) {
            getInstance().mMap.clear();
            LatLng[] point = new LatLng[getInstance().markerPoints.size()];
            for (int i = 0; i < getInstance().markerPoints.size(); i++) {
                point[i] = getInstance().markerPoints.get(i);
                MarkerOptions options = new MarkerOptions();
                options.title("Point " + (i + 1));
                options.position(point[i]);
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                getInstance().mMap.addMarker(options).showInfoWindow();
            }

            if (getInstance().markerPoints.size() > 1) {
                getInstance().mMap.addPolyline(new PolylineOptions()
                        .add(point)
                        .width(10)
                        .color(Color.RED)
                        .startCap(new RoundCap())
                        .endCap(new ButtCap()));
            }
        } else {
            Log.e(TAG, "Map has not been initialize. Call Map.setMap(GoogleMap)");
        }
    }

    // Get the instance of the map
    public static GoogleMap getMap(){
        return getInstance().mMap;
    }

    // Remove all markers poly line and other object inside the map.
    public static void clearMap(){
        if (getInstance().mMap != null) {
            getInstance().mMap.clear();
        }
        if (getInstance().markerPoints != null){
            getInstance().markerPoints.clear();
        }
    }

    // Get marker points stored.
    public static ArrayList<LatLng> getMarkerPoints(){
        return getInstance().markerPoints;
    }

    // Private class that listen to to marker events.
    private static class MarkerClickListener implements GoogleMap.OnMarkerClickListener {

        Context context;

        MarkerClickListener(Context context){
            this.context = context;
        }

        // This gets called every time a marker is pressed.
        @Override
        public boolean onMarkerClick(final Marker marker) {
            double latitude = marker.getPosition().latitude;
            double longitude = marker.getPosition().longitude;

            // Loop through the entire list.
            for (int i = 0; i < getInstance().markerPoints.size(); i++) {

                // Check if latitude and longitude are similar.
                if (latitude == getInstance().markerPoints.get(i).latitude && longitude == getInstance().markerPoints.get(i).longitude) {
                    // Show dialog to confirm deletion.
                    final int finalI = i;
                    final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("Delete");
                    builder.setMessage("Are you sure you want to delete " + marker.getTitle());
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getInstance().markerPoints.remove(finalI);
                            redrawMap();
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Close dialog and show info of selected marker.
                            marker.showInfoWindow();
                        }
                    });
                    builder.create();
                    builder.show();
                    return true;
                }
            }
            return false;
        }
    }

    // Must be call in order for Activity to listen to any events involving the Map class.
    public static void setMapListener(OnMapListener mapListener){
        getInstance().mMapListener = mapListener;
    }

    // Get your current location and return as Location class.
    static Location getCurrentLocation(){
        return getInstance().currentLocation;
    }

    // Set marker on the map.
    static void setMarker(LatLng latLng, String title, BitmapDescriptor icon){
        if (icon == null){
            icon = BitmapDescriptorFactory.defaultMarker();
        }
        MarkerOptions options = new MarkerOptions();
        options.title(title);
        options.position(latLng);
        options.icon(icon);
        getInstance().mMap.addMarker(options).showInfoWindow();
    }

    /*
     Update google maps direction based on Route points.
     */
    static void redrawMap(Route route) {
        clearMap();
        LatLng[] latLng = new LatLng[route.points.points.length];
        for (int i = 0; i < route.points.points.length; i++) {
            latLng[i] = new LatLng(route.points.points[i][0], route.points.points[i][1]);
        }
        if (Mode.FREE == getInstance().currentMode){
            setMarker(latLng[latLng.length-1], "End of Route", BitmapDescriptorFactory.fromBitmap(Icon.getFinish()));
            setMarker(latLng[0], "Start of Route", BitmapDescriptorFactory.fromBitmap(Icon.getStart()));
        }
        getInstance().mMap.addPolyline(new PolylineOptions()
                .add(latLng)
                .width(10)
                .color(Color.RED));
    }

    // Move screen to you current location.
    static void focusSelf(){
        LatLng latLng = new LatLng(getCurrentLocation().getLatitude(), getCurrentLocation().getLongitude());
        moveCamera(latLng);
    }
}
