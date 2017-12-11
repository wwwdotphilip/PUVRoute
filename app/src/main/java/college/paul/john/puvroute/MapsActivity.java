package college.paul.john.puvroute;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private ArrayList<LatLng> markerPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        markerPoints = new ArrayList<>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize local storage.
        SharedPrefs.init(this);
    }

    /*
     Update google maps direction based on Route points.
     */
    private void updateMapRoute(Route route) {
        mMap.clear();
        LatLng[] latLng = new LatLng[route.points.points.length];
        for (int i = 0; i < route.points.points.length; i++){
            latLng[i] = new LatLng(route.points.points[i][0], route.points.points[i][1]);
        }
        mMap.addPolyline(new PolylineOptions()
                .add(latLng)
                .width(5)
                .color(Color.RED));
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
         /*
          Call permission dialog for user to allow or dennie.
          Note that you will not be able to identify your current location if you dennie
          this permission.
         */
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                1);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setOnMarkerClickListener(new MarkerClickListener());
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                markerPoints.add(latLng);
                redrawMap();
            }
        });

        // Listen to any changes happening to MapRoutes
        MapRoutes.setRouteListener(new MapRoutes.RouteListener() {
            @Override
            public void loadComplete() {
                // Do something here once map route initialization is complete.
            }

            @Override
            public void onError(String error) {
                // Do something here if MapRoutes produce any error.
            }

            @Override
            public void onChange(Route route) {
                updateMapRoute(route);
            }
        });
        // Load map routes from server or local file.
        MapRoutes.loadRoutes();
        getLastLocation();
    }

    // Redraw the entire map. All old data are replaced.
    private void redrawMap() {
        mMap.clear();
        LatLng[] point = new LatLng[markerPoints.size()];
        for (int i = 0; i < markerPoints.size(); i++){
            point[i] = markerPoints.get(i);
            MarkerOptions options = new MarkerOptions();
            options.title("Point "+(i+1));
            options.position(point[i]);
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            mMap.addMarker(options).showInfoWindow();
        }

        if (markerPoints.size() > 1){
            mMap.addPolyline(new PolylineOptions()
                    .add(point)
                    .width(5)
                    .color(Color.RED));
        }
    }

    private void getLastLocation() {
         /*
          Call permission dialog for user to allow or dennie.
          Note that you will not be able to identify your current location if you dennie
          this permission.
         */
        ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                1);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Check for last know location
        mFusedLocationProviderClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if(task.isSuccessful()){
                            CameraPosition oldPos = mMap.getCameraPosition();
                            CameraPosition pos = CameraPosition.builder(oldPos).bearing(16).build();
                            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
                        }
                    }
                });
    }

    private class MarkerClickListener implements GoogleMap.OnMarkerClickListener{

        // This gets called every time a marker is pressed.
        @Override
        public boolean onMarkerClick(final Marker marker) {
            double latitude = marker.getPosition().latitude;
            double longitude = marker.getPosition().longitude;

            // Loop through the entire list.
            for (int i = 0; i < markerPoints.size(); i++){

                // Check if latitude and longitude are similar.
                if (latitude == markerPoints.get(i).latitude && longitude == markerPoints.get(i).longitude){
                    // Show dialog to confirm deletion.
                    final int finalI = i;
                    final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                    builder.setTitle("Delete");
                    builder.setMessage("Are you sure you want to delete " + marker.getTitle());
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            markerPoints.remove(finalI);
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
}
