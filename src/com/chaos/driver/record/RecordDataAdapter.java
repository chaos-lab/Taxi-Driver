package com.chaos.driver.record;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.chaos.driver.CommentView;
import com.chaos.driver.DriverActivity;
import com.chaos.driver.util.DriverConst;
import com.chaos.driver.util.TaxiHistorySqlHelper;
import com.chaos.driver.util.TaxiHistorySqlHelper.HistoryItem;

public class RecordDataAdapter extends BaseExpandableListAdapter {
	private String[] groups = { "before", "yesterday", "today" };
	private TaxiHistorySqlHelper mProvider;
	private RecordDataProvider mServerProvider;
	private int mCnt;
	private Context mContext;
	// data array
	private ArrayList<HistoryItem> mRecordCellArray;
	private long[] mID;
	private View[][] mViews;
	public RecordDataAdapter(TaxiHistorySqlHelper provider,
			RecordDataProvider serverProvider,Context context) {
		mProvider = provider;
		mServerProvider = serverProvider;
		mRecordCellArray = new ArrayList<HistoryItem>();
		mContext = context;
	}

	public void loadData(long historyId,int cnt) {
		mID = new long[cnt];
		mViews = new View[3][cnt];
		try {
			long starttime=0;
			TaxiHistorySqlHelper.HistoryItem latestItem = mProvider.queryMaxIdHistory();
			if(latestItem != null){
				starttime = latestItem.mEndTimeStamp;
			}
			String strHistory = mServerProvider.getHistory(
					0,historyId, 0);
			if(strHistory.length() == 0){
				return;
			}
			JSONObject jsonResp = new JSONObject(strHistory);
			strHistory = jsonResp.optString("services");
			if(strHistory == null || strHistory.length() == 0){
				return;
			}
			JSONArray historyArray = new JSONArray(strHistory);
			for (mCnt = 0; mCnt < historyArray.length() && mCnt < cnt; mCnt++) {
				TaxiHistorySqlHelper.HistoryItem item = new TaxiHistorySqlHelper.HistoryItem();
				JSONObject jsonHistory = historyArray.getJSONObject(mCnt);
				String strDrv = jsonHistory.optString("driver");
				String strPsg = jsonHistory.optString("passenger");
				String strDest = jsonHistory.optString("destination");
				String strSrc = jsonHistory.getString("origin");
				String strDrvEval = jsonHistory.optString("driver_evaluation");
				String strPsgEval = jsonHistory
						.optString("passenger_evaluation");
				if (strDrv.length() > 0 && !strDrv.equals("null") ) {
					JSONObject jsonDriver = new JSONObject(strDrv);
					item.mCarNumber = jsonDriver.getString("car_number");
				}
				if (strPsg.length() > 0 && !strPsg.equals("null") ) {
					JSONObject jsonPsg = new JSONObject(strPsg);
					item.mPhoneNumber = jsonPsg.getString("phone_number");
					item.mNickName = jsonPsg.getString("nickname");
				}
				if (strDest.length() > 0 && !strDest.equals("null") ) {
					JSONObject jsonDest = new JSONObject(strDest);
					item.mDestinationLongitude = jsonDest.optDouble("longitude");
					item.mDestinationLatitude = jsonDest.optDouble("latitude");
				}
				JSONObject jsonSrc = new JSONObject(strSrc);
				item.mOriginLongitude = jsonSrc.getDouble("longitude");
				item.mOriginLatitude = jsonSrc.getDouble("latitude");
				if (strDrvEval.length() > 0 && !strDrvEval.equals("null") ) {
					JSONObject jsonDrvEval = new JSONObject(strDrvEval);
					item.mDriverComment = jsonDrvEval.getString("comment");
					item.mDriverEvaluation = jsonDrvEval.getDouble("score");
					item.mDriverCommentTimeStamp = jsonDrvEval.getLong("created_at");
					item.mHistoryState = 1;
				}
				if (strPsgEval.length() > 0 && !strPsgEval.equals("null") ) {
					JSONObject jsonPsgEval = new JSONObject(strPsgEval);
					item.mPassengerComment = jsonPsgEval.getString("comment");
					item.mPassengerEvaluation = jsonPsgEval.getDouble("score");
					item.mPassengerCommentTimeStamp = jsonPsgEval.getLong("created_at");
				}
				item.mId = jsonHistory.getLong("id");
				mRecordCellArray.add(item);
				mProvider.insertHistory(item);
				mID[mCnt] = jsonHistory.getInt("id");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}/*
		ArrayList<HistoryItem> items = mProvider.batchQueryHistory(historyId,
				DriverConst.RECORD_CNT);
		if (items == null) {
			return;
		}
		mCnt = 0;
		for (int i = 0; i < items.size() && mCnt < cnt; i++) {
			if (items.get(i) != null) {
				mRecordCellArray.add(mRecordCellArray.get(i));
				mCnt++;
				mID[i] = items.get(i).mId;
			}
		}*/
	}
	public void evaluate(int groupPos, int childPos){
		HistoryItem item = (HistoryItem)getChild(groupPos,childPos);
		if(item != null){
			DriverActivity.showEvaluate(mContext,item.mId);
		}
	}
	public void update(int groupPos, int childPos){
		HistoryItem item = (HistoryItem)getChild(groupPos,childPos);
		if(item != null){
			long []ids = new long[1];
			ids[0] = item.mId;
			String strResult = mServerProvider.getEvalFromIds(ids);
			if(strResult != null){
				try{
					JSONObject jsonRet = new JSONObject(strResult);
					for (int i = 0; i < ids.length; i++) {
						String stri = jsonRet.optString(String.valueOf(ids[i]));
						JSONObject jsoni = new JSONObject(stri);
						String strPasEval = jsoni
								.optString("passenger_evaluation");
						if (strPasEval != null && strPasEval.length() > 0) {
							JSONObject json = new JSONObject(strPasEval);
							String strPsgCmt = json.optString("comment");
							if (strPsgCmt != null && strPsgCmt.length() > 0) {
								item.mPassengerComment = strPsgCmt;
							}
						}
						String strDrvEval = jsoni
								.optString("driver_evaluation");
						if (strDrvEval != null && strDrvEval.length() > 0) {
							JSONObject json = new JSONObject(strDrvEval);
							String strDrvCmt = json.optString("comment");
							if (strDrvCmt != null && strDrvCmt.length() > 0) {
								item.mDriverComment = strDrvCmt;
							}
						}
						TextView tv = (TextView)getView(groupPos, childPos);
						tv.setText(getChild(groupPos, childPos).toString());
						tv.invalidate();
					}
				}catch(JSONException e){
					e.printStackTrace();;
				}
			}
		}
	}
	public void showDetail(int groupPos, int childPos){
		
	}
	public void remove(int groupPos, int childPos){
		
	}
	public View getView(int groupPos, int childPos){
		return mViews[groupPos][childPos];
	}
	@Override
	public Object getChild(int groupPos, int childPos) {
		if (groupPos < 0 || groupPos > groups.length) {
			return null;
		}
		if (childPos < 0 || childPos >= DriverConst.RECORD_CNT) {
			return null;
		}
		return mRecordCellArray.get(childPos);
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
		mViews[groupPos][childPos] = getGenericView(DriverConst.ITEM, groupPos, childPos);
		return mViews[groupPos][childPos];
	}
	
	@Override
	public int getChildrenCount(int groupPos) {
		return mCnt;
	}

	@Override
	public Object getGroup(int groupPos) {
		if (groupPos < 0 || groupPos > groups.length) {
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
	public View getGroupView(int groupPos, boolean bExpanded, View convertView,
			ViewGroup parent) {
		return getGenericView(DriverConst.TITLE, groupPos, 0);
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int arg0, int arg1) {
		return true;
	}

	public TextView getGenericView(int type, int groupPos, int childPos) {
		// Layout parameters for the ExpandableListView
		TextView textView = new TextView(mContext);
		// Center the text vertically
		textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
		// Set the text starting position
		if (type == DriverConst.TITLE) {
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT, 64);
			textView.setLayoutParams(lp);
			textView.setPadding(50, 0, 0, 0);
			textView.setText((String) getGroup(groupPos));
		} else {
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			textView.setLayoutParams(lp);
			//textView.setClickable(true);
			//textView.setId((groupPos << 0xffff) | (childPos & 0xffff));
			/*textView.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					if ((v.getId() & 0xffff) == 0) {
						// this means more
					}
				}
			});*/
			textView.setPadding(36, 0, 0, 0);
			textView.setScrollContainer(true);
			textView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
			textView.setScrollbarFadingEnabled(true);
			textView.setText(getChild(groupPos, childPos).toString());
		}
		return textView;
	}

}
