package cn.inu1255.we;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.Gravity;
import android.webkit.WebView;
import android.widget.Toast;

import com.hudun.swdt.R;
import com.mobile.auth.gatewayauth.AuthRegisterXmlConfig;
import com.mobile.auth.gatewayauth.AuthUIConfig;
import com.mobile.auth.gatewayauth.AuthUIControlClickListener;
import com.mobile.auth.gatewayauth.PhoneNumberAuthHelper;
import com.mobile.auth.gatewayauth.TokenResultListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.inu1255.we.tools.ActivityTool;
import cn.inu1255.we.tools.ITool;

import static android.content.Context.BATTERY_SERVICE;

public class We {
	// 通用端
	private static int pid = 0;
	private static Map<Integer, java.lang.Process> pmap = new HashMap<>();
	private static Map<String, Method> methods;
	public static CallbackContext ondataContext;
	private static TokenResultListener tokenResultListener;

	public static void init(Context context) {
		if (methods != null) return;
		methods = new HashMap<>();
		for (Method method : We.class.getMethods()) {
			if (Modifier.isPublic(method.getModifiers())) {
				method.setAccessible(true);
				methods.put(method.getName(), method);
			}
		}
		tokenResultListener = new TokenResultListener() {
			@Override
			public void onTokenSuccess(String s) {
				We.emit("onekey", s);
			}

			@Override
			public void onTokenFailed(String s) {
				We.emit("onekey", s);
			}
		};
	}

	// 通用端
	public static String getAllMethods() {
		StringBuilder sb = null;
		for (String s : methods.keySet()) {
			if (sb == null) sb = new StringBuilder(s);
			else sb.append(',').append(s);
		}
		return sb.toString();
	}

	//region base methods
	private static Context getContext() {
		Context ctx = ActivityTool.getContext();
		return ctx;
	}

	public static String getAppName() {
		int labelRes = getContext().getApplicationInfo().labelRes;
		return getContext().getString(labelRes);
	}

	public static void shareText(String text) {
		Intent send = new Intent(Intent.ACTION_SEND);
		send.setType("text/plain");
		send.putExtra(Intent.EXTRA_TEXT, text);
		ActivityTool.startActivity(Intent.createChooser(send, getAppName()));
	}

	public static void shareFile(String filepath, String type) {
		Intent send = new Intent();
		send.setAction(Intent.ACTION_SEND);
		Uri uri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", new File(filepath));
		send.putExtra(Intent.EXTRA_STREAM, uri);
		if (TextUtils.isEmpty(type)) {
			if (filepath.endsWith("png")) type = "image/png";
			else if (filepath.endsWith("jpg")) type = "image/jpeg";
		}
		if (!TextUtils.isEmpty(type))
			send.setType(type);
		ActivityTool.startActivity(Intent.createChooser(send, getAppName()));
	}

