package com.chaos.driver.util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

public class CellPositioning {

	// get location according to GSM cell identifier
	public static Location getGsmLocation(TelephonyManager paramTelephonyManager,StringBuffer strRet) {
		GsmCellLocation localGsmCellLocation = (GsmCellLocation) paramTelephonyManager
				.getCellLocation();

		/*if (localGsmCellLocation == null)
			return null;*/

		int cellId = 13095;//localGsmCellLocation.getCid();
		int areaCode = 20484;//localGsmCellLocation.getLac();
		int mcc = 460;/*Integer.valueOf(
				paramTelephonyManager.getNetworkOperator().substring(0, 3))
				.intValue();*/
		int mnc = 0;/*Integer.valueOf(
				paramTelephonyManager.getNetworkOperator().substring(3, 5))
				.intValue();*/
		String url = "http://www.google.cn/loc/json";
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("version", "1.1.0");
			jsonObj.put("host", "maps.google.cn");
			jsonObj.put("home_mobile_country_code", mcc);
			jsonObj.put("home_mobile_network_code", 0);
			jsonObj.put("address_language", "zh_CN");
			jsonObj.put("request_address", true);
			jsonObj.put("radio_type", "gsm");
			JSONArray jsonArray = new JSONArray();
			JSONObject jsonCell = new JSONObject();
			jsonCell.put("cell_id", cellId);
			jsonCell.put("location_area_code", areaCode);
			jsonCell.put("mobile_country_code", mcc);
			jsonCell.put("mobile_network_code", mnc);
			jsonArray.put(jsonCell);
			jsonObj.put("cell_towers", jsonArray);

			HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
			if (HttpConnectUtil.post(url, jsonObj, rd,false)) {
				Location localLocation = new Location("network");
				JSONObject responseJson = new JSONObject(rd.strResponse);
				JSONObject locJson = (JSONObject) responseJson.get("location");
				localLocation.setLatitude(((Double) locJson.get("latitude"))
						.doubleValue());
				localLocation.setLongitude(((Double) locJson.get("longitude"))
						.doubleValue());
				localLocation.setAccuracy(Float.parseFloat(locJson.get(
						"accuracy").toString()));
				localLocation.setTime(System.currentTimeMillis());
				return localLocation;
			}
			return null;
		} catch (JSONException localJSONException) {
			localJSONException.printStackTrace();
			return null;
		}
	}

	// get location according to CDMA cell identifier
	public static Location getCdmaLocation(
			TelephonyManager paramTelephonyManager, Context paramContext,StringBuffer strRet) {
		CdmaCellLocation localCdmaCellLocation = (CdmaCellLocation) paramTelephonyManager
				.getCellLocation();
		if (localCdmaCellLocation == null)
			return null;
		NetworkInfo localNetworkInfo = ((ConnectivityManager) paramContext
				.getSystemService("connectivity")).getActiveNetworkInfo();
		if (localNetworkInfo == null) {
			return null;
		}
		if (!localNetworkInfo.isConnected()) {
			return null;
		}
		int sysId = localCdmaCellLocation.getSystemId();
		int staId = localCdmaCellLocation.getBaseStationId();
		int netId = localCdmaCellLocation.getNetworkId();
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("version", "1.1.0");
			jsonObj.put("host", "maps.google.cn");
			jsonObj.put("home_mobile_country_code", 460);
			jsonObj.put("home_mobile_network_code", 0);
			jsonObj.put("address_language", "zh_CN");
			jsonObj.put("request_address", true);
			jsonObj.put("radio_type", "cdma");
			JSONArray jsonArray = new JSONArray();
			JSONObject josnCell = new JSONObject();
			josnCell.put("cell_id", staId);
			josnCell.put("location_area_code", netId);
			josnCell.put("mobile_country_code", 460);
			josnCell.put("mobile_network_code", sysId);
			josnCell.put("age", 0);
			josnCell.put("signal_strength", -60);
			josnCell.put("timing_advance", 5555);
			jsonArray.put(josnCell);
			jsonObj.put("cell_towers", jsonArray);
			String url = "http://www.google.cn/loc/json";
			HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
			if (HttpConnectUtil.post(url, jsonObj, rd,false)) {
				JSONObject respJson = new JSONObject(rd.strResponse);
				JSONObject locJson = (JSONObject) respJson.get("location");
				Location loc = new Location("network");
				loc.setLatitude(((Double) locJson.get("latitude"))
						.doubleValue());
				loc.setLongitude(((Double) locJson.get("longitude"))
						.doubleValue());
				loc.setAccuracy(Float.parseFloat(locJson.get("accuracy")
						.toString()));
				loc.setTime(System.currentTimeMillis());
				return loc;
			}
			return null;

		} catch (JSONException localJSONException) {
			localJSONException.printStackTrace();
			return null;
		} 
	}
}
