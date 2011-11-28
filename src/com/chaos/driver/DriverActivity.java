package com.chaos.driver;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.driver.Util.HttpConnectUtil;
import com.chaos.driver.Util.HttpConnectUtil.ResonpseData;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import com.google.android.maps.*;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DriverActivity extends MapActivity {
	/** Called when the activity is first created. */
	// UI elements declaration
	private MapView mView;
	private MapController mCtrl;
	private Thread locationUpdateThread = null;
	private Thread beatHeartThread = null;
	private Button btnLoginout = null; // the button used to login & logout
	private Button btnAccount = null; // the button used to create an account
	private Button btnFree = null; // the button used to declare a taxi to be on
									// hire
	private TextView tvMsg = null;
	private TextView tvOutput = null;
	// normal variable declaration
	// mark activity be running or not
	// update location if this variable is true
	private boolean isRunning = true;
	private DriverInfo mDrvInfo;
	private List<PassengerInfo> mLstPsgInfo;
	private int mStatus;	//current status of the taxi
	private Object mSycObj = new Object();

	//temp variables
	private PassengerInfo mSelPassenger;		//the selected passenger 
	private boolean bFirstInitMap;	//has map been first initialized or not
	// handler for the locationUpdateThread thread
	private Handler thHandler = null;
	private Handler bhHandler = null; // beat heart handler
	private Overlay mTaxiOverlay = null;
	private GeoPoint mTaxiPos;

	// constant
	private final static int LOCATION_UPDATE_INTERVAL = 5000; // unit ms
	private final static int BEAT_HEART_INTERVAL = 5000; // unit ms
	public final static String EXE_CODE = "exe_code";
	private final static String LOC_RESPONSE = "loc response";
	private final static String LOC_LON = "longitude";
	private final static String LOC_LAT = "latitude";
	private final static String REQ_CNT = "request count";
	private final static double LOC2GEO = 1000000.0D;
	public final static String CALLER = "caller info";
	// { exe_code enumeration
	public final static int EXE_LOGIN = 0x10000001;
	public final static int EXE_ACCOUNT = 0x10000002;
	public final static int EXE_CALL_RESPONSE = 0x10000003;

	// }
	//{ taxi status
	public final static int OUT_OF_SERVICE = 0;
	public final static int IDLE = 1;
	public final static int RUNNING = 0;
	//}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		this.updateStatus(IDLE);
		initViews();
		initLocation();
		initThread();
		mDrvInfo = new DriverInfo("visitor", "", "");
		mLstPsgInfo = new LinkedList<PassengerInfo>();
	}

	private void initViews() {
		// UI elements initialization
		mView = (MapView) findViewById(R.id.mapView);
		btnLoginout = (Button) findViewById(R.id.login);
		btnAccount = (Button) findViewById(R.id.register);
		btnFree = (Button) findViewById(R.id.free_declare);
		tvMsg = (TextView)findViewById(R.id.message);
		tvMsg.setVisibility(View.GONE);
		btnLoginout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (btnLoginout.getText().equals("login")) {
					login();
				} else {
					logout();
				}
			}
		});
		btnAccount.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				account();
			}
		});
		btnFree.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				declareFree();
				btnAccount.setVisibility(Button.GONE);
			}
		});
		btnFree.setVisibility(View.GONE);
	}

	private void initLocation() {
		mView.setBuiltInZoomControls(true);
		mCtrl = mView.getController();
		mView.displayZoomControls(true);
		mView.setSatellite(false);
		mView.setStreetView(true);
		mView.setTraffic(false);
		mCtrl.setZoom(13);
		tvMsg.setVisibility(View.VISIBLE);
		/*String[] pointArray = { "32.03", "118.46" };
		double lat = Double.parseDouble(pointArray[0]);
		double lon = Double.parseDouble(pointArray[1]);
		GeoPoint p = new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6));
		taxiMove(p);*/
	}

	private void initThread() {
		locationUpdateThread = new Thread(new Runnable() {
			public void run() {
				while (isRunning) {
					updateLocation();
					try {
						Thread.sleep(LOCATION_UPDATE_INTERVAL);
					} catch (Throwable e) {

					}
				}
			}
		});
		beatHeartThread = new Thread(new Runnable() {
			public void run() {
				while (isRunning) {
					beatHeart();
					try {
						Thread.sleep(BEAT_HEART_INTERVAL);
					} catch (Throwable e) {

					}
				}
			}
		});
		thHandler = new Handler() {
			public void handleMessage(Message msg) {
				if(!bFirstInitMap){
					tvMsg.setVisibility(View.GONE);
					beatHeartThread.start();
					bFirstInitMap = true;
				}
				Double lon = msg.getData().getDouble(LOC_LON) * LOC2GEO;
				Double lat = msg.getData().getDouble(LOC_LAT) * LOC2GEO;
				// get geo point
				GeoPoint p = new GeoPoint(lat.intValue(), lon.intValue());
				taxiMove(p);
			}
		};
		bhHandler = new Handler() {
			public void handleMessage(Message msg) {
				synchronized (mSycObj) {
					int reqCnt = mLstPsgInfo.size();
					mView.getOverlays().clear();
					taxiMove(mTaxiPos);
					for (int i = 0; i < reqCnt; i++) {
						PassengerInfo psg = mLstPsgInfo.get(i);
						Double lon = psg.getSrcLoc().getLongitude()*1000000;
						Double lat = psg.getSrcLoc().getLatitude()*1000000;
						// get geo point
						GeoPoint p = new GeoPoint(lat.intValue(),
								lon.intValue());
						addMetaPassenger(p,psg);
					}
					mLstPsgInfo.clear();
				}
			}
		};
		locationUpdateThread.start();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (EXE_LOGIN == requestCode) {
			if (Login.LOGIN_SUCCEED == resultCode) {
				btnLoginout.setText("logout");
				btnAccount.setVisibility(View.GONE);
				try {
					JSONObject jsonSelf = new JSONObject(
							data.getStringExtra(Login.RET_OBJ));
					mDrvInfo.setPhoneNumber(jsonSelf.getString("phone_number"));
					mDrvInfo.setNickName(jsonSelf.getString("nickname"));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				btnLoginout.setText("login");
			}
			
			String msg = data.getStringExtra(Login.RET_MSG);
			if (msg != null) {
				if (msg.length() > 0) {
					Toast.makeText(mView.getContext(), msg, Toast.LENGTH_LONG)
							.show();
				}
			}
		} else if (EXE_ACCOUNT == requestCode) {
			if (Login.REGISTER_SUCCED == resultCode) {
				btnAccount.setVisibility(View.GONE);
			}
			String msg = data.getStringExtra(Login.RET_MSG);
			if (msg != null) {
				if (msg.length() > 0) {
					Toast.makeText(mView.getContext(), msg, Toast.LENGTH_LONG)
							.show();
				}
			}
		} else if (EXE_CALL_RESPONSE == requestCode) { // call taxi response

			try {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("id", mSelPassenger.getID());
				String url = HttpConnectUtil.WEB + "service/reply";
				HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
				if (HireCall.ACCPECT == resultCode) { // accept this request
					
					jsonObj.put("accept", true);
					btnFree.setVisibility(View.VISIBLE);
					this.updateStatus(RUNNING);
					synchronized (mSycObj) {
						mLstPsgInfo.clear();
						mLstPsgInfo.add(mSelPassenger);
						bhHandler.sendMessage(bhHandler.obtainMessage());
					}
				} else {
					// do nothing
					jsonObj.put("accept", false);
					btnFree.setVisibility(View.GONE);
					this.updateStatus(IDLE);
				}
				if (HttpConnectUtil.post(url, jsonObj, rd)) {
					if (HttpConnectUtil.parseLoginResponse(rd.strResponse) == 0) {
						Toast.makeText(mView.getContext(),
								"Prepare to pick passenger!", Toast.LENGTH_LONG)
								.show();
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	protected boolean isRouteDisplayed() {
		return false;
	}

	// calculate one`s location and push this information to taxi engaged person
	private boolean updateLocation() {
		Location loc = null;
		StringBuffer strRet = new StringBuffer();
		TelephonyManager myPhone = (TelephonyManager) this.getBaseContext()
				.getSystemService(TELEPHONY_SERVICE);
		if (myPhone.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
			loc = CellPositioning.getCdmaLocation(myPhone, getBaseContext(),
					strRet);
		} else if (myPhone.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
			loc = CellPositioning.getGsmLocation(myPhone, strRet);
		}
		if (loc != null) {
			Message msg = thHandler.obtainMessage();
			Bundle bundle = new Bundle();
			bundle.putString(LOC_RESPONSE, strRet.toString());
			bundle.putDouble(LOC_LON, loc.getLongitude());
			bundle.putDouble(LOC_LAT, loc.getLatitude());
			msg.setData(bundle);
			thHandler.sendMessage(msg);
			return true;
		}
		return false;
	}

	// make one be authorized
	private void login() {
		Intent intent = new Intent(DriverActivity.this, Login.class);
		Bundle bundle = new Bundle();
		bundle.putInt(EXE_CODE, EXE_LOGIN);
		intent.putExtras(bundle);
		this.startActivityForResult(intent, EXE_LOGIN);

	}

	// could be called only while one has been authorized
	private void logout() {
		String url = HttpConnectUtil.WEB + "driver/signout";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		if (HttpConnectUtil.post(url, jsonObj, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) == 0) {
				btnLoginout.setText("login");
				btnAccount.setVisibility(View.VISIBLE);
				Toast.makeText(mView.getContext(), "log out succeed!",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	// create an account by offer your information
	private void account() {
		Intent intent = new Intent(DriverActivity.this, Login.class);
		Bundle bundle = new Bundle();
		bundle.putInt(EXE_CODE, EXE_ACCOUNT);
		intent.putExtras(bundle);
		this.startActivityForResult(intent, EXE_ACCOUNT);
	}

	// declare a taxi to be free,subsequently,one can order this taxi by a
	// "CALL_TAXI" request
	private void declareFree() {
		String url = HttpConnectUtil.WEB + "service/complete";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		try{
			jsonObj.put("id", 123);
		}catch(JSONException e){
			e.printStackTrace();
			Log.d("complete","complete failed!");
		}
		if (HttpConnectUtil.get(url, jsonObj, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
				Log.d("complete","failed to connect to server!");
			}
		}
		
		updateStatus(IDLE);
		synchronized (mSycObj) {
			mLstPsgInfo.clear();
			bhHandler.sendMessage(bhHandler.obtainMessage());
		}
	}
	private void updateStatus(int status){
		setStatus(status);
		String url = HttpConnectUtil.WEB + "driver/update";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		try{
			jsonObj.put("status", status);
		}catch(JSONException e){
			e.printStackTrace();
			Log.d("status","update status failed!");
		}
		if (HttpConnectUtil.get(url, jsonObj, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
				Log.d("status","failed to connect to server!");
			}
		}
	}
	private void addTaxiOverlay(GeoPoint p) {
		mView.getOverlays().remove(mTaxiOverlay);
		mTaxiOverlay = new MapOverLay(mView, p, BitmapFactory.decodeResource(
				this.getResources(), R.drawable.taxi),null);
		mView.getOverlays().add(mTaxiOverlay);
	}

	private void taxiMove(GeoPoint p) {
		mCtrl.animateTo(p);
		mTaxiPos = p;
		addTaxiOverlay(p);
		mView.invalidate();
		String url = HttpConnectUtil.WEB + "driver/location/update";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		JSONObject jsonObj = new JSONObject();
		try{
			jsonObj.put("latitude", p.getLatitudeE6());
			jsonObj.put("longitude", p.getLatitudeE6());
		}catch(JSONException e){
			e.printStackTrace();
			Log.d("update location","update location failed!");
		}
		if (HttpConnectUtil.get(url, jsonObj, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) != 0) {
				Log.d("update location","failed to connect to server!");
			}
		}
	}

	private void addMetaPassenger(GeoPoint p,PassengerInfo info) {
		mView.getOverlays().add(
				new MapOverLay(mView, p, BitmapFactory.decodeResource(
						this.getResources(), R.drawable.passenger),info){
					public boolean onTouchEvent(MotionEvent e, MapView view) {
						Point pt = new Point();
						pt.x = (int) e.getX();
						pt.y = (int) e.getY();
						if (beBeneathOverlay(pt)) {
							if (MotionEvent.ACTION_MOVE == e.getAction()) {
								Intent intent = new Intent(DriverActivity.this,
										HireCall.class);
								Bundle bundle = new Bundle();
								bundle.putString(CALLER,
										this.mUserInfo.toString());
								intent.putExtras(bundle);
								startActivityForResult(intent,
										EXE_CALL_RESPONSE);
								mSelPassenger = (PassengerInfo) this.mUserInfo;
								return true;
							}
						}
						return false;
					}
				});
		mView.invalidate();
	}

	private void beatHeart() {
		String url = HttpConnectUtil.WEB + "driver/refresh";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		//if (HttpConnectUtil.get(url, null, rd)) {
		//a dummy one
		if(genDummyUser(rd)){
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) == 0) {
				Message msg = bhHandler.obtainMessage();
				try {
					JSONObject jsonResp = new JSONObject(rd.strResponse);
					if (jsonResp.length() > 0) {
						String strMsg = jsonResp.getString("messages");
						// JSONObject jsonMsg = new JSONObject(strMsg);
						JSONArray jsonArray = new JSONArray(strMsg);
						int iMsgCnt = jsonArray.length();
						synchronized(mSycObj){
							for (int i = 0; i < iMsgCnt; i++) {
								JSONObject jsonMsg = jsonArray.getJSONObject(i);
								String type = jsonMsg.getString("type");
								if (type.equalsIgnoreCase("call-taxi")
										&& mStatus == IDLE) {
									PassengerInfo psg = new PassengerInfo();
									JSONObject jsonPassenger = new JSONObject(
											jsonMsg.getString("passenger"));
									psg.setNickName(jsonPassenger
											.getString("nickname"));
									psg.setPhoneNumber(jsonPassenger
											.getString("phone_number"));
									psg.setID(jsonMsg.getInt("id"));
									psg.setTimeStamp(jsonMsg
											.getInt("timestamp"));
									JSONObject jsonSrcLoc = new JSONObject(
											jsonMsg.optString("location"));
									Location loc = new Location("chaos lab");
									loc.setLongitude(jsonSrcLoc
											.optDouble(LOC_LON));
									loc.setLatitude(jsonSrcLoc
											.optDouble(LOC_LAT));
									psg.setSrcLoc(loc);
									JSONObject jsonDestLoc = new JSONObject(
											jsonMsg.optString("destination"));
									loc = new Location("chaos lab");
									loc.setLongitude(jsonDestLoc
											.optDouble(LOC_LON));
									loc.setLatitude(jsonDestLoc
											.optDouble(LOC_LAT));
									psg.setDestLoc(loc);
									mLstPsgInfo.add(psg);
								} else if (type
										.equalsIgnoreCase("call-taxi-cancel")
										&& mStatus == RUNNING) {

								}else if(type.equalsIgnoreCase("location-update")){
									String pn = jsonMsg.getString("phone_number");
									if (mSelPassenger.getPhoneNumber()
											.equalsIgnoreCase(pn)) {
										Location loc = new Location("chaos lab");
										JSONObject jsonSrcLoc = new JSONObject(
												jsonMsg.optString("location"));
										loc.setLongitude(jsonSrcLoc
												.optDouble(LOC_LON));
										loc.setLatitude(jsonSrcLoc
												.optDouble(LOC_LAT));
										mSelPassenger.setSrcLoc(loc);
										mLstPsgInfo.add(mSelPassenger);
									}
									
								}
							}
						}
					}
					bhHandler.sendMessage(msg);
					//treat it as be running while get message
					//the status will be changed to the real value after the message being processed
					setStatus(RUNNING);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	private boolean genDummyUser(ResonpseData rd) {
		if( mStatus == RUNNING){
			return false;
		}
		JSONObject jsonObj = new JSONObject();
		try{
			jsonObj.put("status", 0);
			JSONArray message = new JSONArray();
			JSONObject m1 = new JSONObject();
			m1.put("type", "call-taxi");
			JSONObject p1 = new JSONObject();
			p1.put("phone_number", "1384323242");
			p1.put("nickname","liufy");
			m1.put("passenger", p1);
			JSONObject srcloc1 = new JSONObject();
			srcloc1.put("longitude", 118.90);
			srcloc1.put("latitude", 32.056);
			m1.put("location",srcloc1);
			JSONObject destloc1 = new JSONObject();
			destloc1.put("longitude", 118.23432);
			destloc1.put("latitude", 32.4343);
			m1.put("destination",destloc1);
			m1.put("id", 1321875549);
			m1.put("timestamp", 1321875549);
			message.put(0, m1);
			
			//m2
			JSONObject m2 = new JSONObject();
			m2.put("type", "call-taxi");
			JSONObject p2 = new JSONObject();
			p2.put("phone_number", "13913394580");
			p2.put("nickname","canglj");
			m2.put("passenger", p2);
			JSONObject srcloc2 = new JSONObject();
			srcloc2.put("longitude", 118.95);
			srcloc2.put("latitude", 32.090);
			m2.put("location",srcloc2);
			JSONObject destloc2 = new JSONObject();
			destloc2.put("longitude", 118.463);
			destloc2.put("latitude", 32.032);
			m2.put("destination",destloc2);
			m2.put("id", 1321875549);
			m2.put("timestamp", 1321875549);
			message.put(1, m2);
			jsonObj.put("messages", message);
			rd.strResponse = jsonObj.toString();
		}catch(JSONException e){
			e.printStackTrace();
		}
		
		return true;
	}

	//getter && setter
	public int getStatus() {
		return mStatus;
	}

	public void setStatus(int mStatus) {
		this.mStatus = mStatus;
	}
}