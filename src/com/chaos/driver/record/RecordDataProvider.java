package com.chaos.driver.record;

import com.chaos.driver.DriverInfo;

public interface RecordDataProvider {
	public String getEvalFromIds(long [] ids);
	public void getDrvEvlInfo(DriverInfo drv);
	public String getEvalFromTime(long time,String phoneNum,int count);
	public String getHistory(long start,long end,int count);
}
