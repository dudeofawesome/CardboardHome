package dudeofawesome.cardboardhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

public class ApplicationItem {
    public int x = 0;
    public Rect pos = new Rect();
    public Bitmap icon;
    public Bitmap iconGry;
    public String name = "";
    public int z = 10;
    public ApplicationInfo appInfo;
    private Intent launchIntent;
    private Context context;

    public ApplicationItem (Rect pos, ApplicationInfo appInfo, PackageManager pkgMan, Context context) {
        this.pos = pos;
        this.icon = ((BitmapDrawable) appInfo.loadIcon(pkgMan)).getBitmap();
        this.iconGry = getGrayscaleBitmap(this.icon);
        this.name = pkgMan.getApplicationLabel(appInfo).toString();
//        this.name = appInfo.packageName;
        this.appInfo = appInfo;
        this.launchIntent = pkgMan.getLaunchIntentForPackage(appInfo.packageName);
        this.context = context;
    }

    public ApplicationItem (Rect pos, Bitmap icon, int type, PackageManager pkgMan, Context context) {
        this.pos = pos;
        this.icon = icon;
        this.iconGry = getGrayscaleBitmap(this.icon);
        if (type == 0) {
            this.name = "Exit Google Cardboard";
            if (Launcher.preferences.getBoolean("use_as_home", true)) {
                this.launchIntent = new Intent(Intent.ACTION_MAIN);
                this.launchIntent.addCategory(Intent.CATEGORY_HOME);
                this.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.launchIntent.setClassName("android", "com.android.internal.app.ResolverActivity");
            }
            else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                String currentHomePackage = pkgMan.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName;
                this.launchIntent = pkgMan.getLaunchIntentForPackage(currentHomePackage);
            }
        }
        else if (type == 1) {
            this.name = "Preferences";
            this.launchIntent = new Intent(context, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        else if (type == 2) {
            this.name = "Adjust Volume";
            this.launchIntent = new Intent(context, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        this.context = context;
    }

    private Bitmap getGrayscaleBitmap(Bitmap color) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorMatrixFilter = new ColorMatrixColorFilter(colorMatrix);
        Bitmap grayscale = color.copy(Bitmap.Config.ARGB_8888, true);
        Paint paint = new Paint();
        paint.setColorFilter(colorMatrixFilter);
        Canvas canvas = new Canvas(grayscale);
        canvas.drawBitmap(grayscale, 0, 0, paint);
        return grayscale;
    }

    public void move (int deltaTime) {
        pos.left += deltaTime / 1000;
    }

    public void launch () {
        context.startActivity(launchIntent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

}
