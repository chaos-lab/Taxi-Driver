package com.chaos.driver;

import com.chaos.driver.util.DriverConst;
import com.google.android.maps.*;
import android.graphics.*;
import android.widget.Toast;

public class MapOverLay extends Overlay {
	private MapView mapView;
	private GeoPoint pos;
	private Bitmap mBmp;
	protected Object mUserInfo;
	public MapOverLay(MapView view, GeoPoint pp,Bitmap bmp,Object info) {
		this.mapView = view;
		this.pos = pp;
		this.mBmp = bmp;
		this.mUserInfo = info;
	}

	public boolean draw(Canvas canvas, MapView view, boolean shadow, long when) {
		super.draw(canvas, view, shadow, when);
		Point pt = new Point();
		mapView.getProjection().toPixels(pos, pt);
		canvas.drawBitmap(mBmp,null,new Rect(pt.x-DriverConst.OVERLAY_WIDTH/2,
				pt.y-DriverConst.OVERLAY_HEIGHT/2,pt.x+DriverConst.OVERLAY_WIDTH/2,
				pt.y+DriverConst.OVERLAY_HEIGHT/2), null);
		return true;
	}

	public boolean beBeneathOverlay(Point pt) {
		Point pt1 = new Point();
		mapView.getProjection().toPixels(pos, pt1);
		if (pt.x >= pt1.x && pt.x <= pt1.x + DriverConst.OVERLAY_WIDTH && pt.y >= pt1.y
				&& pt.y <= pt1.y + DriverConst.OVERLAY_HEIGHT) {
			return true;
		} else {
			return false;
		}
	}
	public boolean onTap(GeoPoint p, MapView v) {
		Point pt = new Point();
		v.getProjection().toPixels(p, pt);
		if (beBeneathOverlay(pt) && mUserInfo != null) {
			Toast.makeText(
					mapView.getContext(),
					mUserInfo.toString(), Toast.LENGTH_LONG).show();
			return true;
		}
		return false;
	}

	public GeoPoint getPos() {
		return pos;
	}
	public boolean isInRect(Rect rect){
		Point pt = new Point();
		mapView.getProjection().toPixels(pos, pt);
		if(pt.x > rect.left && pt.x < rect.right &&
		pt.y > rect.top && pt.y < rect.bottom){
			return true;
		}
		return false;
		
	}
	public boolean isInRect(RectF rect){
		Rect irect = new Rect();
		rect.round(irect);
		return isInRect(irect);
	}

}
