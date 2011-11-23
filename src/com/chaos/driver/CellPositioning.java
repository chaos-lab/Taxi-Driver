package com.chaos.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
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
import android.util.Log;

public class CellPositioning {

	// get location according to GSM cell identifier
	public static Location getGsmLocation(TelephonyManager paramTelephonyManager,StringBuffer strRet) {
		GsmCellLocation localGsmCellLocation = (GsmCellLocation) paramTelephonyManager
				.getCellLocation();
		int cellState = paramTelephonyManager.getCallState();
		int cellDataState = paramTelephonyManager.getDataState();
		int networkTYpe = paramTelephonyManager.getNetworkType();
		int dataActivity = paramTelephonyManager.getDataActivity();

		if (localGsmCellLocation == null)
			return null;

		int cellId = localGsmCellLocation.getCid();
		int areaCode = localGsmCellLocation.getLac();
		int mcc = Integer.valueOf(
				paramTelephonyManager.getNetworkOperator().substring(0, 3))
				.intValue();
		int mnc = Integer.valueOf(
				paramTelephonyManager.getNetworkOperator().substring(3, 5))
				.intValue();

		DefaultHttpClient localDefaultHttpClient = new DefaultHttpClient();
		HttpPost localHttpPost = new HttpPost("http://www.google.cn/loc/json");
		JSONObject localJSONObject1 = new JSONObject();
		try {
			localJSONObject1.put("version", "1.1.0");
			localJSONObject1.put("host", "maps.google.cn");
			localJSONObject1.put("home_mobile_country_code", mcc);
			localJSONObject1.put("home_mobile_network_code", 0);
			localJSONObject1.put("address_language", "zh_CN");
			localJSONObject1.put("request_address", true);
			localJSONObject1.put("radio_type", "gsm");
			JSONArray localJSONArray = new JSONArray();
			JSONObject localJSONObject2 = new JSONObject();
			localJSONObject2.put("cell_id", cellId);
			localJSONObject2.put("location_area_code", areaCode);
			localJSONObject2.put("mobile_country_code", mcc);
			localJSONObject2.put("mobile_network_code", mnc);
			localJSONArray.put(localJSONObject2);
			localJSONObject1.put("cell_towers", localJSONArray);
			StringEntity localStringEntity = new StringEntity(
					localJSONObject1.toString());
			localHttpPost.setEntity(localStringEntity);
			// post request and listen the response
			HttpResponse response = localDefaultHttpClient
					.execute(localHttpPost);
			int ret = response.getStatusLine().getStatusCode();
			if(ret != HttpStatus.SC_OK)
			{
				//TODO:retry if failed!
				return null;
			}
			BufferedReader localBufferedReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			StringBuffer localStringBuffer = new StringBuffer();
			for (String s = localBufferedReader.readLine(); s != null; s = localBufferedReader
					.readLine()) {
				localStringBuffer.append(s);
				strRet.append(s);
			}
			JSONObject responseJson = new JSONObject(
					localStringBuffer.toString());
			JSONObject locJson = (JSONObject) responseJson.get("location");
			Location localLocation = new Location("network");
			localLocation.setLatitude(((Double) locJson.get("latitude"))
					.doubleValue());
			localLocation.setLongitude(((Double) locJson.get("longitude"))
					.doubleValue());
			localLocation.setAccuracy(Float.parseFloat(locJson.get("accuracy")
					.toString()));
			localLocation.setTime(System.currentTimeMillis());
			return localLocation;
		} catch (JSONException localJSONException) {
			localJSONException.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException localUnsupportedEncodingException) {
			localUnsupportedEncodingException.printStackTrace();
			return null;
		} catch (ClientProtocolException localClientProtocolException) {
			localClientProtocolException.printStackTrace();
			return null;
		} catch (IOException localIOException) {
			localIOException.printStackTrace();
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
		DefaultHttpClient localDefaultHttpClient = new DefaultHttpClient();
		HttpPost localHttpPost = new HttpPost("http://www.google.cn/loc/json");
		JSONObject localJSONObject1 = new JSONObject();
		try {
			localJSONObject1.put("version", "1.1.0");
			localJSONObject1.put("host", "maps.google.cn");
			localJSONObject1.put("home_mobile_country_code", 460);
			localJSONObject1.put("home_mobile_network_code", 0);
			localJSONObject1.put("address_language", "zh_CN");
			localJSONObject1.put("request_address", true);
			localJSONObject1.put("radio_type", "cdma");
			JSONArray localJSONArray = new JSONArray();
			JSONObject localJSONObject2 = new JSONObject();
			localJSONObject2.put("cell_id", staId);
			localJSONObject2.put("location_area_code", netId);
			localJSONObject2.put("mobile_country_code", 460);
			localJSONObject2.put("mobile_network_code", sysId);
			localJSONObject2.put("age", 0);
			localJSONObject2.put("signal_strength", -60);
			localJSONObject2.put("timing_advance", 5555);
			localJSONArray.put(localJSONObject2);
			localJSONObject1.put("cell_towers", localJSONArray);
			HttpResponse response = localDefaultHttpClient
					.execute(localHttpPost);
			int ret = response.getStatusLine().getStatusCode();
			StringEntity localStringEntity = new StringEntity(
					localJSONObject1.toString());
			localHttpPost.setEntity(localStringEntity);
			BufferedReader localBufferedReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			StringBuffer localStringBuffer = new StringBuffer();
			for (String s = localBufferedReader.readLine(); s != null; s = localBufferedReader
					.readLine()) {
				localStringBuffer.append(s);
				strRet.append(s);
			}
			JSONObject respJson = new JSONObject(localStringBuffer.toString());
			JSONObject locJson = (JSONObject) respJson.get("location");
			Location loc = new Location("network");
			loc.setLatitude(((Double) locJson.get("latitude")).doubleValue());
			loc.setLongitude(((Double) locJson.get("longitude")).doubleValue());
			loc.setAccuracy(Float
					.parseFloat(locJson.get("accuracy").toString()));
			loc.setTime(System.currentTimeMillis());
			return loc;

		} catch (JSONException localJSONException) {
			localJSONException.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException localUnsupportedEncodingException) {
			localUnsupportedEncodingException.printStackTrace();
			return null;
		} catch (ClientProtocolException localClientProtocolException) {
			localClientProtocolException.printStackTrace();
			return null;
		} catch (IOException localIOException) {
			localIOException.printStackTrace();
			return null;
		}
	}
}
