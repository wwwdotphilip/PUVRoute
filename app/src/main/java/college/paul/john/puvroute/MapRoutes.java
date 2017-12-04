package college.paul.john.puvroute;

import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

class MapRoutes {
    private static final String TAG = "MapRoutes";
    private static volatile MapRoutes instance;
    private Route route;

    private MapRoutes(){
        route = new Route();
    }

    static MapRoutes getInstance(){
        synchronized (MapRoutes.class){
            if (instance == null){
                instance = new MapRoutes();
            }
        }
        return instance;
    }

    static void getRoutes(){
        getInstance().route = SharedPrefs.getRoute();
        if (getInstance().route == null){
            getInstance().route = new Route();
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("route");
            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    for (DataSnapshot routeSnapshot: dataSnapshot.getChildren()) {
                        getInstance().route.name = routeSnapshot.getKey();
                        for (DataSnapshot data: routeSnapshot.getChildren()){
                            String value = String.valueOf(data.getValue());
                            if (data.getKey().equals("description")){
                                getInstance().route.description = String.valueOf(data.getValue());
                            } else if (data.getKey().equals("points")){
                                getInstance().route.points = new Gson().fromJson(value, Points.class);
                            }
                        }
                    }
                    Log.v(TAG, SharedPrefs.storeRoute(getInstance().route)?"Store routes success.":"Store routes fail.");
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Failed to read value
                    Log.e(TAG, error.toString());
                }
            });
        } else {
            Log.v(TAG, "Using stored routes.");
        }
    }
}
