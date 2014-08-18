package dudeofawesome.cardboardhome;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.vrtoolkit.cardboard.CardboardActivity;

import java.util.ArrayList;
import java.util.List;


public class GameActivity extends CardboardActivity implements SensorEventListener {

    private MyView gameView = null;
    public static ArrayList<ApplicationItem> installedApps = new ArrayList<ApplicationItem>();
    public static float[] accelData = {0f, 0f, 0f};
    public static float[] gyroData = {0f, 0f, 0f};
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;


    private final int MAX_NUMBER_OF_APPS = 5;
    private final int APP_SPACING = 115;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_game);
        gameView = new MyView(this);
        setContentView(gameView);

        makeImmersive();

        if (!isMyAppLauncherDefault()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setClassName("android", "com.android.internal.app.ResolverActivity");
            startActivity(intent);
//            startActivity(Intent.createChooser(intent, "Choose Launcher"));
        }

        // get installed apps
        List<ApplicationInfo> packages = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
        int i = 0;
        for (i = 0; i < packages.size() && installedApps.size() < MAX_NUMBER_OF_APPS; i++) {
            if ((packages.get(i).packageName.toLowerCase().contains("cardboard") || packages.get(i).packageName.toLowerCase().contains("dive") || packages.get(i).packageName.toLowerCase().contains("vr") || packages.get(i).packageName.equals("com.dudeofawesome.SuperHexagon")) && !packages.get(i).packageName.equals("com.dudeofawesome.cardboardhome")) {
                installedApps.add(new ApplicationItem(new Rect((installedApps.size() - 1) * APP_SPACING, 315, 92, 92), packages.get(i), getPackageManager(), getBaseContext()));
            }
        }


        // attempt to get only Google Cardboard apps
