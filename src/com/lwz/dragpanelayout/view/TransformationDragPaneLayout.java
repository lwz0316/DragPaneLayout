/* Copyright 2015 Liu Wenzhu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lwz.dragpanelayout.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.lwz.dragpanelayout.view.DragPaneLayout.OnPaneStateChangedListener;
import com.nineoldandroids.view.ViewHelper;

/**
 * 添加变换动画的 可拖动容器布局
 * @author Liu Wenzhu<lwz0316@gmail.com>
 * 2015-3-12 下午4:29:54
 */
public class TransformationDragPaneLayout extends DragPaneLayout implements OnPaneStateChangedListener {

	private View mDragPane;
	private View mSecondaryPane;
	private OnPaneStateChangedListener mPaneStateChangedListener;
	
	private float mDragPaneScale = 0.8f;
	
	public TransformationDragPaneLayout(Context context) {
		this(context, null);
	}

	public TransformationDragPaneLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TransformationDragPaneLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		super.setOnPaneStateChangedListener(this);
	}
	
	@Override
	public void setDragPane(View pane) {
		super.setDragPane(pane);
		mDragPane = pane;
	}
	
	public void setSecondaryView(int viewId) {
		setSecondaryPane(findViewById(viewId));
	}
	
	public void setSecondaryPane(View pane) {
		mSecondaryPane = pane;
	}
	
	@Override
	public void setOnPaneStateChangedListener(OnPaneStateChangedListener l) {
		mPaneStateChangedListener = l;
	}

	@Override
	public void onPaneClosed() {
		if( mPaneStateChangedListener != null ) {
			mPaneStateChangedListener.onPaneClosed();
		}
	}

	@Override
	public void onPaneOpened(Mode mode, float offset) {
		if( mPaneStateChangedListener != null ) {
			mPaneStateChangedListener.onPaneOpened(mode, offset);
		}
	}

	@Override
	public void onPaneDragged(Mode mode, float offset) {
		transDragPane(offset);
		transSecondaryPane(offset);
		transBackground(offset);
		
		if( mPaneStateChangedListener != null ) {
			mPaneStateChangedListener.onPaneDragged(mode, offset);
		}
	}
	
	private void transDragPane(float offset) {
		float absOffset = Math.abs(offset);
		float scale = (mDragPaneScale - 1) * absOffset + 1;
		ViewHelper.setScaleX(mDragPane, scale);
		ViewHelper.setScaleY(mDragPane, scale);
	}
	
	private void transSecondaryPane(float offset) {
		float absOffset = Math.abs(offset);
		int secondaryViewWidth = mSecondaryPane.getMeasuredWidth();
		float transX = (secondaryViewWidth >> 1) * (1 - absOffset );
		float scaleY = 0.2f * absOffset + mDragPaneScale;
		if( mSecondaryPane != null ) {
			ViewHelper.setTranslationX(mSecondaryPane, transX);
			ViewHelper.setScaleY(mSecondaryPane, scaleY);
			ViewHelper.setScaleX(mSecondaryPane, scaleY);
		}
	}
	
	private void transBackground(float offset) {
		float absOffset = Math.abs(offset);
		Drawable background = getBackground();
		if( background != null ) {
			background.setColorFilter(evaluate(absOffset, Color.argb(0x99, 0, 0, 0), Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);
		}
	}
	
	private Integer evaluate(float fraction, Object startValue, Integer endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;
        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;
        return (int) ((startA + (int) (fraction * (endA - startA))) << 24)
                | (int) ((startR + (int) (fraction * (endR - startR))) << 16)
                | (int) ((startG + (int) (fraction * (endG - startG))) << 8)
                | (int) ((startB + (int) (fraction * (endB - startB))));
    }

}
