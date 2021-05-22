package cn.inu1255.we.tools;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.mobile.auth.gatewayauth.AuthUIConfig;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import cn.inu1255.we.CallbackFunction;
import cn.inu1255.we.JSCallback;

public class ITool {
	private static String TAG = "iTool";
	public static int width;
	public static int height; // 总高度
	public static int nav_height = 0; // 顶部高度
	public static int btn_height = 0; // 底部高度
	public static int dpi = 1;

	public static void init(Context context) {
		WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Display defaultDisplay = windowManager.getDefaultDisplay();
		DisplayMetrics metrics = new DisplayMetrics();
		defaultDisplay.getRealMetrics(metrics);
		Resources resources = context.getResources();
		int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
		nav_height = resources.getDimensionPixelSize(resourceId);
		btn_height = getNavigationBarHeight(context);
		width = metrics.widthPixels;
		height = metrics.heightPixels;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.DONUT) {
			dpi = metrics.densityDpi;
		}
	}

	public static void info(Object msg) {
		Log.i(TAG, msg == null ? "null" : msg.toString());
	}

	public static void warn(Object msg) {
		Log.w(TAG, msg == null ? "null" : msg.toString());
	}

	public static void error(Object msg) {
		Log.e(TAG, msg == null ? "null" : msg.toString());
	}

	public static void callback(CallbackContext cb, Object data) {
		if (data == null)
			cb.success();
		else if (data instanceof Integer)
			cb.success((Integer) data);
		else if (data instanceof String)
			cb.success((String) data);
		else if (data instanceof Boolean)
			cb.success((Boolean) data ? 1 : 0);
		else if (data instanceof Double)
			cb.success(data.toString());
		else if (data instanceof JSONObject)
			cb.success((JSONObject) data);
		else if (data instanceof JSONArray)
			cb.success((JSONArray) data);
		else cb.success();
	}

	/**
	 * 非全面屏下 虚拟键高度(无论是否隐藏)
	 */
	public static int getNavigationBarHeight(Context context) {
		int result = 0;
		int resourceId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = context.getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	public static String streamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static boolean isCallbackMethod(Method method) {
		return method.getAnnotation(CallbackFunction.class) != null;
	}

	public static Context getPackageContext(Context context, String packageName) {
		Context pkgContext = null;
		if (context.getPackageName().equals(packageName)) {
			pkgContext = context;
		} else {
			// 创建第三方应用的上下文环境
			try {
				pkgContext = context.createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE);
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		return pkgContext;
	}

	public static Object[] makeParams(Method method, JSONArray args, JSCallback cb) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		Object[] params = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> cls = parameterTypes[i];
			switch (cls.getSimpleName()) {
				case "int":
					params[i] = args.optInt(i);
					break;
				case "String":
					params[i] = args.isNull(i) ? null : args.optString(i);
					break;
				case "boolean":
					params[i] = args.optBoolean(i);
					break;
				case "float":
					params[i] = (float) args.optDouble(i);
					break;
				case "double":
					params[i] = args.optDouble(i);
					break;
				case "JSONObject":
					params[i] = args.optJSONObject(i);
					break;
				case "JSONArray":
					params[i] = args.optJSONArray(i);
					break;
				case "long":
					params[i] = args.optLong(i);
					break;
				case "Drawable":
					Object opt = args.opt(i);
					if (opt instanceof Integer) {
						ColorDrawable drawable = new ColorDrawable();
						drawable.setColor((Integer) opt);
						params[i] = drawable;
					} else {

					}
					break;
				case "Object":
				case "JSCallback":
				default:
					if (isCallbackMethod(method) && i == parameterTypes.length - 1) {
						params[i] = cb;
					} else {
						params[i] = args.opt(i);
					}
					break;
			}
		}
		return params;
	}

	public static String getUrlHash(String url) {
		int i = url.indexOf('?');
		String s = (i >= 0) ? url.substring(0, i) : url;
		i = s.lastIndexOf('.');
		return md5(url, null) + (i >= 0 ? s.substring(i) : "");
	}


	public static File downloadFile(final String urlpath, String filepath) {
		File file = new File(filepath);
		if (file.exists() && file.length() > 0) {
			return file;
		}
		try {
			URL url = new URL(urlpath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(30000);
			conn.setRequestMethod("GET");
			if (conn.getResponseCode() == 200) {
				InputStream is = null;
				byte[] buf = new byte[2048];
				int len = 0;
				FileOutputStream fos = null;
				// 储存下载文件的目录
				try {
					fos = new FileOutputStream(file);
					while ((len = is.read(buf)) != -1) {
						fos.write(buf, 0, len);
					}
					fos.flush();
					return file;
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						if (is != null)
							is.close();
					} catch (IOException e) {
					}
					try {
						if (fos != null)
							fos.close();
					} catch (IOException e) {
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Bitmap drawable2Bitmap(Drawable icon) {
		Bitmap bitmap;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && icon instanceof AdaptiveIconDrawable) {
			bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			icon.draw(canvas);
		} else {
			bitmap = ((BitmapDrawable) icon).getBitmap();
		}
		return bitmap;
	}

	public static String drawable2Base64(Drawable icon) {
		Bitmap bitmap = drawable2Bitmap(icon);
		return bitmap2Base64(bitmap);
	}

	public static byte[] readBitmap(Bitmap bitmap) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}

	public static String bytes2Base64(byte[] bytes) {
		return Base64.encodeToString(bytes, Base64.DEFAULT).replaceAll("\n", "");
	}

	public static String bitmap2Base64(Bitmap bitmap) {
		byte[] bytes = readBitmap(bitmap);
		return bytes2Base64(bytes);
	}

	public static Bitmap base642Bitmap(String base64) {
		byte[] bitmapByte = Base64.decode(base64, Base64.DEFAULT);
		return BitmapFactory.decodeByteArray(bitmapByte, 0, bitmapByte.length);
	}

	public static String isToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line);
				sb.append("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static boolean isCallback(String action) {
		return action.startsWith("cb:");
	}

	public static String randomCallback(String key) {
		return new StringBuilder().append("cb:").append(key).append(new Date().getTime() % 86400000).append('.').append((int) (Math.random() * 10000)).append("\n").toString();
	}

	public static MediaCodec getVideoMediaCodec() {
		MediaFormat format = MediaFormat.createVideoFormat("video/avc", 720, 1280);
		//设置颜色格式
		format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
			MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		//设置比特率(设置码率，通常码率越高，视频越清晰)
		format.setInteger(MediaFormat.KEY_BIT_RATE, 1000 * 1024);
		//设置帧率
		format.setInteger(MediaFormat.KEY_FRAME_RATE, 10);
		//关键帧间隔时间，通常情况下，你设置成多少问题都不大。
		format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
		// 当画面静止时,重复最后一帧，不影响界面显示
		format.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / 45);
		format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
		//设置复用模式
		format.setInteger(MediaFormat.KEY_COMPLEXITY, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
		MediaCodec mediaCodec = null;
		try {
//            MediaRecorder mediaRecorder = new MediaRecorder();
			mediaCodec = MediaCodec.createEncoderByType("video/avc");
			mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		} catch (Exception e) {
			e.printStackTrace();
			if (mediaCodec != null) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					mediaCodec.reset();
				}
				mediaCodec.stop();
				mediaCodec.release();
				mediaCodec = null;
			}
		}
		return mediaCodec;
	}

	public static Bitmap getBitmapFromView(View v) {
		Bitmap b = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.RGB_565);
		Canvas c = new Canvas(b);
		v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
		// Draw background
		Drawable bgDrawable = v.getBackground();
		if (bgDrawable != null) {
			bgDrawable.draw(c);
		} else {
			c.drawColor(Color.WHITE);
		}
		// Draw view to canvas
		v.draw(c);
		return b;
	}

	public static byte[] base642Bytes(String base64) {
		try {
			return Base64.decode(base64, Base64.DEFAULT);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static String getAppSign(Context context, String pkgname, String algorithm) {
		//获取原始签名
		Signature[] signs = getRawSignature(context, pkgname);
		try {
			return getSignValidString(signs[0].toByteArray(), algorithm);
		} catch (Exception e) {
		}
		return "";
	}

	private static String getSignValidString(byte[] paramArrayOfByte, String algorithm) throws NoSuchAlgorithmException {
		MessageDigest localMessageDigest = MessageDigest.getInstance(algorithm);
		localMessageDigest.update(paramArrayOfByte);
		return toHexString(localMessageDigest.digest());
	}

	public static String toHexString(byte[] paramArrayOfByte) {
		if (paramArrayOfByte == null) {
			return null;
		}
		StringBuilder localStringBuilder = new StringBuilder(2 * paramArrayOfByte.length);
		for (int i = 0; ; i++) {
			if (i >= paramArrayOfByte.length) {
				return localStringBuilder.toString();
			}
			String str = Integer.toString(0xFF & paramArrayOfByte[i], 16);
			if (str.length() == 1) {
				localStringBuilder.append("0");
			}
			localStringBuilder.append(str);
		}
	}

	private static Signature[] getRawSignature(Context context, String packageName) {
		if (packageName == null || packageName.length() == 0) {
			return null;
		}
		try {
			PackageInfo info = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
			if (info != null) {
				return info.signatures;
			}
			//errout("info is null, packageName = " + packageName);
			return null;
		} catch (PackageManager.NameNotFoundException e) {
			//errout("NameNotFoundException");
			return null;
		}
	}

	public static int getIdByName(String packageName, String className, String name) {
		Class r = null;
		int id = 0;
		try {
			r = Class.forName(packageName + ".R");
			Class[] classes = r.getClasses();
			Class desireClass = null;
			for (int i = 0; i < classes.length; ++i) {
				if (classes[i].getName().split("\\$")[1].equals(className)) {
					desireClass = classes[i];
					break;
				}
			}
			if (desireClass != null)
				id = desireClass.getField(name).getInt(desireClass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return id;
	}

	public static JSONObject applyAll(Object obj, JSONObject config) {
		JSONObject ret = new JSONObject();
		if (obj == null) return ret;
		Class<?> cls = obj.getClass();
		Method[] methods = cls.getMethods();
		HashMap<String, Method> map = new HashMap<>();
		for (Method method : methods) {
			map.put(method.getName(), method);
		}
		Iterator<String> keys = config.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			Method method = map.get(key);
			if (method == null) continue;
			Object[] objects = ITool.makeParams(method, config.optJSONArray(key), null);
			try {
				ret.put(key, method.invoke(obj, objects));
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	public static String md5(String s, String algorithm) {
		if (TextUtils.isEmpty(s))
			return "";
		if (TextUtils.isEmpty(algorithm))
			algorithm = "MD5";
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance(algorithm);
			byte[] bytes = digest.digest(s.getBytes());
			return ITool.toHexString(bytes);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}
}
