package com.youmu.voicemanager.utils;

import android.os.Environment;

import java.io.File;

public class FileUtils {

	/**
	 * SD卡是否可用
	 */
	public static boolean isSDCardAvailable() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}

	public static String recAudioPath(String file) {
		return new File(Environment.getExternalStorageDirectory(), file).getAbsolutePath();
	}

	public static File recAudioDir(String path) {
		File file = new File(path);
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}
}
