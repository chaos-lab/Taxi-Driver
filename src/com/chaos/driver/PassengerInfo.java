
package com.chaos.driver;

import android.location.Location;

public class PassengerInfo {
		private String mNickName;
		private String mPhoneNumber;
		private Location mSrcLoc;
		private Location mDestLoc;
		private int mID;
		private int mTimeStamp;
		
		public PassengerInfo(String nickName, String phoneNUm) {
			mNickName = nickName;
			mPhoneNumber = phoneNUm;
			mID = 0;
			mTimeStamp = 0;
			mSrcLoc = new Location("chaos lab");
			mDestLoc = new Location("chaos lab");
		}
		public PassengerInfo() {
			mNickName = "";
			mPhoneNumber = "";
			mID = 0;
			mTimeStamp = 0;
			mSrcLoc = new Location("chaos lab");
			mDestLoc = new Location("chaos lab");
		}
		public String toString() {
			return "PassengerInfo" + "\nmNickName:" + mNickName + 
			"\nmPhoneNumber:" + mPhoneNumber +
			"\nlongitude:" + mSrcLoc.getLongitude() +
			"\nlatitude:" + mSrcLoc.getLatitude();
		}
		public PassengerInfo clone(){
			PassengerInfo psg = new PassengerInfo(this.mNickName,this.mPhoneNumber);
			psg.mID = this.mID;
			psg.mTimeStamp = this.mTimeStamp;
			psg.mSrcLoc = new Location("chaos lab");
			psg.mSrcLoc.set(this.mSrcLoc);
			psg.mDestLoc = new Location("chaos lab");
			psg.mDestLoc.set(this.mDestLoc);
			return psg;
		}
		//getter & setter
		public String getNickName() {
			return mNickName;
		}

		public void setNickName(String mNickName) {
			this.mNickName = mNickName;
		}

		public String getPhoneNumber() {
			return mPhoneNumber;
		}

		public void setPhoneNumber(String mPhoneNumber) {
			this.mPhoneNumber = mPhoneNumber;
		}
		public Location getSrcLoc() {
			return mSrcLoc;
		}

		public void setSrcLoc(Location mSrcLoc) {
			this.mSrcLoc = mSrcLoc;
		}

		public Location getDestLoc() {
			return mDestLoc;
		}

		public void setDestLoc(Location mDestLoc) {
			this.mDestLoc = mDestLoc;
		}

		public int getID() {
			return mID;
		}

		public void setID(int mID) {
			this.mID = mID;
		}

		public int getTimeStamp() {
			return mTimeStamp;
		}

		public void setTimeStamp(int mTimeStamp) {
			this.mTimeStamp = mTimeStamp;
		}
	}
