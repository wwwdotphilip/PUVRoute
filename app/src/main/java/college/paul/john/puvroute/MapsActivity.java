package college.paul.john.puvroute;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Random;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastLocation;
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
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);
        getLastLocation();

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                markerPoints.add(latLng);
                mMap.clear();
                LatLng[] point = new LatLng[markerPoints.size()];
                for (int i = 0; i < markerPoints.size(); i++){
                    point[i] = markerPoints.get(i);
                    MarkerOptions options = new MarkerOptions();
                    options.position(point[i]);
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                    mMap.addMarker(options);
                }
                mMap.addPolyline(new PolylineOptions()
                        .add(latLng)
                        .width(5)
                        .color(Color.RED));
            }
        });

        // Listen to any changes happening to MapRoutes
        MapRoutes.setRouteListener(new MapRoutes.RouteListener() {
            @Override
            public void loadComplete() {
                Log.v(TAG, "Loading map complete");
            }

            @Override
            public void onError(String error) {
                Log.v(TAG, error);
            }

            @Override
            public void onChange(Route route) {
                updateMapRoute(route);
            }
        });
        // Load map routes from server or local file.
        MapRoutes.loadRoutes();

        // TODO: 12/5/17 Remove below code.
        ArrayList<Route> routes = MapRoutes.getRouteList();
        Random random = new Random();
        MapRoutes.changeRoute(routes.get(random.nextInt(routes.size())).name);
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

        mFusedLocationProviderClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        mLastLocation = task.getResult();
                        if(task.isSuccessful()){
                            CameraPosition oldPos = mMap.getCameraPosition();
                            CameraPosition pos = CameraPosition.builder(oldPos).bearing(16).build();
                            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
                        }
                    }
                });
    }
}
