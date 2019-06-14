package com.anagog.jedaicleanplayground;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.anagog.jedai.common.JedAIEvent;
import com.anagog.jedai.common.JedAIEventListener;
import com.anagog.jedai.core.api.JedAI;
import com.anagog.jedai.plugin.smartpoi.JedAISmartPoi;
import com.anagog.jedai.plugin.smartpoi.ServiceConfig;
import com.anagog.jedai.plugin.smartpoi.SmartPoiAlgorithmType;
import com.anagog.jedaicleanplayground.jedaiutils.JedAIHelper;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_MIN;

public class App extends Application {
    private static final int REQUEST_CODE = 12;
    private static final String FG_CHANNEL_ID = "JedAI Foreground";
    private static final String NOTIFICATION_CHANNEL_ID = "JedAI Demo";

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

        // Foreground service is optional but will significantly improve the performance of the SDK
        createNotificationChannelsIfNeeded();
        setupForegroundNotification();

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

    private void createNotificationChannelsIfNeeded() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        createForegroundChannel();
        createNotificationChannel();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_ID, IMPORTANCE_DEFAULT);
        channel.setDescription(NOTIFICATION_CHANNEL_ID);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createForegroundChannel() {
        NotificationChannel channel = new NotificationChannel(
                FG_CHANNEL_ID, FG_CHANNEL_ID, IMPORTANCE_MIN);
        channel.setDescription(FG_CHANNEL_ID);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setVibrationPattern(new long[]{0});
        channel.setLightColor(Color.RED);
        channel.setSound(null, null);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setImportance(IMPORTANCE_MIN);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        notificationManager.createNotificationChannel(channel);
    }

    private void setupForegroundNotification() {
        Intent openActivityIntent = new Intent(this, ActivityMain.class);
        openActivityIntent.setAction(Intent.ACTION_MAIN);
        openActivityIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                REQUEST_CODE,
                openActivityIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builderCompat =
                new NotificationCompat.Builder(getApplicationContext(), FG_CHANNEL_ID)
                        .setContentTitle("Android P")
                        .setContentText("Android P")
                        .setContentIntent(pendingIntent)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .setLargeIcon(
                                BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        Notification notification = builderCompat.build();

        JedAI.setForegroundNotification(notification);
    }

}
