package college.paul.john.puvroute;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

class SharedPrefs {
    private static volatile SharedPrefs instance;
    private SharedPreferences mSharedPreferences;

    private SharedPrefs(){

    }

    static SharedPrefs getInstance(){
        synchronized (SharedPrefs.class){
            if (instance == null){
                instance = new SharedPrefs();
            }
        }
        return instance;
    }

    static void init(Context context){
        getInstance().mSharedPreferences = context.getSharedPreferences("puvrouter", Context.MODE_PRIVATE);
    }

    static boolean storeRoutes(ArrayList<Route> routes){
        try {
            String result = new Gson().toJson(routes);
            getInstance().mSharedPreferences.edit().putString("routes", result).apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static ArrayList<Route> getRoute(){
        ArrayList<Route> routeList = new ArrayList<>();
        String result = getInstance().mSharedPreferences.getString("routes", null);
        if (result != null){
            routeList = new Gson().fromJson(result, new TypeToken<ArrayList<Route>>() {}.getType());
        }
        return routeList;
    }
}
