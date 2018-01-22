package college.paul.john.puvroute;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.arlib.floatingsearchview.FloatingSearchView;
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
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private LinearLayout mMapMakerParent;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final String TAG = "MapsActivity";
    private Drawer mDrawer;
    private View mMapView;
    private TextView destination;
    private View view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        view = findViewById(R.id.layoutSearch);
        mMapMakerParent = findViewById(R.id.llMapMakerParent);
        destination = findViewById(R.id.tvDestination);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mMapView = mapFragment.getView();

        mDrawer = new DrawerBuilder()
                .withTranslucentStatusBar(true)
                .withActivity(this)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("PUV Router").withIcon(R.mipmap.ic_launcher),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withIdentifier(1).withIcon(GoogleMaterial.Icon.gmd_add).withName("Add Route")
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        switch ((int) drawerItem.getIdentifier()){
                            case 1:
                                Map.setMode(Map.Mode.MAP_MAKER);
                                break;
                            default:
                                break;
                        }
                        return false;
                    }
                })
                .build();
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
            public void onChangeMode(int mode) {
                switch (mode){
                    case Map.Mode.FREE:
                        destination.setText(R.string.set_destination);
                        mMapMakerParent.setVisibility(View.GONE);
                        break;
                    case Map.Mode.MAP_MAKER:
                        destination.setText(R.string.map_maker_mode);
                        mMapMakerParent.setVisibility(View.VISIBLE);
                        break;
                    case Map.Mode.ROUTE:
                        mMapMakerParent.setVisibility(View.GONE);
                        break;
                    default:
                        break;

                }
            }
        });
        if (mMapView != null &&
                mMapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mMapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 30, 380);
        }

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
                destination.setText(route.name);
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
        if (Map.getMode() == Map.Mode.MAP_MAKER){
            if (Map.getMarkerPoints().size() > 0){
                Map.clearMap();
            } else {
                Map.setMode(Map.Mode.FREE);
            }
        }
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

    public void showMapSearch(View view) {
        /*
            Open a search intent to pick your destination.
        */
        try {
            Intent intent =
                    new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                            .build(MapsActivity.this);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException e) {
            Log.e(TAG, e.toString());
        } catch (GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, e.toString());
        }
    }

    /*
        Open the navigation drawer
     */
    public void showDrawer(View view){
        mDrawer.openDrawer();
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
