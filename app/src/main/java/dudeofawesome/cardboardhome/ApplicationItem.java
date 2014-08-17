package dudeofawesome.cardboardhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.preference.Preference;

/**
 * Created by DudeOfAwesome on 8/15/14.
 */
public class ApplicationItem {
    public int x = 0;
    public Rect pos = new Rect();
    public Bitmap icon;
    public String name = "";
    public boolean selected = false;
    public int z = 1;
    public ApplicationInfo appInfo;
    private Intent launchIntent;
    private Context context;

    public ApplicationItem (Rect pos, ApplicationInfo appInfo, PackageManager pkgMan, Context context) {
        this.pos = pos;
        this.icon = ((BitmapDrawable) appInfo.loadIcon(pkgMan)).getBitmap();
        this.name = appInfo.name;
        this.appInfo = appInfo;
        this.launchIntent = pkgMan.getLaunchIntentForPackage(appInfo.packageName);
        this.context = context;
    }

    public ApplicationItem (Rect pos, Bitmap icon, int type, Context context) {
        this.pos = pos;
        this.icon = icon;
        if (type == 0) {
            this.launchIntent = new Intent(Intent.ACTION_MAIN);
            this.launchIntent.addCategory(Intent.CATEGORY_HOME);
            this.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.launchIntent.setClassName("android", "com.android.internal.app.ResolverActivity");
        }
        else if (type == 1) {
            // TODO add settings launcher
            this.launchIntent = new Preference(context).getIntent();
        }
        this.context = context;
    }

    public void move (int deltaTime) {
        pos.left += deltaTime / 1000;
    }

    public void launch () {
        context.startActivity(launchIntent);
    }

}
