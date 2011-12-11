package com.chaos.driver;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.chaos.driver.util.DriverConst;
import com.google.android.maps.MapView;

public class TaxiMapView extends MapView{
	private DriverActivity mDriver;
	GestureDetector mGestureDetector;
	public TaxiMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener(){
			public void onLongPress(MotionEvent evt){
				mDriver.enableGesture(new Point((int)evt.getX(),(int)evt.getY()));
			}
		});
	}
	public boolean onTouchEvent(MotionEvent e){
		if(mDriver != null){
			mDriver.updatePerspective(DriverConst.VIEW_MODE);
		}
		mGestureDetector.onTouchEvent(e);
		return super.onTouchEvent(e);
	}
	//getter && setter
	public void setDriver(DriverActivity driver){
		mDriver= driver;
	}
}
