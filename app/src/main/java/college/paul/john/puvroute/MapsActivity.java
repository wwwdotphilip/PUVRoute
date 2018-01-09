package college.paul.john.puvroute;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private TextView message;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final String TAG = "MapsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        message = findViewById(R.id.tvMessage);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize local storage.
        SharedPrefs.init(this);
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
        Map.init(MapsActivity.this, googleMap);
        Map.setMode(Map.Mode.FREE);
        Map.setMapListener(new Map.OnMapListener() {
            @Override
            public void onChangeMode(Map.Mode mode) {
                // Do something here when map changes mode.
            }
        });

        // Listen to any changes happening to MapRoutes
        MapRoutes.setRouteListener(new MapRoutes.RouteListener() {
            @Override
            public void loadComplete() {
                // Do something here once map route initialization is complete.
//                MapRoutes.randomRoute(); // Todo Remove or comment out this code if you are not testing.
            }

            @Override
            public void onError(String error) {
                // Do something here if MapRoutes produce any error.
            }

            @Override
            public void onChange(Route route) {
                message.setText(route.name);
                updateMapRoute(route);
            }

            @Override
            public void onUpdate(ArrayList<Route> routes) {
                // Do something here when route list has been updated.
            }
        });
        // Load map routes from server or local file.
        MapRoutes.loadRoutes();
    }

    // Button event for cancel
    public void cancelMapMaker(View view) {
        Map.clearMap();
    }

    // Button event for save
    public void saveMapMaker(final View view) {
        MapRoutes.addRoute(MapsActivity.this, Map.getMarkerPoints(), false, null);
    }

    /*
     Update google maps direction based on Route coordinates.
     */
    private void updateMapRoute(Route route) {
        Map.clearMap();
        LatLng[] latLng = new LatLng[route.points.coordinates.length];
        for (int i = 0; i < route.points.coordinates.length; i++) {
            latLng[i] = new LatLng(route.points.coordinates[i][0], route.points.coordinates[i][1]);
        }
        Map.getMap().addPolyline(new PolylineOptions()
                .add(latLng)
                .width(5)
                .color(Color.RED));
    }

    /*
     Open a search intent to pick your destination.
     */
    public void searchMap(View view) {
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .build(this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            Log.e(TAG, e.toString());
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, e.toString());
        }
    }

    /*
      Listen to any activity result from intent.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                Log.i(TAG, "Place: " + place.getName());
                MapRoutes.setDestination(place);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.i(TAG, status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
                Log.i(TAG, "Cancelled.");
            }
        }
    }
}
