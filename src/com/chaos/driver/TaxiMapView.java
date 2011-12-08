package com.chaos.driver;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.google.android.maps.MapView;

public class TaxiMapView extends MapView{
	private DriverActivity mDriver;
	public TaxiMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public boolean onTouchEvent(MotionEvent e){
		if(mDriver != null){
			mDriver.updatePerspective(DriverConst.VIEW_MODE);
		}
		return super.onTouchEvent(e);
	}
	//getter && setter
	public void setDriver(DriverActivity driver){
		mDriver= driver;
	}
}
