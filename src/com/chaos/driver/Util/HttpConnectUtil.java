package com.chaos.driver.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.driver.DriverActivity;


public class HttpConnectUtil {
	public final static String WEB = "http://taxi.no.de/";
	private static DefaultHttpClient mClient = new DefaultHttpClient();
	private static Object mSycObj = new Object();
	public static class ResonpseData{
		//actually, a string is enough.build a complex structure to make java process arguments as reference
		public String strResponse = new String();
		public int retCode;
	}
	public static boolean post(String url, JSONObject data, ResonpseData rd) {
		// HttpPost post = new HttpPost("http://taxi.no.de/" + SITE);
		HttpPost post = new HttpPost(url);
		post.setHeader("Content-Type", "application/x-www-form-urlencoded");
		try {
			StringEntity localStringEntity = new StringEntity("json_data="
					+ data.toString());
			post.setEntity(localStringEntity);
			// post request and listen the response
			HttpResponse response;
			synchronized(mSycObj){
				response = mClient.execute(post);
			}
			int ret = response.getStatusLine().getStatusCode();
			if(HttpStatus.SC_OK != ret){
				Integer i = new Integer(ret);
				rd.strResponse = "process post request failed!error code:" + i.toString();
				rd.retCode = ret;
				return false; 
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			StringBuilder strBuilder = new StringBuilder();
			for (String s = reader.readLine(); s != null; s = reader.readLine()) {
				strBuilder.append(s);
			}
			//strResponse = strBuilder.toString();
			rd.strResponse = strBuilder.toString();
			return true;
		} catch (UnsupportedEncodingException localUnsupportedEncodingException) {
			rd.strResponse = "UnsupportedEncodingException";
			localUnsupportedEncodingException.printStackTrace();
			return false; 
		} catch (ClientProtocolException localClientProtocolException) {
			rd.strResponse = "ClientProtocolException";
			localClientProtocolException.printStackTrace();
			return false; 
		} catch (IOException localIOException) {
			rd.strResponse = "IOException";
			localIOException.printStackTrace();
			return false; 
		}
	}
	public static boolean get(String url, JSONObject data, ResonpseData rd){
		String completeUri = url;
		if(data != null){
			url +=  "?json_data=" + URLEncoder.encode(data.toString());
		}
		HttpGet get = new HttpGet(completeUri);
		try{
			HttpResponse response;
			synchronized(mSycObj){
				response = mClient.execute(get);
			}
			int ret = response.getStatusLine().getStatusCode();
			if(HttpStatus.SC_OK != ret){
				Integer i = new Integer(ret);
				rd.strResponse = "process get request failed!error code:" + i.toString();
				rd.retCode = ret;
				return false; 
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));
			StringBuilder strBuilder = new StringBuilder();
			for (String s = reader.readLine(); s != null; s = reader.readLine()) {
				strBuilder.append(s);
			}
			rd.strResponse = strBuilder.toString();
			return true;
		}catch (UnsupportedEncodingException localUnsupportedEncodingException) {
			localUnsupportedEncodingException.printStackTrace();
			rd.strResponse = "UnsupportedEncodingException";
			return false; 
		} catch (ClientProtocolException localClientProtocolException) {
			rd.strResponse = "ClientProtocolException";
			localClientProtocolException.printStackTrace();
			return false; 
		} catch (IOException localIOException) {
			rd.strResponse = "IOException";
			localIOException.printStackTrace();
			return false; 
		}
	}
	public static int parseLoginResponse(String response) {
		try {
			JSONObject jsonObj = new JSONObject(response);
			return jsonObj.getInt("status");
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}

}