	public static boolean saveImageToGallery(String filepath, String title, String description) {
		Context context = getContext();
		try {
			MediaStore.Images.Media.insertImage(context.getContentResolver(), filepath, title, description);
			return true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	private static PhoneNumberAuthHelper getOneKey() {
		Context context = getContext();
		return PhoneNumberAuthHelper.getInstance(context, tokenResultListener);
	}

	public static String getCurrentCarrierName() {
		return getOneKey().getCurrentCarrierName();
	}

	public static void quitLoginPage() {
		getOneKey().quitLoginPage();
	}

	//	type 1：本机号码校验 2: ⼀键登录
	public static void checkEnvAvailable(int type) {
		getOneKey().checkEnvAvailable(type == 0 ? 2 : type);
	}

	public static void setAuthSDKInfo(String s) {
		getOneKey().setAuthSDKInfo(s);
	}

	public static JSONObject setAuthUIConfig(JSONObject config) {
		AuthUIConfig.Builder builder = new AuthUIConfig.Builder();
		JSONObject ret = ITool.applyAll(builder, config);
		getOneKey().setAuthUIConfig(builder.create());
		return ret;
	}

	public static void setAuthXMLConfig(JSONObject config) {
		PhoneNumberAuthHelper oneKey = getOneKey();
		oneKey.removeAuthRegisterXmlConfig();
		oneKey.removeAuthRegisterViewConfig();
		AuthRegisterXmlConfig.Builder xmlConfig = new AuthRegisterXmlConfig.Builder();
		PnsViewDelegate pnsViewDelegate = new PnsViewDelegate(config);
		xmlConfig.setLayout(R.layout.fragment_auth_login, pnsViewDelegate);
		oneKey.addAuthRegisterXmlConfig(xmlConfig.build());
		oneKey.setUIClickListener((code, context1, s1) -> {
			switch (code) {
				case "700002": // 点击一键登录
					pnsViewDelegate.checkLogin();
					break;
			}
		});
	}

	@CallbackFunction
	public static void getLoginToken(int timeout, JSCallback cb) {
		getOneKey().getLoginToken(getContext(), timeout);
	}

	public static int setDebug(int debug) {
		ActivityTool.runOnUiThread(() -> {
			WebView.setWebContentsDebuggingEnabled(debug > 0);
		});
		return 1;
	}

	public static void emit(String type, String s) {
		if (ondataContext != null) {
			try {
				PluginResult result = new PluginResult(PluginResult.Status.OK, type + "\n" + s);
				result.setKeepCallback(true);
				ondataContext.sendPluginResult(result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static String deviceInfo() {
		StringBuilder sb = new StringBuilder();
		sb.append(Build.PRODUCT).append('\n');
		sb.append(Build.BRAND).append('\n');
		sb.append(Build.MODEL).append('\n');
		sb.append(Build.FINGERPRINT).append('\n');
		sb.append(Build.MANUFACTURER).append('\n');
		sb.append(Build.HOST).append('\n');
		sb.append(Build.VERSION.RELEASE).append('\n');
		sb.append(Build.HARDWARE).append('\n');
		return sb.toString();
	}

	private static String packageInfo2String(PackageInfo packageInfo, PackageManager manager) {
		StringBuilder sb = new StringBuilder();
		// flags
		sb.append(packageInfo.applicationInfo.flags);
		sb.append(',');
		// appname
		sb.append(packageInfo.applicationInfo.loadLabel(manager));
		sb.append(',');
		sb.append(packageInfo.packageName);
		sb.append(',');
		sb.append(packageInfo.versionName);
		sb.append(',');
		sb.append(packageInfo.versionCode);
		sb.append('\n');
		return sb.toString();
	}

	public static String getIPLocal() {
		StringBuilder sb = new StringBuilder();
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						if (sb.length() > 0) sb.append(',');
						sb.append(inetAddress.getHostAddress());
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		return sb.toString();
	}

	public static String getApps(int flag) {
		Context context = getContext();
		if (context == null) return "";
		PackageManager manager = context.getPackageManager();
		List<PackageInfo> packages = manager.getInstalledPackages(0);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < packages.size(); i++) {
			PackageInfo packageInfo = packages.get(i);
			if (flag == 1 && (packageInfo.applicationInfo.flags & 1) == 1)
				continue;
			if (flag == 2 && (packageInfo.applicationInfo.flags & 1) == 0)
				continue;
			String s = packageInfo2String(packageInfo, manager);
			sb.append(s);
		}
		return sb.toString();
	}

	public static int getIp() {
		Context context = getContext();
		if (context == null) return 0;
		try {
			WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			return wifiInfo.getIpAddress();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return 0;
	}

	//endregion

	//region system methods

	@CallbackFunction
	public static void sleep(long ms, JSCallback cb) {
		Context context = getContext();
		new Handler(context.getMainLooper()).postDelayed(() -> cb.success(null), ms);
	}

	public static int getBatteryLevel() {
		int batteryLevel;
		Context context = getContext();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			BatteryManager batteryManager = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
			batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
		} else {
			Intent intent = new ContextWrapper(context.getApplicationContext()).
				registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			batteryLevel = (intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) * 100) /
				intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
		}
		return batteryLevel;
	}

	public static int copy(String text, String label) {
		Context context = getContext();
		if (context == null) return 0;
		ActivityTool.runOnUiThread(() -> {
			ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText(label, text);
			clipboard.setPrimaryClip(clip);
		});
		return 1;
	}

	@CallbackFunction
	public static void paste(JSCallback cb) {
		Context context = getContext();
		if (context == null) {
			cb.success("");
			return;
		}
		ActivityTool.runOnUiThread(() -> {
			ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			if (!clipboard.hasPrimaryClip()) {
				cb.success("");
				return;
			}
			ClipData clipData = clipboard.getPrimaryClip();
			int count = clipData.getItemCount();
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < count; ++i) {
				ClipData.Item item = clipData.getItemAt(i);
				CharSequence str = item.getText();
				if (str != null) sb.append(str);
			}
			cb.success(sb.toString());
		});
	}

	public static int toast(String text, int duration) {
		Context context = getContext();
		if (context == null) return 0;
		ActivityTool.runOnUiThread(() -> {
			Toast.makeText(context, text, duration).show();
		});
		return 1;
	}

	public static int shell(String cmd) {
		try {
			java.lang.Process p = Runtime.getRuntime().exec(cmd);
			pid++;
			pmap.put(pid, p);
		} catch (IOException ignored) {
			return 0;
		}
		return pid;
	}

	public static int shellExitValue(int pid) {
		java.lang.Process p = pmap.get(pid);
		return p.exitValue();
	}

	public static int shellExit(int pid) {
		if (!pmap.containsKey(pid)) return 0;
		java.lang.Process p = pmap.get(pid);
		pmap.remove(pid);
		p.destroy();
		return 1;
	}

	public static String shellRead(int pid, int size, int io) {
		if (!pmap.containsKey(pid)) return "";
		final byte b[] = new byte[size];
		java.lang.Process p = pmap.get(pid);
		try {
			if (io == 0) {
				p.getInputStream().read(b);
			} else {
				p.getErrorStream().read(b);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		return ITool.bytes2Base64(b);
	}

	public static int shellWrite(int pid, String b64) {
		if (!pmap.containsKey(pid)) return 0;
		final byte b[] = ITool.base642Bytes(b64);
		java.lang.Process p = pmap.get(pid);
		try {
			p.getOutputStream().write(b);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		return 1;
	}

	public static String getUserSerial() {
		Context context = getContext();
		Object userManager = context.getSystemService(Context.USER_SERVICE);
		if (null == userManager) return "";
		try {
			Method myUserHandleMethod = android.os.Process.class.getMethod("myUserHandle", (Class<?>[]) null);
			Object myUserHandle = myUserHandleMethod.invoke(android.os.Process.class, (Object[]) null);
			Method getSerialNumberForUser = userManager.getClass().getMethod("getSerialNumberForUser", myUserHandle.getClass());
			Long userSerial = (Long) getSerialNumberForUser.invoke(userManager, myUserHandle);
			if (userSerial != null) {
				return String.valueOf(userSerial);
			} else {
				return "";
			}
		} catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException ignored) {
		}
		return "";
	}

	public static int callService(String pkg, String cls, JSONObject extra) {
		Context context = getContext();
		if (context == null) return 0;
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(pkg, cls));
		if (extra != null) {
			String action = extra.optString("action", null);
			if (action != null) {
				extra.remove("action");
				intent.setAction(action);
			}
			String data = extra.optString("data", null);
			if (data != null) {
				extra.remove("data");
				intent.setData(Uri.parse(data));
			}
			String type = extra.optString("type", null);
			if (type != null) {
				extra.remove("type");
				intent.setType(type);
			}
			int flags = extra.optInt("flags", 0);
			extra.remove("flags");
			intent.setFlags(flags);

			for (Iterator<String> it = extra.keys(); it.hasNext(); ) {
				String k = it.next();
				if (k.startsWith("e")) {
					Object value = extra.opt(k);
					k = k.substring(1);
					if (value instanceof Integer)
						intent.putExtra(k, (Integer) value);
					else if (value instanceof String)
						intent.putExtra(k, (String) value);
					else if (value instanceof Boolean)
						intent.putExtra(k, (Boolean) value);
					else if (value instanceof Float)
						intent.putExtra(k, (Float) value);
				} else if (k.startsWith("c")) {
					intent.addCategory(extra.optString(k));
				}
			}
		}
		return context.getApplicationContext().startService(intent) == null ? 0 : 1;
	}

	public static int isInstall(String pkg) {
		Context context = getContext();
		if (context == null) return 0;
		PackageManager pm = context.getPackageManager();
		int app_installed;
		try {
			pm.getPackageInfo(pkg, PackageManager.GET_GIDS);
			app_installed = 1;
		} catch (PackageManager.NameNotFoundException e) {
			app_installed = 0;
		} catch (RuntimeException e) {
			app_installed = 0;
		}
		return app_installed;
	}

	public static String getApkPath(String packageName) {
		Context context = getContext();
		if (context == null) return "";
		String appDir = "";
		try {
			//通过包名获取程序源文件路径
			appDir = context.getPackageManager().getApplicationInfo(packageName, 0).sourceDir;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return appDir;
	}

	public static String getAppSign(String pkgname, String algorithm) {
		Context context = getContext();
		if (context == null) return "";
		return ITool.getAppSign(context, pkgname, algorithm);
	}

	public static String md5(String s, String algorithm) {
		return ITool.md5(s, algorithm);
	}

	private static File getFile(String name) {
		Context context = getContext();
		if (name.startsWith("file:///android_asset/")) {
			File outFile = getFile("cache://" + name.substring(8));
			outFile.getParentFile().mkdirs();
			try {
				InputStream myInput = context.getAssets().open(name.substring(22));
				FileOutputStream myOutput = new FileOutputStream(outFile);
				byte[] buffer = new byte[1024];
				int length;
				while ((length = myInput.read(buffer)) > 0) {
					myOutput.write(buffer, 0, length);
				}
				myOutput.flush();
				myInput.close();
				myOutput.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return outFile;
		}
		if (name.startsWith("file://"))
			return new File(name.substring(7));
		if (name.startsWith("files://"))
			return new File(context.getFilesDir(), name.substring(8));
		if (name.startsWith("cache://"))
			return new File(context.getCacheDir(), name.substring(8));
		if (name.startsWith("sdcard://"))
			return new File(Environment.getExternalStorageDirectory(), name.substring(9));
		if (name.startsWith("efiles://"))
			return new File(context.getExternalFilesDir(null), name.substring(9));
		if (name.startsWith("ecache://"))
			return new File(context.getExternalCacheDir(), name.substring(9));
		return new File(context.getFilesDir(), name);
	}

	public static String getFilePath(String name) {
		return getFile(name).getAbsolutePath();
	}

	public static boolean mkdirs(String name) {
		File file = getFile(name);
		return file.mkdirs();
	}

	public static String readFile(String name) {
		File file = getFile(name);
		try {
			FileInputStream inputStream = new FileInputStream(file);
			int size = inputStream.available();
			byte[] bs = new byte[size];
			inputStream.read(bs);
			return ITool.bytes2Base64(bs);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		return "";
	}

	public static boolean writeFile(String name, String b64) {
		File file = getFile(name);
		if (b64 == null) {
			return (file.delete());
		}
		try {
			FileOutputStream stream = new FileOutputStream(file);
			byte[] bytes = ITool.base642Bytes(b64);
			stream.write(bytes);
			return true;
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}
		return false;
	}

	public static int copyFile(String src, String dst) {
		File srcfile = getFile(src);
		File dstfile = getFile(dst);
		int r, total = 0;
		try {
			FileInputStream is = new FileInputStream(srcfile);
			FileOutputStream os = new FileOutputStream(dstfile);
			byte[] bs = new byte[1024];
			while ((r = is.read(bs)) > 0) {
				os.write(bs);
				total += r;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return total;
	}

	// endregion

	// region ui methods
	public static String screenSize() {
		return new StringBuilder().append(ITool.width).append(",").append(ITool.height).append(",").append(ITool.nav_height).append(",").append(ITool.btn_height).append(",").append(ITool.dpi).toString();
	}

	public static int addFlags(int flag) {
		return ActivityTool.addFlags(flag);
	}

	public static int clearFlags(int flag) {
		return ActivityTool.clearFlags(flag);
	}

	public static int setSystemUiVisibility(int flag) {
		return ActivityTool.setSystemUiVisibility(flag);
	}

	public static int setStatusBarColor(int color) {
		return ActivityTool.setStatusBarColor(color);
	}

	public static Method getMethod(String action) {
		return methods.get(action);
	}

}
