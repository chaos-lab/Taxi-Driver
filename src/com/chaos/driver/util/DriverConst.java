package com.chaos.driver.util;

public class DriverConst {
	// constant
	public final static int LOCATION_UPDATE_INTERVAL = 5000; // unit ms
	public final static int BEAT_HEART_INTERVAL = 5000; // unit ms
	public final static String EXE_CODE = "exe_code";
	public final static String LOC_RESPONSE = "loc response";
	public final static String LOC_LON = "longitude";
	public final static String LOC_LAT = "latitude";
	public final static String REQ_CNT = "request count";
	public final static double LOC2GEO = 1000000.0D;
	public final static String CALLER = "caller info";
	// { exe_code enumeration
	public final static int EXE_LOGIN = 0x10000001;
	public final static int EXE_ACCOUNT = 0x10000002;
	public final static int EXE_CALL_RESPONSE = 0x10000003;

	// }
	// { taxi status
	public final static int OUT_OF_SERVICE = 0;
	public final static int IDLE = 1;
	public final static int RUNNING = 2;
	public final static int PREPROCESS = 3;
	public final static int PRERUNNING = 4;
	// }
	// { privilege status
	public final static int VISITOR = 0;
	public final static int LOGIN = 1;

	// }
	//perspective
	public final static int DRIVE_MODE = 0; //in which one always focus on the car
	public final static int VIEW_MODE = 1;		//in which one can view the map freely
	public static Object SyncObj = new Object();
	//passenger information update mask
	public final static int PSG_MASK_APPEND = 1;
	public final static int PSG_MASK_UPDATE = 4;
	public final static int PSG_MASK_CANCEL = 2;
	
	public final static int RECORD_CNT = 10;
	public final static int TITLE = 0;
	public final static int ITEM = 1;
	
	public final static int OVERLAY_WIDTH = 48;
	public final static int OVERLAY_HEIGHT = 48;

	public final static String SELECT = "select";
	public final static String REFUSE = "delete";
	public final static String ACCEPT = "accept";
	

	public final static int LOGIN_SUCCEED = 0;
	public final static int LOGIN_FAILED = 1;
	public final static int REGISTER_SUCCED = 2;
	public final static int REGISTER_FAILED = 3;
	public final static int USER_CANCELLED = 4;
	public final static String REG_SITE = "driver/signup";
	public final static String LOGIN_SITE = "driver/signin";
	public final static String RET_MSG = "ret_msg";
	public final static String RET_OBJ = "ret_obj";
	public final static String LOGIN_USR = "login_usr";
	public final static String LOGIN_PSW = "login_psw";
	public final static String SER_KEY="ser_key";
}
