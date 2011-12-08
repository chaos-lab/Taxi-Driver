package com.chaos.driver;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.driver.Util.CellPositioning;
import com.chaos.driver.Util.HttpConnectUtil;
import com.google.android.maps.GeoPoint;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;

public class DriverAssist implements Executive {
	private DriverActivity mDriver;
	private Handler mHandler;
	private Handler mPsgHandler;
	private GeoPoint mTaxiPos;
	private PassengerSrcProvider mPsgProvider;
	//focused passengers
	private List<PassengerInfo> mLstPsgInfo;	
	private Heart mHeart;
	public DriverAssist(DriverActivity d){
		mDriver = d;
		mHandler = new Handler() {
			public void handleMessage(Message msg) {
				// send location update message to server
				String url = HttpConnectUtil.WEB + "driver/location/update";
				HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
				JSONObject jsonObj = new JSONObject();
				try {
					synchronized (DriverConst.SyncObj) {
						jsonObj.put("latitude",
								mTaxiPos.getLatitudeE6() / 1000000.0);
						jsonObj.put("longitude",
								mTaxiPos.getLongitudeE6() / 1000000.0);
						mDriver.taxiMove(mTaxiPos);
					}
				} catch (JSONException e) {
					e.printStackTrace();
					Log.d("update location", "update location failed!");
				}
				if (HttpConnectUtil.post(url, jsonObj, rd)) {
					if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
						Log.d("update location", "failed to connect to server!");
					}
				}
			}
		};
		mLstPsgInfo = new LinkedList<PassengerInfo>();

