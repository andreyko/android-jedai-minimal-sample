* import core library;

     - add this dependency to your module level build.gradle file:

            dependencies {
                ...
                implementation "com.anagog.jedai:core:4.3.0"
                ...
            }

    - fill in "repositories" section with your credentials:

            repositories {
                ...
                maven {
                    url "https://repository.anagog.com/artifactory/gradle-release/"
                }
                ...
            }

    - add manifest placeholders at "defaultConfig" section:

            android {
                ...
                defaultConfig {
                    applicationId "com.anagog.jedaiplayground"
                    ...
                    manifestPlaceholders = [appId: "com.anagog.jedaiplayground"]
                }
                ...
            }




* add permissions

    In order to receive "activity" events from JedAI SDK add this permission at AndroidManifest.xml :

        <uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION"/>




* add supporting libs

    In order to receive "activity" events from JedAI SDK add this dependency to module level build.gradle :

        implementation 'com.google.android.gms:play-services-location:16.0.0'



* include SDK

    At the class of your choice (Application/Activity/Fragment/etc.) add following to connect your app with SDK:

        JedAI jedAI = JedAI.getInstance();
        jedAI.setup(this);

        EventConfig.Builder builder = new EventConfig.Builder();
        builder.onEventTypes(
                JedAIEvent.VISIT_START_EVENT_TYPE |
                JedAIEvent.VISIT_END_EVENT_TYPE |
                JedAIEvent.GEOFENCE_ENTER_EVENT_TYPE |
                JedAIEvent.GEOFENCE_EXIT_EVENT_TYPE |
                JedAIEvent.ACTIVITY_START_EVENT_TYPE |
                JedAIEvent.ACTIVITY_END_EVENT_TYPE
        );



* register listener

    At the class of your choice (Application/Activity/Fragment/etc.) add following to connect your app with SDK:

        JedAI jedAI = JedAI.getInstance();
        if (jedAI != null) {
            jedAI.registerEvents(jedAIEventListener, builder.build());
        }

        JedAIEventListener jedAIEventListener = new JedAIEventListener() {
                @Override
                public void onEvent(JedAIEvent event) {
                    /* read internals of JedAIEvent.class for better understanding payload of the events */
                }
        };



* start jedai after permissions check

    Request runtime permissions for Android M and higher:


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            if (requestCheckPermissions()) {
                JedAI jedAI = JedAI.getInstance();
                if (jedAI != null) {
                    jedAI.start();
                }
            }
            ...
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
                        break;
                    }
                }
            }

