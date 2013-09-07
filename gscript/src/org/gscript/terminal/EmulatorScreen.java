package org.gscript.terminal;

import java.nio.ByteBuffer;

import org.gscript.settings.ShellProfile;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Scroller;

public class EmulatorScreen extends View {

	public static final String LOG_TAG = "EmulatorView";
	public static final int GESTURE_MODE_SCROLL = 0;
	public static final int GESTURE_MODE_CURSOR = 1;
	
	static final boolean CURSOR_REGION_DEBUG = false;
	static final int GESTURE_BORDER_DIP = 75;
	
	static final int CURSOR_REPEAT_DELAY 	= 300; 
	static final int CURSOR_DIRECTION_LEFT 	= 0;
	static final int CURSOR_DIRECTION_RIGHT = 1;
	static final int CURSOR_DIRECTION_UP 	= 2;
	static final int CURSOR_DIRECTION_DOWN 	= 3;

	ScreenBufferParcelable mScreenBuffer;

	boolean mAutoScrollEnabled = true;
	boolean mQuickScrollEnabled = true;
	boolean mQuickScroll = false;
	boolean mResizing = false;

	volatile int mUpdateRequested;

	int mCanvasPadding;

	ByteBuffer mRowBuffer;
	
	char[] mCharSequence;
	float mCharDecent;
	float mCharWidth;
	float mLineHeight;
	
	int mTextColor;
	int mBackColor;

	int mScreenHeight;
	int mScreenWidth;
	
	int mScreenFillTop;
	int mScreenRows;
	int mScreenCols;

	Paint mBackgroundPaint = new Paint();
	Paint mForegroundPaint = new Paint();

	int mGestureMode = GESTURE_MODE_SCROLL;
	int mGestureBorderSize;
	
	boolean mCursorVisible;
	RectF[] mCursorRegions = new RectF[4];
	int mCursorDirection;
	int mCursorPointers;

	GestureDetector mGestureDetector;

	int mPrevScrollY;
	Scroller mScroller;
	EmulatorScreenListener mListener;

	public EmulatorScreen(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initializeView();
	}

	public EmulatorScreen(Context context, AttributeSet attrs) {
		super(context, attrs);
		initializeView();
	}

	public EmulatorScreen(Context context) {
		super(context);
		initializeView();
	}

	void initializeView() {

		setScrollContainer(true);
		setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
		setScrollbarFadingEnabled(true);

		mGestureDetector = new GestureDetector(getContext(),
				new GestureListener());

		mScroller = new Scroller(getContext());
		
		/* initialize empty regions so that they can be set in onLayout */
		for(int i=0; i < mCursorRegions.length; ++i)
			mCursorRegions[i] = new RectF();
	}

	public void initialize(ShellProfile profile,
			ScreenBufferParcelable screenBuffer) {

		final DisplayMetrics metrics = getResources().getDisplayMetrics();

		mScreenBuffer = screenBuffer;

		mTextColor = mScreenBuffer.getTextColor();
		mBackColor = mScreenBuffer.getBackColor();

		mBackgroundPaint.setColor(ColorScheme.getColor(mBackColor, false));
		mBackgroundPaint.setStyle(Style.FILL);

		/* also update the window background color if possible */

		final Context context = getContext();
		
		if (context instanceof Activity) {
			Activity parent = (Activity) context;
			parent.getWindow().setBackgroundDrawable(
					new ColorDrawable(ColorScheme.getColor(mBackColor, false)));
		}

		int textSize = Integer.parseInt(profile.fontsize);
		float textSizeSp = TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_SP, textSize, metrics);

		mForegroundPaint.setColor(ColorScheme.getColor(mTextColor, false));
		mForegroundPaint.setStyle(Style.FILL);
		mForegroundPaint.setTextAlign(Align.LEFT);
		mForegroundPaint.setTypeface(Typeface.MONOSPACE);
		mForegroundPaint.setUnderlineText(false);
		mForegroundPaint.setStrikeThruText(false);
		mForegroundPaint.setFakeBoldText(false);
		mForegroundPaint.setAntiAlias(true);
		mForegroundPaint.setTextSize(textSizeSp);

