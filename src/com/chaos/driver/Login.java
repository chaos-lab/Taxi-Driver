package com.chaos.driver;
import org.json.JSONException;
import org.json.JSONObject;

import com.chaos.driver.util.DriverConst;
import com.chaos.driver.util.HttpConnectUtil;

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
				ret_code[0] = DriverConst.LOGIN_SUCCEED;
				ret_code[1] = DriverConst.LOGIN_FAILED;
				site = DriverConst.LOGIN_SITE;
			} else if (DriverConst.EXE_ACCOUNT == cmd_code) {
				ret_code = new int[2];
				site = new String();
				ret_code[0] = DriverConst.REGISTER_SUCCED;
				ret_code[1] = DriverConst.REGISTER_FAILED;
				site = DriverConst.REG_SITE;
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
				setResult(register(),intent);
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

	private int register() {
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
				intent.putExtra(DriverConst.RET_MSG, rd.strResponse);
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
					intent.putExtra(DriverConst.RET_MSG, msg);
					intent.putExtra(DriverConst.RET_OBJ,jsonObj.getJSONObject("self").toString());
					intent.putExtra(DriverConst.LOGIN_USR, phoneView.getText().toString());
					intent.putExtra(DriverConst.LOGIN_PSW, pswView.getText().toString());
				}
				return type.ret_code[0];
			} else {
				if (DriverConst.EXE_LOGIN == type.cmd_code) {
					Log.e("login",response);
					intent.putExtra(DriverConst.RET_MSG, "login failed! \ndetail: \n"+response);
				} else {
					Log.e("account",response);
					intent.putExtra(DriverConst.RET_MSG, "create account failed!\ndetail: \n"+response);
				}
				return type.ret_code[1];
			}
		} catch (JSONException e) {
			e.printStackTrace();
			intent.putExtra(DriverConst.RET_MSG, response);
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
	public void onBackPressed(){
		setResult(DriverConst.USER_CANCELLED,intent);
		finish();
	}
}
