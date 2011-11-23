
package com.chaos.driver;

public class PassengerInfo {
		private String mNickName;
		private String mPhoneNumber;

		public PassengerInfo(String nickName, String phoneNUm) {
			mNickName = nickName;
			mPhoneNumber = phoneNUm;
		}

		public String toString() {
			return "DriverInfo:\n" + "mNickName:" + mNickName + "mPhoneNumber:"
					+ mPhoneNumber;
		}
		//getter & setter
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
	}
