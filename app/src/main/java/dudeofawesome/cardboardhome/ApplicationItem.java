package dudeofawesome.cardboardhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;

public class ApplicationItem {
    public int x = 0;
    public Rect pos = new Rect();
    public Bitmap icon;
    public String name = "";
    public int z = 2;
    public ApplicationInfo appInfo;
    private Intent launchIntent;
    private Context context;

    public ApplicationItem (Rect pos, ApplicationInfo appInfo, PackageManager pkgMan, Context context) {
        this.pos = pos;
        this.icon = ((BitmapDrawable) appInfo.loadIcon(pkgMan)).getBitmap();
//        this.icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        this.name = pkgMan.getApplicationLabel(appInfo).toString();
//        this.name = appInfo.packageName;
        this.appInfo = appInfo;
        this.launchIntent = pkgMan.getLaunchIntentForPackage(appInfo.packageName);
        this.context = context;
    }

    public ApplicationItem (Rect pos, Bitmap icon, int type, Context context) {
        this.pos = pos;
        this.icon = icon;
        if (type == 0) {
            this.name = "Exit Google Cardboard";
            this.launchIntent = new Intent(Intent.ACTION_MAIN);
            this.launchIntent.addCategory(Intent.CATEGORY_HOME);
            this.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.launchIntent.setClassName("android", "com.android.internal.app.ResolverActivity");
        }
        else {
            this.name = "Preferences";
            this.launchIntent = new Intent(context, SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        this.context = context;
    }

    public void move (int deltaTime) {
        pos.left += deltaTime / 1000;
    }

    public void launch () {
        context.startActivity(launchIntent);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

}
