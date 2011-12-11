package com.chaos.driver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.MotionEvent;

import com.chaos.driver.util.DriverConst;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class PassengerOverlay extends MapOverLay {
	private DriverActivity mDriver;
	public PassengerOverlay(MapView view, GeoPoint pp,Bitmap bmp,Object info,DriverActivity driver) {
		super(view, pp, bmp, info);
		mDriver = driver;
	}
	public boolean onTouchEvent(MotionEvent e, MapView view) {
		Point pt = new Point();
		pt.x = (int) e.getX();
		pt.y = (int) e.getY();
		if (beBeneathOverlay(pt)) {
			if (MotionEvent.ACTION_MOVE == e.getAction()
					&& DriverConst.PREPROCESS == mDriver.getStatus()
					&& mDriver.getPriStatus() == DriverConst.LOGIN) {
				Intent intent = new Intent(mDriver.getBaseContext(),
						HireCall.class);
				Bundle bundle = new Bundle();
				bundle.putString(DriverConst.CALLER,
						this.mUserInfo.toString());
				intent.putExtras(bundle);
				mDriver.startActivityForResult(intent,
						DriverConst.EXE_CALL_RESPONSE);
				mDriver.setCurPsger((PassengerInfo)mUserInfo);
				mDriver.setStatus(DriverConst.PRERUNNING);
				return true;
			}
		}
		return false;
	}
}
