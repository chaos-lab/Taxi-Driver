package com.chaos.driver.record;

import java.io.File;
import java.sql.Date;

import com.chaos.driver.R;
import com.chaos.driver.util.DriverConst;
import com.chaos.driver.util.TaxiHistorySqlHelper;
import com.chaos.driver.util.TaxiHistorySqlHelper.HistoryItem;
import com.google.android.maps.GeoPoint;

import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class RecordPage extends ExpandableListActivity {
	private RecordDataAdapter mAdapter;
	TaxiHistorySqlHelper mProvider;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.registerForContextMenu(this.getExpandableListView());
		String path = new File(Environment.getExternalStorageDirectory(),"taxi\\repo1").getAbsolutePath();
		mProvider = new TaxiHistorySqlHelper(
				getBaseContext(), path, null, 1);
		mProvider.open();
		//initProvider();
		buildAdapter(19);
	}
	public void onDestroy(){
		super.onDestroy();
		mProvider.close();
	}
	private void initProvider() {
		for(int i=0;i<20;i++){
			mProvider.insertHistory(new TaxiHistorySqlHelper.HistoryItem(this,i,"A"+i+i+i,"name"+i,
					"phone"+i,32000000+i*10000,new GeoPoint(32000000+i*10000,118000000+i*5000),
					new GeoPoint(32000000+i*20000,118000000+i*10000),i%5,i%5,"thanks for your money!",
					"thanks for your drive!"	));
		}
		
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.clear();
		this.getMenuInflater().inflate(R.menu.historymenu, menu);
	}

	public void buildAdapter(int historyId) {
		mAdapter = new RecordDataAdapter(mProvider, historyId);
		this.setListAdapter(mAdapter);
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case 0:
			break;
		case 1:
			break;
		case 2:
			break;
		}
		return true;
	}

	public class RecordDataAdapter extends BaseExpandableListAdapter {
		private String[] groups = { "before", "yesterday", "today"};
		TaxiHistorySqlHelper mProvider = null;
		int mCnt;
		TaxiHistorySqlHelper.HistoryItem[] mRecordCellArray = new TaxiHistorySqlHelper.HistoryItem[DriverConst.RECORD_CNT];
		int[] mID = new int[DriverConst.RECORD_CNT];

		public RecordDataAdapter(TaxiHistorySqlHelper provider, int historyId) {
			mProvider = provider;
			mRecordCellArray = mProvider.batchQueryHistory(historyId,
					DriverConst.RECORD_CNT);
			mCnt = 0;
			if(mRecordCellArray == null){
				return;
			}
			for (int i = 0; i < mRecordCellArray.length; i++) {
				if (mRecordCellArray[i] != null) {
					mCnt++;
					mID[i] = mRecordCellArray[i].mId;
				}
			}
		}

		@Override
		public Object getChild(int groupPos, int childPos) {
			if (groupPos < 0 || groupPos > groups.length) {
				return null;
			}
			if (childPos < 0 || childPos >= DriverConst.RECORD_CNT) {
				return null;
			}
			return mRecordCellArray[childPos];
		}

		@Override
		public long getChildId(int groupPos, int childPos) {
			if (groupPos < 0 || groupPos > groups.length) {
				return -1;
			}
			if (childPos < 0 || childPos >= DriverConst.RECORD_CNT) {
				return -1;
			}
			return mID[childPos];
		}

		@Override
		public View getChildView(int groupPos, int childPos, boolean bLastOne,
				View oldView, ViewGroup parent) {
			return getGenericView(DriverConst.ITEM,groupPos,childPos);
		}

		@Override
		public int getChildrenCount(int groupPos) {
			return mCnt;
		}

		@Override
		public Object getGroup(int groupPos) {
			if(groupPos <0 || groupPos > groups.length){
				return null;
			}
			return groups[groupPos];
		}

		@Override
		public int getGroupCount() {
			return groups.length;
		}

		@Override
		public long getGroupId(int groupPos) {
			return groupPos;
		}

		@Override
		public View getGroupView(int groupPos, boolean bExpanded,
				View convertView, ViewGroup parent) {
			return getGenericView(DriverConst.TITLE,groupPos,0);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int arg0, int arg1) {
			return true;
		}

		public TextView getGenericView(int type,int groupPos, int childPos) {
			// Layout parameters for the ExpandableListView
			TextView textView = new TextView(RecordPage.this);
			// Center the text vertically
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			// Set the text starting position
			if (type == DriverConst.TITLE) {
				AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, 64);
				textView.setLayoutParams(lp);
				textView.setPadding(50, 0, 0, 0);
				textView.setText((String)getGroup(groupPos));
			} else {
				AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
						ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				textView.setLayoutParams(lp);
				textView.setClickable(true);
				textView.setId((groupPos<<0xffff)|(childPos&0xffff));
				textView.setOnClickListener(new OnClickListener(){
					public void onClick(View v){
						if((v.getId()&0xffff) == 0){
							//this means more
						}
					}
				});
				textView.setPadding(36, 0, 0, 0);
				textView.setScrollContainer(true);
				textView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
				textView.setScrollbarFadingEnabled(true);
				textView.setText(getChild(groupPos, childPos).toString());
			}
			return textView;
		}

	}
}
