package com.chaos.driver.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.conn.ssl.SSLSocketFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONException;
import org.json.JSONObject;


public class HttpConnectUtil {
	public final static String WEB = "http://taxi.no.de/";
	static final int MAX_TOTAL_CONNECTIONS = 100;
	static final int MAX_ROUTE_CONNECTIONS = 100;
	static final int WAIT_TIMEOUT = 10 * 1000; // 10 seconds
	static final int READ_TIMEOUT = 20 * 1000; // 20 seconds
	static final int CONNECT_TIMEOUT = 60 * 60 * 1000; // 1 hour
	private static DefaultHttpClient mClient = null;
	static {
		BasicHttpParams httpParams = new BasicHttpParams();
		ConnManagerParams.setMaxTotalConnections(httpParams,
				MAX_TOTAL_CONNECTIONS);
		ConnManagerParams.setTimeout(httpParams, WAIT_TIMEOUT);
		ConnPerRouteBean connPerRoute = new ConnPerRouteBean(
				MAX_ROUTE_CONNECTIONS);
		ConnManagerParams.setMaxConnectionsPerRoute(httpParams, connPerRoute);
		HttpConnectionParams.setConnectionTimeout(httpParams, CONNECT_TIMEOUT);
		HttpConnectionParams.setSoTimeout(httpParams, READ_TIMEOUT);

		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		registry.register(new Scheme("https", SSLSocketFactory
				.getSocketFactory(), 443));

		mClient = new DefaultHttpClient(new ThreadSafeClientConnManager(
				httpParams, registry), httpParams);
	}
	public static class ResonpseData{
		//actually, a string is enough.build a complex structure to make java process arguments as reference
		public String strResponse = new String();
		public int retCode;
	}
	private static StringEntity getEntity(JSONObject data,boolean bTaxiSpec){
		try {
			String str = data.toString();
			if(bTaxiSpec){
				str = "json_data=" + str;
			}
			return new StringEntity(str);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static boolean post(String url, JSONObject data, ResonpseData rd){
		return post(url,data,rd,true);
	}
	public static boolean post(String url, JSONObject data, ResonpseData rd,boolean bTaxiSpec) {
		// HttpPost post = new HttpPost("http://taxi.no.de/" + SITE);
		HttpPost post = new HttpPost(url);
		post.setHeader("Content-Type", "application/x-www-form-urlencoded");
		try {
			StringEntity localStringEntity = getEntity(data,bTaxiSpec);
			post.setEntity(localStringEntity);
			// post request and listen the response
			HttpResponse response;
			synchronized(mClient){
				response = mClient.execute(post);
			}
			if(response == null){
				return false;
			}
			if(response.getStatusLine() == null){
				return false;
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
			synchronized(mClient){
				response = mClient.execute(get);
			}
			if(response == null){
				return false;
			}
			if(response.getStatusLine() == null){
				return false;
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
