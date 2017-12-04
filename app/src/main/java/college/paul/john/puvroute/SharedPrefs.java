package college.paul.john.puvroute;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

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

    static boolean storeRoute(Route route){
        try {
            String result = new Gson().toJson(route, Route.class);
            getInstance().mSharedPreferences.edit().putString("routes", result).apply();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    static Route getRoute(){
        Route route = null;
        String result = getInstance().mSharedPreferences.getString("routes", null);
        if (result != null){
            route = new Gson().fromJson(result, Route.class);
        }
        return route;
    }
}