		mPsgHandler = new Handler(){
			public void handleMessage(Message msg) {
				//only authorized user can get these message
				if (mDriver.getPriStatus() == DriverConst.LOGIN) {
					//synchronize
					synchronized (DriverConst.SyncObj) {
						//get passenger update information from provider
						List<PassengerInfo> lstInfo = new LinkedList<PassengerInfo>();
						int type = mPsgProvider.getPassengerResource(lstInfo);
						if(-1 == type){
							mHeart.stop();
							mDriver.setStatus(DriverConst.IDLE);
							mDriver.setPriStatus(DriverConst.VISITOR);
						}else{
							int iSize = lstInfo.size();
							//if some fresh one comes
							if ((type & DriverConst.PSG_MASK_APPEND) != 0) {
								if (mDriver.getStatus() == DriverConst.IDLE) {
									mDriver.clearOverlay();
									if (mTaxiPos != null) {
										mDriver.taxiMove(mTaxiPos);
									}
									mLstPsgInfo.addAll(lstInfo);
									for (int p = 0; p < iSize; p++) {
										PassengerInfo psg = mLstPsgInfo.get(p);
										mDriver.addMetaPassenger(psg);
									}
								}
							}
							if ((type & DriverConst.PSG_MASK_UPDATE) != 0) {
								switch(mDriver.getStatus()){
								case DriverConst.IDLE:
									mDriver.clearOverlay();
									if (mTaxiPos != null) {
										mDriver.taxiMove(mTaxiPos);
									}
									mLstPsgInfo.addAll(lstInfo);
									for (int p = 0; p < iSize; p++) {
										PassengerInfo psg = mLstPsgInfo.get(p);
										mDriver.addMetaPassenger(psg);
									}
									break;
								case DriverConst.PREPROCESS:
								case DriverConst.PRERUNNING:
								case DriverConst.RUNNING:
									int mySize = mLstPsgInfo.size();
									for (int p = 0; p < iSize; p++) {
										PassengerInfo psg = lstInfo.get(p);
										for (int q = 0; q < mySize; q++) {
											PassengerInfo myPsg = lstInfo.get(p);
											if (myPsg.getID() == psg.getID()) {
												mDriver.updatePassenger(psg);
											}
										}
									}
									break;
								}
								
							}
							if ((type & DriverConst.PSG_MASK_CANCEL) != 0) {
								int mySize = mLstPsgInfo.size();
								for (int p = 0; p < mySize; p++) {
									PassengerInfo myPsg = mLstPsgInfo.get(p);
									boolean needRemove = true;
									for (int q = 0; q < iSize; q++) {
										PassengerInfo psg = lstInfo.get(p);
										if (myPsg.getID() == psg.getID()) {
											needRemove = false;
											continue;
										}
									}
									if(needRemove){
										mLstPsgInfo.remove(myPsg);
										mDriver.cancelTaxi(myPsg);
										if(mLstPsgInfo.size() == 0){
											mDriver.setStatus(DriverConst.IDLE);
										}
									}
								}
							}
						}						
					}
				}
			}
		};
		mHeart = new Heart();
		mPsgProvider = new PassengerSrcProvider();
		mHeart.addAction(this);
		mHeart.addAction(mPsgProvider);
	}
		
	public void login(){
		Intent intent = new Intent(mDriver.getBaseContext(), Login.class);
		Bundle bundle = new Bundle();
		bundle.putInt(DriverConst.EXE_CODE, DriverConst.EXE_LOGIN);
		intent.putExtras(bundle);
		mDriver.startActivityForResult(intent, DriverConst.EXE_LOGIN);
	}
	public boolean logout(){
		String url = HttpConnectUtil.WEB + "driver/signout";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		if (HttpConnectUtil.post(url, jsonObj, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) == 0) {
				mHeart.stop();
				mDriver.setStatus(DriverConst.IDLE);
				mDriver.setPriStatus(DriverConst.VISITOR);
				return true;
			}
		}
		return false;
	}
	// create an account by offer your information
	public void account() {
		Intent intent = new Intent(mDriver.getBaseContext(), Login.class);
		Bundle bundle = new Bundle();
		bundle.putInt(DriverConst.EXE_CODE, DriverConst.EXE_ACCOUNT);
		intent.putExtras(bundle);
		mDriver.startActivityForResult(intent, DriverConst.EXE_ACCOUNT);
	} 
	// declare a taxi to be free,subsequently,one can order this taxi by a
	// "CALL_TAXI" request
	public boolean declareFree(int id) {
		String url = HttpConnectUtil.WEB + "service/complete";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("id", id);
		} catch (JSONException e) {
			e.printStackTrace();
			Log.d("complete", "complete failed!");
			return false;
		}
		if (HttpConnectUtil.post(url, jsonObj, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
				Log.d("complete", "failed to connect to server!");
				return false;
			}
		}
		return true;
	}
	public void execute(){
		updateLocation();
		// update passenger
		Message msg = mPsgHandler.obtainMessage();;
		mPsgHandler.sendMessage(msg);
	}
	// calculate one`s location and push this information to taxi engaged person
	private boolean updateLocation() {
		Location loc = null;
		StringBuffer strRet = new StringBuffer();
		TelephonyManager myPhone = (TelephonyManager) mDriver.getBaseContext()
				.getSystemService(Context.TELEPHONY_SERVICE);
		if (myPhone.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
			loc = CellPositioning.getCdmaLocation(myPhone, mDriver.getBaseContext(),
					strRet);
		} else if (myPhone.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
			loc = CellPositioning.getGsmLocation(myPhone, strRet);
		}
		if (loc != null) {
			Double lon = loc.getLongitude() * DriverConst.LOC2GEO;
			Double lat = loc.getLatitude() * DriverConst.LOC2GEO;
			// get geo point
			synchronized (DriverConst.SyncObj) {
				GeoPoint p = new GeoPoint(lat.intValue(), lon.intValue());
				if (mTaxiPos == null || CalcDistance(p, mTaxiPos) >= 100) {
					mTaxiPos = p;
					mHandler.sendMessage(mHandler.obtainMessage());
				}
			}
			return true;
		}
		return false;
	}
	public void updateStatus(int status) {
		String url = HttpConnectUtil.WEB + "driver/taxi/update";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("state", status);
		} catch (JSONException e) {
			e.printStackTrace();
			Log.d("status", "update status failed!");
		}
		if (HttpConnectUtil.post(url, jsonObj, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
				Log.d("status", "failed to connect to server!");
			}
		}
	}

	public void processActivityResult(int requestCode, int resultCode) {
		if (DriverConst.EXE_LOGIN == requestCode) {
			if (Login.LOGIN_SUCCEED == resultCode) {
				mHeart.start();
			}
		}
	}
	
	private double CalcDistance(GeoPoint from, GeoPoint to)
	{
	    double rad = 6371*1000; //Earth radius in m
	    //Convert to radians
	    double p1X = from.getLongitudeE6() / 180.0 /DriverConst.LOC2GEO* Math.PI;
	    double p1Y = from.getLatitudeE6() / 180.0/DriverConst.LOC2GEO * Math.PI;
	    double p2X = to.getLongitudeE6() / 180.0/DriverConst.LOC2GEO * Math.PI;
	    double p2Y = to.getLatitudeE6() / 180.0/DriverConst.LOC2GEO * Math.PI;
	    return Math.abs(Math.acos(Math.sin(p1Y) * Math.sin(p2Y) +
	        Math.cos(p1Y) * Math.cos(p2Y) * Math.cos(p2X - p1X)) * rad);
	}

	public void updatePassenger(int state, PassengerInfo psg) {
		if(state == DriverConst.RUNNING){
			synchronized(DriverConst.SyncObj){
				mLstPsgInfo.clear();
				mLstPsgInfo.add(psg);
			}
		}
		
	}
}
