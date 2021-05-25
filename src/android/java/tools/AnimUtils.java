package cn.inu1255.we.tools;

import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;

public final class AnimUtils {
	public static final ObjectAnimator getShakeByPropertyAnim(View view, float scaleSmall, float scaleLarge, float shakeDegrees, long duration) {
		PropertyValuesHolder scaleXValuesHolder = PropertyValuesHolder.ofKeyframe(View.SCALE_X, new Keyframe[]{Keyframe.ofFloat(0.0F, 1.0F), Keyframe.ofFloat(0.25F, scaleSmall), Keyframe.ofFloat(0.5F, scaleLarge), Keyframe.ofFloat(0.75F, scaleLarge), Keyframe.ofFloat(1.0F, 1.0F)});
		PropertyValuesHolder scaleYValuesHolder = PropertyValuesHolder.ofKeyframe(View.SCALE_Y, new Keyframe[]{Keyframe.ofFloat(0.0F, 1.0F), Keyframe.ofFloat(0.25F, scaleSmall), Keyframe.ofFloat(0.5F, scaleLarge), Keyframe.ofFloat(0.75F, scaleLarge), Keyframe.ofFloat(1.0F, 1.0F)});
		PropertyValuesHolder rotateValuesHolder = PropertyValuesHolder.ofKeyframe(View.ROTATION, new Keyframe[]{Keyframe.ofFloat(0.0F, 0.0F), Keyframe.ofFloat(0.2F, -shakeDegrees), Keyframe.ofFloat(0.4F, shakeDegrees), Keyframe.ofFloat(0.6F, -shakeDegrees), Keyframe.ofFloat(0.8F, shakeDegrees), Keyframe.ofFloat(1.0F, 0.0F)});
		ObjectAnimator var10000 = ObjectAnimator.ofPropertyValuesHolder(view, new PropertyValuesHolder[]{scaleXValuesHolder, scaleYValuesHolder, rotateValuesHolder});
		ObjectAnimator objectAnimator = var10000;
		objectAnimator.setDuration(duration);
		return objectAnimator;
	}
}
