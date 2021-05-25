package cn.inu1255.we;

import android.animation.ObjectAnimator;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.hudun.swdt.R;
import com.mobile.auth.gatewayauth.ui.AbstractPnsViewDelegate;

import org.json.JSONObject;

import cn.inu1255.we.tools.AnimUtils;

public class PnsViewDelegate extends AbstractPnsViewDelegate {
	private static ObjectAnimator objAnim;
	private final JSONObject config;
	private CheckBox checkBox;

	public PnsViewDelegate(JSONObject config) {
		this.config = config;
	}

	boolean checkLogin() {
		//隐藏状态时  表示直接通过
		if (checkBox == null || checkBox.getVisibility() != View.VISIBLE || checkBox.isChecked())
			return true;
		if (!objAnim.isRunning())
			objAnim.start();
		We.toast("勾选并同意协议才可登录", 0);
		return false;
	}

	@Override
	public void onViewCreated(View view) {

		View wechatLogin = findViewById(R.id.btn_wechat_login);
		View qqLogin = findViewById(R.id.btn_qq_login);
		View phoneLogin = findViewById(R.id.btn_phone_login);
		ImageView ivLogo = (ImageView) findViewById(R.id.iv_logo);

		if (config.optBoolean("wechatLogin"))
			wechatLogin.setVisibility(View.VISIBLE);
		else
			wechatLogin.setVisibility(View.GONE);
		if (config.optBoolean("qqLogin"))
			qqLogin.setVisibility(View.VISIBLE);
		else
			qqLogin.setVisibility(View.GONE);
		if (config.optBoolean("phoneLogin"))
			phoneLogin.setVisibility(View.VISIBLE);
		else
			phoneLogin.setVisibility(View.GONE);

		ivLogo.setImageResource(R.mipmap.ic_launcher);

		view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
			@Override
			public void onViewAttachedToWindow(View v) {
				View root = v.getRootView();
				View mProtocolView = root.findViewById(R.id.authsdk_protocol_view);
				checkBox = root.findViewById(R.id.authsdk_checkbox_view);
				if (mProtocolView != null) {
					objAnim = AnimUtils.getShakeByPropertyAnim(mProtocolView, 1f, 1f, 2f, 800);
					RelativeLayout.LayoutParams rl = (RelativeLayout.LayoutParams) mProtocolView.getLayoutParams();
					rl.addRule(RelativeLayout.BELOW, R.id.authsdk_login_view);
					rl.topMargin = 60;
				}
			}

			@Override
			public void onViewDetachedFromWindow(View view) {
				objAnim = null;
				checkBox = null;
			}
		});

		findViewById(R.id.img_exit_wechat).setOnClickListener(view1 -> {
			We.emit("goto", "-1");
		});

		wechatLogin.setOnClickListener(view1 -> {
			if (checkLogin())
				We.emit("goto", "wechatLogin");
		});
		qqLogin.setOnClickListener(view1 -> {
			if (checkLogin())
				We.emit("goto", "qqLogin");
		});
		phoneLogin.setOnClickListener(view1 -> {
			if (checkLogin())
				We.emit("goto", "phoneLogin");
		});
	}
}
