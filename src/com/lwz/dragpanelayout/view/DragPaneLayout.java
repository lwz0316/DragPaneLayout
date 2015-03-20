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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewGroupCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 可拖动的容器布局
 * @author Liu Wenzhu<lwz0316@gmail.com>
 * 2015-3-5 下午1:58:42
 */
public class DragPaneLayout extends FrameLayout {
	
	/**
	 * 打开面板的模式
	 */
	public static enum Mode {
		/** 左侧打开 */
		LEFT,
		/** 右侧打开 */
		RIGHT,
		/** 左右都可以打开*/
		BOTH
	}
	
	public static interface OnPaneStateChangedListener {
		/**
		 * 关闭
		 */
		public void onPaneClosed();
		
		/**
		 * 打开状态
		 * @param mode {@link Mode}
		 */
		public void onPaneOpened(Mode mode, float offset);
		
		/**
		 * @param mode
		 * @param offset
		 * 	<li>当 mode = {@link Mode#RIGHT} 时，offset 的值为 [-1.0f, 0]
		 * 	<li>当 mode = {@link Mode#LEFT} 时， offset 的值为  [0, 1.0f]
		 * 	<li>当 mode = {@link Mode#BOTH} 时， offset 的值为  [-1.0f, 1]
		 */
		public void onPaneDragged(Mode mode, float offset);
		
	}
	
	public class OnSimplePanelStateChangedListener implements OnPaneStateChangedListener {

		@Override public void onPaneClosed() {
		}

		@Override public void onPaneOpened(Mode mode, float offset) {
		}

		@Override public void onPaneDragged(Mode mode, float offset) {
		}
		
	}
	
	private static final float TOUCH_SLOP_SENSITIVITY = 0.5f;
	
	/**
	 * 不允许父视图阻断触摸事件的最大Y轴偏移量，超过了这个偏移量就允许父视图阻止触摸事件。
	 * 通过 {@link #requestDisallowInterceptTouchEvent(boolean)} 来生效。
	 * 作用是防止有滚动条的父视图干扰触摸事件
	 */
	private static final float DISALLOW_INTECEPT_TOUCH_EVNET_MAX_Y_OFFSET = 20;
    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second
    
	private ViewDragHelper mDragHelper;
	private ViewDragCallback mViewDragCallback;
	private View mDragPane;
	private Mode mMode = Mode.RIGHT;
	private int mDragRange;
	private int mDragLeft;
	private float mDragOffset;
	final float mDensity;
	/** 拖动是否可以开启 */
	private boolean mDragOpenable = true;
	Rect mDragViewVisibleBounds = new Rect();
	
