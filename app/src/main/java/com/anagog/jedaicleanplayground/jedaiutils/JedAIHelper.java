package com.anagog.jedaicleanplayground.jedaiutils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateUtils;
import android.util.Log;

import com.anagog.jedai.common.EventConfig;
import com.anagog.jedai.common.JedAIEvent;
import com.anagog.jedai.common.JedAIEventListener;
import com.anagog.jedai.common.activity.ActivityEvent;
import com.anagog.jedai.common.contracts.ActivityHistoryContract;
import com.anagog.jedai.common.poi.Poi;
import com.anagog.jedai.common.poi.Point;
import com.anagog.jedai.common.trip.ClusteredTrip;
import com.anagog.jedai.core.api.JedAI;
import com.anagog.jedai.plugin.smartpoi.JedAISmartPoi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Common functionality for operating JedAI
 */
public class JedAIHelper {
    private static final String TAG = "jedAIAPI";

    public static void startJedAi() {
        //jedAi start only after getting user permissions
        JedAI jedAI = JedAI.getInstance();
        if (jedAI != null) {
            jedAI.start();
        }
    }

    //a helper function that demonstrates how to register to all real-time events
    public static void registerAllEventsListener(JedAIEventListener listener) {
        EventConfig.Builder builder = new EventConfig.Builder();
        builder.onEventTypes(
                JedAIEvent.VISIT_START_EVENT_TYPE |
                        JedAIEvent.VISIT_END_EVENT_TYPE |
                        JedAIEvent.GEOFENCE_ENTER_EVENT_TYPE |
                        JedAIEvent.GEOFENCE_EXIT_EVENT_TYPE |
                        JedAIEvent.ACTIVITY_START_EVENT_TYPE |
                        JedAIEvent.ACTIVITY_END_EVENT_TYPE
        );

        JedAI jedAI = JedAI.getInstance();
        if (jedAI != null) {
            jedAI.registerEvents(listener, builder.build());
        }
    }

    private static SQLiteDatabase getReadableDatabase() {
        JedAI jedAIApi = JedAI.getInstance();
        if (jedAIApi == null) {
            return null;
        }
        return jedAIApi.getJedAIDb();
    }

    /**
     * get all in-vehicle activities longer than 5 minutes
     * NOTE: the best practise is not to access the database on the main thread
     */
    public static void printAllInVehicleEvents() {
        SQLiteDatabase db = getReadableDatabase();

        if (db == null) {
            return;
        }

        try (Cursor cursorActivities = db.query(ActivityHistoryContract.TABLE_NAME,
                new String[]{
                        ActivityHistoryContract.COLUMN_NAME_STOP_TIMESTAMP,
                        ActivityHistoryContract.COLUMN_NAME_START_TIMESTAMP,
                        ActivityHistoryContract.COLUMN_NAME_ACTIVITY_TYPE,
                        ActivityHistoryContract.COLUMN_NAME_VEHICLE_TYPE
                },
                ActivityHistoryContract.COLUMN_NAME_STOP_TIMESTAMP + " - " +
                        ActivityHistoryContract.COLUMN_NAME_START_TIMESTAMP + " > " + 5 * DateUtils.MINUTE_IN_MILLIS + " and " +
                        ActivityHistoryContract.COLUMN_NAME_ACTIVITY_TYPE + "=" + ActivityEvent.IN_VEHICLE,
                null, null, null, null)) {
            while (cursorActivities.moveToNext()) {
                long durationMinutes = (cursorActivities.getLong(0) - cursorActivities.getLong(1)) / DateUtils.MINUTE_IN_MILLIS;
                Log.i(TAG, "activity type: " +
                        ActivityEvent.activityToString(cursorActivities.getInt(cursorActivities.getColumnIndex(ActivityHistoryContract.COLUMN_NAME_ACTIVITY_TYPE))) +
                        ", Duration: " + durationMinutes + " min vehicle type " +
                        cursorActivities.getInt(cursorActivities.getColumnIndex(ActivityHistoryContract.COLUMN_NAME_VEHICLE_TYPE)));
            }
        }
    }

    public static void printHome() {
        Poi home = JedAISmartPoi.getInstance().getHome();
        Log.i(TAG, "Home: " + (home == null ? "null" : home.toString()));
    }

    public static void printPrimaryOffice() {
        Poi primaryOffice = JedAISmartPoi.getInstance().getPrimaryOffice();
        Log.i(TAG, "Primary office: " + (primaryOffice == null ? "null" : primaryOffice.toString()));
    }

    public static void printAllOffices() {
        List<Poi> allOffices = JedAISmartPoi.getInstance().getAllOffices();

        if (allOffices.isEmpty()) {
            Log.i(TAG, "No offices");
            return;
        }

        for (Poi office : allOffices) {
            Log.i(TAG, "Office: " + (office == null ? "null" : office.toString()));
        }
    }

    public static void printAllTrips() {
        long startTime = 0;
        long endTime = Long.MAX_VALUE;
        int minimalNumberOfTrips = 3;

        JedAI jedAIApi = JedAI.getInstance();
        List<ClusteredTrip> clusteredTrips = null;

        if (jedAIApi != null) {
            clusteredTrips = jedAIApi.getClusteredTrips(startTime, endTime, minimalNumberOfTrips);
        }

        if (clusteredTrips == null || clusteredTrips.isEmpty()) {
            Log.i(TAG, "No trips clusters");
            return;
        }
        for (int i = 0; i < clusteredTrips.size(); ++i) {
            ClusteredTrip clusteredTrip = clusteredTrips.get(i);

            if (clusteredTrip != null) {
                Point startLocation = clusteredTrip.getStartLocation();
                Point stopLocation = clusteredTrip.getStopLocation();

                Log.i(TAG, String.format(
                        "Trips cluster #%d: " +
                                "StartLatitude = %s, " +
                                "StartLongitude = %s, " +
                                "StopLatitude = %s, " +
                                "StopLongitude = %s, " +
                                "TripsInCluster = %d",
                        i,
                        startLocation == null ? "?" : String.valueOf(startLocation.getLatitude()),
                        startLocation == null ? "?" : String.valueOf(startLocation.getLongitude()),
                        stopLocation == null ? "?" : String.valueOf(stopLocation.getLatitude()),
                        stopLocation == null ? "?" : String.valueOf(stopLocation.getLongitude()),
                        clusteredTrip.getTrips() == null ? 0 : clusteredTrip.getTrips().size()));
            } else {
                Log.i(TAG, String.format("Trips cluster #%d is null", i));
            }
        }
    }

    public static void copyDBonFirstRun(Context context) {
        String appDataPath = context.getApplicationInfo().dataDir;
        File dbFolder = new File(appDataPath + "/databases");
        dbFolder.mkdir();

        String jedaiDb = "jedai.db";
        String personalPoi = "personal_poi.db";

        File jedaiDBFile = new File(dbFolder, jedaiDb);
        File personalPoiDBFile = new File(dbFolder, personalPoi);

        copyDB(context, jedaiDb, jedaiDBFile);
        copyDB(context, personalPoi, personalPoiDBFile);
    }

    private static void copyDB(Context context, String dbName, File file) {
        try {
            InputStream inputStream = context.getAssets().open(dbName);

            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
