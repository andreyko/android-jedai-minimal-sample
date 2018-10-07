package com.anagog.jedaicleanplayground.jedaiutils;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

import com.anagog.jedai.common.EventConfig;
import com.anagog.jedai.common.JedAIEvent;
import com.anagog.jedai.common.JedAIEventListener;
import com.anagog.jedai.common.activity.ActivityEvent;
import com.anagog.jedai.common.contracts.LocationHistoryContract;
import com.anagog.jedai.common.contracts.TopPoiContract;
import com.anagog.jedai.common.contracts.VisitHistoryContract;
import com.anagog.jedai.core.api.JedAI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Tourdyiev Roman on 06.10.2018.
 */
public class JedAIHelper {

    // CursorLoaders for querying needed visits/locations
    public static class VisitsLoader extends CursorLoader {

        public VisitsLoader(Context context) {
            super(context, VisitHistoryContract.getContentUri(context), VisitHistoryContract.PROJECTION_ALL, null, null, null);
        }

        public VisitsLoader(Context context, String poiID) {
            super(context, VisitHistoryContract.getContentUri(context), VisitHistoryContract.PROJECTION_ALL, VisitHistoryContract.VisitContract.COLUMN_POI_ID + "=?", new String[]{poiID}, null);
        }
    }

    public static class PoiVisitsLoader extends CursorLoader {

        public PoiVisitsLoader(Context context) {
            super(context, TopPoiContract.getContentUri(context), TopPoiContract.PROJECTION_ALL, null, null, null);
        }

        public PoiVisitsLoader(Context context, int visits) {
            super(context, TopPoiContract.getContentUri(context), TopPoiContract.PROJECTION_ALL, TopPoiContract.COLUMN_SCORE + ">=?", new String[]{"" + visits}, null);
        }
    }

    public static class LocationHistoryLoader extends CursorLoader {

        public LocationHistoryLoader(Context context) {
            super(context, LocationHistoryContract.getContentUri(context), LocationHistoryContract.PROJECTION_ALL, null, null, null);
        }

        public LocationHistoryLoader(Context context, long TIME) {
            super(context, LocationHistoryContract.getContentUri(context), LocationHistoryContract.PROJECTION_ALL, LocationHistoryContract.COLUMN_NAME_LOCATION_TIME + ">?", new String[]{String.valueOf(TIME)}, null);
        }
    }

    public static void StartJedAi() {
        //jedAi start only after getting user permissions
        JedAI jedAI = JedAI.getInstance();
        if (jedAI != null) {
            jedAI.start();
        }
    }

    //a helper function that demonstrates how to register to all real-time events
    public static void RegisterAllEventsListener(JedAIEventListener listener) {
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


    //get all in-vehicle activities longer than 5 minutes
    public static Cursor getAllInVehicleEvents() {

        String query = "select StopTimestamp, StartTimeStamp, ActivityType from Activity where" +
            " StopTimestamp-StartTimestamp > 5*60*1000 and ActivityType = " + ActivityEvent.IN_VEHICLE + " order by _id ASC";

        SQLiteDatabase db = getReadableDatabase();
        String[] args = {};
        Cursor cursorActivities = db.rawQuery(query, args);

        while(cursorActivities.moveToNext())
        {
            long durationMinutes = (cursorActivities.getLong(0) -  cursorActivities.getLong(1))/1000/60;
            Log.e("jedAIAPI", "activity type: " + cursorActivities.getInt(2) + ", Duration: " + durationMinutes);
        }
        return cursorActivities;

    }

    public Location getHomeLocation() {

        SQLiteDatabase db = getReadableDatabase();

        if (db != null) {

            //advanced SQL query to try and find the home location of the user
            //in this query we take all Visits in which the user spent the night in (23:00 to 07:00)
            String query =
                    "select PoiId, Latitude, Longitude,\n" +
                            "cast(strftime('%s',datetime((EnterTimestampLocal)/1000,'unixepoch')) as integer) as startTime,\n" +
                            "cast(strftime('%s',datetime((ExitTimestampLocal)/1000,'unixepoch')) as integer) as endTime,\n" +
                            "cast(strftime('%s',datetime((ExitTimestampLocal)/1000,'unixepoch','start of day', '-1 hours')) as integer) as nightStart,\n" +
                            "cast(strftime('%s',datetime((ExitTimestampLocal)/1000,'unixepoch','start of day', '+7 hours')) as integer) as nightEnd,\n" +
                            "(ExitTimestamp - EnterTimestamp) / 1000.0 as Duration\n" +
                            "from Visit\n" +
                            "where ExitTimestamp <> 0 and" +
                            "((startTime < nightStart and endTime > nightEnd) or " +
                            "(startTime < nightStart and endTime > nightStart and endTime < nightEnd and endTime-nightStart > 4*60*60) or " +
                            "(startTime > nightStart and startTime < nightEnd and endTime > nightEnd and nightEnd-startTime > 4*60*60) or" +
                            "(startTime > nightStart and endTime < nightEnd and Duration > 4*60*60))";

            String[] args = {};
            Cursor cursorHome = db.rawQuery(query, args);

            ArrayList<Double> latArray = new ArrayList<>();
            ArrayList<Double> lonArray = new ArrayList<>();

            if (cursorHome != null) {
                cursorHome.moveToFirst();

                //add all Visits Lat/Long into a n array
                while (!cursorHome.isAfterLast()) {
                    latArray.add(cursorHome.getDouble(1));
                    lonArray.add(cursorHome.getDouble(2));
                    cursorHome.moveToNext();
                }
                cursorHome.close();

                //calculate the median of the Lat/Long and set it as the home location
                double medianLat = medianArray(latArray);
                double medianLon = medianArray(lonArray);
                Location homeLocation = new Location("");
                homeLocation.setLatitude(medianLat);
                homeLocation.setLongitude(medianLon);
                return homeLocation;
            }
        }
        return null;
    }

    //a helper function to calculate a median of an array
    private double medianArray(ArrayList<Double> array) {
        if (array.size()==0){
            return 0.0D;
        }
        //calculate median of home locations
        ArrayList<Double> arrayToMedian = new ArrayList<>(array);

        Collections.sort(arrayToMedian);
        double median;
        if (arrayToMedian.size() % 2 == 0)
            median = (arrayToMedian.get(arrayToMedian.size() / 2) + arrayToMedian.get(arrayToMedian.size() / 2 - 1)) / 2;
        else
            median = arrayToMedian.get(arrayToMedian.size() / 2);

        return median;
    }


    public static void copyDBonFirstRun(Context context){
        String appDataPath = context.getApplicationInfo().dataDir;
        File dbFolder = new File(appDataPath + "/databases");
        dbFolder.mkdir();
        File jedaiDB = new File(dbFolder + "/jedai.db");
        copyDB(context, "jedai.db", jedaiDB);
    }

    private static void copyDB(Context context, String dbName, File file){
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

        }
    }
}
