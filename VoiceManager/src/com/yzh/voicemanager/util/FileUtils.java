package com.yzh.voicemanager.util;

import android.os.Environment;

public class FileUtils {

	/**
	 * SD卡是否可用
	 */
	public static boolean isSDCardAvailable() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}
}
