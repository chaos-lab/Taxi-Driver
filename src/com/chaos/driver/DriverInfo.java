package com.chaos.driver;

public class DriverInfo {
	private String mNickName;
	private String mPhoneNumber;
	private String mCarNumber;
	private boolean mbFree;

	public DriverInfo(String nickName, String phoneNUm, String carNum) {
		mNickName = nickName;
		mPhoneNumber = phoneNUm;
		mCarNumber = carNum;
		setFree(true);
	}

	public String toString() {
		return "PassengerInfo:\n" + "mNickName:" + mNickName + "mPhoneNumber:"
				+ mPhoneNumber + "mCarNumber:" + mCarNumber;
	}

	// getter & setter
	public String getmNickName() {
		return mNickName;
	}

	public void setmNickName(String mNickName) {
		this.mNickName = mNickName;
	}

	public String getmPhoneNumber() {
		return mPhoneNumber;
	}

	public void setmPhoneNumber(String mPhoneNumber) {
		this.mPhoneNumber = mPhoneNumber;
	}

	public String getmCarNumber() {
		return mCarNumber;
	}

	public void setmCarNumber(String mCarNumber) {
		this.mCarNumber = mCarNumber;
	}

	public void setFree(boolean mbFree) {
		this.mbFree = mbFree;
	}

	public boolean isFree() {
		return mbFree;
	}
}
