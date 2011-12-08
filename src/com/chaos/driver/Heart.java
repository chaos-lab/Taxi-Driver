package com.chaos.driver;

import java.util.LinkedList;
import java.util.List;

public class Heart {
	private Thread mHeartThread = null;
	// mark activity be running or not
	// update location if this variable is true
	private boolean isRunning = true;
	private List<Executive> mLstExe;
	public Heart(){
		mLstExe = new LinkedList<Executive>();
	}
	public void start() {
		isRunning = true;
		mHeartThread = new Thread(new Runnable() {
			public void run() {
				while (isRunning) {
					beatHeart();
					try {
						Thread.sleep(DriverConst.LOCATION_UPDATE_INTERVAL);
					} catch (Throwable e) {

					}
				}
			}
		});
		mHeartThread.start();
	}
	public void stop(){
		try {
			isRunning = false;
			mHeartThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	public void addAction(Executive e){
		mLstExe.add(e);
	}
	public void removeAction(Executive e){
		mLstExe.remove(e);
	}
	private void beatHeart() {
		int iSize = mLstExe.size();
		for(int i=0;i<iSize;i++){
			Executive e = mLstExe.get(i);
			if(e != null){
				e.execute();
			}
		}
	}
}
