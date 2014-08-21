package dudeofawesome.cardboardhome;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardDeviceParams;
import com.google.vrtoolkit.cardboard.sensors.MagnetSensor;
import com.google.vrtoolkit.cardboard.sensors.NfcSensor;

import java.util.ArrayList;
import java.util.List;


public class Launcher extends CardboardActivity implements SensorEventListener {

    private MyView gameView = null;
    public static ArrayList<ApplicationItem> installedApps = new ArrayList<ApplicationItem>();
    public int iconCenter = 0;
    public static float accelData = 0f;
    public static float[] gyroData = {0f, 0f, 0f};
    public static float rawAccelData = 0f;
    public static float[] rawGyroData = {0f, 0f, 0f};
    public static float accelDataOld = 0f;
    public static float[] gyroDataOld = {0f, 0f, 0f};
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    public static SharedPreferences preferences;
    public Vibrator mVibrator;
    private Bitmap wallpaper;
    private boolean premium = false;
    private boolean drawWallpaper = true;

    private final int APP_SPACING = 115;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        gameView = new MyView(this);
        setContentView(gameView);

        makeImmersive();

        if (!isMyAppLauncherDefault() && preferences.getBoolean("use_as_home", true)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setClassName("android", "com.android.internal.app.ResolverActivity");
            startActivity(intent);
        }

        installedApps.clear();
        // check for premium package
        List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        for (int i = 0; i < packages.size(); i++) {
            if (packages.get(i).packageName.equals("com.dudeofawesome.cardboardhomeunlocker")) {
                premium = true;
                preferences.edit().putBoolean("premium", premium);
                preferences.edit().apply();
                break;
            }
        }
        drawWallpaper = preferences.getBoolean("draw_wallpaper", true);
        // get installed apps
        for (int i = 0; i < packages.size(); i++) {
            if ((packages.get(i).packageName.toLowerCase().contains("cardboard") || packages.get(i).packageName.toLowerCase().contains("dive") || packages.get(i).packageName.toLowerCase().contains("vr") || packages.get(i).packageName.equals("com.dudeofawesome.SuperHexagon")) && !packages.get(i).packageName.equals("com.dudeofawesome.cardboardhome") && !packages.get(i).packageName.equals("com.dudeofawesome.cardboardhomeunlocker")) {
                installedApps.add(new ApplicationItem(new Rect((installedApps.size() - 1) * APP_SPACING, 315, 92, 92), packages.get(i), getPackageManager(), getBaseContext()));
            }
            else {
                String[] packageNames = preferences.getString("package_names_to_add", "").split(", ");
                for (int j = 0; j < packageNames.length; j++){
                    if (getPackageManager().getApplicationLabel(packages.get(i)).toString().toLowerCase().equals(packageNames[j].toLowerCase()) && !packages.get(i).packageName.equals("com.dudeofawesome.cardboardhome")) {
                        installedApps.add(new ApplicationItem(new Rect((installedApps.size() - 1) * APP_SPACING, 315, 92, 92), packages.get(i), getPackageManager(), getBaseContext()));
                    }
                }
            }
        }
        installedApps.add(new ApplicationItem(new Rect((installedApps.size() - 1) * APP_SPACING, 315, 92, 92), BitmapFactory.decodeResource(getResources(), R.drawable.settings_icon), 1, getBaseContext()));
        installedApps.add(new ApplicationItem(new Rect((installedApps.size() - 1) * APP_SPACING, 315, 92, 92), BitmapFactory.decodeResource(getResources(), R.drawable.exit_icon), 0, getBaseContext()));

