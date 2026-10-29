package com.aether.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
        hideBars();
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override protected void onResume() { super.onResume(); hideBars(); web.requestFocus(); web.evaluateJavascript("window.refreshApps&&refreshApps()", null); }
    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); web.evaluateJavascript("window.goHome&&goHome()", null); }
    @Override public void onBackPressed() { /* launcher: do nothing */ }
    @Override public boolean onKeyDown(int k, KeyEvent e) { return super.onKeyDown(k, e); }

    class Bridge {
        @JavascriptInterface public String getApps() {
            PackageManager pm = getPackageManager();
            Map<String, String> out = new LinkedHashMap<>();
            String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
            for (String c : cats) {
                Intent q = new Intent(Intent.ACTION_MAIN).addCategory(c);
                for (ResolveInfo r : pm.queryIntentActivities(q, 0)) {
                    String pkg = r.activityInfo.packageName;
                    if (pkg.equals(getPackageName()) || out.containsKey(pkg)) continue;
                    out.put(pkg, String.valueOf(r.loadLabel(pm)));
                }
            }
            List<Map.Entry<String, String>> l = new ArrayList<>(out.entrySet());
            Collections.sort(l, new Comparator<Map.Entry<String, String>>() {
                public int compare(Map.Entry<String, String> a, Map.Entry<String, String> b) { return a.getValue().compareToIgnoreCase(b.getValue()); }
            });
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append("{\"p\":\"").append(l.get(i).getKey()).append("\",\"n\":\"")
                  .append(l.get(i).getValue().replace("\\", "\\\\").replace("\"", "\\\"")).append("\"}");
            }
            return sb.append(']').toString();
        }

        @JavascriptInterface public String getIcon(String pkg) {
            try {
                Drawable d = getPackageManager().getApplicationIcon(pkg);
                Bitmap bm = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bm);
                d.setBounds(0, 0, 128, 128);
                d.draw(c);
                ByteArrayOutputStream o = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.PNG, 100, o);
                bm.recycle();
                return "data:image/png;base64," + Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP);
            } catch (Exception e) { return ""; }
        }

        @JavascriptInterface public void launch(String pkg) {
            PackageManager pm = getPackageManager();
            Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (i == null) i = pm.getLaunchIntentForPackage(pkg);
            if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); }
        }

        @JavascriptInterface public void openSettings() {
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
