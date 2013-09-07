package org.gscript.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/* Simple Checkable LinearLayout used for backwards compatibility because the activated state 
 * for checked items in listviews is not available on pre-HONEYCOMB (SDK-11) devices */

public class CheckableListItem extends LinearLayout implements
		android.widget.Checkable {

	boolean mChecked = false;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public CheckableListItem(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public CheckableListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CheckableListItem(Context context) {
		super(context);
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		mChecked = checked;
		
		updateCheckedState();
	}

	@Override
	public void toggle() {
		mChecked = !mChecked;
		
		updateCheckedState();
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		
		updateCheckedState();
	}

	void updateCheckedState() {
		
		Drawable drawable = this.getBackground();

		if (drawable instanceof StateListDrawable) {
			StateListDrawable list = (StateListDrawable) drawable;
			list.setState(new int[] { 
					this.isChecked() 	? android.R.attr.state_checked	: 0,
					this.isSelected() 	? android.R.attr.state_selected	: 0,
					this.isPressed() 	? android.R.attr.state_pressed	: 0,
					this.isFocused() 	? android.R.attr.state_focused	: 0,
					this.isEnabled() 	? android.R.attr.state_enabled	: 0,
					});
		}
	}
}
