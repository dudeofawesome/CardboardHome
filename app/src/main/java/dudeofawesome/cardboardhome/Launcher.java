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
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.speech.SpeechRecognizer;
import android.view.WindowManager;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardDeviceParams;
import com.google.vrtoolkit.cardboard.sensors.HeadTracker;
import com.google.vrtoolkit.cardboard.sensors.MagnetSensor;
import com.google.vrtoolkit.cardboard.sensors.NfcSensor;

import java.security.Key;
import java.util.ArrayList;
import java.util.List;


public class Launcher extends CardboardActivity implements SensorEventListener {

    public static final double TAU = Math.PI * 2;
    private MyView gameView = null;
    public static ArrayList<ApplicationItem> installedApps = new ArrayList<ApplicationItem>();
    public int iconCenter = 0;
    public float rotationalOffset = 0;
    public static float accelData = 0f;
    public static float rawAccelData = 0f;
    public static float rawGyroData = 0f;
    public static float accelDataOld = 0f;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private HeadTracker headTracker;
    private boolean forceAccelerometer = false;
    private float tweenStep = 0;
    public static SharedPreferences preferences;
    public Vibrator mVibrator;
    private Bitmap wallpaper;
    private boolean premium = false;
    private boolean drawWallpaper = true;
    private boolean startingApp = false;
    private ApplicationItem appToLaunch = null;
    private SpeechRecognizer speechRecog = null;
    private Intent recognizerIntent = null;
    private boolean readyToListen = false;
    private boolean listening = false;
    private int originalVolume = 0;
    public boolean volumePanelExpanded = false;
    public int volumePanelPosition = 0;
    public int volumePanelKnobPosition = 0;
    public int volumePanelWidth = 1;