		mCanvasPadding = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 10, metrics);

		mLineHeight = mForegroundPaint.getFontSpacing();
		mCharWidth = mForegroundPaint.measureText("A");
		mCharDecent = mForegroundPaint.descent();

		mUpdateRequested++;

		post(UpdateRunnable);
	}

	public boolean getCursorVisible() {
		return mCursorVisible;
	}
	
	public void setCursorVisible(boolean visible) {
		if(mCursorVisible != visible) {
			mCursorVisible = visible;
			invalidate();
		}
	}
	
	public int getGestureMode() {
		return mGestureMode;
	}

	public void setGestureMode(int mode) {
		mGestureMode = mode;

		if (mListener != null)
			mListener.onGestureModeChanged(mGestureMode);
	}

	public boolean getAutoScrollEnabled() {
		return mAutoScrollEnabled;
	}

	public void setAutoScrollEnabled(boolean enabled) {
		mAutoScrollEnabled = enabled;

		/* update scroll position when auto-scroll is enabled */
		if (mAutoScrollEnabled) {
			if (!mScroller.isFinished())
				mScroller.forceFinished(true);

			scrollTo(0, (int) (mScreenFillTop * mLineHeight));
		}

		if (mListener != null)
			mListener.onAutoScrollChanged(mAutoScrollEnabled);
	}

	public boolean toggleAutoScrollEnabled() {
		setAutoScrollEnabled(!mAutoScrollEnabled);

		return mAutoScrollEnabled;
	}

	public boolean getQuickScrollEnabled() {
		return mQuickScrollEnabled;
	}

	public void setQuickScrollEnabled(boolean enabled) {
		mQuickScrollEnabled = enabled;
	}

	public boolean toggleQuickScrollEnabled() {
		setQuickScrollEnabled(!mQuickScrollEnabled);
		return mQuickScrollEnabled;
	}

	void toggleSoftInput() {
		InputMethodManager imm = (InputMethodManager) getContext()
				.getSystemService(Context.INPUT_METHOD_SERVICE);

		imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
	}

	@Override
	protected int computeVerticalScrollRange() {
		return (int) ((mScreenFillTop + mScreenRows) * mLineHeight);
	}

	@Override
	public void computeScroll() {

		if (mScroller.computeScrollOffset()) {
			if (mPrevScrollY == -1) {
				mPrevScrollY = mScroller.getStartY();
			}

			int dy = mScroller.getCurrY() - mPrevScrollY;

			mPrevScrollY = mScroller.getCurrX();
			mPrevScrollY = mScroller.getCurrY();

			scrollBy(dy);

		} else {
			mPrevScrollY = -1;
		}

	}

	private void scrollBy(float dy) {

		float scrollY = this.getScrollY();

		scrollY += dy;
		if (scrollY < 0)
			scrollY = 0;

		float scrollRange = computeVerticalScrollRange()
				- (mScreenRows * mLineHeight);

		if (scrollY > scrollRange)
			scrollY = scrollRange;

		this.scrollTo(0, (int) scrollY);
	}

	private class GestureListener extends SimpleOnGestureListener {

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if(e.getY() < mGestureBorderSize) {
				/* scroll to top */
				setAutoScrollEnabled(false);
				scrollTo(0, 0);
			} else if(e.getY() > (getHeight() - mGestureBorderSize)) {
				/* scroll to bottom */
				setAutoScrollEnabled(true);
			} else {
				toggleSoftInput();
			}
			return true;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			/* stop scrolling */
			if (!mScroller.isFinished()) {
				mScroller.forceFinished(true);
				return true;
			}

			return super.onDown(e);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {

			switch (mGestureMode) {
			case GESTURE_MODE_SCROLL:
				setAutoScrollEnabled(false);

				/*
				 * check if we need to handle a quick scroll based on the first
				 * event
				 */
				
				mQuickScroll = mQuickScrollEnabled
						&& e1.getX() > (getWidth() - 100); 
				
				if (mQuickScroll) {

					float evY = Math.min(Math.max(e2.getY(), 0), getHeight());
					int row = (int) (((float) mScreenFillTop / 100f) * (100f / (float) (getHeight() - 1) * evY));
					row = Math.min(Math.max(row, 0), mScreenFillTop);

					scrollTo(0, (int) (row * mLineHeight));

				} else {
					
					/* normal scroll */
					scrollBy(distanceY);
				}
				break;
			}
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {

			switch (mGestureMode) {
			case GESTURE_MODE_SCROLL:

				/* do not fling on a quickscroll event */
				if(!mQuickScroll) {
					
					setAutoScrollEnabled(false);
	
					int max_top = (int) (computeVerticalScrollRange() - (mScreenRows * mLineHeight));
					int min_top = 0;
	
					int startY = getScrollY();
	
					mScroller.fling(0, startY, (int) -velocityX, (int) -velocityY,
							0, 0, min_top, max_top);
	
					invalidate();
					
				}

				break;
			case GESTURE_MODE_CURSOR:
				if(Math.abs(velocityX) > Math.abs(velocityY)) {
					/* home/end */
					if(velocityX > 0) {
						sendBytes(EmulatorInput.KeyBinding.get(EmulatorInput.KEYCODE_MOVE_END));
					} else {
						sendBytes(EmulatorInput.KeyBinding.get(EmulatorInput.KEYCODE_MOVE_HOME));
					}
				} else {
					/* page up/down */
					if(velocityY > 0) {
						sendBytes(EmulatorInput.KeyBinding.get(EmulatorInput.KEYCODE_PAGE_UP));
					} else {
						sendBytes(EmulatorInput.KeyBinding.get(EmulatorInput.KEYCODE_PAGE_DOWN));
					}
				}
				break;
			}
			

			return true;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (mGestureMode) {
		case GESTURE_MODE_SCROLL:
			mGestureDetector.onTouchEvent(event);
			break;
		case GESTURE_MODE_CURSOR:

			if (event.getAction() == MotionEvent.ACTION_DOWN
					|| event.getPointerCount() != mCursorPointers) {
				
				removeCallbacks(CursorModeRunnable);

				mCursorPointers = event.getPointerCount();
				
				if(getHeight() < getWidth()) {
					/* left / right first */
					for(int i=0; i < mCursorRegions.length; ++i) {
						if(mCursorRegions[i].contains(event.getX(), event.getY())) {
							mCursorDirection = i;
							post(CursorModeRunnable);
							return true;
						}
					}
				} else {
					/* handle up/down first */
					for(int i=mCursorRegions.length-1; i >= 0; --i) {
						if(mCursorRegions[i].contains(event.getX(), event.getY())) {
							mCursorDirection = i;
							post(CursorModeRunnable);
							return true;
						}
					}
				}
				
				mGestureDetector.onTouchEvent(event);
				
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				removeCallbacks(CursorModeRunnable);
			}
			
			mGestureDetector.onTouchEvent(event);
			break;
		}
		return true;
	}

	private Runnable CursorModeRunnable = new Runnable() {
		@Override
		public void run() {
			switch (mCursorDirection) {
			case CURSOR_DIRECTION_LEFT:
				sendBytes(EmulatorInput.KeyBinding.get(EmulatorInput.KEYCODE_DPAD_LEFT));
				break;
			case CURSOR_DIRECTION_RIGHT:
				sendBytes(EmulatorInput.KeyBinding.get(EmulatorInput.KEYCODE_DPAD_RIGHT));
				break;
			case CURSOR_DIRECTION_UP:
				sendBytes(EmulatorInput.KeyBinding.get(EmulatorInput.KEYCODE_DPAD_UP));
				break;
			case CURSOR_DIRECTION_DOWN:
				sendBytes(EmulatorInput.KeyBinding.get(EmulatorInput.KEYCODE_DPAD_DOWN));
				break;
			}
			
			postDelayed(this, CURSOR_REPEAT_DELAY / mCursorPointers);
		}
	};

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {

		if (changed) {

			/* reset screen rows and cols so that a resize will get triggered */
			mScreenRows = 0;
			mScreenCols = 0;

			mScreenWidth = right - left;
			mScreenHeight = bottom - top;
			
			int maxSize = (mScreenWidth > mScreenHeight) ? mScreenHeight / 4 : mScreenWidth / 4;
			mGestureBorderSize = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, GESTURE_BORDER_DIP, getResources().getDisplayMetrics());
			
			mGestureBorderSize = Math.min(mGestureBorderSize, maxSize);
			
			/* set cursor regions */
			mCursorRegions[CURSOR_DIRECTION_LEFT].set(0, 0, mGestureBorderSize, mScreenHeight);
			mCursorRegions[CURSOR_DIRECTION_RIGHT].set(mScreenWidth-mGestureBorderSize, 0, mScreenWidth, mScreenHeight);
			mCursorRegions[CURSOR_DIRECTION_UP].set(0, 0, mScreenWidth, mGestureBorderSize);
			mCursorRegions[CURSOR_DIRECTION_DOWN].set(0, mScreenHeight-mGestureBorderSize, mScreenWidth, mScreenHeight);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		/* clear screen */
		canvas.drawPaint(mBackgroundPaint);

		if (mScreenBuffer != null && !mResizing) {

			if (mScreenRows == 0 || mScreenCols == 0) {

				int height = (mScreenHeight - mCanvasPadding);
				int width = (mScreenWidth - mCanvasPadding);

				mScreenRows = (int) Math.floor((float) height / mLineHeight);
				mScreenCols = (int) Math.floor((float) width / mCharWidth);

				/* calculate real line height and char offsets */
				width = width - (int) ((float) width / (float) mScreenCols);
				height = height - (int) ((float) height / (float) mScreenRows);

				if (mScreenBuffer.getScreenRows() != mScreenRows
						|| mScreenBuffer.getScreenCols() != mScreenCols) {

					mResizing = true;

					/* request screenbuffer resize */
					removeCallbacks(UpdateRunnable);

					if (mListener != null) {
						mListener.onWindowSizeChangeRequested(mScreenRows,
								mScreenCols, canvas.getWidth(),
								canvas.getHeight());

						return;
					}
				}
			}

			if (mCharSequence == null || mCharSequence.length < mScreenCols)
				mCharSequence = new char[mScreenCols];

			int scrollTopRow = (int) Math
					.floor(((float) getScrollY() + (mLineHeight - 1))
							/ mLineHeight);

			int cursorRow = mScreenBuffer.getCursorRow(true);
			int cursorCol = mScreenBuffer.getCursorCol();

			for (int row = scrollTopRow; row < (scrollTopRow + mScreenRows); ++row) {

				mRowBuffer = mScreenBuffer.getRowData(mRowBuffer, row, true);
				if (mRowBuffer == null)
					return;

				int rowFlags = mRowBuffer.getInt();
				if (rowFlags != -1) {

					int pos = mRowBuffer.position();
					int cols = (mRowBuffer.capacity() - pos) / 4;

					float rowOffset = (mCanvasPadding / 2)
							+ (row * mLineHeight) - mCharDecent;

					int sequenceEncoding = 0;
					int sequenceLength = 0;
					float sequenceOffset = 0;

					for (int col = 0; col < cols; ++col) {

						float offsetX = (mCanvasPadding / 2)
								+ (col * mCharWidth);

						if (row == cursorRow && col == cursorCol && mCursorVisible) {
							mBackgroundPaint.setColor(ColorScheme.getColor(
									mTextColor, true));
							canvas.drawRect(
									(offsetX + 1),
									(rowOffset + 1) + mCharDecent,
									(offsetX - 1) + mCharWidth,
									(rowOffset - 1) + mLineHeight + mCharDecent,
									mBackgroundPaint);
						}

						int encodedChar = mRowBuffer.getInt();

						/*
						 * try to break line in to sequences with matching
						 * colors and text options
						 */

						if (sequenceEncoding == (encodedChar & 0x00ffffff)) {

							char bChar = (char) ((encodedChar >> 24) & 0xff);
							mCharSequence[sequenceLength] = bChar;

							sequenceLength++;

						} else {

							if (sequenceLength > 0) {

								/* render previous sequence */

								byte bTextEffects = (byte) ((sequenceEncoding >> 16) & 0xff);

								if ((bTextEffects & TextEffects.IGNORE) != TextEffects.IGNORE) {

									byte bBackColor = (byte) (sequenceEncoding & 0xff);
									byte bTextColor = (byte) ((sequenceEncoding >> 8) & 0xff);

									mForegroundPaint
											.setFakeBoldText(TextEffects
													.hasTextEffect(
															bTextEffects,
															TextEffects.BOLD));
									mForegroundPaint
											.setUnderlineText(TextEffects
													.hasTextEffect(
															bTextEffects,
															TextEffects.UNDERLINE));
									mForegroundPaint
											.setTextSkewX(TextEffects
													.hasTextEffect(
															bTextEffects,
															TextEffects.ITALIC) ? -0.25f
													: 0);

									mBackgroundPaint.setColor(ColorScheme
											.getColor(bBackColor, false));

									mForegroundPaint
											.setColor(ColorScheme.getColor(
													bTextColor,
													(bTextColor == bBackColor)));

									canvas.drawText(mCharSequence, 0,
											sequenceLength, sequenceOffset,
											rowOffset + mLineHeight,
											mForegroundPaint);
								}
							}

							sequenceLength = 0;
							sequenceEncoding = (encodedChar & 0x00ffffff);
							sequenceOffset = offsetX;

							byte bChar = (byte) ((encodedChar >> 24) & 0xff);
							mCharSequence[sequenceLength] = (char) bChar;

							sequenceLength++;

						}
					}

					/* render possible remaining sequence */

					if (sequenceLength > 0) {

						byte bTextEffects = (byte) ((sequenceEncoding >> 16) & 0xff);

						if ((bTextEffects & TextEffects.IGNORE) != TextEffects.IGNORE) {

							byte bBackColor = (byte) (sequenceEncoding & 0xff);
							byte bTextColor = (byte) ((sequenceEncoding >> 8) & 0xff);

							mForegroundPaint.setFakeBoldText(TextEffects
									.hasTextEffect(bTextEffects,
											TextEffects.BOLD));
							mForegroundPaint.setUnderlineText(TextEffects
									.hasTextEffect(bTextEffects,
											TextEffects.UNDERLINE));
							mForegroundPaint.setTextSkewX(TextEffects
									.hasTextEffect(bTextEffects,
											TextEffects.ITALIC) ? -0.25f : 0);

							mBackgroundPaint.setColor(ColorScheme.getColor(
									bBackColor, false));

							mForegroundPaint.setColor(ColorScheme.getColor(
									bTextColor, (bTextColor == bBackColor)));

							canvas.drawText(mCharSequence, 0, sequenceLength,
									sequenceOffset, rowOffset + mLineHeight,
									mForegroundPaint);
						}
					}
				}
			}

			if(CURSOR_REGION_DEBUG)
			if(mGestureMode==GESTURE_MODE_CURSOR) {
				mBackgroundPaint.setColor(0x33cccccc);
				canvas.translate(0, getScrollY());
				for(int i=0; i < mCursorRegions.length; ++i) {
					canvas.drawRect(mCursorRegions[i], mBackgroundPaint);
				}
			}			
			
			/* reset back-color */
			mBackgroundPaint.setColor(ColorScheme.getColor(mBackColor, false));
		}
	}

	public void onProcessEvent(int event) {

		if (mScreenBuffer != null) {

			switch (event) {
			case TerminalEvent.SCREEN_RESIZE:

				mCharSequence = new char[mScreenBuffer.getScreenCols()];
				mResizing = false;

				post(UpdateRunnable);
				mUpdateRequested++;
				break;

			case TerminalEvent.SCREEN_UPDATE:

				/*
				 * only lines below the screen top can get updated.. check if
				 * those are in-view if not ignore the update for now
				 */
				mUpdateRequested++;
				break;
			}
		}
	}

	private Runnable UpdateRunnable = new Runnable() {
		@Override
		public void run() {

			int delay = 150;
			if (mUpdateRequested > 0) {

				/* check more frequently */
				if (mUpdateRequested > 2)
					delay = 100;

				mUpdateRequested = 0;

				mScreenFillTop = mScreenBuffer.getScreenFillTop();
				if (mAutoScrollEnabled)
					scrollTo(0, (int) (mScreenFillTop * mLineHeight));

				invalidate();
			}

			/* auto re-check for screen updates every 150ms */
			postDelayed(this, delay);
		}
	};

	private void sendBytes(byte[] b) {
		sendBytes(b, b.length);
	}
	
	private void sendBytes(byte[] b, int length) {
		if (mListener != null)
			mListener.onEmulatorInput(b, length);
	}

	public void setEmulatorScreenListener(EmulatorScreenListener listener) {
		mListener = listener;
	}

	public interface EmulatorScreenListener {

		public void onEmulatorInput(byte[] b, int length);

		public void onAutoScrollChanged(boolean enabled);

		public void onGestureModeChanged(int mode);

		public void onWindowSizeChangeRequested(int rows, int cols, int width,
				int height);
	}
}
