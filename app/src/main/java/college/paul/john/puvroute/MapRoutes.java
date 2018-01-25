package college.paul.john.puvroute;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Random;

/*
    This class is the bridge between the ui and backend of the app
    All data handling should be call here.
 */
class MapRoutes {
    private static final String TAG = "MapRoutes";
    private static volatile MapRoutes instance;
    private ArrayList<Route> routeList;
    private RouteListener mListener;

    public interface RouteListener {
        void loadComplete();

        void onError(String error);

        void onChange(Route route);

        void onUpdate(ArrayList<Route> routes);
    }

    private MapRoutes() {
        routeList = new ArrayList<>();
    }

    private static MapRoutes getInstance() {
        synchronized (MapRoutes.class) {
            if (instance == null) {
                // Create a new instance if there are no existing one.
                instance = new MapRoutes();
            }
        }
        return instance;
    }

    // Listen to any changes happening to MapRoutes
    static void setRouteListener(RouteListener listener) {
        getInstance().mListener = listener;
    }

    /*
        Load route data from firebase or local storage.
     */
    static void loadRoutes() {
        getInstance().routeList = SharedPrefs.getRoute();
        if (getInstance().routeList.size() < 1) {
            downloadFromServer();

        } else {
            // Use locally stored database
            if (getInstance().mListener != null) {
                getInstance().mListener.loadComplete();
            }
        }
    }

