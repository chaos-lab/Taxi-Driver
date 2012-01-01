package com.chaos.driver.record;

import java.io.File;
import java.util.Date;

import com.chaos.driver.R;
import com.chaos.driver.util.DriverConst;
import com.chaos.driver.util.TaxiHistorySqlHelper;
import android.app.ExpandableListActivity;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListView;

public class RecordPage extends ExpandableListActivity {
	private RecordDataAdapter mAdapter;
	TaxiHistorySqlHelper mProvider;
	long mSelPos;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.registerForContextMenu(this.getExpandableListView());
		String path = new File(Environment.getExternalStorageDirectory(),"taxi\\repo1").getAbsolutePath();
		mProvider = new TaxiHistorySqlHelper(getBaseContext());
		mProvider.open();
		mAdapter = new RecordDataAdapter(mProvider,(RecordDataProvider)this.getIntent().getExtras().getParcelable(DriverConst.SER_KEY),this);
		//initProvider();
		Date date = new Date();
		date.setYear(2011);
		date.setMonth(12);
		date.setDate(25);
		date.setHours(12);
		//String strTIme = "1324888477125";
		buildAdapter(date.getTime());//System.currentTimeMillis());
	}
	public void onDestroy(){
		super.onDestroy();
		mProvider.close();
	}

	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.clear();
		this.getMenuInflater().inflate(R.menu.historymenu, menu);	
		ExpandableListView.ExpandableListContextMenuInfo expend_menu_info = (ExpandableListView.ExpandableListContextMenuInfo)menuInfo;
		mSelPos = expend_menu_info.packedPosition;
		int type = ExpandableListView.getPackedPositionType(mSelPos);
		if(type == ExpandableListView.PACKED_POSITION_TYPE_CHILD){
			menu.clear();
			this.getMenuInflater().inflate(R.menu.historymenu, menu);			
		}
	}

	public void buildAdapter(long historyId) {
		mAdapter.loadData(historyId,DriverConst.RECORD_CNT);
		this.setListAdapter(mAdapter);
	}

	public boolean onContextItemSelected(MenuItem item) {
		int childPos = ExpandableListView.getPackedPositionChild(mSelPos);
		int groupPos = ExpandableListView.getPackedPositionGroup(mSelPos);
		switch (item.getItemId()) {
		case R.id.detail:
			mAdapter.showDetail(groupPos, childPos);
			break;
		case R.id.evaluate:
			mAdapter.evaluate(groupPos, childPos);
			break;
		case R.id.retrieve:
			mAdapter.update(groupPos, childPos);
			break;
		case R.id.delete:
			mAdapter.remove(groupPos, childPos);
			break;
		}
		return true;
	}
}
