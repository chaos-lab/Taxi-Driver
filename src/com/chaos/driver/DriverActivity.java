package com.chaos.driver;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.driver.record.RecordPage;
import com.chaos.driver.util.DriverConst;
import com.chaos.driver.util.HttpConnectUtil;
import com.google.android.maps.*;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class DriverActivity extends MapActivity implements GestureHandler{
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
	private GestureView mGestureView;
	private CommentView mCommentView;
	// }

	String mUsrName;
	String mPassword;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mAssist = new DriverAssist(this);
		initViews();
		initLocation();
		this.updateStatus(DriverConst.IDLE);
		setPriStatus(DriverConst.VISITOR);
		mDrvInfo = new DriverInfo("visitor", "", "");
		mPsgOverlayMap = new HashMap<PassengerInfo, Overlay>();
		//restore previous saved state
		SharedPreferences prefs = this.getPreferences(0);
		if(prefs != null){
			mUsrName = prefs.getString(DriverConst.LOGIN_USR,null);
			mPassword = prefs.getString(DriverConst.LOGIN_PSW,null);
		}
		if(mUsrName != null ){
			Bundle bundle = new Bundle();
			if(mAssist.registerDirectly(mUsrName, mPassword,bundle)){
				postLogin(bundle, true);
			}else{
				postLogin(bundle, false);
			}
		}
		updateUI();
	}
	protected void onPause(){
		SharedPreferences.Editor editor = this.getPreferences(0).edit();
		if (mTaxiPos != null) {
			editor.putInt(DriverConst.LOC_LAT, mTaxiPos.getLatitudeE6());
			editor.putInt(DriverConst.LOC_LON, mTaxiPos.getLongitudeE6());
			if (mAssist != null) {
				mAssist.pause(editor);
			}
		}
		editor.commit();
		super.onPause();
	}
	protected void onResume(){
		super.onResume();
		SharedPreferences prefs = this.getPreferences(0);
		mTaxiPos = new GeoPoint(prefs.getInt(DriverConst.LOC_LAT, 0),
				prefs.getInt(DriverConst.LOC_LON, 0));
		if (mAssist != null) {
			mAssist.resume(prefs);
		}
		taxiMove(mTaxiPos);
	}
	public void onBackPressed(){
		AlertDialog dlg = new AlertDialog.Builder(this).setTitle("are you sure to exit Taxi?")
		.setPositiveButton("Exit", new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dlg, int arg1) {
				dlg.dismiss();
				exit();		
			}
			
		}).setNegativeButton("Cancel", new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dlg, int arg1) {
				dlg.cancel();
			}
		}).create();
		dlg.show();
	}
	public void onDestroy(){
		mAssist.destroy();
		super.onDestroy();
	}
	public void exit(){
		SharedPreferences.Editor editor = this.getPreferences(0).edit();
		if (mUsrName != null) {
			editor.putString(DriverConst.LOGIN_USR, mUsrName);
		}
		if(mPassword != null){
			editor.putString(DriverConst.LOGIN_PSW, mPassword);
		}
		editor.commit();
		super.onBackPressed();
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
		mGestureView = (GestureView)findViewById(R.id.gesture);
		mGestureView.setHandler(this);
		mCommentView = new CommentView(this);
		disableGesture();
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
	public void postLogin(Bundle bundle,boolean bSucceed){
		if (bSucceed) {
			setPriStatus(DriverConst.LOGIN);
			try {
				JSONObject jsonSelf = new JSONObject(
						bundle.getString(DriverConst.RET_OBJ));
				mDrvInfo.setPhoneNumber(jsonSelf.getString("phone_number"));
				mDrvInfo.setNickName(jsonSelf.getString("nickname"));
				int state = jsonSelf.optInt("state", -1);
				if (state != -1) {
					setStatus(state);
				}
				mUsrName = bundle.getString(DriverConst.LOGIN_USR);
				mPassword = bundle.getString(DriverConst.LOGIN_PSW);
				
				if (state == DriverConst.RUNNING) {
					Log.i("login message", jsonSelf.toString());
					JSONObject jsonPsg = new JSONObject(
							jsonSelf.getString("passenger"));
					mSelPassenger = new PassengerInfo();
					mSelPassenger.setNickName(jsonPsg.getString("nickname"));
					mSelPassenger.setPhoneNumber(jsonPsg
							.getString("phone_number"));
					String strLoc = jsonPsg.optString("location");

					if (strLoc.length() > 0) {
						Location srcLoc = new Location("chaos lab");
						JSONObject josnLoc = new JSONObject(strLoc);
						srcLoc.setLongitude(josnLoc
								.optDouble(DriverConst.LOC_LON));
						srcLoc.setLatitude(josnLoc
								.optDouble(DriverConst.LOC_LAT));
						mSelPassenger.setSrcLoc(srcLoc);
					}

					mSelPassenger.setID(jsonSelf.getInt("id"));
					Double lon = mSelPassenger.getSrcLoc().getLongitude()
							* DriverConst.LOC2GEO;
					Double lat = mSelPassenger.getSrcLoc().getLatitude()
							* DriverConst.LOC2GEO;
					GeoPoint p = new GeoPoint(lat.intValue(), lon.intValue());
					MapOverLay o = new PassengerOverlay(mView, p,
							BitmapFactory.decodeResource(this.getResources(),
									R.drawable.passengermale), mSelPassenger,
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
		String msg = bundle.getString(DriverConst.RET_MSG);
		if (msg != null) {
			if (msg.length() > 0) {
				Toast.makeText(mView.getContext(), msg, Toast.LENGTH_LONG)
						.show();
			}
		}
	}
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		mAssist.processActivityResult(requestCode, resultCode);
		if (DriverConst.EXE_LOGIN == requestCode) {
			if (DriverConst.LOGIN_SUCCEED == resultCode) {
				postLogin(data.getExtras(),true);
			}
			postLogin(data.getExtras(),false);
			
		} else if (DriverConst.EXE_ACCOUNT == requestCode) {
			String msg = data.getStringExtra(DriverConst.RET_MSG);
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
			mUsrName = null;
			mPassword = null;
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
			showEvaluate();
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
						R.drawable.passengermale), info, this);
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
							R.drawable.passengermale), info, this);
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
		mPsgOverlayMap.clear();
	}

	public void setCurPsger(PassengerInfo psg) {
		mSelPassenger = psg;
	}
	@Override
	public boolean processGesture(String name,RectF rect) {
		MapOverLay selOverlay = null;
		PassengerInfo psg = null;
		List<Overlay> lstOverlay = mView.getOverlays();
		Set<PassengerInfo> o = mPsgOverlayMap.keySet();
		Object[] oa = o.toArray();
		for(int i=0;i<oa.length;i++){
			MapOverLay mol = (MapOverLay)mPsgOverlayMap.get((PassengerInfo)oa[i]);
			if(mol.isInRect(rect) && mPsgOverlayMap.containsValue(mol)){
				selOverlay = mol;
				psg = (PassengerInfo)oa[i];
				break;
			}
		}
		if(selOverlay == null){
			return false;
		}
		if(name.equals(DriverConst.SELECT)){
		}else if(name.equals(DriverConst.REFUSE) && getStatus() == DriverConst.PREPROCESS){
			mPsgOverlayMap.remove(psg);
			mView.getOverlays().remove(selOverlay);			
		}else if(name.equals(DriverConst.ACCEPT) && getStatus() == DriverConst.PREPROCESS){
			Intent intent = new Intent(getBaseContext(),
					HireCall.class);
			Bundle bundle = new Bundle();
			bundle.putString(DriverConst.CALLER,
					psg.toString());
			intent.putExtras(bundle);
			startActivityForResult(intent,
					DriverConst.EXE_CALL_RESPONSE);
			setCurPsger(psg);
			setStatus(DriverConst.PRERUNNING);			
		}
		return false;
	}
	public void onLongClick(){
		
	}
	public void enableGesture(Point pt) {
		mGestureView.enableGesture(pt);
		
	}
	public void disableGesture(){
		mGestureView.disableGesture();
	}

	public boolean onCreateOptionsMenu(Menu menu){
		super.onCreateOptionsMenu(menu);
		menu.clear();
		this.getMenuInflater().inflate(R.menu.mainmenu, menu);
		if(getPriStatus() == DriverConst.LOGIN){
			menu.getItem(0).setVisible(true);
		}else{
			menu.getItem(0).setVisible(false);
		}
		return true;
	}
	public boolean onPrepareOptionsMenu(Menu menu){
		super.onPrepareOptionsMenu(menu);
		menu.clear();
		this.getMenuInflater().inflate(R.menu.mainmenu, menu);
		if(getPriStatus() == DriverConst.LOGIN){
			menu.getItem(0).setVisible(true);
		}else{
			menu.getItem(0).setVisible(false);
		}
		return true;
	}
	public boolean onOptionsItemSelected(MenuItem menuItem){
		switch(menuItem.getItemId()){
		case R.id.history:
			Intent i = new Intent(DriverActivity.this,com.chaos.driver.record.RecordPage.class);
			this.startActivity(i);
			break;
		}
		return true;
	}
	public void showEvaluate(){
		mCommentView.reset();
		AlertDialog dlg =  new AlertDialog.Builder(this).setTitle("evaluation")
		.setView(mCommentView).setPositiveButton("ok", new DialogInterface.OnClickListener() {
			
			@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mSelPassenger != null) {
							mAssist.comment(mSelPassenger.getID(),
									mCommentView.getScore(),
									mCommentView.getComments());
						}
			}
		}).setNegativeButton("cancel",null).create();
		dlg.show();
	}
}