    // Download routes from firebase server.
    static void downloadFromServer() {
        // Download and use firebase database.
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("route");
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot routeSnapshot : dataSnapshot.getChildren()) {
                    Route route = new Route();
                    route.name = routeSnapshot.getKey();
                    for (DataSnapshot data : routeSnapshot.getChildren()) {
                        String value = String.valueOf(data.getValue());
                        if (data.getKey().equals("description")) {
                            route.description = String.valueOf(data.getValue());
                        } else if (data.getKey().equals("points")) {
                            route.points = new Gson().fromJson(value, Points.class);
                        } else if (data.getKey().equals("id")){
                            route.id = String.valueOf(data.getValue());
                        }
                    }
                    getInstance().routeList.add(route);
                }
                if (getInstance().mListener != null) {
                    getInstance().mListener.loadComplete();
                }
                Log.v(TAG, SharedPrefs.storeRoutes(getInstance().routeList) ? "Store routes success." : "Store routes fail.");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.e(TAG, error.toString());
                if (getInstance().mListener != null) {
                    getInstance().mListener.onError(error.toString());
                }
            }
        });
    }

    /*
        Add route to to server
     */
    static void addRoute(final Context context, final ArrayList<LatLng> markerPoints, final boolean confirmation, final String routeName) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String positive = "Done";
        final String[] tempRouteName = new String[1];
        EditText input = null;

        // check if dialog is a confirmation to overwrite an existing route.
        // If it is true then show message that an existing route name is present.
        if (confirmation) {
            if (routeName != null)
                tempRouteName[0] = routeName;
            positive = "Yes";
            builder.setMessage(tempRouteName[0] + " already exist in the database. Would you like to overwrite it?");
        } else {
            input = new EditText(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            input.setHint("Route Name");
            builder.setView(input);
        }

        builder.setTitle("Save Route");
        final EditText finalInput = input;
        builder.setPositiveButton(positive, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (finalInput != null) {
                    tempRouteName[0] = finalInput.getText().toString();
                }
                if (tempRouteName[0].isEmpty()) {
                    Toast.makeText(context, "Route name is empty", Toast.LENGTH_SHORT).show();
                    addRoute(context, markerPoints, confirmation, routeName);
                } else {
                    final ProgressDialog progressDialog = new ProgressDialog(context);
                    progressDialog.setMessage("Updating database");
                    progressDialog.show();
                    if (!confirmation) {
                        for (Route item : getInstance().routeList) {
                            if (item.name.equals(tempRouteName[0])) {
                                addRoute(context, markerPoints, true, tempRouteName[0]);
                                return;
                                // All the codes below will be skipped because there is an existing route name.
                            }
                        }
                    }

                    Log.v(TAG, "Updating firebase.");
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference dbRef = database.getReference("route");

                    final double[][] latlong = new double[markerPoints.size()][2];
                    for (int i = 0; i < markerPoints.size(); i++) {
                        latlong[i][0] = markerPoints.get(i).latitude;
                        latlong[i][1] = markerPoints.get(i).longitude;
                    }
                    final String points = "{   \"points\":" + new Gson().toJson(latlong) + "}";
                    final String key = dbRef.push().getKey();
                    // Send the data to server.
                    dbRef.child(tempRouteName[0]).child("id").setValue(key);
                    dbRef.child(tempRouteName[0]).child("points")
                            .setValue(points, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                    // The new data has been save to the server.
                                    Route route = new Route();
                                    route.name = tempRouteName[0];
                                    route.points = new Points();
                                    route.points.points = latlong;
                                    route.id = key;
                                    MapRoutes.updateRoute(route);
                                    Map.setMode(Map.Mode.FREE);
                                    Map.clearMap();
                                    Toast.makeText(context, "New route saved", Toast.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                }
                            });
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create();
        builder.show();
    }

    // Update route list and store to local storage.
    static void updateRoute(Route route) {
        Log.v(TAG, "Updating route");
        boolean skip = false;
        for (Route item : getInstance().routeList){
            if (item.id.equals(route.id)){
                skip = true;
                break;
            }
        }
        if (!skip) {
            getInstance().routeList.add(route);
            SharedPrefs.storeRoutes(getInstance().routeList);
            if (getInstance().mListener != null) {
                getInstance().mListener.onUpdate(getInstance().routeList);
            }
        }
    }

    // Get list of routes.
    static ArrayList<Route> getRouteList() {
        return getInstance().routeList;
    }

    // Change to a new route.
    static void changeRoute(String id) {
        Route route = null;
        for (Route item : getInstance().routeList) {
            if (item.id.equals(id)) {
                route = item;
                break;
            }
        }
        if (route != null) {
            if (getInstance().mListener != null) {
                getInstance().mListener.onChange(route);
            }
            Map.redrawMap(route);
        }
    }

    // Display a randomize selection of routes from the routelist..
    static void randomRoute() {
        ArrayList<Route> routes = getInstance().routeList;
        if (routes.size() > 0 && routes.size() > 1) {
            Random rand = new Random();
            int index = rand.nextInt(routes.size()-1);
            changeRoute(routes.get(index).id);
        } else if(routes.size() == 1){
            changeRoute(routes.get(0).id);
        }
    }

    static void setDestination(Place destination){
        Double[] shortestPath = new Double[2];
        Double lowestDistance = null;
        Double lowestOrigin = null;
        LatLng selected = null;
        ArrayList<Route> routeList = getInstance().routeList;
        Route selectedRoute = null;
        Location currentLocation = Map.getCurrentLocation();
        for (Route item : routeList) {
            Log.i(TAG, "Route " + item.name);
            for (double[] points :item.points.points) {
                double destinationDistance = Utilities.distance(destination.getLatLng().latitude, destination.getLatLng().longitude, points[0], points[1]);
                if (lowestDistance == null || lowestDistance > destinationDistance){
                    lowestDistance = destinationDistance;
                    selected = new LatLng(points[0], points[1]);
                    selectedRoute = item;
                }
            }
        }
        if (selected != null){
            changeRoute(selectedRoute.id);
            LatLng origin = new LatLng(selected.latitude, selected.longitude);
            LatLng dest = new LatLng(destination.getLatLng().latitude, destination.getLatLng().longitude);
            new Parser.FetchUrl().execute(Parser.getUrl(origin, dest));
        }
        if (selectedRoute != null){
            for (double[] points : selectedRoute.points.points) {
                double originDistance = Utilities.distance(currentLocation.getLatitude(), currentLocation.getLongitude(),
                        points[0], points[1]);
                if (lowestOrigin == null || lowestOrigin > originDistance){
                    lowestOrigin = originDistance;
                    shortestPath[0] = points[0];
                    shortestPath[1] = points[1];
                }
            }
        }
        if (shortestPath.length > 1) {
            LatLng origin = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            LatLng dest = new LatLng(shortestPath[0], shortestPath[1]);
            new Parser.FetchUrl().execute(Parser.getUrl(origin, dest));
        }
        Map.setMarker(destination.getLatLng(), "Destination: " + destination.getName(),
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Map.focusSelf();
    }

    static void showRouteList(final Context context){
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);
        builderSingle.setIcon(R.mipmap.ic_launcher);
        builderSingle.setTitle("Route List");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.select_dialog_item);
        for (Route item : getRouteList()) {
            arrayAdapter.add(item.name);
        }

        builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Map.setMode(Map.Mode.FREE);
                Route route = getInstance().routeList.get(which);
                changeRoute(route.id);
                Map.moveCamera(new LatLng(route.points.points[0][0], route.points.points[0][1]));
            }
        });
        builderSingle.show();
    }
}
