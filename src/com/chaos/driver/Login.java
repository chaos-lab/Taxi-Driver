package com.chaos.driver;
import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.driver.Util.HttpConnectUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Login extends Activity {
	// UI member
	private Button btnSubmit = null;
	private EditText phoneView = null;
	private EditText pswView = null;
	private EditText nick_name_view = null;
	private EditText car_num_view = null;
	// data member
	private Type type;
	private Intent intent; // the intent used to response to invoker
	// constant definition
	public final static int LOGIN_SUCCEED = 0;
	public final static int LOGIN_FAILED = 1;
	public final static int REGISTER_SUCCED = 2;
	public final static int REGISTER_FAILED = 3;
	private final static String REG_SITE = "driver/signup";
	private final static String LOGIN_SITE = "driver/signin";
	public final static String RET_MSG = "ret_msg";
	public final static String RET_OBJ = "ret_obj";

	// internal class definition
	public static class Type {
		private int[] ret_code;
		private String site;
		private int cmd_code;

		Type(int cmd_code) throws Throwable {
			this.cmd_code = cmd_code;
			if (DriverConst.EXE_LOGIN == cmd_code) {
				ret_code = new int[2];
				site = new String();
				ret_code[0] = Login.LOGIN_SUCCEED;
				ret_code[1] = Login.LOGIN_FAILED;
				site = Login.LOGIN_SITE;
			} else if (DriverConst.EXE_ACCOUNT == cmd_code) {
				ret_code = new int[2];
				site = new String();
				ret_code[0] = Login.REGISTER_SUCCED;
				ret_code[1] = Login.REGISTER_FAILED;
				site = Login.REG_SITE;
			} else {
				throw new Throwable();
			}
		}
	}

	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.login);
		intent = new Intent();
		btnSubmit = (Button) findViewById(R.id.submit);
		btnSubmit.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//intent.setAction(String.valueOf(register()));
				setResult(register(getIntent().getExtras().get("lock")),intent);
				finish();
			}
		});
		try {
			int cmd_code = this.getIntent().getExtras()
					.getInt(DriverConst.EXE_CODE);
			initView(cmd_code);
			type = new Type(cmd_code);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private int register(Object lock) {
		String url = HttpConnectUtil.WEB + type.site;
		try {
			JSONObject jsonObj = new JSONObject();
			if (DriverConst.EXE_LOGIN == type.cmd_code) {
				jsonObj.put("phone_number", phoneView.getText());
				jsonObj.put("password", pswView.getText());
			} else if (DriverConst.EXE_ACCOUNT == type.cmd_code) {
				jsonObj.put("phone_number", phoneView.getText());
				jsonObj.put("password", pswView.getText());
				jsonObj.put("nickname", nick_name_view.getText());
				jsonObj.put("car_number", car_num_view.getText());
			}
			HttpConnectUtil.ResonpseData rd = new HttpConnectUtil.ResonpseData();
			if (HttpConnectUtil.post(url, jsonObj, rd)) {
				return parseLoginResponse(rd.strResponse);
			}else{
				Log.e("register/login",rd.strResponse);
				intent.putExtra(RET_MSG, rd.strResponse);
			}

			return type.ret_code[1];

		} catch (JSONException jsonException) {
			jsonException.printStackTrace();
			return type.ret_code[1];
		}

	}

	public int parseLoginResponse(String response) {
		try {
			JSONObject jsonObj = new JSONObject(response);
			int status = HttpConnectUtil.parseLoginResponse(response);
			if (0 == status) {
				if (DriverConst.EXE_LOGIN == type.cmd_code) {
					String msg = jsonObj.getString("message");
					intent.putExtra(RET_MSG, msg);
					intent.putExtra(RET_OBJ,jsonObj.getJSONObject("self").toString());
				}
				return type.ret_code[0];
			} else {
				if (DriverConst.EXE_LOGIN == type.cmd_code) {
					Log.e("login",response);
					intent.putExtra(RET_MSG, "login failed! \ndetail: \n"+response);
				} else {
					Log.e("account",response);
					intent.putExtra(RET_MSG, "create account failed!\ndetail: \n"+response);
				}
				return type.ret_code[1];
			}
		} catch (JSONException e) {
			e.printStackTrace();
			intent.putExtra(RET_MSG, response);
			return type.ret_code[1];
		}
	}

	/**
	 * @param cmd_code
	 * @throws Throwable
	 */
	private void initView(int cmd_code) throws Throwable {
		phoneView = (EditText) findViewById(R.id.phone_num);
		pswView = (EditText) findViewById(R.id.psw);
		nick_name_view = (EditText) findViewById(R.id.nick_name);
		car_num_view = (EditText) findViewById(R.id.car_number);
		if (DriverConst.EXE_LOGIN == cmd_code) {
			nick_name_view.setVisibility(View.GONE);
			car_num_view.setVisibility(View.GONE);
			((TextView) findViewById(R.id.name_label)).setVisibility(View.GONE);
			((TextView) findViewById(R.id.car_label)).setVisibility(View.GONE);
		} else if (DriverConst.EXE_ACCOUNT == cmd_code) {
			nick_name_view.setVisibility(View.VISIBLE);
			car_num_view.setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.name_label))
					.setVisibility(View.VISIBLE);
			((TextView) findViewById(R.id.car_label))
					.setVisibility(View.VISIBLE);

		} else {
			throw new Throwable();
		}
	}
/*
	private boolean genJSONData(int cmd_code, JSONObject jsonObj) {
		try {
			if (DriverConst.EXE_LOGIN == cmd_code) {
				jsonObj.put("phone_number", phoneView.getText());
				jsonObj.put("password", pswView.getText());
				return true;
			} else if (DriverConst.EXE_ACCOUNT == cmd_code) {
				jsonObj.put("phone_number", phoneView.getText());
				jsonObj.put("password", pswView.getText());
				jsonObj.put("nickname", nick_name_view.getText());
				jsonObj.put("car_number", car_num_view.getText());
				return true;
			} else {
				return false;
			}
		} catch (JSONException jsonException) {
			jsonException.printStackTrace();
			return false;
		}
	}*/
}