////        List<ResolveInfo> packages = getPackageManager().queryBroadcastReceivers(new Intent(Intent.ACTION_ALL_APPS, Uri.parse("cardboard://v.1.0.0")), PackageManager.GET_ACTIVITIES);
//        List<ResolveInfo> packages = getPackageManager().queryIntentActivities(new Intent("android.nfc.action.NDEF_DISCOVERED"), 0);
//        int i = 0;
//        System.out.println("There are " + packages.size() + " Cardboard apps");
//        for (i = 0; i < packages.size() && installedApps.size() < MAX_NUMBER_OF_APPS; i++) {
//            ApplicationInfo appInfo = packages.get(i).activityInfo.applicationInfo;
//            installedApps.add(new ApplicationItem(new Rect(i * 100, 315, 92, 92), appInfo, getPackageManager(), getBaseContext()));
//        }

        installedApps.add(new ApplicationItem(new Rect((installedApps.size() - 1) * APP_SPACING, 315, 92, 92), BitmapFactory.decodeResource(getResources(), R.drawable.settings_icon), 1, getBaseContext()));
        installedApps.add(new ApplicationItem(new Rect((installedApps.size() - 1) * APP_SPACING, 315, 92, 92), BitmapFactory.decodeResource(getResources(), R.drawable.exit_icon), 0, getBaseContext()));

        SensorEventListener mSensorListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor arg0, int arg1) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                Sensor sensor = event.sensor;
                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    accelData = event.values;
                }else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    gyroData = event.values;
                }
            }
        };

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(mSensorListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    public void onCardboardTrigger() {
        gameView.magnetPull();
    }

    @Override
    public void onRemovedFromCardboard() {

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
        final PackageManager packageManager = (PackageManager) getPackageManager();

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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
//        accelData = sensorEvent.values;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public class MyView extends View {
        Paint paint = new Paint();
        int width = 0;
        int height = 0;
        Rect cursorPosition = new Rect(width / 2 - 1, height / 2 - 1, width / 2 + 1, height / 2 + 1);
        DisplayMetrics displayMetrics = new DisplayMetrics();

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
            width = getWidth() / 2;
            height = getHeight();
            cursorPosition = new Rect(width / 2 - 1, 0, width / 2 + 1, height);

            if (selectedApp == -1) {
                for (int i = 0; i < GameActivity.installedApps.size(); i++) {
                    if (Rect.intersects(cursorPosition, new Rect(GameActivity.installedApps.get(i).x, GameActivity.installedApps.get(i).pos.top, GameActivity.installedApps.get(i).x + GameActivity.installedApps.get(i).pos.right, GameActivity.installedApps.get(i).pos.top + GameActivity.installedApps.get(i).pos.bottom))) {
                        selectedApp = i;
                        timeSelected = 0;
                    }
                }
            }
            else {
                timeSelected++;
                if (timeSelected > selectionTime) {
                    GameActivity.installedApps.get(selectedApp).launch();
                }
                else if (!Rect.intersects(cursorPosition, new Rect(GameActivity.installedApps.get(selectedApp).x, GameActivity.installedApps.get(selectedApp).pos.top, GameActivity.installedApps.get(selectedApp).x + GameActivity.installedApps.get(selectedApp).pos.right, GameActivity.installedApps.get(selectedApp).pos.top + GameActivity.installedApps.get(selectedApp).pos.bottom))) {
                    selectedApp = -1;
                    timeSelected = -1;
                }
            }

            // cause redraw
            invalidate();
        }

        public void magnetPull () {
            if (selectedApp != -1) {
                GameActivity.installedApps.get(selectedApp).launch();
            }
        }

        private void move () {
            int deltaTime = (int) (System.currentTimeMillis() - timeOfLastFrame);
            for (int i = 0; i < GameActivity.installedApps.size(); i++) {
                GameActivity.installedApps.get(i).move(deltaTime);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);
            int radius;
            radius = 100;


            paint.setColor(Color.BLACK);
            canvas.drawPaint(paint);

            // draw left eye
            paint.setColor(Color.parseColor("#CD5C5C"));
            canvas.drawCircle(width / 2, height / 2, radius * ((float) timeSelected / selectionTime), paint);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(width / 2, height / 2, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < GameActivity.installedApps.size(); i++) {
                GameActivity.installedApps.get(i).x = (int) (GameActivity.installedApps.get(i).pos.left + (((GameActivity.gyroData[0]) != 0.00f ? GameActivity.gyroData[0] : GameActivity.accelData[1]) * 100));
                if (GameActivity.installedApps.get(i).x < width && GameActivity.installedApps.get(i).x + GameActivity.installedApps.get(i).pos.right > 0) {
                    if (i != selectedApp)
                        canvas.drawBitmap(GameActivity.installedApps.get(i).icon, null, new Rect(GameActivity.installedApps.get(i).x - 1, GameActivity.installedApps.get(i).pos.top, GameActivity.installedApps.get(i).x - 1 + GameActivity.installedApps.get(i).pos.right, GameActivity.installedApps.get(i).pos.top + GameActivity.installedApps.get(i).pos.bottom), paint);
                    else {
                        canvas.drawBitmap(GameActivity.installedApps.get(i).icon, null, new Rect(GameActivity.installedApps.get(i).x - 1 - GameActivity.installedApps.get(i).z - 7, GameActivity.installedApps.get(i).pos.top - 7, GameActivity.installedApps.get(i).x - 1 - GameActivity.installedApps.get(i).z + GameActivity.installedApps.get(i).pos.right + 14, GameActivity.installedApps.get(i).pos.top + GameActivity.installedApps.get(i).pos.bottom + 14), paint);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setColor(Color.WHITE);
                        paint.setTextSize(20);
                        canvas.drawText(GameActivity.installedApps.get(i).name, GameActivity.installedApps.get(i).x + (GameActivity.installedApps.get(i).pos.right / 2), GameActivity.installedApps.get(i).pos.top + 120, paint);
                    }
                }
            }



            // draw right eye
            paint.setColor(Color.BLACK);
            canvas.drawRect(width, 0, width * 2, height, paint);
            paint.setColor(Color.parseColor("#CD5C5C"));
            canvas.drawCircle(width + width / 2, height / 2, radius * ((float) timeSelected / selectionTime), paint);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(width + width / 2, height / 2, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < GameActivity.installedApps.size(); i++) {
                if (GameActivity.installedApps.get(i).x + width > width && GameActivity.installedApps.get(i).x + width < width * 2) {
                    if (i != selectedApp)
                        canvas.drawBitmap(GameActivity.installedApps.get(i).icon, null, new Rect(GameActivity.installedApps.get(i).x + width + 1, GameActivity.installedApps.get(i).pos.top, (int) GameActivity.installedApps.get(i).x + width + GameActivity.installedApps.get(i).z + GameActivity.installedApps.get(i).pos.right, GameActivity.installedApps.get(i).pos.top + GameActivity.installedApps.get(i).pos.bottom), paint);
                    else {
                        canvas.drawBitmap(GameActivity.installedApps.get(i).icon, null, new Rect(GameActivity.installedApps.get(i).x + width + 1 + GameActivity.installedApps.get(i).z - 7, GameActivity.installedApps.get(i).pos.top - 7, GameActivity.installedApps.get(i).x + 1 + width + GameActivity.installedApps.get(i).z + GameActivity.installedApps.get(i).pos.right + 14, GameActivity.installedApps.get(i).pos.top + GameActivity.installedApps.get(i).pos.bottom + 14), paint);
                        paint.setTextAlign(Paint.Align.CENTER);
                        paint.setColor(Color.WHITE);
                        paint.setTextSize(20);
                        canvas.drawText(GameActivity.installedApps.get(i).name, GameActivity.installedApps.get(i).x + (GameActivity.installedApps.get(i).pos.right / 2), GameActivity.installedApps.get(i).pos.top + 120, paint);
                    }
                }
            }



            //draw divider
            paint.setColor(Color.GRAY);
            canvas.drawRect(width - 1, 0, width + 1, height, paint);

            gameLoop();
        }
    }
}