	/**
     * Stores whether or not the pane was open the last time it was slideable.
     * If open/close operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    private boolean mPreservedOpenState;
    private int mBothModeSildeOffsetState;
    private boolean mFirstLayout = true;
    
    private GestureDetectorCompat mGestureDetector;
    
    private OnPaneStateChangedListener mPaneStateChangedListener;
    private OnPaneStateChangedListener mPaneStateChangedProxy = new OnPaneStateChangedListener() {
		
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
			if( mPaneStateChangedListener != null ) {
				mPaneStateChangedListener.onPaneDragged(mode, offset);
			}
		}
	};
	
	public DragPaneLayout(Context context) {
		this(context, null);
	}

	public DragPaneLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DragPaneLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		mDensity = context.getResources().getDisplayMetrics().density;
		
		mViewDragCallback = new ViewDragCallback();
		mDragHelper = ViewDragHelper.create(this, TOUCH_SLOP_SENSITIVITY, mViewDragCallback);
		mDragHelper.setMinVelocity(MIN_FLING_VELOCITY * mDensity);
		
		ViewCompat.setImportantForAccessibility(this,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
		
		// So that we can catch the back button ?
        setFocusableInTouchMode(true);
        setClickable(true);
        ViewGroupCompat.setMotionEventSplittingEnabled(this, false);
        
        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);
	}
	
	private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			boolean disallowIntercept = 
					(Math.abs(distanceY) < (DISALLOW_INTECEPT_TOUCH_EVNET_MAX_Y_OFFSET * mDensity))
					|| !isClosed();
			requestDisallowInterceptTouchEvent(disallowIntercept);
			return super.onScroll(e1, e2, distanceX, distanceY);
		}

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}
    	
    };
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mFirstLayout = true;
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mFirstLayout = true;
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if( mFirstLayout ) {
			if( mPreservedOpenState ) {
				openPane();
			} else {
				closePane();
			}
		}
		mFirstLayout = false;
	}
	
	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw) {
            mFirstLayout = true;
        }
    }
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if( !mDragOpenable && isClosed()) {
			return super.onInterceptTouchEvent(ev);
		}
		int action = MotionEventCompat.getActionMasked(ev);
		if( action == MotionEvent.ACTION_CANCEL
				|| action == MotionEvent.ACTION_UP) {
			mDragHelper.cancel();
			return false;
		}
		return mDragHelper.shouldInterceptTouchEvent(ev) 
				|| shouldInterceptDragPaneTouchEvent(ev); 
	}
	/*
	 * 拦截 PaneView 的Touch 事件
	 * 非关闭状态，并且当前手指触摸在 PaneView 上, 则拦截Touch 事件
	 * @param ev 触摸事件
	 * @return true 拦截， false 不拦截
	 */
	private boolean shouldInterceptDragPaneTouchEvent(MotionEvent ev) {
		// 非关闭状态，那么就不允许DragView 获取焦点
		if( !isClosed() ) {
			mDragViewVisibleBounds.set(mDragPane.getLeft(), mDragPane.getTop(), mDragPane.getRight(), mDragPane.getBottom());
			return mDragViewVisibleBounds.contains((int)ev.getX(), (int)ev.getY());
		}
		return false;
	}
	
	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(!mDragOpenable && isClosed()) {
			return super.onTouchEvent(event);
		}
//		requestDisallowInterceptTouchEvent(true);
		mGestureDetector.onTouchEvent(event);
		mDragHelper.processTouchEvent(event);
		return true;
	}
	
	/**
	 * 设置 PaneView
	 * @param viewId pane view 的 id
	 * @see DragPaneLayout#setDragPane(View)
	 */
	public void setDragPane(int viewId) {
		setDragPane(findViewById(viewId));
	}
	
	/**
	 * 设置 PaneView
	 * @param view pane view
	 */
	public void setDragPane(View view) {
		mDragHelper.abort();
		if( mDragPane != view ) {
			mDragPane = view;
		}
	}
	
	/**
	 * 设置 PaneView 最大拖动范围
	 * @param dragRange
	 */
	public void setDragRange(int dragRange ) {
		if( mDragRange != dragRange ) {
			closePane();
			mDragRange = dragRange;
		}
	}
	
	/**
	 * 获取当前 PaneView 拖动范围
	 * @return dragRange
	 */
	public int getDragRange() {
		return mDragRange;
	}
	
	@Override
	public void computeScroll() {
		if( mDragHelper.continueSettling(true) ) {
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}
	
	/**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     * @param velocity initial velocity in case of fling, or 0.
     */
    boolean smoothSlideTo(float slideOffset, int velocity) {
        int startBound = 0;
        int x = (int) (startBound + slideOffset * mDragRange);
        if (mDragHelper.smoothSlideViewTo(mDragPane, x, mDragPane.getTop())) {
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }
    
    private void onPaneDragged(int newLeft) {
    	mDragOffset = (float) (newLeft) / mDragRange;
        mPaneStateChangedProxy.onPaneDragged(mMode, mDragOffset);
    }
    
    /**
     * 关闭 PaneView
     */
    public void closePane() {
    	if( mDragPane == null || isClosed()) {
    		return;
    	}
    	smoothSlideTo(0f, 0);
    }
    
    /**
     * 打开 PaneView
     */
    public void openPane() {
    	if( mDragPane == null ) {
    		return;
    	}
    	if( Mode.LEFT == mMode ) {
    		smoothSlideTo(1.0f, 0);
    	} else if( mFirstLayout && Mode.BOTH == mMode) {
    		smoothSlideTo(mBothModeSildeOffsetState, 0);
    	} else { // Mode.RIGHT || Mode.BOTH
    		smoothSlideTo(-1.0f, 0);
    	}
    }
    
    /**
     * PaneView 是否开启状态
     * @return true 已经是开启状态, false 其他情况
     */
    public boolean isOpened() {
    	return Float.valueOf(Math.abs(mDragOffset)).intValue() == 1;
    }
    
    /**
     * PaneView 是否关闭状态
     * @return true 已经关闭, false 其他情况
     */
    public boolean isClosed() {
    	return Math.abs(mDragOffset) < 0.0009f;
    }
    
    /**
     * 设置 PaneView 的开启模式 {@link Mode}
     * @param mode {@link Mode}
     */
    public void setMode(Mode mode) {
    	if( mMode != mode ) {
    		if( mode != Mode.BOTH ) {
    			closePane();
    		}
    		mMode = mode;
    	}
    }
    
    /**
     * 设置 PaneView 是否允许被拖动打开
     * @param openable true 允许拖动打开; false 不允许拖动打开，但是可以通过 {@link #openPane()} 方法打开 
     */
    public void setDragOpenable(boolean openable) {
    	mDragOpenable = openable;
    }
    
    /**
     * 是否允许拖动打开 PaneView
     * @return true 允许拖动打开, false 不允许拖动打开
     * @see #setDragOpenable(boolean)
     */
    public boolean isDragOpenable() {
    	return mDragOpenable;
    }
    
    /**
     * 设置 PaneView 状态改变监听
     * @param l
     */
    public void setOnPaneStateChangedListener(OnPaneStateChangedListener l) {
    	mPaneStateChangedListener = l;
    }
    
	class ViewDragCallback extends ViewDragHelper.Callback {

		@Override
		public boolean tryCaptureView(View view, int arg1) {
			return mDragPane == view;
		}
		
		@Override
		public int clampViewPositionVertical(View child, int top, int dy) {
			return child.getTop();
		}

		@Override
		public int clampViewPositionHorizontal(View child, int left, int dx) {
			int newLeft = mDragLeft + left;
            int startBound = 0;
            int endBound = startBound + getViewHorizontalDragRange(child);
            if( Mode.RIGHT == mMode ) {
            	newLeft = Math.max(Math.min(newLeft, -startBound), -endBound);
            } else if( Mode.LEFT == mMode ){
            	newLeft = Math.min(Math.max(newLeft, startBound), endBound);
            } else {
        		newLeft = Math.min(Math.max(newLeft, -endBound), endBound);
            }
            return newLeft;
		}
		
		@Override
		public int getViewHorizontalDragRange(View child) {
				return mDragRange;
		}
		
		@Override
		public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
			onPaneDragged(left);
		}
		
		@Override
		public void onViewReleased(View releasedChild, float xvel, float yvel) {
			int finalLeft = 0;
			if( Mode.RIGHT == mMode ) {
				// 向左边
	            if (xvel < 0 || (xvel == 0 && mDragOffset < -0.5f)) {
	            	finalLeft -= mDragRange;
	            }
			} else if( Mode.LEFT == mMode ) {
				// 向右边
	            if (xvel > 0 || (xvel == 0 && mDragOffset > 0.5f)) {
	            	finalLeft += mDragRange;
	            }
			} else {
				// Mode.BOTH == mMode
				// 向左边
				final int viewLeft = releasedChild.getLeft();
	            if (xvel < 0 || (xvel == 0 && mDragOffset < -0.5f)) {
	            	if( viewLeft < 0 ) {
	            		finalLeft -= mDragRange;
	            	}
	            } else if (xvel > 0 || (xvel == 0 && mDragOffset > 0.5f)) {
	            	if( viewLeft > 0 ) {
		            	finalLeft += mDragRange;
		            }
	            }
			}
			mDragHelper.settleCapturedViewAt(finalLeft, releasedChild.getTop());
			// don't forget this
			// 不加这句话就没有还原的效果
			invalidate();
		}
		
		@Override
		public void onViewDragStateChanged(int state) {
			super.onViewDragStateChanged(state);
			if( ViewDragHelper.STATE_IDLE == state ) {
				if (mDragOffset == 0) {
			    	mPaneStateChangedProxy.onPaneClosed();
			    	mPreservedOpenState = false;
	            } else {
			    	mPaneStateChangedProxy.onPaneOpened(mMode, mDragOffset);
			    	mPreservedOpenState = true;
	            }
			}
		}
		
		@Override
		public void onEdgeDragStarted(int edgeFlags, int pointerId) {
			mDragHelper.captureChildView(mDragPane, pointerId);
		}
		
	}
	
	@Override
    protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);
        ss.isOpen = isOpened();
        ss.isDragOpenable = isDragOpenable();
        ss.mode = mMode;
        ss.bothModeDragOffsetState = (int) mDragOffset;
        ss.dragRange = mDragRange;
        return ss;
    }
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setMode(ss.mode);
        setDragOpenable(ss.isDragOpenable);
        mPreservedOpenState = ss.isOpen;
        mBothModeSildeOffsetState = ss.bothModeDragOffsetState;
        mDragRange = ss.dragRange;
        if (ss.isOpen) {
            openPane();
        } else {
            closePane();
        }
	}
	
	static class SavedState extends BaseSavedState {
        boolean isOpen;
        boolean isDragOpenable;
        Mode mode;
        int bothModeDragOffsetState; // for Mode.BOTH only. left open OR right open OR close
        int dragRange;
        
        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            isOpen = in.readInt() != 0;
            isDragOpenable = in.readInt() != 0;
            mode = Mode.valueOf(in.readString());
            bothModeDragOffsetState = in.readInt();
            dragRange = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(isOpen ? 1 : 0);
            out.writeInt(isDragOpenable ? 1 : 0);
            out.writeString(mode.toString());
            out.writeInt(bothModeDragOffsetState);
            out.writeInt(dragRange);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
