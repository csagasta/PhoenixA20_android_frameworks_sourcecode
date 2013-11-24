package com.android.server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.R.bool;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.provider.Settings;
import android.database.ContentObserver;
import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

public class DynamicPowerPolicyChangeThread extends Thread {

	//Broadcast
	private final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
	private final String ACTION_WINDOW_ROTATION = "android.window.action.ROTATION";
	
	//Settings 
	private final static String SETTINGS_SECURE_SYSTEM_WORK_MODE = "system_work_mode";
	private final static String SETTINGS_USER_WORK_MODE_NORMAL = "1";
	private final static String SETTINGS_USER_WORK_MODE_HIGHT_PERFORMANCE = "2"; 
	
	// app config file name
	private final String APP_DETECT_CONFIG_FILE_NAME = "app_list.conf";

	// CPU
	private final String CMD_SET_CPU_TO_USB_POLICY = "echo usb > /sys/class/sw_powernow/mode";

	private final String CMD_SET_CPU_TO_NORMAL_POLICY = "echo normal > /sys/class/sw_powernow/mode";

	private final String CMD_SET_CPU_TO_EXTREMITY_POLICY = "echo extremity > /sys/class/sw_powernow/mode";

	private final String CMD_SET_CPU_TO_USER_EVENT_NOTIFY = "echo userevent > /sys/class/sw_powernow/mode";

	private final String CMD_SET_CPU_TO_PERFORMANCE_POLICY = "echo performance > /sys/class/sw_powernow/mode";
	
	private final String CMD_SET_CPU_TO_FANTASYS_POLICY = "echo fantasys > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";

	private Context mContext;
	private ActivityManager mActivityManager;

	private boolean mIsNeedToSetWorkMode = false;
	private boolean mIsUsbConnected = false;
	private boolean mIsInUserMode = false;

	public DynamicPowerPolicyChangeThread(Context context) {
		// TODO Auto-generated constructor stub
		mContext = context;
		mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		
		//Anyway,set fantasys policy
		setCPUWorkMode(CMD_SET_CPU_TO_FANTASYS_POLICY);
		
		mSettingsContentObserver.register(mContext.getContentResolver());
		if(null == Settings.Secure.getString(mContext.getContentResolver(), SETTINGS_SECURE_SYSTEM_WORK_MODE)){
				Settings.Secure.putString(mContext.getContentResolver(), "system_work_mode", "1");		
		}
		if(!Settings.Secure.getString(mContext.getContentResolver(), 
				SETTINGS_SECURE_SYSTEM_WORK_MODE).equals(SETTINGS_USER_WORK_MODE_NORMAL)){
				mIsInUserMode = true;
				setUserWorkMode(Settings.Secure.getString(mContext.getContentResolver(), SETTINGS_SECURE_SYSTEM_WORK_MODE));
				//Log.v("kinier", "is in user mode!!");
		}else{
				setCPUWorkMode(CMD_SET_CPU_TO_NORMAL_POLICY);	
		}
		
	  IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_USB_STATE);
		mContext.registerReceiver(mUsbStatusReceiver, intentFilter);
		
