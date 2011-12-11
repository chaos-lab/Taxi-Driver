package com.chaos.driver.record;

import java.io.File;
import java.sql.Date;

import com.chaos.driver.util.DriverConst;
import com.chaos.driver.util.TaxiHistorySqlHelper;

import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
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
		String path = new File(Environment.getExternalStorageDirectory(),"taxi\\repo").getAbsolutePath();
		mProvider = new TaxiHistorySqlHelper(
				getBaseContext(), path, null, 1);
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("record");
		menu.add(0, 0, 0, "detail");
		menu.add(0, 1, 1, "delete");
		menu.add(0, 2, 2, "evaluate");
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
			for (int i = 0; i < DriverConst.RECORD_CNT; i++) {
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
			return getGenericView(DriverConst.ITEM);
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
			return getGenericView(DriverConst.TITLE);
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int arg0, int arg1) {
			return true;
		}

		public TextView getGenericView(int type) {
			// Layout parameters for the ExpandableListView
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, 64);

			TextView textView = new TextView(RecordPage.this);
			textView.setLayoutParams(lp);
			// Center the text vertically
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			// Set the text starting position
			if (type == DriverConst.TITLE) {
				textView.setPadding(50, 0, 0, 0);
			} else {
				textView.setPadding(36, 0, 0, 0);
			}
			return textView;
		}

	}
}
