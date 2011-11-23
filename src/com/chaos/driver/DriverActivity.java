package com.chaos.driver;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.driver.Util.HttpConnectUtil;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

import com.google.android.maps.*;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.telephony.TelephonyManager;
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
	private TextView tvOutput = null;
	// normal variable declaration
	// mark activity be running or not
	// update location if this variable is true
	private boolean isRunning = true;
	private DriverInfo mDrvInfo;
	private PassengerInfo mPsgInfo;
	// handler for the locationUpdateThread thread
	Handler thHandler = null;
	Handler bhHandler = null; // beat heart handler
	Overlay mTaxiOverlay = null;
	GeoPoint mTaxiPos;

	// constant
	private final static int LOCATION_UPDATE_INTERVAL = 5000; // unit ms
	private final static int BEAT_HEART_INTERVAL = 5000; // unit ms
	public final static String EXE_CODE = "exe_code";
	private final static String LOC_RESPONSE = "loc response";
	private final static String LOC_LON = "Longitude";
	private final static String LOC_LAT = "latitude";
	private final static String REQ_CNT = "request count";
	private final static double LOC2GEO = 1000000.0D;
	public final static String CALLER = "caller info";
	// { exe_code enumeration
	public final static int EXE_LOGIN = 0x10000001;
	public final static int EXE_ACCOUNT = 0x10000002;
	public final static int EXE_CALL_RESPONSE = 0x10000003;

	// }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		initViews();
		initLocation();
		initThread();
		mDrvInfo = new DriverInfo("visitor", "", "");
	}

	private void initViews() {
		// UI elements initialization
		mView = (MapView) findViewById(R.id.mapView);
		btnLoginout = (Button) findViewById(R.id.login);
		btnAccount = (Button) findViewById(R.id.register);
		btnFree = (Button) findViewById(R.id.free_declare);
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
	}

	private void initLocation() {
		mView.setBuiltInZoomControls(true);
		mCtrl = mView.getController();
		mView.displayZoomControls(true);
		mView.setSatellite(false);
		mView.setStreetView(true);
		mView.setTraffic(false);
		String[] pointArray = { "32.03", "118.46" };
		double lat = Double.parseDouble(pointArray[0]);
		double lon = Double.parseDouble(pointArray[1]);
		GeoPoint p = new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6));
		taxiMove(p);
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
				// test information
				// String strLocResponse =
				// msg.getData().getString(LOC_RESPONSE);
				// tvOutput.setText(strLocResponse);
				Double lon = msg.getData().getDouble(LOC_LON) * LOC2GEO;
				Double lat = msg.getData().getDouble(LOC_LAT) * LOC2GEO;
				// get geo point
				GeoPoint p = new GeoPoint(lat.intValue(), lon.intValue());
				taxiMove(p);
			}
		};
		bhHandler = new Handler() {
			public void handleMessage(Message msg) {
				int reqCnt = msg.getData().getInt(REQ_CNT);
				for (int i = 0; i < reqCnt; i++) {
					Double lon = msg.getData().getDouble(LOC_LON) * LOC2GEO;
					Double lat = msg.getData().getDouble(LOC_LAT) * LOC2GEO;
					// get geo point
					GeoPoint p = new GeoPoint(lat.intValue(), lon.intValue());
					// TODO: show geography location of caller
					// addMetaPassenger(p);
					try {
						JSONObject jsonObj = new JSONObject(msg.getData()
								.getString(CALLER));
						mPsgInfo = new PassengerInfo(
								jsonObj.getString("nickname"),
								jsonObj.getString("phone_number"));
						Intent intent = new Intent(DriverActivity.this,
								HireCall.class);
						Bundle bundle = new Bundle();
						bundle.putString(CALLER, mPsgInfo.toString());
						intent.putExtras(bundle);
						startActivityForResult(intent, EXE_CALL_RESPONSE);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		};
		// locationUpdateThread.start();
		beatHeartThread.start();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (EXE_LOGIN == requestCode) {
			if (Login.LOGIN_SUCCEED == resultCode) {
				btnLoginout.setText("logout");
				btnAccount.setVisibility(View.GONE);
				try {
					String str = data.getStringExtra(Login.RET_OBJ);
					JSONObject jsonSelf = new JSONObject(
							data.getStringExtra(Login.RET_OBJ));
					mDrvInfo.setmPhoneNumber(jsonSelf.getString("phone_number"));
					mDrvInfo.setmNickName(jsonSelf.getString("nickname"));
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
				jsonObj.put("to", mPsgInfo.getmPhoneNumber());
				// TODO:real number
				jsonObj.put("from", mDrvInfo.getmPhoneNumber());
				jsonObj.put("type", "call-taxi-reply");
				JSONObject jsonData = new JSONObject();
				String url = HttpConnectUtil.WEB + "driver/message";
				HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
				if (HireCall.ACCPECT == resultCode) { // accept this request
					// TODO:UI elements update
					jsonData.put("accept", true);
					btnFree.setVisibility(View.GONE);

				} else {
					// do nothing
					jsonData.put("accept", false);
					btnFree.setVisibility(View.VISIBLE);
				}
				jsonObj.put("data", jsonData);
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
	private void updateLocation() {
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
		}
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

	}

	private void addTaxiOverlay(GeoPoint p) {
		mView.getOverlays().remove(mTaxiOverlay);
		mTaxiOverlay = new MapOverLay(mView, p, BitmapFactory.decodeResource(
				this.getResources(), R.drawable.driver));
		mView.getOverlays().add(mTaxiOverlay);
	}

	private void taxiMove(GeoPoint p) {
		mCtrl.animateTo(p);
		mTaxiPos = p;
		addTaxiOverlay(p);
		mView.invalidate();
	}

	private void addMetaPassenger(GeoPoint p) {
		mView.getOverlays().add(
				new MapOverLay(mView, p, BitmapFactory.decodeResource(
						this.getResources(), R.drawable.passenger)));
	}

	private void beatHeart() {
		String url = HttpConnectUtil.WEB + "driver/refresh";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		if (HttpConnectUtil.get(url, null, rd)) {
			if (HttpConnectUtil.parseLoginResponse(rd.strResponse) == 0) {
				Message msg = bhHandler.obtainMessage();
				Bundle bundle = new Bundle();
				bundle.putString(LOC_RESPONSE, rd.strResponse);
				try {
					JSONObject jsonResp = new JSONObject(rd.strResponse);
					if (jsonResp.length() > 0) {
						String strMsg = jsonResp.getString("messages");
						// JSONObject jsonMsg = new JSONObject(strMsg);
						JSONArray jsonArray = new JSONArray(strMsg);
						if (jsonArray.length() > 0) {
							JSONObject jsonMsg = jsonArray.getJSONObject(0);
							String type = jsonMsg.getString("type");
							if (type.equalsIgnoreCase("call-taxi")) {
								// TODO: process
								JSONObject jsonData = new JSONObject(
										jsonMsg.getString("data"));
								JSONObject jsonPassenger = new JSONObject(
										jsonData.getString("passenger"));
								bundle.putString(CALLER,
										jsonPassenger.toString());
								bundle.putDouble(LOC_LON, Double
										.parseDouble(jsonPassenger
												.getString(LOC_LON)));
								bundle.putDouble(LOC_LAT, Double
										.parseDouble(jsonPassenger
												.getString(LOC_LAT)));
								bundle.putInt(REQ_CNT, 1); // only one passenger
															// avariable so far
							}
						}
					}
					msg.setData(bundle);
					bhHandler.sendMessage(msg);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
}