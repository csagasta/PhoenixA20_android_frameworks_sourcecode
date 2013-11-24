/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.softwinner;

import java.io.IOException;
import android.os.ServiceManager;
import java.lang.Integer;
import java.lang.String;
import java.util.HashMap;
import android.util.Log;

/**
 * �����ṩһ���ӿڿ���չ��SystemMix����,��Ҫ�����Ǹ���Ӧ��rootȨ���Զ�/д��ǰ��Ȩ�޷��ʵ���Ϣ.
 * �ͻ��ɸ����Լ�������,��չSystemMix�������ṩ�Ĺ���
 * {@hide}
 */
public class SystemMix
{
	public static final String TAG = "SystemMix";

    private SystemMix()
    {
    }

	static {
		System.loadLibrary("systemmix_jni");
		nativeInit();
	}

	private static native void nativeInit();
    private static native String nativeGetProperty(String key);
	private static native int nativeSetProperty(String key, String value);
    private static native int nativeGetFileData(byte[] desData, int count, String filePath);

    /**
    * get a property's value
    */
	public static String getProperty(String key){
	    return nativeGetProperty(key);
	}

    /**
    * set a property's value
    */
	public static void setProperty(String key, String value){
		if(key != null && value != null){
			nativeSetProperty(key,value);
		}
	}
	
	/**
	* get command para in /proc/cmdline
	*/
	public static String getCmdPara(String name){
	    HashMap<String, String> paraMap = mapPara();
	    return paraMap.get(name);    
	}
	
	@SuppressWarnings("null")
	private static HashMap<String, String> mapPara(){
	    HashMap<String, String> paraMap = new HashMap<String, String>();
		String cmdline = getCmdLine();
		Log.d(TAG,"getCmdLine = " + cmdline);
		if(cmdline != null){
			String[] list = cmdline.split(" ");
			if(list != null){
				for(int i = 0; i < list.length; i++){
					String[] map = list[i].split("=");
					if(map == null || map.length != 2){
						continue;
					}
					paraMap.put(map[0], map[1]);
				}
			}
		}
		return paraMap;
	}
	
	private static String getCmdLine(){
	    byte[] desData = new byte[256];
	    int ret = nativeGetFileData(desData, desData.length, "/proc/cmdline");
	    String str = null;
	    if(ret > 0){
	        str = new String(desData);    
	    }
	    return str;
	}
}

