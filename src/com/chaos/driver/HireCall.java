package com.chaos.driver;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class HireCall extends Activity{
	private Button mBtnAccept;
	private Button mBtnRefuse;
	private TextView mTvPassenger;
	public final static int ACCPECT = 1;
	public final static int REFUSE = 0;
	
	public void onCreate(Bundle bundle){
		super.onCreate(bundle);
		
		this.setContentView(R.layout.hire_call);
		mBtnAccept = (Button)findViewById(R.id.accept);
		mBtnRefuse = (Button)findViewById(R.id.refuse);
		mTvPassenger = (TextView)findViewById(R.id.caller_info);
		mTvPassenger.setText(getIntent().getExtras().getString(DriverActivity.CALLER));
		mBtnAccept.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				setResult(ACCPECT);
				finish();
			}
		});
		mBtnRefuse.setOnClickListener(new OnClickListener(){
			public void onClick(View v){
				setResult(REFUSE);
				finish();
			}
		});
	}
}

