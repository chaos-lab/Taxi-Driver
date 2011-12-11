package com.chaos.driver;

import java.io.File;
import java.util.List;
import java.util.Set;

import com.chaos.driver.util.DriverConst;

import android.content.Context;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class GestureView extends GestureOverlayView {
	GestureLibrary mGestureLib;
	Gesture mGesture;
	GestureHandler mHandler;
	Bitmap mFingerBmp;
	Point mCurPt;

	public GestureView(Context context, AttributeSet set) {
		super(context, set);
		mGestureLib = GestureLibraries.fromRawResource(context, R.raw.gestures);
		mGestureLib.setOrientationStyle(GESTURE_STROKE_TYPE_SINGLE);
		mGestureLib.load();
		this.addOnGestureListener(new CustomizedGestureListener());
		mFingerBmp = BitmapFactory.decodeResource(getResources(),
				R.drawable.finger);
		mCurPt = new Point(0, 0);
	}

	public void draw(Canvas canvas) {
		super.draw(canvas);
		if (this.getVisibility() == View.VISIBLE) {
			canvas.drawBitmap(mFingerBmp, null, new Rect(mCurPt.x, mCurPt.y
					- DriverConst.OVERLAY_HEIGHT, mCurPt.x
					+ DriverConst.OVERLAY_WIDTH, mCurPt.y), null);
		}

	}

	public String findGesture(Gesture gesture) {
		List<Prediction> lstRecRes = mGestureLib.recognize(gesture);
		double maxScore = 0;
		int maxIndex = -1;
		String resPrediction = null;
		for (int i = 0; i < lstRecRes.size(); i++) {
			Prediction prediction = lstRecRes.get(i);
			if (prediction.score > maxScore) {
				maxScore = prediction.score;
				maxIndex = i;
				resPrediction = prediction.name;
			}
		}
		if (maxIndex != -1 && maxScore >= 1) {
			return resPrediction;
		}
		return null;
	}

	private class CustomizedGestureListener implements OnGestureListener {

		public void onGestureStarted(GestureOverlayView overlay,
				MotionEvent event) {
		}

		public void onGestureEnded(GestureOverlayView overlay, MotionEvent event) {
			Gesture gesture = overlay.getGesture();
			if (event.getAction() == MotionEvent.ACTION_UP) {
				String strName = findGesture(gesture);
				if (mHandler != null && strName != null) {
					mHandler.processGesture(strName, gesture.getBoundingBox());
				}
			}
			disableGesture();
		}

		public void onGestureCancelled(GestureOverlayView overlay,
				MotionEvent event) {
		}

		public void onGesture(GestureOverlayView overlay, MotionEvent event) {
		}
	}

	public void setHandler(GestureHandler handler) {
		this.mHandler = handler;
	}

	public void enableGesture(Point pt) {
		setVisibility(VISIBLE);
		mCurPt = pt;		
		this.setFocusable(true);
		this.requestFocus();
	}

	public void disableGesture() {
		setVisibility(GONE);
		this.setFocusable(false);
		invalidate();		
	}
}