    private final int APP_SPACING = 115;
    private final float TWEEN_TIMING = 0.5f * 60;
    private final String[] LAUNCH_COMMANDS = {"open", "launch", "play", "start", "begin"};
    private final String[] EXIT_COMMANDS = {"exit", "close", "quit", "stop"};
    private final String[] SETTINGS_COMMANDS = {"preferences", "settings", "options"};

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
                preferences.edit().putBoolean("premium", premium).commit();
                break;
            }
        }
        drawWallpaper = preferences.getBoolean("draw_wallpaper", true);
        // get installed apps
        for (int i = 0; i < packages.size(); i++) {
            if ((packages.get(i).packageName.toLowerCase().contains("cardboard") || packages.get(i).packageName.toLowerCase().contains("dive") || packages.get(i).packageName.toLowerCase().contains("vr") || packages.get(i).packageName.toLowerCase().contains("virtual") || packages.get(i).packageName.toLowerCase().contains("reality") || packages.get(i).packageName.equals("com.dudeofawesome.SuperHexagon")) && !packages.get(i).packageName.equals("com.dudeofawesome.cardboardhome") && !packages.get(i).packageName.equals("com.dudeofawesome.cardboardhomeunlocker")) {
                String[] ignoreNames = preferences.getString("blacklist", "").split(", ");
                boolean appOnBlacklist = false;
                for (int j = 0; j < ignoreNames.length; j++) {
                    if (getPackageManager().getApplicationLabel(packages.get(i)).toString().toLowerCase().equals(ignoreNames[j].toLowerCase())) {
                        appOnBlacklist = true;
                    }
                }
                if (!appOnBlacklist)
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
        installedApps.add(new ApplicationItem(new Rect((installedApps.size() - 1) * APP_SPACING, 315, 92, 92), BitmapFactory.decodeResource(getResources(), R.drawable.volume_icon), 2, getPackageManager(), getBaseContext()));
        installedApps.add(new ApplicationItem(new Rect((installedApps.size() - 1) * APP_SPACING, 315, 92, 92), BitmapFactory.decodeResource(getResources(), R.drawable.settings_icon), 1, getPackageManager(), getBaseContext()));
        installedApps.add(new ApplicationItem(new Rect((installedApps.size() - 1) * APP_SPACING, 315, 92, 92), BitmapFactory.decodeResource(getResources(), R.drawable.exit_icon), 0, getPackageManager(), getBaseContext()));

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
                    tweenStep = (rawAccelData - accelData) / TWEEN_TIMING;
                }
                else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    rawGyroData += event.values[0];
                }
            }
        };

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(mSensorListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        headTracker = new HeadTracker(getApplicationContext());
        headTracker.startTracking();


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

        AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        originalVolume = mgr.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (preferences.getBoolean("listen_for_voice", true))
            mgr.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

        if (premium && preferences.getBoolean("listen_for_voice", true)) {
            RecognitionListener recognitionListener = new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {

                }

                @Override
                public void onBeginningOfSpeech() {
                    listening = true;
                }

                @Override
                public void onRmsChanged(float v) {

                }

                @Override
                public void onBufferReceived(byte[] bytes) {

                }

                @Override
                public void onEndOfSpeech() {
                    listening = false;
                }

                @Override
                public void onError(int i) {

                }

                @Override
                public void onResults(Bundle bundle) {
                    checkForCommands(bundle);
                    listening = false;
                }

                @Override
                public void onPartialResults(Bundle bundle) {
                    checkForCommands(bundle);
                }

                private void checkForCommands(Bundle bundle) {
                    ArrayList<String> voiceText = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    for (int i = 0; i < voiceText.size(); i++) {
                        if (voiceText.get(i).toLowerCase().contains("okay cardboard") && voiceText.get(i).toLowerCase().split("okay cardboard").length > 0) {
                            String request = voiceText.get(i).toLowerCase().split("okay cardboard")[1].toLowerCase().replace(" ", "");
                            print(request);
                            for (int j = 0; j < LAUNCH_COMMANDS.length; j++) {
                                if (request.contains(LAUNCH_COMMANDS[j])) {
                                    print(request);
                                    for (int k = 0; k < installedApps.size(); k++) {
                                        if (request.contains(installedApps.get(k).name.toLowerCase().replace(" ", ""))) {
                                            mVibrator.vibrate(90);
                                            installedApps.get(k).launch();
                                            return;
                                        }
                                    }
                                }
                            }
                            for (int j = 0; j < EXIT_COMMANDS.length; j++) {
                                if (request.contains(EXIT_COMMANDS[j])) {
                                    installedApps.get(installedApps.size() - 1).launch();
                                    return;
                                }
                            }
                            for (int j = 0; j < SETTINGS_COMMANDS.length; j++) {
                                if (request.contains(SETTINGS_COMMANDS[j])) {
                                    installedApps.get(installedApps.size() - 2).launch();
                                    return;
                                }
                            }
                        }
                    }
                }

                @Override
                public void onEvent(int i, Bundle bundle) {

                }
            };


            recognizerIntent = RecognizerIntent.getVoiceDetailsIntent(getBaseContext());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 30000);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30000);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 30000);
            speechRecog = SpeechRecognizer.createSpeechRecognizer(getBaseContext());
            speechRecog.setRecognitionListener(recognitionListener);
            readyToListen = true;
            listening = true;

            speechRecog.startListening(recognizerIntent);
        }
        if (premium) {
            wallpaper = ((BitmapDrawable) WallpaperManager.getInstance(this).getDrawable()).getBitmap();
        }
    }

    private void print(String request) {
        System.out.println(request);
    }

    private void updateTweens() {
        accelData += tweenStep;
    }

    private void prepareToLaunch (ApplicationItem app) {
        appToLaunch = app;
        startingApp = true;
    }

    private void launchApp () {
        startingApp = false;
        if (appToLaunch.name.equals("Adjust Volume")) {
            // Bring up volume adjustment panel
//            if (originalVolume == 0) {
//                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//                originalVolume = audioManager.getStreamMaxVolume(audioManager.STREAM_MUSIC);
//            }
//            else
//                originalVolume = 0;
            volumePanelExpanded = true;
            volumePanelPosition = (int) ((gameView.headFloats[0] - rotationalOffset) * 500);
            AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            volumePanelKnobPosition = (originalVolume / audioManager.getStreamMaxVolume(audioManager.STREAM_MUSIC)) * 100;

            // Ready to launch other app
            gameView.appStartAnimationPosition = 0;
        }
        else {
            AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

            appToLaunch.launch();
        }
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
            return true;
        else if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) && preferences.getBoolean("disable_volume_buttons", false))
            return true;
        else
            return super.onKeyDown(keyCode, event);
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
        private Paint paint = new Paint();
        private int width = 0;
        private int height = 0;
        private Rect cursorPosition = new Rect(width / 2 - 1, height / 2 - 1, width / 2 + 1, height / 2 + 1);
        private int appStartAnimationPosition = 0;
        private Rect freeAllocate = new Rect();

        long timeOfLastFrame = 0;

        public MyView(Context context) {
            super(context);

            paint.setStyle(Paint.Style.FILL);
            width = getWidth() / 2;
            volumePanelWidth = width - 480;
            height = getHeight();
            timeOfLastFrame = System.currentTimeMillis();

            gameLoop();
        }

        private int selectedApp = -1;
        private long timeSelected = -1;
        private final long selectionTime = 200;

        private void gameLoop () {
            move();
            updateTweens();
            if (width == 0) {
                width = getWidth() / 2;
                volumePanelWidth = width - 480;
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
//                        installedApps.get(selectedApp).launch();
                        prepareToLaunch(installedApps.get(selectedApp));
                    }
                }
                freeAllocate.set(installedApps.get(selectedApp).x, installedApps.get(selectedApp).pos.top, installedApps.get(selectedApp).x + installedApps.get(selectedApp).pos.right, installedApps.get(selectedApp).pos.top + installedApps.get(selectedApp).pos.bottom);
                if (!Rect.intersects(cursorPosition, freeAllocate)) {
                    selectedApp = -1;
                    timeSelected = -1;
                }
            }

            if (startingApp) {
                if (appStartAnimationPosition > width / 2 && appStartAnimationPosition > height / 2) {
                    launchApp();
                }
                else {
                    appStartAnimationPosition += 30;
                }
            }

            if (readyToListen /*&& !listening*/ && preferences.getBoolean("listen_for_voice", true)) {
                listening = true;
                speechRecog.startListening(recognizerIntent);
            }

            // cause redraw
            invalidate();
        }

        public void magnetPull () {
            if (volumePanelExpanded) {
                // set volume
                volumePanelExpanded = false;
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                originalVolume = (int) (audioManager.getStreamMaxVolume(audioManager.STREAM_MUSIC) * (Math.abs(volumePanelKnobPosition - volumePanelWidth) / (float) volumePanelWidth));
            }
            else if (selectedApp != -1) {
                if (preferences.getBoolean("vibrate_on_selection", true))
                    mVibrator.vibrate(50);
                prepareToLaunch(installedApps.get(selectedApp));
            }
            else {
                // reset rotation
                rotationalOffset = headFloats[0];
            }
        }

        float headMatrix[] = new float[16];
        float headFloats[] = new float[3];

        private void move () {
            if (volumePanelExpanded) {
//                volumePanelKnobPosition = (int) (((headFloats[0] - rotationalOffset) * 500) / volumePanelPosition) * 100;
                volumePanelKnobPosition = (int) ((gameView.headFloats[0] - rotationalOffset) * 500) - volumePanelPosition;
                volumePanelKnobPosition = (volumePanelKnobPosition < 0) ? 0 : volumePanelKnobPosition;
                volumePanelKnobPosition = (volumePanelKnobPosition > volumePanelWidth) ? volumePanelWidth : volumePanelKnobPosition;
            }
            for (int i = 0; i < installedApps.size(); i++) {
                installedApps.get(i).pos.top = (height / 2) - (installedApps.get(i).pos.bottom / 2);
                if (rawGyroData == 0.0000f || forceAccelerometer || headTracker == null ) {
                    installedApps.get(i).x = (int) (installedApps.get(i).pos.left + (accelData * 100)) - iconCenter;
                }
                else {
                    headTracker.getLastHeadView(headMatrix, 0);
                    headFloats = getEulerFromMat(headMatrix);

//                    if ((int) (headFloats[0] * 100) / 100 <= Math.PI + (15 * Math.PI / 16) && (int) (headFloats[0] * 100) / 100 >= Math.PI - (15 * Math.PI / 16) && rotationalOffset >= Math.PI / 2 && rotationalOffset <= -Math.PI / 2) { // accounts for weird behavior at PI rads
//                        installedApps.get(i).x = (int) (installedApps.get(i).pos.left + ((headFloats[0] - rotationalOffset) * 500)) - iconCenter;
//                    }
//                    else
                    installedApps.get(i).x = (int) (installedApps.get(i).pos.left + ((headFloats[0] - rotationalOffset) * 500)) - iconCenter;
                }
            }
        }

        float normalizeRotation (float rot) {
            if (rot > TAU)
                rot -= TAU;
            else if (rot < -TAU)
                rot += TAU;
            else
                return rot;

            return normalizeRotation(rot);
        }

        float[] getEulerFromMat(float[] rotMatrix) {
            float x, y, z;

            float _11, _12, _13, _14;
            float _21, _22, _23, _24;
            float _31, _32, _33, _34;
            float _41, _42, _43, _44;

            _11 = rotMatrix[0];
            _12 = rotMatrix[1];
            _13 = rotMatrix[2];
            _14 = rotMatrix[3];

            _21 = rotMatrix[4];
            _22 = rotMatrix[5];
            _23 = rotMatrix[6];
            _24 = rotMatrix[7];

            _31 = rotMatrix[8];
            _32 = rotMatrix[9];
            _33 = rotMatrix[10];
            _34 = rotMatrix[11];

            _41 = rotMatrix[12];
            _42 = rotMatrix[13];
            _43 = rotMatrix[14];
            _44 = rotMatrix[15];

            if (_11 == 1.0f)
            {
                x = (float) Math.atan2(_13, _34);
                y = 0;
                z = 0;

            }else if (_11 == -1.0f)
            {
                x = (float) Math.atan2(_13, _34);
                y = 0;
                z = 0;
            }else
            {

                x = (float) Math.atan2(-_31,_11);
                y = (float) Math.asin(_21);
                z = (float) Math.atan2(-_23,_22);
            }

            float[] _return = {x, y, z};
            return _return;
        }

        float[] getMatFromEuler(float[] eulerAngles) {
            float x = eulerAngles[0], y = eulerAngles[1], z = eulerAngles[2];

            float _11 = 0, _12 = 0, _13 = 0, _14 = 0;
            float _21 = 0, _22 = 0, _23 = 0, _24 = 0;
            float _31 = 0, _32 = 0, _33 = 0, _34 = 0;
            float _41 = 0, _42 = 0, _43 = 0, _44 = 0;

            float transMatrix[] = new float[16];

            float X[][] = new float[3][3];
            float Y[][] = new float[3][3];
            float Z[][] = new float[3][3];

            X[2][2] = (float) Math.cos(x);
            X[2][3] = (float) -Math.sin(x);
            X[3][2] = (float) Math.sin(x);
            X[3][3] = (float) Math.cos(x);

            Y[1][1] = (float) Math.cos(y);
            Y[1][3] = (float) Math.sin(y);
            Y[3][1] = (float) -Math.sin(y);
            Y[3][3] = (float) Math.cos(y);

            Z[1][1] = (float) Math.cos(z);
            Z[1][2] = (float) -Math.sin(z);
            Z[2][1] = (float) Math.sin(z);
            Z[2][2] = (float) Math.cos(z);

            float[][] _rotMatrix = matrixMultiply(matrixMultiply(Z, Y), X);
//            int k = 0;
//            for (int i = 0; i < _rotMatrix.length; i++) {
//                for (int j = 0; j < _rotMatrix[i].length; j++) {
//                    transMatrix[k] = _rotMatrix[i][j];
//                    k++;
//                }
//                transMatrix[k] = 0;
//                k++;
//            }
//            transMatrix[12] = 0;
//            transMatrix[13] = 0;
//            transMatrix[14] = 0;
//            transMatrix[15] = 1;

            transMatrix[0] = _rotMatrix[0][0];
            transMatrix[1] = _rotMatrix[0][1];
            transMatrix[2] = _rotMatrix[0][2];
            transMatrix[3] = 0;

            transMatrix[4] = _rotMatrix[1][0];
            transMatrix[5] = _rotMatrix[1][1];
            transMatrix[6] = _rotMatrix[1][2];
            transMatrix[7] = 0;

            transMatrix[8] = _rotMatrix[2][0];
            transMatrix[9] = _rotMatrix[2][1];
            transMatrix[10] = _rotMatrix[2][2];
            transMatrix[11] = 0;

            transMatrix[12] = 0;
            transMatrix[13] = 0;
            transMatrix[14] = 0;
            transMatrix[15] = 1;

            return transMatrix;
        }

        public float[][] matrixMultiply(float[][] A, float[][] B) {

            int aRows = A.length;
            int aColumns = A[0].length;
            int bRows = B.length;
            int bColumns = B[0].length;

            if (aColumns != bRows) {
                throw new IllegalArgumentException("A:Rows: " + aColumns + " did not match B:Columns " + bRows + ".");
            }

            float[][] C = new float[aRows][bColumns];
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    C[i][j] = 0.00000f;
                }
            }

            for (int i = 0; i < aRows; i++) { // aRow
                for (int j = 0; j < bColumns; j++) { // bColumn
                    for (int k = 0; k < aColumns; k++) { // aColumn
                        C[i][j] += A[i][k] * B[k][j];
                    }
                }
            }

            return C;
        }

        Display dis = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Bitmap lEyeBit = Bitmap.createBitmap(dis.getWidth() / 2, dis.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap rEyeBit = Bitmap.createBitmap(dis.getWidth() / 2, dis.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas lEye = new Canvas(lEyeBit);
        Canvas rEye = new Canvas(rEyeBit);

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int radius;
            radius = 100;

            paint.setColor(Color.BLACK);
            canvas.drawPaint(paint);

            // draw left eye
            if (premium && drawWallpaper) {
                freeAllocate.set(0, 0, width, height);
                lEye.drawBitmap(wallpaper, null, freeAllocate, paint);
            }
            else {
                paint.setColor(Color.BLACK);
                lEye.drawPaint(paint);
            }

            paint.setAntiAlias(true);

            paint.setColor(Color.parseColor("#CD5C5C"));
            if (preferences.getBoolean("launch_on_hover", true))
                lEye.drawCircle(width / 2, height / 2, radius * ((float) timeSelected / selectionTime), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            lEye.drawCircle(width / 2, height / 2, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            if (startingApp) {
                lEye.drawCircle(width / 2, height / 2, appStartAnimationPosition, paint);
            }
            for (int i = 0; i < installedApps.size(); i++) {
                if (installedApps.get(i).x < width && installedApps.get(i).x + installedApps.get(i).pos.right > 0) {
                    if (i != selectedApp || volumePanelExpanded) {
                        freeAllocate.set(installedApps.get(i).x - 1, installedApps.get(i).pos.top, installedApps.get(i).x - 1 + installedApps.get(i).pos.right, installedApps.get(i).pos.top + installedApps.get(i).pos.bottom);
                        lEye.drawBitmap(installedApps.get(i).iconGry, null, freeAllocate, paint);
                    }
                }
            }
            if (volumePanelExpanded) {
                paint.setColor(Color.argb(200, 100, 100, 100));
                if (100 - (int) ((headFloats[0] - rotationalOffset) * 500) > width / 2 - 25) // overscroll to the right
                    freeAllocate.set(width / 2 - width - 200 - 25, height / 2 - 25, width / 2 + 25, height / 2 + 25);
                else if (100 + (int) ((headFloats[0] - rotationalOffset) * 500) > width / 2 - 25) // overscroll to the left
                    freeAllocate.set(width / 2 - 25, height / 2 - 25, width, height / 2 + 25);
                else
                    freeAllocate.set(100 + (int) ((headFloats[0] - rotationalOffset) * 500), height / 2 - 25, width - 100 + (int) ((headFloats[0] - rotationalOffset) * 500), height / 2 + 25);
                lEye.drawRoundRect(new RectF(freeAllocate), 50f, 50f, paint);

                paint.setColor(Color.rgb(0, 100, 255));
                if (120 + (int) ((headFloats[0] - rotationalOffset) * 500) > width / 2 - 5)
                    freeAllocate.set(width / 2 - 5, height / 2 - 5, width, height / 2 + 5);
                else if (width - 120 + (int) ((headFloats[0] - rotationalOffset) * 500) < width / 2 + 5)
                    freeAllocate.set(width / 2 - width - 240, height / 2 - 5, width / 2 - 5, height / 2 + 5);
                else
                    freeAllocate.set(120 + (int) ((headFloats[0] - rotationalOffset) * 500), height / 2 - 5, width - 120 + (int) ((headFloats[0] - rotationalOffset) * 500), height / 2 + 5);
                lEye.drawRect(freeAllocate, paint);

                paint.setColor(Color.rgb(0, 50, 255));
                lEye.drawCircle(width / 2, height / 2, 14, paint);
            }
            if (!volumePanelExpanded && selectedApp != -1) {
                freeAllocate.set(installedApps.get(selectedApp).x + 1 + installedApps.get(selectedApp).z - 14, installedApps.get(selectedApp).pos.top - 14, installedApps.get(selectedApp).x + 1 + installedApps.get(selectedApp).z + installedApps.get(selectedApp).pos.right + 28, installedApps.get(selectedApp).pos.top + installedApps.get(selectedApp).pos.bottom + 28);
                lEye.drawBitmap(installedApps.get(selectedApp).icon, null, freeAllocate, paint);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(Color.WHITE);
                paint.setTextSize(20);
                lEye.drawText(installedApps.get(selectedApp).name, installedApps.get(selectedApp).x + (installedApps.get(selectedApp).pos.right / 2), installedApps.get(selectedApp).pos.top + 120, paint);
            }



            // draw right eye
            if (premium && drawWallpaper) {
                freeAllocate.set(0, 0, width, height);
                rEye.drawBitmap(wallpaper, null, freeAllocate, paint);
            }
            else {
                paint.setColor(Color.BLACK);
                rEye.drawPaint(paint);
            }
            paint.setColor(Color.parseColor("#CD5C5C"));
            if (preferences.getBoolean("launch_on_hover", true))
                rEye.drawCircle(width / 2, height / 2, radius * ((float) timeSelected / selectionTime), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            rEye.drawCircle(width / 2, height / 2, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            if (startingApp) {
                rEye.drawCircle(width / 2, height / 2, appStartAnimationPosition, paint);
            }
            for (int i = 0; i < installedApps.size(); i++) {
                if (installedApps.get(i).x < width && installedApps.get(i).x + installedApps.get(i).pos.right > 0) {
                    if (i != selectedApp || volumePanelExpanded) {
                        freeAllocate.set(installedApps.get(i).x - 1, installedApps.get(i).pos.top, installedApps.get(i).x - 1 + installedApps.get(i).pos.right, installedApps.get(i).pos.top + installedApps.get(i).pos.bottom);
                        rEye.drawBitmap(installedApps.get(i).iconGry, null, freeAllocate, paint);
                    }
                }
            }
            if (volumePanelExpanded) {
                paint.setColor(Color.argb(200, 100, 100, 100));
                if (100 - (int) ((headFloats[0] - rotationalOffset) * 500) > width / 2 - 25) // overscroll to the right
                    freeAllocate.set(width / 2 - width - 200 - 25, height / 2 - 25, width / 2 + 25, height / 2 + 25);
                else if (100 + (int) ((headFloats[0] - rotationalOffset) * 500) > width / 2 - 25) // overscroll to the left
                    freeAllocate.set(width / 2 - 25, height / 2 - 25, width, height / 2 + 25);
                else
                    freeAllocate.set(100 + (int) ((headFloats[0] - rotationalOffset) * 500), height / 2 - 25, width - 100 + (int) ((headFloats[0] - rotationalOffset) * 500), height / 2 + 25);
                rEye.drawRoundRect(new RectF(freeAllocate), 50f, 50f, paint);

                paint.setColor(Color.rgb(0, 100, 255));
                if (120 + (int) ((headFloats[0] - rotationalOffset) * 500) > width / 2 - 5)
                    freeAllocate.set(width / 2 - 5, height / 2 - 5, width, height / 2 + 5);
                else if (width - 120 + (int) ((headFloats[0] - rotationalOffset) * 500) < width / 2 + 5)
                    freeAllocate.set(width / 2 - width - 240, height / 2 - 5, width / 2 - 5, height / 2 + 5);
                else
                    freeAllocate.set(120 + (int) ((headFloats[0] - rotationalOffset) * 500), height / 2 - 5, width - 120 + (int) ((headFloats[0] - rotationalOffset) * 500), height / 2 + 5);
                rEye.drawRect(freeAllocate, paint);

                paint.setColor(Color.rgb(0, 50, 255));
                rEye.drawCircle(width / 2, height / 2, 14, paint);
            }
            if (!volumePanelExpanded && selectedApp != -1) {
                freeAllocate.set(installedApps.get(selectedApp).x - 1 - installedApps.get(selectedApp).z - 14 - 4, installedApps.get(selectedApp).pos.top - 14, installedApps.get(selectedApp).x - 1 - installedApps.get(selectedApp).z + installedApps.get(selectedApp).pos.right + 28, installedApps.get(selectedApp).pos.top + installedApps.get(selectedApp).pos.bottom + 28);
                rEye.drawBitmap(installedApps.get(selectedApp).icon, null, freeAllocate , paint);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setColor(Color.WHITE);
                paint.setTextSize(20);
                rEye.drawText(installedApps.get(selectedApp).name, installedApps.get(selectedApp).x + (installedApps.get(selectedApp).pos.right / 2), installedApps.get(selectedApp).pos.top + 120, paint);
            }


            //render eyes to screen
            float pupilaryDist = ((100 - preferences.getInt("interpupilary_distance", 50)) / 50) * 0.0635f; // this should be the distance between the user's pupils in meters -- 0.0635m is average
            int shiftImage = (int) (pupilaryDist * 1023.6220472); // for the OPO, this needs to equal 65
//            print("An interpupilary distance of " + pupilaryDist + " is equal to " + shiftImage * 2 + " pixels.");
            canvas.drawBitmap(lEyeBit, new Rect(0,0,width,height), new Rect(shiftImage,0,width + shiftImage, height), paint);
            canvas.drawBitmap(rEyeBit, new Rect(shiftImage,0,width,height), new Rect(width,0,getWidth() - shiftImage, height), paint);

            //draw divider
            paint.setColor(Color.GRAY);
            canvas.drawRect(width - 5, 0, width + 5, height, paint);

            if (appStartAnimationPosition > 0) {
                paint.setColor(Color.BLACK);
                paint.setAlpha((int) ((appStartAnimationPosition / (height / 2f)) * 255f));
                canvas.drawPaint(paint);
            }

            gameLoop();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            magnetPull();
            return false;
        }
    }
}