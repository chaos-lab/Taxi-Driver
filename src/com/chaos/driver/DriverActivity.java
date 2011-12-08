package com.chaos.driver;

import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.driver.Util.HttpConnectUtil;
import com.google.android.maps.GeoPoint;

import com.google.android.maps.*;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DriverActivity extends MapActivity {
	/** Called when the activity is first created. */
	// UI elements declaration
	private TaxiMapView mView;
	private MapController mCtrl;
	private Button btnLoginout = null; // the button used to login & logout
	private Button btnAccount = null; // the button used to create an account
	private Button btnFree = null; // the button used to declare a taxi to be on
									// hire
	private Button btnBack = null;
	private TextView tvMsg = null;
	private TextView tvStatus = null;
	// normal variable declaration
	private DriverInfo mDrvInfo;
	private int mStatus; // current status of the taxi
	private int mPriStatus; // privilege status
	private int mPerspective;
	// temp variables
	private PassengerInfo mSelPassenger; // the selected passenger
	// handler for the locationUpdateThread thread
	private Overlay mTaxiOverlay = null;
	private GeoPoint mTaxiPos;

	private DriverAssist mAssist;
	private HashMap<PassengerInfo, Overlay> mPsgOverlayMap;

	// }
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mAssist = new DriverAssist(this);
		initViews();
		initLocation();
		this.updateStatus(DriverConst.IDLE);
		setPriStatus(DriverConst.VISITOR);
		updateUI();
		mDrvInfo = new DriverInfo("visitor", "", "");
		mPsgOverlayMap = new HashMap<PassengerInfo, Overlay>();
		Dialog dlg = new AlertDialog.Builder(this).setTitle("test alert dialog")
		.setSingleChoiceItems(R.array.colors, 0, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String []colors = getResources().getStringArray(R.array.colors);
				new AlertDialog.Builder(DriverActivity.this)
                .setMessage("You selected: " + which + " , " + colors[which])
                .show();
				dialog.cancel();
				
			}
		}).create();
		
		dlg.show();
	}

	private void initViews() {
		// UI elements initialization
		mView = (TaxiMapView) findViewById(R.id.mapView);
		mView.setDriver(this);
		btnLoginout = (Button) findViewById(R.id.login);
		btnAccount = (Button) findViewById(R.id.register);
		btnFree = (Button) findViewById(R.id.free_declare);
		tvMsg = (TextView) findViewById(R.id.message);
		tvStatus = (TextView) findViewById(R.id.status);
		btnBack = (Button) findViewById(R.id.backtocar);
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
			}
		});
		btnBack.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				updatePerspective(DriverConst.DRIVE_MODE);
				if (getPriStatus() == DriverConst.LOGIN && mTaxiPos != null) {
					mCtrl.animateTo(mTaxiPos);
				}
			}
		});
		updateUI();
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
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		mAssist.processActivityResult(requestCode, resultCode);
		if (DriverConst.EXE_LOGIN == requestCode) {
			if (Login.LOGIN_SUCCEED == resultCode) {
				setPriStatus(DriverConst.LOGIN);
				try {
					JSONObject jsonSelf = new JSONObject(
							data.getStringExtra(Login.RET_OBJ));
					mDrvInfo.setPhoneNumber(jsonSelf.getString("phone_number"));
					mDrvInfo.setNickName(jsonSelf.getString("nickname"));
					int state = jsonSelf.optInt("state", -1);
					if (state != -1) {
						setStatus(state);
					}
					if(state == DriverConst.RUNNING){
						Log.i("login message",jsonSelf.toString());
						JSONObject jsonPsg = new JSONObject(jsonSelf.getString("passenger"));
						mSelPassenger = new PassengerInfo();
						mSelPassenger.setNickName(jsonPsg
								.getString("nickname"));
						mSelPassenger.setPhoneNumber(jsonPsg
								.getString("phone_number"));
						String strLoc = jsonPsg.optString("location");
						if(strLoc.length() > 0){
							Location srcLoc = new Location("chaos lab");
							JSONObject josnLoc = new JSONObject(strLoc);
							srcLoc.setLongitude(josnLoc.optDouble(DriverConst.LOC_LON));
							srcLoc.setLatitude(josnLoc.optDouble(DriverConst.LOC_LAT));
							mSelPassenger.setSrcLoc(srcLoc);
						}
						
						mSelPassenger.setID(jsonSelf.getInt("id"));
						Double lon = mSelPassenger.getSrcLoc().getLongitude()
								* DriverConst.LOC2GEO;
						Double lat = mSelPassenger.getSrcLoc().getLatitude()
								* DriverConst.LOC2GEO;
						GeoPoint p = new GeoPoint(lat.intValue(),
								lon.intValue());
						MapOverLay o = new PassengerOverlay(mView, p,
								BitmapFactory.decodeResource(
										this.getResources(),
										R.drawable.passenger), mSelPassenger,
								this);
						mView.getOverlays().clear();
						mView.getOverlays().add(o);
						mPsgOverlayMap.put(mSelPassenger, o);
						mAssist.updatePassenger(state, mSelPassenger);
						mView.invalidate();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			String msg = data.getStringExtra(Login.RET_MSG);
			if (msg != null) {
				if (msg.length() > 0) {
					Toast.makeText(mView.getContext(), msg, Toast.LENGTH_LONG)
							.show();
				}
			}
		} else if (DriverConst.EXE_ACCOUNT == requestCode) {
			String msg = data.getStringExtra(Login.RET_MSG);
			if (msg != null) {
				if (msg.length() > 0) {
					Toast.makeText(mView.getContext(), msg, Toast.LENGTH_LONG)
							.show();
				}
			}
		} else if (DriverConst.EXE_CALL_RESPONSE == requestCode) { // call taxi response
			try {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("id", mSelPassenger.getID());
				String url = HttpConnectUtil.WEB + "service/reply";
				HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
				if (HireCall.ACCPECT == resultCode) { // accept this request

					jsonObj.put("accept", true);
					this.setStatus(DriverConst.RUNNING);
					clearOverlay();
					taxiMove(mTaxiPos);
					addMetaPassenger(mSelPassenger);
				} else {
					// do nothing
					jsonObj.put("accept", false);
					this.updateStatus(DriverConst.IDLE);
					cancelTaxi(mSelPassenger);
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
		updateUI();
	}

	protected boolean isRouteDisplayed() {
		return false;
	}

	// make one be authorized
	private void login() {
		mAssist.login();
	}

	// could be called only while one has been authorized
	private void logout() {
		if (mAssist.logout()) {
			setPriStatus(DriverConst.VISITOR);
			Toast.makeText(mView.getContext(), "log out succeed!",
					Toast.LENGTH_LONG).show();
			updateUI();
		}
	}

	// create an account by offer your information
	private void account() {
		mAssist.account();
	}

	// declare a taxi to be free,subsequently,one can order this taxi by a
	// "CALL_TAXI" request
	private void declareFree() {
		if (null == mSelPassenger) {
			setStatus(DriverConst.IDLE);
			updateUI();
		} else if (mAssist.declareFree(mSelPassenger.getID())) {
			setStatus(DriverConst.IDLE);
			updateUI();
			mSelPassenger = null;
			clearOverlay();
			taxiMove(mTaxiPos);
		}
	}

	private void updateStatus(int status) {
		setStatus(status);
		mAssist.updateStatus(status);
	}

	private void addTaxiOverlay(GeoPoint p) {
		mView.getOverlays().remove(mTaxiOverlay);
		mTaxiOverlay = new MapOverLay(mView, p, BitmapFactory.decodeResource(
				this.getResources(), R.drawable.taxi), mDrvInfo);
		mView.getOverlays().add(mTaxiOverlay);
	}

	public void taxiMove(GeoPoint p) {
		if (p != null) {
			mCtrl.animateTo(p);
			mTaxiPos = p;
			addTaxiOverlay(p);
			mView.invalidate();
			tvMsg.setVisibility(View.GONE);
		}
	}

	public void addMetaPassenger(PassengerInfo info) {
		if (getStatus() == DriverConst.RUNNING) {
			if (info != mSelPassenger) {
				return;
			}
		} else {
			setStatus(DriverConst.PREPROCESS);
		}
		Double lon = info.getSrcLoc().getLongitude() * DriverConst.LOC2GEO;
		Double lat = info.getSrcLoc().getLatitude() * DriverConst.LOC2GEO;
		GeoPoint p = new GeoPoint(lat.intValue(), lon.intValue());
		MapOverLay o = new PassengerOverlay(mView, p,
				BitmapFactory.decodeResource(this.getResources(),
						R.drawable.passenger), info, this);
		mView.getOverlays().add(o);
		mPsgOverlayMap.put(info, o);
		mView.invalidate();
	}

	public boolean updatePassenger(PassengerInfo info) {
		if (getStatus() == DriverConst.RUNNING) {
			if (info != mSelPassenger) {
				return false;
			}
		}
		if (mPsgOverlayMap.containsKey(info)) {
			MapOverLay old = (MapOverLay) mPsgOverlayMap.get(info);
			mView.getOverlays().remove(old);
			Double lon = info.getSrcLoc().getLongitude() * DriverConst.LOC2GEO;
			Double lat = info.getSrcLoc().getLatitude() * DriverConst.LOC2GEO;
			GeoPoint p = new GeoPoint(lat.intValue(), lon.intValue());
			MapOverLay fresh = new PassengerOverlay(mView, p,
					BitmapFactory.decodeResource(this.getResources(),
							R.drawable.passenger), info, this);
			mView.getOverlays().add(fresh);
			mView.invalidate();
			mPsgOverlayMap.put(info, fresh);
		}
		return true;
	}

	public boolean cancelTaxi(PassengerInfo info) {
		if (getStatus() == DriverConst.RUNNING) {
			if (info != mSelPassenger) {
				return false;
			}
			setStatus(DriverConst.IDLE);
		}
		if (mPsgOverlayMap.containsKey(info)) {
			MapOverLay old = (MapOverLay) mPsgOverlayMap.get(info);
			mView.getOverlays().remove(old);
			mPsgOverlayMap.remove(info);
			Toast.makeText(getBaseContext(),
					info.toString() + "\nhas just cancelled the taxi call!",
					Toast.LENGTH_SHORT).show();
		}
		return true;
	}

	private void updateUI() {
		switch (mPriStatus) {
		case DriverConst.LOGIN:
			btnLoginout.setText("logout");
			String welcom = "welcome";
			if (mDrvInfo != null) {
				welcom += ", " + mDrvInfo.getNickName() + "!";
			}
			tvStatus.setText(welcom);
			btnAccount.setVisibility(View.GONE);
			break;
		case DriverConst.VISITOR:
			btnLoginout.setText("login");
			tvStatus.setText("welcome,visitor!");
			btnAccount.setVisibility(View.VISIBLE);
			break;
		default:
			Log.d("error", "unexcepted privilege status");
			break;
		}
		switch (mStatus) {
		case DriverConst.OUT_OF_SERVICE:
			btnFree.setVisibility(View.GONE);
			break;
		case DriverConst.IDLE:
		case DriverConst.PREPROCESS:
		case DriverConst.PRERUNNING:
			btnFree.setVisibility(View.GONE);
			break;
		case DriverConst.RUNNING:
			btnFree.setVisibility(View.VISIBLE);
			break;
		default:
			Log.d("error", "unexcepted status");
			break;
		}
		switch (mPerspective) {
		case DriverConst.DRIVE_MODE:
			btnBack.setVisibility(View.GONE);
			break;
		case DriverConst.VIEW_MODE:
			btnBack.setVisibility(View.VISIBLE);
			break;
		default:
			break;
		}
	}

	public void updatePerspective(int p) {
		setPerspective(p);
		updateUI();
	}

	// getter && setter
	public int getStatus() {
		return mStatus;
	}

	public void setStatus(int mStatus) {
		this.mStatus = mStatus;
	}

	public int getPriStatus() {
		return mPriStatus;
	}

	public void setPriStatus(int status) {
		this.mPriStatus = status;
	}

	public int getPerspective() {
		return mPerspective;
	}

	public void setPerspective(int p) {
		this.mPerspective = p;
	}

	public void clearOverlay() {
		mView.getOverlays().clear();
	}

	public void setCurPsger(PassengerInfo psg) {
		mSelPassenger = psg;
	}
}