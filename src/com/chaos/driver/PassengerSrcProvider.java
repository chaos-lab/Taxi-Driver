package com.chaos.driver;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.location.Location;
import android.util.Log;

import com.chaos.driver.util.DriverConst;
import com.chaos.driver.util.HttpConnectUtil;
import com.chaos.driver.util.HttpConnectUtil.ResonpseData;

public class PassengerSrcProvider implements Executive{

	private List<PassengerInfo> mLstPsgInfo;	//focused passengers,need synchronized
	private int mUpdateType;
	public PassengerSrcProvider(){
		mLstPsgInfo = new LinkedList<PassengerInfo>();
		mUpdateType = 0;
		
	}
	public int getPassengerResource(List<PassengerInfo> list){
		synchronized (DriverConst.SyncObj) {
			int iSize = mLstPsgInfo.size(); 
			for (int p = 0; p < iSize; p++) {
				PassengerInfo psg = mLstPsgInfo.get(p);
				list.add(psg.clone());
			}
			return mUpdateType;
		}
	}
	public void execute(){
		String url = HttpConnectUtil.WEB + "driver/refresh";
		HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
		//if (HttpConnectUtil.get(url, null, rd)) {
		// a dummy one
		if (genDummyUser(rd)) {
			int retCode = HttpConnectUtil.parseLoginResponse(rd.strResponse);
			if(retCode == 1){
				synchronized (DriverConst.SyncObj) {
					mUpdateType = -1;
				}
			}else if (retCode == 0) {
				try {
					JSONObject jsonResp = new JSONObject(rd.strResponse);
					if (jsonResp.length() > 0) {
						String strMsg = jsonResp.getString("messages");
						// JSONObject jsonMsg = new JSONObject(strMsg);
						JSONArray jsonArray = new JSONArray(strMsg);
						int iMsgCnt = jsonArray.length();
						if (iMsgCnt > 0) {
							synchronized (DriverConst.SyncObj) {
								mUpdateType = 0;
							}
						}
						for (int i = 0; i < iMsgCnt; i++) {
							JSONObject jsonMsg = jsonArray.getJSONObject(i);
							String type = jsonMsg.getString("type");
							if (type.equalsIgnoreCase("call-taxi")) {
								PassengerInfo psg = new PassengerInfo();
								JSONObject jsonPassenger = new JSONObject(
										jsonMsg.getString("passenger"));
								psg.setNickName(jsonPassenger
										.getString("nickname"));
								psg.setPhoneNumber(jsonPassenger
										.getString("phone_number"));
								psg.setID(jsonMsg.getInt("id"));
								psg.setTimeStamp(jsonMsg.getInt("timestamp"));
								String src = jsonMsg.optString("location");
								if (src.length() > 0) {
									JSONObject jsonSrcLoc = new JSONObject(src);
									Location loc = new Location("chaos lab");
									loc.setLongitude(jsonSrcLoc
											.optDouble(DriverConst.LOC_LON));
									loc.setLatitude(jsonSrcLoc
											.optDouble(DriverConst.LOC_LAT));
									psg.setSrcLoc(loc);
								}
								String dest = jsonMsg.optString("destination");
								if (dest.length() > 0) {
									Location loc = new Location("chaos lab");
									JSONObject jsonDestLoc = new JSONObject(dest);
									loc = new Location("chaos lab");
									loc.setLongitude(jsonDestLoc
											.optDouble(DriverConst.LOC_LON));
									loc.setLatitude(jsonDestLoc
											.optDouble(DriverConst.LOC_LAT));
									psg.setDestLoc(loc);
								}
								synchronized (DriverConst.SyncObj) {
									// all requesting  passenger will be shown
									mLstPsgInfo.add(psg); 
									mUpdateType |= DriverConst.PSG_MASK_APPEND;
								}
								Log.i("refresh","append passenger!");
							} else if (type
									.equalsIgnoreCase("call-taxi-cancel")) {
								int id = jsonMsg.getInt("id");
								synchronized (DriverConst.SyncObj) {
									// only check my interesting passenger list
									int iSize = mLstPsgInfo.size(); 
									for (int p = 0; p < iSize; p++) {
										PassengerInfo psg = mLstPsgInfo.get(p);
										if (psg.getID() == id) {
											mLstPsgInfo.remove(psg);
											mUpdateType |= DriverConst.PSG_MASK_CANCEL;
											Log.i("refresh","passenger cancel!");
										}
									}
								}

							} else if (type.equalsIgnoreCase("location-update")) {
								String pn = jsonMsg.getString("phone_number");
								synchronized (DriverConst.SyncObj) {
									int iSize = mLstPsgInfo.size();
									for (int p = 0; p < iSize; p++) {
										PassengerInfo psg = mLstPsgInfo.get(p);
										if (psg.getPhoneNumber()
												.equalsIgnoreCase(pn)) {
											Location loc = new Location(
													"chaos lab");
											JSONObject jsonSrcLoc = new JSONObject(
													jsonMsg.optString("location"));
											loc.setLongitude(jsonSrcLoc
													.optDouble(DriverConst.LOC_LON));
											loc.setLatitude(jsonSrcLoc
													.optDouble(DriverConst.LOC_LAT));
											psg.setSrcLoc(loc);
											mUpdateType |= DriverConst.PSG_MASK_UPDATE;
											Log.i("refresh","passenger update!");
										}
									}//end of for
								}//end of else
							}//end of else if (type.equalsIgnoreCase("location-update"))
						}//end of for (int i = 0; i < iMsgCnt; i++)
					}//end of if (jsonResp.length() > 0) 
				} catch (JSONException e) {
					e.printStackTrace();
				}//end of try
			}
		}
	}
	private boolean genDummyUser(ResonpseData rd) {
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("status", 0);
			JSONArray message = new JSONArray();
			JSONObject m1 = new JSONObject();
			m1.put("type", "call-taxi");
			JSONObject p1 = new JSONObject();
			p1.put("phone_number", "1384323242");
			p1.put("nickname", "liufy");
			m1.put("passenger", p1);
			JSONObject srcloc1 = new JSONObject();
			srcloc1.put("longitude", 118.48);
			srcloc1.put("latitude", 32.056);
			m1.put("location", srcloc1);
			JSONObject destloc1 = new JSONObject();
			destloc1.put("longitude", 118.23432);
			destloc1.put("latitude", 32.4343);
			m1.put("destination", destloc1);
			m1.put("id", 1321875549);
			m1.put("timestamp", 1321875549);
			message.put(0, m1);

			// m2
			JSONObject m2 = new JSONObject();
			m2.put("type", "call-taxi");
			JSONObject p2 = new JSONObject();
			p2.put("phone_number", "13913394580");
			p2.put("nickname", "canglj");
			m2.put("passenger", p2);
			JSONObject srcloc2 = new JSONObject();
			srcloc2.put("longitude", 118.47);
			srcloc2.put("latitude", 32.090);
			m2.put("location", srcloc2);
			JSONObject destloc2 = new JSONObject();
			destloc2.put("longitude", 118.463);
			destloc2.put("latitude", 32.032);
			m2.put("destination", destloc2);
			m2.put("id", 1321875549);
			m2.put("timestamp", 1321875549);
			message.put(1, m2);
			jsonObj.put("messages", message);
			rd.strResponse = jsonObj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return true;
	}
}
