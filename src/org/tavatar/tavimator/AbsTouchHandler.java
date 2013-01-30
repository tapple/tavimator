/**
 * 
 */
package org.tavatar.tavimator;

import android.content.Context;

/**
 * @author tapple
 *
 */
public abstract class AbsTouchHandler implements AnimationTouchHandler {
	private Context mContext;
	private int mNameId, mShortNameId;

	public AbsTouchHandler(Context context, int nameId, int shortNameId) {
		mContext = context;
		mNameId = nameId;
		mShortNameId = shortNameId;
	}

	@Override
	public String shortToolName() {
		return mContext.getResources().getString(mShortNameId);
	}

	@Override
	public String toolName() {
		return mContext.getResources().getString(mNameId);
	}

	@Override
	public String toString() {
		return toolName();
	}
}