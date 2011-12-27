package com.chaos.driver;

import java.util.ArrayList;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;

public class ButtonGrid extends GridView{
	Context mContext;
	public ButtonGrid(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	public ButtonGrid(Context context){
		super(context);
		mContext = context;
	}
	public static class MyButton{
		public String mText;
		public int mImgId;
		public OnClickListener mClickRoutine;
		MyButton(String text,int resId,OnClickListener listener){
			mText = text;
			mImgId = resId;
			mClickRoutine = listener;
		}
	}
	public static class ButtonListAdapter extends BaseAdapter{
		ArrayList<MyButton> mButtons;
		ButtonGrid mParentGrid;
		public ButtonListAdapter(ButtonGrid grid){
			mButtons = new ArrayList<MyButton>();
			mParentGrid = grid;
		}
		public void addButton(String text,int resId,OnClickListener listener){
			mButtons.add(new MyButton(text,resId,listener));
		}
		@Override
		public int getCount() {
			return mButtons.size();
		}

		@Override
		public Object getItem(int position) {
			return mButtons.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageButton ib = null;
			if(convertView == null){
				ib = new ImageButton(mParentGrid.getContext());
				ib.setLayoutParams(new GridView.LayoutParams(50,50));
				ib.setEnabled(true);
				ib.setScaleType(ImageView.ScaleType.FIT_XY);
			}else{
				ib = (ImageButton)convertView;
			}
			MyButton btn = (MyButton) getItem(position);
			ib.setImageResource(btn.mImgId);
			ib.setOnClickListener(btn.mClickRoutine);
			return ib;
		}
		
	}
	
}
