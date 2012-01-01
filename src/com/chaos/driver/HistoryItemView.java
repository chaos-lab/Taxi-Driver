package com.chaos.driver;

import java.util.Date;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HistoryItemView extends LinearLayout{
	TextView mTextView;
	CheckBox [] mStarArray;
	EditText mTime;
	public HistoryItemView(Context context, AttributeSet attrs,int score,String desc,long time) {
		super(context, attrs);
		init(context,score,desc,time);
	}
	public HistoryItemView(Context context,int score,String desc,long time){
		super(context);
		init(context,score,desc,time);
	}
	private void init(Context context,int score,String desc,long time){
		LayoutInflater.from(context).inflate(R.layout.history_item, this,true);
		if(score > 5){
			score = 5;
		}
		mStarArray = new CheckBox[5];
		mStarArray[0] = (CheckBox)findViewById(R.id.score1);
		mStarArray[1] = (CheckBox)findViewById(R.id.score2);
		mStarArray[2] = (CheckBox)findViewById(R.id.score3);
		mStarArray[3] = (CheckBox)findViewById(R.id.score4);
		mStarArray[4] = (CheckBox)findViewById(R.id.score5);
		for(int i=0;i<score;i++){
			mStarArray[i].setVisibility(View.VISIBLE);
			mStarArray[i].setChecked(true);
			mStarArray[i].setClickable(false);
		}
		for(int i=score;i<5;i++){
			mStarArray[i].setVisibility(View.GONE);
			mStarArray[i].setChecked(false);
			mStarArray[i].setClickable(false);
		}
		mTextView = (TextView)findViewById(R.id.tv_history);
		mTextView.setText(desc);
		mTime = (EditText)findViewById(R.id.time);
		if(time == 0){
			mTime.setVisibility(View.GONE);
		}else{
			Date date = new Date(time);
			mTime.setText(date.toGMTString());
		}
	}

}
