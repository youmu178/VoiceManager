package com.yzh.voicemanager.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.os.Environment;

public class Utils {

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

	/**
	 * 当前时间格式
	 */
	@SuppressLint("SimpleDateFormat")
	public static String getTime() {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
		Date curDate = new Date(System.currentTimeMillis());
		String time = formatter.format(curDate);
		return time;
	}
}
