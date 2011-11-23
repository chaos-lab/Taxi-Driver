package com.chaos.driver;

import com.google.android.maps.*;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.widget.Toast;

public class MapOverLay extends Overlay {
	private MapView mapView;
	private GeoPoint pos;
	private Bitmap mBmp;

	public MapOverLay(MapView view, GeoPoint pp,Bitmap bmp) {
		this.mapView = view;
		this.pos = pp;
		this.mBmp = bmp;
	}

	public boolean draw(Canvas canvas, MapView view, boolean shadow, long when) {
		super.draw(canvas, view, shadow, when);
		Point pt = new Point();
		mapView.getProjection().toPixels(pos, pt);
		canvas.drawBitmap(mBmp, pt.x, pt.y, null);
		return true;
	}

	public boolean beBeneathOverlay(Point pt) {
		Point pt1 = new Point();
		mapView.getProjection().toPixels(pos, pt1);
		if (pt.x >= pt1.x && pt.x <= pt1.x + mBmp.getWidth() && pt.y >= pt1.y
				&& pt.y <= pt1.y + mBmp.getHeight()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean onTouchEvent(MotionEvent e, MapView view) {
		Point pt = new Point();
		pt.x = (int) e.getX();
		pt.y = (int) e.getY();
		if (beBeneathOverlay(pt)) {
			if (MotionEvent.ACTION_DOWN == e.getAction()) {
				GeoPoint gp = view.getProjection().fromPixels((int) e.getX(),
						(int) e.getY());
				Toast.makeText(
						mapView.getContext(),
						gp.getLatitudeE6() / 1E6 + " , " + gp.getLongitudeE6()
								/ 1E6, Toast.LENGTH_SHORT).show();
			}

		}
		return false;
	}

	public boolean onTap(GeoPoint p, MapView v) {
		Point pt = new Point();
		v.getProjection().toPixels(p, pt);
		if (beBeneathOverlay(pt)) {
			Toast.makeText(
					mapView.getContext(),
					p.getLatitudeE6() / 1E6 + " , " + p.getLongitudeE6()
							/ 1E6, Toast.LENGTH_SHORT).show();
			return true;
		}
		return false;
	}

}
