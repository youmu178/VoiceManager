package com.yzh.voicemanager.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimeMethod {

	/**
	 * 将以毫秒为单位的时间转换成字符串格式的时间
	 * @param time	时间，以毫秒为单位
	 */
	public static String getTimeStrFromMillis(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		String result = sdf.format(new Date(time));
		return result;
	}
}
