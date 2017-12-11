package college.paul.john.puvroute;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.ArrayList;

/*
    This class is the bridge between the ui and backend of the app
    All data handling should be call here.
 */
class MapRoutes {
    private static final String TAG = "MapRoutes";
    private static volatile MapRoutes instance;
    private ArrayList<Route> routeList;
    private RouteListener mListener;

    public interface RouteListener{
        void loadComplete();
        void onError(String error);
        void onChange(Route route);
    }

    private MapRoutes(){
        routeList = new ArrayList<>();
    }

    private static MapRoutes getInstance(){
        synchronized (MapRoutes.class){
            if (instance == null){
                // Create a new instance if there are no existing one.
                instance = new MapRoutes();
            }
        }
        return instance;
    }

    static void setRouteListener(RouteListener listener){
        getInstance().mListener = listener;
    }

    /*
        Load route data from firebase or local storage.
     */
    static void loadRoutes(){
        getInstance().routeList = SharedPrefs.getRoute();
        if (getInstance().routeList.size() < 1){
            // Download and use firebase database.
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("route");
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    for (DataSnapshot routeSnapshot: dataSnapshot.getChildren()) {
                        Route route = new Route();
                        route.name = routeSnapshot.getKey();
                        for (DataSnapshot data: routeSnapshot.getChildren()){
                            String value = String.valueOf(data.getValue());
                            if (data.getKey().equals("description")){
                                route.description = String.valueOf(data.getValue());
                            } else if (data.getKey().equals("points")){
                                route.points = new Gson().fromJson(value, Points.class);
                            }
                        }
                        getInstance().routeList.add(route);
                    }
                    if (getInstance().mListener != null){
                        getInstance().mListener.loadComplete();
                    }
                    Log.v(TAG, SharedPrefs.storeRoutes(getInstance().routeList)?"Store routes success.":"Store routes fail.");
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.e(TAG, error.toString());
                    if (getInstance().mListener != null){
                        getInstance().mListener.onError(error.toString());
                    }
                }
            });
        } else {
            // Use locally stored database
            if (getInstance().mListener != null){
                getInstance().mListener.loadComplete();
            }
        }
    }

    // Get list of routes.
    static ArrayList<Route> getRouteList(){
        return getInstance().routeList;
    }

    // Change to a new route.
    static void changeRoute(String routeName){
        Route route = null;
        for (Route item : getInstance().routeList){
            if (item.name.equals(routeName)){
                route = item;
                break;
            }
        }
        if (route != null){
            if (getInstance().mListener != null){
                getInstance().mListener.onChange(route);
            }
        }
    }
}