	  IntentFilter intentFilter2 = new IntentFilter();
		intentFilter2.addAction(ACTION_WINDOW_ROTATION);
		mContext.registerReceiver(mWindowRotationReceiver, intentFilter2);
	
	}
	
	@Override
	protected void finalize() throws Throwable {
		// TODO Auto-generated method stub
		mContext.unregisterReceiver(mUsbStatusReceiver);
		mContext.unregisterReceiver(mWindowRotationReceiver);
		mSettingsContentObserver.unregister(mContext.getContentResolver());
		super.finalize();
	}
	
	private SettingsContentObserver mSettingsContentObserver = new SettingsContentObserver(new Handler()) {
	    @Override
	    public void onChange(boolean selfChange, Uri uri) {
	        String settingValue = Settings.Secure.getString(mContext.getContentResolver(), SETTINGS_SECURE_SYSTEM_WORK_MODE);
					//Log.v("kinier", "system_work_mode change " + settingValue);
					setUserWorkMode(settingValue);
	    }
	};
	
	private BroadcastReceiver mWindowRotationReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			//System.out.println("###" + ACTION_WINDOW_ROTATION);
			setCPUWorkMode(CMD_SET_CPU_TO_USER_EVENT_NOTIFY);
		}
	};

	private BroadcastReceiver mUsbStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(ACTION_USB_STATE)) {
				
				boolean isInBenchmarkMode = isNeedToSetBenchmarkMode();
				boolean isConnected = intent.getExtras().getBoolean("connected");
			
				if (isConnected) {
					//System.out.println("##usb is connected!!!");
					mIsUsbConnected = true;
					if (!isInBenchmarkMode) {
									
						//System.out.println("##usb is connected CMD_SET_CPU_TO_USB_POLICY!!!");
						setCPUWorkMode(CMD_SET_CPU_TO_USB_POLICY);
					} 

				} else {
					//System.out.println("##usb is disconnected!!!");
					mIsUsbConnected = false;
					
					if(!isInBenchmarkMode && mIsInUserMode){
						setCPUWorkMode(CMD_SET_CPU_TO_PERFORMANCE_POLICY);
					}else if (!isInBenchmarkMode && !mIsInUserMode) {
						setCPUWorkMode(CMD_SET_CPU_TO_NORMAL_POLICY);
					} 

				}

			}

		}
	};

	@Override
	public void run() {
		// TODO Auto-generated method stub
		super.run();

		while (true) {
			
			boolean isInBenchmarkMode =  isNeedToSetBenchmarkMode();

			if (isInBenchmarkMode && !mIsNeedToSetWorkMode) {

				setCPUWorkMode(CMD_SET_CPU_TO_EXTREMITY_POLICY);
				mIsNeedToSetWorkMode = true;
			//	System.out.println("set benchmark policy...........");

			} else if (!isInBenchmarkMode && mIsNeedToSetWorkMode) {

				mIsNeedToSetWorkMode = false;
				if (mIsUsbConnected) {
					setCPUWorkMode(CMD_SET_CPU_TO_USB_POLICY);
				}else if(mIsInUserMode){
					setUserWorkMode(Settings.Secure.getString(mContext.getContentResolver(), SETTINGS_SECURE_SYSTEM_WORK_MODE));
				}else {
					//System.out.println("##usb is connected CMD_SET_CPU_TO_NORMAL_POLICY!!!");
					setCPUWorkMode(CMD_SET_CPU_TO_NORMAL_POLICY);
				}
			//	System.out.println("resume cpu policy...........");
			}

			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	
	public void setUserWorkMode(String mode){
		
			boolean isInBenchmarkMode = isNeedToSetBenchmarkMode();
			
			if(mode.equals(SETTINGS_USER_WORK_MODE_HIGHT_PERFORMANCE)){
					mIsInUserMode = true;
					if(!isInBenchmarkMode && !mIsUsbConnected){
						setCPUWorkMode(CMD_SET_CPU_TO_PERFORMANCE_POLICY);
					}
			} else if(mode.equals(SETTINGS_USER_WORK_MODE_NORMAL)){
				 	mIsInUserMode = false;
				 	
				 	if(!mIsUsbConnected && !isInBenchmarkMode ){
						setCPUWorkMode(CMD_SET_CPU_TO_NORMAL_POLICY);
					}
			}
	}
	
	
	public boolean  isNeedToSetBenchmarkMode(){
			
		ComponentName componentName = mActivityManager.getRunningTasks(1).get(0).topActivity;
		String packageName = componentName.getPackageName();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(mContext.getAssets().open(APP_DETECT_CONFIG_FILE_NAME)));
			String tmpStr = null;
			
			while((tmpStr=br.readLine()) != null){
				if(tmpStr.compareTo(packageName) == 0){
					return true;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return false;
	}

	public void setCPUWorkMode(String cmd) {
		executeShellCmd(cmd);
	}

	public void executeRootCmd(String cmd) {
		DataOutputStream dos = null;
		DataInputStream dis = null;
		try {
			Process p = Runtime.getRuntime().exec("su");
			dos = new DataOutputStream(p.getOutputStream());
			cmd += "\n";
			dos.writeBytes(cmd);
			dos.flush();
			dos.writeBytes("exit\n");
			dos.flush();
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				if (dos != null)
					dos.close();
				if (dis != null)
					dis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public String executeShellCmd(String cmd) {
		Process process = null;
		BufferedReader br = null;
		String s = null;
		String resultStr = "";
		try {
			process = Runtime.getRuntime().exec(
					new String[] { "/system/bin/sh", "-c", cmd });
			br = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			while ((s = br.readLine()) != null) {
				if (s != null) {
					resultStr += s;
				}
			}
			// //System.out.println(resultStr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (process != null) {
				process.destroy();
			}
			process = null;
			if (br != null) {
				try {
					br.close();
				} catch (Exception e) {
				}
				br = null;
			}
		}

		return resultStr;
	}
	
  private static abstract class SettingsContentObserver extends ContentObserver {

	    public SettingsContentObserver(Handler handler) {
	        super(handler);
	    }
	
	    public void register(ContentResolver contentResolver) {
	        contentResolver.registerContentObserver(Settings.Secure.getUriFor(
	                DynamicPowerPolicyChangeThread.SETTINGS_SECURE_SYSTEM_WORK_MODE), false, this);
	    }
	
	    public void unregister(ContentResolver contentResolver) {
	        contentResolver.unregisterContentObserver(this);
	    }
	
	    @Override
	    public abstract void onChange(boolean selfChange, Uri uri);
	}

}
