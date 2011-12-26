package com.chaos.driver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.driver.record.RecordDataProvider;
import com.chaos.driver.util.CellPositioning;
import com.chaos.driver.util.DriverConst;
import com.chaos.driver.util.HttpConnectUtil;
import com.google.android.maps.GeoPoint;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.TelephonyManager;
import android.util.Log;

public class DriverAssist implements Executive, RecordDataProvider, Parcelable {
	private DriverActivity mDriver;
	private Handler mHandler;
	private Handler mPsgHandler;
	private GeoPoint mTaxiPos;
	private PassengerSrcProvider mPsgProvider;
	// focused passengers,need to synchronize
	private List<PassengerInfo> mLstPsgInfo;
	private Heart mHeart;
	private long mCommentId;

	public DriverAssist(DriverActivity d) {
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

		mPsgHandler = new Handler() {
			public void handleMessage(Message msg) {
				// only authorized user can get these message
				if (mDriver.getPriStatus() == DriverConst.LOGIN) {
					// synchronize
					synchronized (DriverConst.SyncObj) {
						// get passenger update information from provider
						List<PassengerInfo> lstInfo = new LinkedList<PassengerInfo>();
						int type = mPsgProvider.getPassengerResource(lstInfo);
						if (-1 == type) {
							mHeart.stop();
							mDriver.setStatus(DriverConst.IDLE);
							mDriver.setPriStatus(DriverConst.VISITOR);
						} else {
							int iSize = lstInfo.size();
							// if some fresh one comes
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
								switch (mDriver.getStatus()) {
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
											PassengerInfo myPsg = lstInfo
													.get(p);
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
									if (needRemove) {
										mLstPsgInfo.remove(myPsg);
										mDriver.cancelTaxi(myPsg);
										if (mLstPsgInfo.size() == 0) {
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

	public DriverAssist() {

	}

	public void login() {
		Intent intent = new Intent(mDriver.getBaseContext(), Login.class);
		Bundle bundle = new Bundle();
		bundle.putInt(DriverConst.EXE_CODE, DriverConst.EXE_LOGIN);
		intent.putExtras(bundle);
		mDriver.startActivityForResult(intent, DriverConst.EXE_LOGIN);
	}

	public boolean logout() {
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

	public void execute() {
		updateLocation();
		// update passenger
		Message msg = mPsgHandler.obtainMessage();
		;
		mPsgHandler.sendMessage(msg);
	}

	// calculate one`s location and push this information to taxi engaged person
	private boolean updateLocation() {
		Location loc = null;
		StringBuffer strRet = new StringBuffer();
		TelephonyManager myPhone = (TelephonyManager) mDriver.getBaseContext()
				.getSystemService(Context.TELEPHONY_SERVICE);
		if (myPhone.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
			loc = CellPositioning.getCdmaLocation(myPhone,
					mDriver.getBaseContext(), strRet);
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
			if (DriverConst.LOGIN_SUCCEED == resultCode) {
				mHeart.start();
			}
		}
	}

	private double CalcDistance(GeoPoint from, GeoPoint to) {
		double rad = 6371 * 1000; // Earth radius in m
		// Convert to radians
		double p1X = from.getLongitudeE6() / 180.0 / DriverConst.LOC2GEO
				* Math.PI;
		double p1Y = from.getLatitudeE6() / 180.0 / DriverConst.LOC2GEO
				* Math.PI;
		double p2X = to.getLongitudeE6() / 180.0 / DriverConst.LOC2GEO
				* Math.PI;
		double p2Y = to.getLatitudeE6() / 180.0 / DriverConst.LOC2GEO * Math.PI;
		return Math.abs(Math.acos(Math.sin(p1Y) * Math.sin(p2Y) + Math.cos(p1Y)
				* Math.cos(p2Y) * Math.cos(p2X - p1X))
				* rad);
	}

	public void updatePassenger(int state, PassengerInfo psg) {
		if (state == DriverConst.RUNNING) {
			synchronized (DriverConst.SyncObj) {
				mLstPsgInfo.clear();
				mLstPsgInfo.add(psg);
			}
		}

	}

	public void comment( int score, String strComment) {
		String url = HttpConnectUtil.WEB + "service/evaluate";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("id", mCommentId);
			jsonObj.put("score", score);
			if (strComment != null && strComment.length() > 0) {
				jsonObj.put("comment", strComment);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Log.d("comment", "comment failed!");
		}
		if (HttpConnectUtil.post(url, jsonObj, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
				Log.d("comment", "failed to connect to server!");
			}
		}
	}

	/*****************************************************************/
	/*
	 * get the evaluation information for the driver
	 */
	public void getDrvEvlInfo(DriverInfo drv) {
		String result = getEvalFromTime(System.currentTimeMillis(),
				drv.getPhoneNumber(), 1);
		if (result != null && result.length() > 0) {
			try {
				JSONObject jsonObj = new JSONObject(result);
				drv.setScore(jsonObj.getDouble("average_score"));
				drv.setTotal(jsonObj.getInt("total_service_count"));
			} catch (JSONException e) {
				e.printStackTrace();
				Log.d("evaluation", "get evaluation failed!");
			}
		}
	}

	// get evaluations from ids
	public String getEvalFromIds(long[] ids) {
		String url = HttpConnectUtil.WEB + "service/evaluations";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		try {
			ArrayList<Long> c = new ArrayList<Long>();
			for(int i=0;i<ids.length;i++){
				c.add(new Long(ids[i]));
			}
			JSONArray ja = new JSONArray(c);
			jsonObj.put("ids", ja);
			if (HttpConnectUtil.get(url, jsonObj, rd)) {
				if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
					Log.d("get evaluations", "failed to connect to server!");
				} else {
					return rd.strResponse;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Log.d("get evaluations", "get evaluations failed!");
			return null;
		}
		return null;
	}

	// get evaluation from time
	public String getEvalFromTime(long time, String phoneNum, int count) {
		String url = HttpConnectUtil.WEB + "service/user/evaluations";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("phone_number", phoneNum);
			jsonObj.put("start_time", time);
			jsonObj.put("count", count);
		} catch (JSONException e) {
			e.printStackTrace();
			Log.d("get evaluations", "get evaluations failed!");
			return null;
		}
		if (HttpConnectUtil.get(url, jsonObj, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
				Log.d("get evaluations", "failed to connect to server!");
			} else {
				return rd.strResponse;
			}
		}
		return null;
	}

	// get histories
	public String getHistory(long start, long end, int count) {
		String url = HttpConnectUtil.WEB + "service/history";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("start_time", start);
			jsonObj.put("end_time", end);
			if (count == 0) {
				count = 10;
			}
			jsonObj.put("count", count);
		} catch (JSONException e) {
			e.printStackTrace();
			Log.d("get evaluations", "get evaluations failed!");
			return null;
		}
		if (HttpConnectUtil.get(url, jsonObj, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
				Log.d("get evaluations", "failed to connect to server!");
			} else {
				return rd.strResponse;
			}
		}
		return null;
	}

	public void pause(SharedPreferences.Editor editor) {
		editor.putInt(DriverConst.LOC_LAT, mTaxiPos.getLatitudeE6());
		editor.putInt(DriverConst.LOC_LON, mTaxiPos.getLongitudeE6());
		mHeart.stop();
	}

	public void resume(SharedPreferences prefs) {
		mTaxiPos = new GeoPoint(prefs.getInt(DriverConst.LOC_LAT, 0),
				prefs.getInt(DriverConst.LOC_LON, 0));
		if (DriverConst.LOGIN == mDriver.getPriStatus()) {
			mHeart.start();
		}
	}

	public void destroy() {
		mHeart.stop();
	}

	public boolean registerDirectly(String user, String psw, Bundle bundle) {
		String url = HttpConnectUtil.WEB + DriverConst.LOGIN_SITE;
		try {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("phone_number", user);
			jsonObj.put("password", psw);
			HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
			if (HttpConnectUtil.post(url, jsonObj, rd)) {
				if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
					Log.d("get evaluations", "failed to connect to server!");
					bundle.putString(DriverConst.RET_MSG,
							"login failed! \ndetail: \n" + rd.strResponse);
					return false;
				} else {
					jsonObj = new JSONObject(rd.strResponse);
					bundle.putString(DriverConst.RET_MSG,
							jsonObj.getString("message"));
					bundle.putString(DriverConst.RET_OBJ, jsonObj
							.getJSONObject("self").toString());
					return true;
				}
			} else {
				Log.e("login", rd.strResponse);
				return false;
			}

		} catch (JSONException jsonException) {
			jsonException.printStackTrace();
			bundle.putString(DriverConst.RET_MSG, "login failed! ");
			return false;
		}

	}

	public static final Parcelable.Creator<DriverAssist> CREATOR = new Creator<DriverAssist>() {
		public DriverAssist createFromParcel(Parcel source) {
			DriverAssist assist = new DriverAssist();
			return assist;
		}

		public DriverAssist[] newArray(int size) {
			return new DriverAssist[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
	}

	public void setCommentId(long id) {
		mCommentId = id;		
	}
}
