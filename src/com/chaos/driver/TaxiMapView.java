package com.chaos.driver;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;

import com.chaos.driver.util.DriverConst;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

public class TaxiMapView extends MapView{
	private DriverActivity mDriver;
	GestureDetector mGestureDetector;
	Context mContext;
	MotionEvent mMotionEvt;
	AlertDialog mDlg;
	public TaxiMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener(){
			public void onLongPress(MotionEvent evt){
				mMotionEvt = evt;
				ButtonGrid grid = new ButtonGrid(mContext);
				ButtonGrid.ButtonListAdapter adapter = new ButtonGrid.ButtonListAdapter(grid);
				adapter.addButton("gesture", R.drawable.finger, new OnClickListener(){
					@Override
					public void onClick(View v) {
						mDlg.dismiss();
						mDriver.enableGesture(new Point((int)mMotionEvt.getX(),(int)mMotionEvt.getY()));
					}
					
				});
				adapter.addButton("send location", R.drawable.passengerfemale, new OnClickListener(){

					@Override
					public void onClick(View v) {
						mDlg.dismiss();
						GeoPoint gp = getProjection().fromPixels((int)mMotionEvt.getX(), (int)mMotionEvt.getY());
						mDriver.sendLocation(gp,"someplace");
					}
					
				});
				grid.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
				grid.setAdapter(adapter);
				mDlg =  new AlertDialog.Builder(mContext)
				.setView(grid).create();
				mDlg.show();
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
