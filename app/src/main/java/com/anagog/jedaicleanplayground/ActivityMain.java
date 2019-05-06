package com.anagog.jedaicleanplayground;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;
import com.anagog.jedaicleanplayground.jedaiutils.JedAIHelper;

public class ActivityMain extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //jedAi start only after getting user permissions
        if (requestCheckPermissions()) {
            JedAIHelper.startJedAi();
        }
    }

    private boolean requestCheckPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);

            return false;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for (int result : grantResults) {

            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You have to accept all permission for using this app!", Toast.LENGTH_LONG).show();
                return;
            }
        }

        JedAIHelper.startJedAi();
    }



    @Override
    protected void onResume() {
        super.onResume();
        JedAIHelper.printAllInVehicleEvents();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
