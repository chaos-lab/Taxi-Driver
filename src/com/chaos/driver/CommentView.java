package com.chaos.driver;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class CommentView extends LinearLayout {
	CheckBox [] mStarArray;
	int mScore;
	EditText mComment;
	String mStrComment;
	public CommentView(Context context){
		super(context);
		init(context);
	}
	public CommentView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void init(Context context) {
		LayoutInflater.from(context).inflate(R.layout.evaluate,this,true);
		mStarArray = new CheckBox[5];
		mStarArray[0] = (CheckBox)findViewById(R.id.star1);
		mStarArray[1] = (CheckBox)findViewById(R.id.star2);
		mStarArray[2] = (CheckBox)findViewById(R.id.star3);
		mStarArray[3] = (CheckBox)findViewById(R.id.star4);
		mStarArray[4] = (CheckBox)findViewById(R.id.star5);
		mComment = (EditText)findViewById(R.id.et_comments);
		int width = this.getWidth()/5;
		mStarArray[0].setWidth(width);
		mStarArray[0].setHeight(width);
		mStarArray[1].setWidth(width);
		mStarArray[1].setHeight(width);
		mStarArray[2].setWidth(width);
		mStarArray[2].setHeight(width);
		mStarArray[3].setWidth(width);
		mStarArray[3].setHeight(width);
		mStarArray[4].setWidth(width);
		mStarArray[4].setHeight(width);
		mStarArray[0].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				mScore = 1;
				mStarArray[0].setChecked(true);
				mStarArray[1].setChecked(false);
				mStarArray[2].setChecked(false);
				mStarArray[3].setChecked(false);
				mStarArray[4].setChecked(false);
			}
			
		});
		mStarArray[1].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				mScore = 2;
				mStarArray[0].setChecked(true);
				mStarArray[1].setChecked(true);
				mStarArray[2].setChecked(false);
				mStarArray[3].setChecked(false);
				mStarArray[4].setChecked(false);
			}
			
		});
		mStarArray[2].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				mScore = 3;
				mStarArray[0].setChecked(true);
				mStarArray[1].setChecked(true);
				mStarArray[2].setChecked(true);
				mStarArray[3].setChecked(false);
				mStarArray[4].setChecked(false);
			}
			
		});
		mStarArray[3].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				mScore = 4;
				mStarArray[0].setChecked(true);
				mStarArray[1].setChecked(true);
				mStarArray[2].setChecked(true);
				mStarArray[3].setChecked(true);
				mStarArray[4].setChecked(false);
			}
			
		});
		mStarArray[4].setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				mScore = 5;
				mStarArray[0].setChecked(true);
				mStarArray[1].setChecked(true);
				mStarArray[2].setChecked(true);
				mStarArray[3].setChecked(true);
				mStarArray[4].setChecked(true);
			}
			
		});
		mComment.setOnEditorActionListener(new OnEditorActionListener(){

			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				mStrComment =  new String(mComment.getText().toString());
				return false;
			}
			
		});
	}
	public int getScore(){
		return mScore;
	}
	public String getComments(){
		return mStrComment;
	}
	public void reset() {
		mComment.setText("");	
		mStarArray[0].setChecked(false);
		mStarArray[1].setChecked(false);
		mStarArray[2].setChecked(false);
		mStarArray[3].setChecked(false);
		mStarArray[4].setChecked(false);
	}

}
