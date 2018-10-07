package com.anagog.jedaicleanplayground;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.anagog.jedai.common.JedAIEvent;
import com.anagog.jedai.common.JedAIEventListener;
import com.anagog.jedai.core.api.JedAI;
import com.anagog.jedaicleanplayground.jedaiutils.JedAIHelper;


/**
 * Created by Tourdyiev Roman on 05.10.2018.
 */
public class App extends Application {

    private SharedPreferences preferences;


    //this is the listener that will be called by the jedAi SDK for real time events
    private JedAIEventListener jedAIEventListener = new JedAIEventListener() {
        @Override
        public void onEvent(JedAIEvent event) {
        // saving events into explorer.db for later use in real-time events list
        if (event.getType() == JedAIEvent.GEOFENCE_ENTER_EVENT_TYPE || event.getType() == JedAIEvent.GEOFENCE_EXIT_EVENT_TYPE) {
            Log.e("jedAi", "Received GEOFENCE EVENT");
        } else if (event.getType() == JedAIEvent.VISIT_START_EVENT_TYPE || event.getType() == JedAIEvent.VISIT_END_EVENT_TYPE) {
            Log.e("jedAi", "Received VISIT EVENT");
        } else if (event.getType() == JedAIEvent.ACTIVITY_START_EVENT_TYPE || event.getType() == JedAIEvent.ACTIVITY_END_EVENT_TYPE) {
            Log.e("jedAi", "Received ACTIVITY EVENT");
        }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        //copy database on very first run
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.contains("isFirstRun")){
            JedAIHelper.copyDBonFirstRun(this);
            preferences.edit().putBoolean("isFirstRun", false).apply();
        }

        // JedAI SDK setup, preferable to be called here on the App onCreate
        JedAI.setup(this);
        // this helper fuction register a listener for the real time events
        JedAIHelper.RegisterAllEventsListener(jedAIEventListener);

        //don't start the jedAi sdk here! do it in the main activity after getting the user permissions.

    }

}
