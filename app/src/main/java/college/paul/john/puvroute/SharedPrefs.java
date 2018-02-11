package college.paul.john.puvroute;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import college.paul.john.puvroute.model.Route;

/*
    This class is responsible for storing and acquiring route data locally.
    All local storage should be done here.
 */
public class SharedPrefs {
    private static volatile SharedPrefs instance;
    private SharedPreferences mSharedPreferences;

    private SharedPrefs() {

    }

    private static SharedPrefs getInstance() {
        synchronized (SharedPrefs.class) {
            if (instance == null) {
                // Create a new instance if there are no existing one.
                instance = new SharedPrefs();
            }
        }
        return instance;
    }

    // Initialize sharedpreference.
    static void init(Context context) {
        getInstance().mSharedPreferences = context.getSharedPreferences("puvrouter", Context.MODE_PRIVATE);
    }

    // Store routes locally.
    public static boolean storeRoutes(ArrayList<Route> routes) {
        try {
            String result = new Gson().toJson(routes);
            getInstance().mSharedPreferences.edit().putString("routes", result).apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get routes from local storage.
    public static ArrayList<Route> getRoute() {
        ArrayList<Route> routeList = new ArrayList<>();
        String result = getInstance().mSharedPreferences.getString("routes", null);
        if (result != null) {
            routeList = new Gson().fromJson(result, new TypeToken<ArrayList<Route>>() {
            }.getType());
        }
        return routeList;
    }
}