        iconCenter = (int) ((((installedApps.size() + 1) / 2) - 4.5) * APP_SPACING);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        SensorEventListener mSensorListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor arg0, int arg1) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    accelDataOld = rawAccelData;
                    rawAccelData = event.values[1];
                }
                else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    gyroDataOld = rawGyroData;
                    rawGyroData = event.values;
                }
            }
        };

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(mSensorListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        MagnetSensor magnetSensor = new MagnetSensor(getApplicationContext());
        MagnetSensor.OnCardboardTriggerListener magnetTriggerListener = new MagnetSensor.OnCardboardTriggerListener() {
            @Override
            public void onCardboardTrigger() {
                gameView.magnetPull();
            }
        };
        magnetSensor.setOnCardboardTriggerListener(magnetTriggerListener);
        magnetSensor.start();

//        NfcSensor nfcSensor = NfcSensor.getInstance(getApplicationContext());
        NfcSensor.OnCardboardNfcListener nfcListener = new NfcSensor.OnCardboardNfcListener() {
            @Override
            public void onInsertedIntoCardboard(CardboardDeviceParams cardboardDeviceParams) {

            }

            @Override
            public void onRemovedFromCardboard() {
                installedApps.get(installedApps.size() - 1).launch();
            }
        };

        if (premium) {
            wallpaper = ((BitmapDrawable) WallpaperManager.getInstance(this).getDrawable()).getBitmap();
        }

    }

    private void updateTweens() {
        accelData = (rawAccelData + accelDataOld) / 2;

        gyroData[0] = (rawGyroData[0] + gyroDataOld[0]) / 2;
        gyroData[1] = (rawGyroData[1] + gyroDataOld[1]) / 2;
        gyroData[2] = (rawGyroData[2] + gyroDataOld[2]) / 2;
    }

    private void makeImmersive() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                          | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                          | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                          | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                          | View.SYSTEM_UI_FLAG_FULLSCREEN
                          | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    public boolean isMyAppLauncherDefault() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        filters.add(filter);

        // the packageName of your application
        String packageName = getPackageName();
        List<ComponentName> preferredActivities = new ArrayList<ComponentName>();
        final PackageManager packageManager = getPackageManager();

        // You can use name of your package here as third argument
        packageManager.getPreferredActivities(filters, preferredActivities, null);

        for (ComponentName activity : preferredActivities) {
            if (packageName.equals(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return (id == R.id.action_settings || super.onOptionsItemSelected(item));
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        makeImmersive();
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public class MyView extends View {
        Paint paint = new Paint();
        int width = 0;
        int height = 0;
        Rect cursorPosition = new Rect(width / 2 - 1, height / 2 - 1, width / 2 + 1, height / 2 + 1);
        private Rect freeAllocate = new Rect();

        long timeOfLastFrame = 0;

        public MyView(Context context) {
            super(context);

            paint.setStyle(Paint.Style.FILL);
            width = getWidth() / 2;
            height = getHeight();
            timeOfLastFrame = System.currentTimeMillis();

            gameLoop();
        }

        private int selectedApp = -1;
        private long timeSelected = -1;
        private final long selectionTime = 200;

        private void gameLoop () {
//            move();
            updateTweens();
            if (width == 0) {
                width = getWidth() / 2;
                height = getHeight();
                cursorPosition = new Rect(width / 2 - 1, 0, width / 2 + 1, height);
            }

            if (selectedApp == -1) {
                for (int i = 0; i < installedApps.size(); i++) {
                    freeAllocate.set(installedApps.get(i).x, installedApps.get(i).pos.top, installedApps.get(i).x + installedApps.get(i).pos.right, installedApps.get(i).pos.top + installedApps.get(i).pos.bottom);
                    if (Rect.intersects(cursorPosition, freeAllocate)) {
                        selectedApp = i;
                        timeSelected = 0;
                    }
                }
            } else {
                timeSelected++;
                if (timeSelected > selectionTime) {
                    if (preferences.getBoolean("launch_on_hover", true)) {
                        if (preferences.getBoolean("vibrate_on_selection", true))
                            mVibrator.vibrate(50);
                        installedApps.get(selectedApp).launch();
                    }
                }
                freeAllocate.set(installedApps.get(selectedApp).x, installedApps.get(selectedApp).pos.top, installedApps.get(selectedApp).x + installedApps.get(selectedApp).pos.right, installedApps.get(selectedApp).pos.top + installedApps.get(selectedApp).pos.bottom);
                if (!Rect.intersects(cursorPosition, freeAllocate)) {
                    selectedApp = -1;
                    timeSelected = -1;
                }
            }

            // cause redraw
            invalidate();
        }

        public void magnetPull () {
            if (selectedApp != -1) {
                if (preferences.getBoolean("vibrate_on_selection", true))
                    mVibrator.vibrate(50);
                installedApps.get(selectedApp).launch();
            }
        }

        private void move () {
            int deltaTime = (int) (System.currentTimeMillis() - timeOfLastFrame);
            for (int i = 0; i < installedApps.size(); i++) {
                installedApps.get(i).move(deltaTime);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);
            int radius;
            radius = 100;


            if (premium && drawWallpaper) {
                freeAllocate.set(0, 0, width, height);
                canvas.drawBitmap(wallpaper, null, freeAllocate, paint);
            }
            else {
                paint.setColor(Color.BLACK);
                canvas.drawPaint(paint);
            }

            // draw left eye
            paint.setColor(Color.parseColor("#CD5C5C"));
            if (preferences.getBoolean("launch_on_hover", true))
                canvas.drawCircle(width / 2, height / 2, radius * ((float) timeSelected / selectionTime), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            canvas.drawCircle(width / 2, height / 2, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < installedApps.size(); i++) {
                installedApps.get(i).x = (int) (installedApps.get(i).pos.left + (((gyroData[0]) != 0.00f ? gyroData[0] : accelData) * 100)) - iconCenter;
                if (installedApps.get(i).x < width && installedApps.get(i).x + installedApps.get(i).pos.right > 0) {
                    if (i != selectedApp) {
                        freeAllocate.set(installedApps.get(i).x - 1, installedApps.get(i).pos.top, installedApps.get(i).x - 1 + installedApps.get(i).pos.right, installedApps.get(i).pos.top + installedApps.get(i).pos.bottom);
                        canvas.drawBitmap(installedApps.get(i).icon, null, freeAllocate, paint);
                    }
                    else {
                        freeAllocate.set(installedApps.get(i).x - 1 - installedApps.get(i).z - 7, installedApps.get(i).pos.top - 7, installedApps.get(i).x - 1 - installedApps.get(i).z + installedApps.get(i).pos.right + 14, installedApps.get(i).pos.top + installedApps.get(i).pos.bottom + 14);
                        canvas.drawBitmap(installedApps.get(i).icon, null,freeAllocate , paint);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setColor(Color.WHITE);
                        paint.setTextSize(20);
                        canvas.drawText(installedApps.get(i).name, installedApps.get(i).x + (installedApps.get(i).pos.right / 2), installedApps.get(i).pos.top + 120, paint);
                    }
                }
            }



            // draw right eye
            if (premium && drawWallpaper) {
                freeAllocate.set(width, 0, width * 2, height);
                canvas.drawBitmap(wallpaper, null, freeAllocate, paint);
            }
            else {
                paint.setColor(Color.BLACK);
                canvas.drawRect(width, 0, width * 2, height, paint);
            }
            paint.setColor(Color.parseColor("#CD5C5C"));
            if (preferences.getBoolean("launch_on_hover", true))
                canvas.drawCircle(width + width / 2, height / 2, radius * ((float) timeSelected / selectionTime), paint);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(width + width / 2, height / 2, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < installedApps.size(); i++) {
                if (installedApps.get(i).x + width > width && installedApps.get(i).x + width < width * 2) {
                    if (i != selectedApp) {
                        freeAllocate.set(installedApps.get(i).x + width + 1, installedApps.get(i).pos.top, installedApps.get(i).x + width + installedApps.get(i).z + installedApps.get(i).pos.right, installedApps.get(i).pos.top + installedApps.get(i).pos.bottom);
                        canvas.drawBitmap(installedApps.get(i).icon, null, freeAllocate, paint);
                    }
                    else {
                        freeAllocate.set(installedApps.get(i).x + width + 1 + installedApps.get(i).z - 7, installedApps.get(i).pos.top - 7, installedApps.get(i).x + 1 + width + installedApps.get(i).z + installedApps.get(i).pos.right + 14, installedApps.get(i).pos.top + installedApps.get(i).pos.bottom + 14);
                        canvas.drawBitmap(installedApps.get(i).icon, null, freeAllocate, paint);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setColor(Color.WHITE);
                        paint.setTextSize(20);
                        canvas.drawText(installedApps.get(i).name, installedApps.get(i).x + (installedApps.get(i).pos.right / 2) + width, installedApps.get(i).pos.top + 120, paint);
                    }
                }
            }



            //draw divider
            paint.setColor(Color.GRAY);
            canvas.drawRect(width - 1, 0, width + 1, height, paint);

            gameLoop();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            magnetPull();
            return false;
        }
    }
}