package cn.inu1255.we;

import android.app.Activity;
import android.webkit.WebView;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cn.inu1255.we.tools.ActivityTool;
import cn.inu1255.we.tools.ITool;

/**
 * This class echoes a string called from JavaScript.
 */
public class WePlugin extends CordovaPlugin {

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		Activity activity = cordova.getActivity();
		ActivityTool.setPlugin(this);
		We.init(activity);
		try {
			WebView wv = (WebView) this.webView.getView();
			wv.getSettings().setTextZoom(100);
		} catch (Exception e) {
		}
	}

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
		if (action.equals("ondata")) {
			PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
			pluginResult.setKeepCallback(true);
			callbackContext.sendPluginResult(pluginResult);
			We.ondataContext = callbackContext;
			return true;
		}
		if (action.equals("init")) {
			callbackContext.success(We.getAllMethods());
			return true;
		}
		Method method = We.getMethod(action);
		if (method == null) return false;
		this.cordova.getThreadPool().execute(() -> {
			Object[] params = ITool.makeParams(method, args, data -> ITool.callback(callbackContext, data));
			try {
				if (ITool.isCallbackMethod(method))
					method.invoke(null, params);
				else
					ITool.callback(callbackContext, method.invoke(null, params));
				return;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				callbackContext.error(e.getMessage());
			} catch (InvocationTargetException e) {
				Throwable te = e.getTargetException();
				if (te != null) {
					te.printStackTrace();
					callbackContext.error(te.toString());
				} else {
					e.printStackTrace();
					callbackContext.error(e.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
				callbackContext.error(e.toString());
			}
		});
		return true;
	}
}
