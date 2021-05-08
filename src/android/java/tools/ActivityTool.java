package cn.inu1255.we.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.Window;

import org.apache.cordova.CordovaPlugin;

public class ActivityTool {

	private static CordovaPlugin plugin;
	private static Activity activity;

	public static void setPlugin(CordovaPlugin p) {
		plugin = p;
		activity = plugin.cordova.getActivity();
		ITool.init(activity);
	}

	public static boolean isConnected() {
		return activity != null;
	}

	public static Context getContext() {
		return activity;
	}

	public static void startActivityForResult(Intent intent, int code) {
		if (plugin.cordova != null && plugin != null)
			plugin.cordova.startActivityForResult(plugin, intent, code);
	}

	public static void runOnUiThread(Runnable action) {
		Activity activity = plugin.cordova.getActivity();
		if (activity != null)
			activity.runOnUiThread(action);
	}

	public static int addFlags(int flag) {
		if (activity == null) return 0;
		activity.runOnUiThread(() -> activity.getWindow().addFlags(flag));
		return 1;
	}

	public static int clearFlags(int flag) {
		if (activity == null) return 0;
		activity.runOnUiThread(() -> activity.getWindow().clearFlags(flag));
		return 1;
	}

	public static int setSystemUiVisibility(int flag) {
		if (activity == null) return 0;
		activity.runOnUiThread(() -> {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				activity.getWindow().getDecorView().setSystemUiVisibility(flag);
			}
		});
		return 1;
	}

	public static int setStatusBarColor(int color) {
		if (activity == null) return 0;
		activity.runOnUiThread(() -> {
			Window window = activity.getWindow();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				window.setStatusBarColor(color);
				window.setNavigationBarColor(color);
			}
		});
		return 1;
	}

	public static void startActivity(Intent intent) {
		if (activity != null)
			activity.startActivity(intent);
	}
}
