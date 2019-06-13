package com.anagog.jedaicleanplayground;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.anagog.jedai.common.JedAIEvent;
import com.anagog.jedai.common.JedAIEventListener;
import com.anagog.jedai.core.api.JedAI;
import com.anagog.jedai.plugin.smartpoi.JedAISmartPoi;
import com.anagog.jedai.plugin.smartpoi.ServiceConfig;
import com.anagog.jedai.plugin.smartpoi.SmartPoiAlgorithmType;
import com.anagog.jedaicleanplayground.jedaiutils.JedAIHelper;

public class App extends Application {

    private SharedPreferences preferences;

    /**
     * this is the listener that will be called by the jedAi SDK for real time events
     */
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
        if (!preferences.contains("isFirstRun")) {
            JedAIHelper.copyDBonFirstRun(this);
            preferences.edit().putBoolean("isFirstRun", false).apply();
        }

        // JedAI SDK setup, preferable to be called here on the App onCreate
        JedAI.setup(this);
        // this helper function register a listener for the real time events
        JedAIHelper.registerAllEventsListener(jedAIEventListener);


        // The code below needs instance of the JedAI
        // So we must exit if JedAI is null
        if (JedAI.getInstance() == null) {
            return;
        }

        // Allow increasing locations sampling during riding
        JedAI.getInstance().setInVehicleLocationRate(10, true);

        // Initialize and start SmartPoi module
        JedAISmartPoi.setup(this, JedAI.getInstance());
        JedAISmartPoi.getInstance().setServices(createServiceConfig());
        JedAISmartPoi.getInstance().start();

        //don't start the jedAi sdk here! do it in the main activity after getting the user permissions.
    }

    private ServiceConfig createServiceConfig() {
        return new ServiceConfig.Builder()
                .setServiceInfo(
                        SmartPoiAlgorithmType.Algorithm_3,
                        true,
                        null) // <-- nothing to initialize here
                .setServiceInfo(
                        SmartPoiAlgorithmType.Algorithm_4,
                        true,
                        null) // <-- nothing to initialize here
                .build();
    }

}
