package college.paul.john.puvroute;

import android.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

/*
    This class handles all Google map view activities
    Code for manipulating google maps should be done here.
 */
class Map {
    private static final String TAG = "Map";
    private static volatile Map instance;
    private GoogleMap mMap;
    private ArrayList<LatLng> markerPoints;
    private int currentMode = 0;
    boolean justLoaded = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private OnMapListener mMapListener;

    public static interface OnMapListener{
        void onChangeMode(Mode mode);
    }

    static void init(Context context, GoogleMap map) {
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
        getInstance().mMap.getUiSettings().setMapToolbarEnabled(true);

        getInstance().mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                getInstance().markerPoints.add(latLng);
                redrawMap();
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

    static void setMode(int mode) {
        getInstance().currentMode = mode;
    }

    static int getMode(){
        return getInstance().currentMode;
    }

    static void getLastLocation(Context context) {
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
                                if (task.isSuccessful()) {
                                    if (getInstance().justLoaded) {
                                        getInstance().justLoaded = false;
                                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                                .target(new LatLng(task.getResult().getLatitude(),
                                                        task.getResult().getLongitude()))   // Sets the center of the map to location user
                                                .zoom(17)   // Sets the zoom
                                                .build();
                                        getInstance().mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                                    }
                                }
                            }
                        });
            } else {
                Log.e(TAG, "Map has not been initialize. Call Map.setMap(GoogleMap)");
            }
        }
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
                        .width(5)
                        .color(Color.RED));
            }
        } else {
            Log.e(TAG, "Map has not been initialize. Call Map.setMap(GoogleMap)");
        }
    }

    static GoogleMap getMap(){
        return getInstance().mMap;
    }

    static void clearMap(){
        if (getInstance().mMap != null) {
            getInstance().mMap.clear();
        }
        if (getInstance().markerPoints != null){
            getInstance().markerPoints.clear();
        }
    }

    static ArrayList<LatLng> getMarkerPoints(){
        return getInstance().markerPoints;
    }

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

    static void setMapListener(OnMapListener mapListener){
        getInstance().mMapListener = mapListener;
    }

    class Mode {
        static final int FREE = 0;
        static final int ROUTE = 1;
        static final int MAP_MAKER = 2;
    }
}