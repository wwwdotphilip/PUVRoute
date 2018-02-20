package college.paul.john.puvroute;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import college.paul.john.puvroute.core.Map;
import college.paul.john.puvroute.core.MapRoutes;
import college.paul.john.puvroute.model.Mode;
import college.paul.john.puvroute.model.Route;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private LinearLayout mMapMakerParent;
    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    private static final String TAG = "MapsActivity";
    private Drawer mDrawer;
    private View mMapView;
    private TextView destination;
    private ProgressDialog mProgressDialog;
    private FirebaseAuth mAuth;
    private FirebaseUser mCurrentUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Icon.init(this);
        mAuth = FirebaseAuth.getInstance();
        mProgressDialog = new ProgressDialog(this);
        mMapMakerParent = findViewById(R.id.llMapMakerParent);
        destination = findViewById(R.id.tvDestination);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mMapView = mapFragment.getView();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Initialize local storage.
        SharedPrefs.init(this);

        mCurrentUser = mAuth.getCurrentUser();
        createDrawer();
    }

    private void createDrawer() {
        // Create the drawer object.
        mDrawer = new DrawerBuilder()
                .withTranslucentStatusBar(true)
                .withActivity(this)
                .withDisplayBelowStatusBar(true)
                .withOnDrawerListener(new Drawer.OnDrawerListener() {
                    @Override
                    public void onDrawerOpened(View drawerView) {

                    }

                    @Override
                    public void onDrawerClosed(View drawerView) {
                        mDrawer.setSelection(0);
                    }

                    @Override
                    public void onDrawerSlide(View drawerView, float slideOffset) {

                    }
                })
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        switch ((int) drawerItem.getIdentifier()) {
                            case 1:
                                Map.clearMap();
                                Map.setMode(Mode.MAP_MAKER);
                                break;
                            case 2:
                                Map.clearMap();
                                MapRoutes.showRemoveRouteList(MapsActivity.this);
                                break;
                            case 3:
                                Map.setMode(Mode.ROUTE);
                                MapRoutes.showRouteList(MapsActivity.this);
                                break;
                            case 4:
                                MapRoutes.downloadFromServer();
                                break;
                            case 5:
                                AlertDialog.Builder signInBuilder = new AlertDialog.Builder(MapsActivity.this);
                                signInBuilder.setTitle("Admin Sign-in");

                                LayoutInflater inflater = MapsActivity.this.getLayoutInflater();
                                @SuppressLint("InflateParams") View sigInView = inflater.inflate(R.layout.layout_sign_in, null);
                                final EditText email = sigInView.findViewById(R.id.etEmail), password = sigInView.findViewById(R.id.etPassword);

                                signInBuilder.setView(sigInView);
                                signInBuilder.setPositiveButton("Sign-In", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        mProgressDialog.setMessage("Signing in...");
                                        mProgressDialog.show();
                                        mAuth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                                                .addOnCompleteListener(MapsActivity.this, new OnCompleteListener<AuthResult>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                                        mProgressDialog.dismiss();
                                                        if (task.isSuccessful()) {
                                                            // Sign in success, update UI with the signed-in user's information
                                                            mCurrentUser = mAuth.getCurrentUser();
                                                            updateDrawer();
                                                        } else {
                                                            // If sign in fails, display a message to the user.
                                                            Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content),
                                                                    "Authentication failed.", Snackbar.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    }
                                });
                                signInBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });
                                signInBuilder.show();
                                break;
                            case 6:
                                mAuth.signOut();
                                mCurrentUser = null;
                                Map.setMode(Mode.FREE);
                                Map.clearMap();
                                updateDrawer();
                                break;
                            case 7:
                                Utilities.showLegends(MapsActivity.this);
                                break;
                            default:
                                break;
                        }
                        return false;
                    }
                }).build();
        updateDrawer();
    }

    private void updateDrawer() {
        if (mDrawer != null) {
            mDrawer.removeAllItems();
            IDrawerItem[] drawerItems = {new PrimaryDrawerItem().withIdentifier(0).withName("PUV Router").withIcon(R.mipmap.ic_launcher),
                    new DividerDrawerItem(),
                    new SecondaryDrawerItem().withIdentifier(3).withIcon(GoogleMaterial.Icon.gmd_view_carousel).withName("View All Routes"),
                    new SecondaryDrawerItem().withIdentifier(4).withIcon(GoogleMaterial.Icon.gmd_update).withName("Download latest route"),
                    new SecondaryDrawerItem().withIdentifier(7).withIcon(GoogleMaterial.Icon.gmd_update).withName("Legends"),
                    new SecondaryDrawerItem().withIdentifier(5).withIcon(GoogleMaterial.Icon.gmd_label).withName("Admin Sign-In")};

            if (mCurrentUser != null) {
                drawerItems = new IDrawerItem[]{new PrimaryDrawerItem().withIdentifier(0).withName("PUV Router").withIcon(R.mipmap.ic_launcher),
                        new DividerDrawerItem(),
                        new SecondaryDrawerItem().withIdentifier(1).withIcon(GoogleMaterial.Icon.gmd_add).withName("Add Route"),
                        new SecondaryDrawerItem().withIdentifier(2).withIcon(GoogleMaterial.Icon.gmd_remove).withName("Remove Route"),
                        new SecondaryDrawerItem().withIdentifier(3).withIcon(GoogleMaterial.Icon.gmd_view_carousel).withName("View All Routes"),
                        new SecondaryDrawerItem().withIdentifier(4).withIcon(GoogleMaterial.Icon.gmd_update).withName("Download latest route"),
                        new SecondaryDrawerItem().withIdentifier(7).withIcon(GoogleMaterial.Icon.gmd_label).withName("Legends"),
                        new SecondaryDrawerItem().withIdentifier(6).withIcon(GoogleMaterial.Icon.gmd_mail).withName("Sign Out")};
            }
            mDrawer.addItems(drawerItems);
        }
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
        Map.setMode(Mode.FREE);
        Map.setMapListener(new Map.OnMapListener() {
            @Override
            public void onChangeMode(int mode) {
                switch (mode) {
                    case Mode.FREE:
                        destination.setText(R.string.set_destination);
                        mMapMakerParent.setVisibility(View.GONE);
                        destination.setBackgroundColor(Color.WHITE);
                        break;
                    case Mode.MAP_MAKER:
                        destination.setText(R.string.map_maker_mode);
                        mMapMakerParent.setVisibility(View.VISIBLE);
                        destination.setBackgroundColor(getResources().getColor(R.color.green));
                        break;
                    case Mode.ROUTE:
                        mMapMakerParent.setVisibility(View.GONE);
                        destination.setBackgroundColor(Color.WHITE);
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
            public void onStart() {
                mProgressDialog.setMessage("Downloading database.");
                mProgressDialog.show();
            }

            @Override
            public void loadComplete() {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
                Snackbar.make(getWindow().getDecorView().findViewById(android.R.id.content),
                        "Route updated.", Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                // Do something here if MapRoutes produce any error.
            }

            @Override
            public void onChange(Route route) {
                destination.setText(route.name);
            }
        });
        // Load map routes from server or local file.
        MapRoutes.loadRoutes();
    }

    // Button event for cancel
    public void cancelMapMaker(View view) {
        Map.clearMap();
        Map.setMode(Mode.FREE);
    }

    // Button event for save
    public void saveMapMaker(final View view) {
        MapRoutes.addRoute(MapsActivity.this, Map.getMarkerPoints(), false, null);
    }

    public void showMapSearch(View view) {
        /*
            Open a search intent to pick your destination.
        */
        if (Map.getMode() != Mode.MAP_MAKER) {
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
    }

    /*
        Open the navigation drawer
     */
    public void showDrawer(View view) {
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

                Map.setMode(Mode.ROUTE);
                if (MapRoutes.getRouteList().size() > 0) {
                    // If route is found we set the destination.
                    MapRoutes.setDestination(place);
                } else {
                    Toast.makeText(getApplicationContext(), "No routes found", Toast.LENGTH_SHORT).show();
                }
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
