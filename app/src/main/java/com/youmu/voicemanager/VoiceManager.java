package com.youmu.voicemanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.youmu.voicemanager.utils.CommonDefine;
import com.youmu.voicemanager.utils.FileUtils;
import com.youmu.voicemanager.utils.TimeMethod;

public class VoiceManager {
    private ArrayList<File> mRecList = new ArrayList<File>();
    private Activity mActivity = null;
    private View mVRecRoot, mVPlaRoot, mVRecFinish;
    private ImageView mIVRecOperate, mIVPlaOperate, mIVPlaStop;
    private TextView mTVRecText, mTVRecTime, mTVPlaCurrent, mTVPlaTotal;
    private SeekBar mSBPlayProgress;
    private int mSavedState, mDeviceState = CommonDefine.MEDIA_STATE_UNDEFINE;
    private MediaRecorder mMediaRecorder = null;
    private MediaPlayer mMediaPlayer = null;
    private String mRecTimePrev, mPlaFilePath;
    private long mRecTimeSum = 0;
    private String filePath = "";// 录音所存放的位置  "/com.youmu.voicemanager/audio"
    private VoiceCallBack voiceCallBack;

    private MediaPlayer.OnCompletionListener mPlayCompetedListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            mDeviceState = CommonDefine.MEDIA_STATE_PLAY_STOP;
            mHandler.removeMessages(CommonDefine.MSG_TIME_INTERVAL);

            mMediaPlayer.stop();
            mMediaPlayer.release();

            mSBPlayProgress.setProgress(0);
            mTVPlaCurrent.setText("00:00:00");
            mIVPlaOperate.setImageResource(R.mipmap.voice_play);
        }
    };

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            TimeMethod ts;
            int current;
            try {
                switch (msg.what) {
                    case CommonDefine.MSG_TIME_INTERVAL:
                        if (mDeviceState == CommonDefine.MEDIA_STATE_RECORD_DOING) {
                            ts = TimeMethod.timeSpanToNow(mRecTimePrev);
                            mRecTimeSum += ts.mDiffSecond;
                            mRecTimePrev = TimeMethod.getTimeStrFromMillis(ts.mNowTime);

                            ts = TimeMethod.timeSpanSecond(mRecTimeSum);
                            mTVRecTime.setText(String.format("%02d:%02d:%02d",
                                    ts.mSpanHour, ts.mSpanMinute, ts.mSpanSecond));

                            mHandler.sendEmptyMessageDelayed(CommonDefine.MSG_TIME_INTERVAL, 1000);

                        } else if (mDeviceState == CommonDefine.MEDIA_STATE_PLAY_DOING) {
                            current = mMediaPlayer.getCurrentPosition();
                            mSBPlayProgress.setProgress(current);

                            ts = TimeMethod.timeSpanSecond(current / 1000);
                            mTVPlaCurrent.setText(String.format("%02d:%02d:%02d",
                                    ts.mSpanHour, ts.mSpanMinute, ts.mSpanSecond));

                            mHandler.sendEmptyMessageDelayed(CommonDefine.MSG_TIME_INTERVAL, 1000);
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
            }
        }
    };

    public void setVoiceListener (VoiceCallBack callBack) {
        voiceCallBack = callBack;
    }

    public VoiceManager(Activity act, String filePath) {
        this.mActivity = act;
        this.filePath = filePath;

        mVRecRoot = mActivity.findViewById(R.id.unit_voice_record_root);
        mIVRecOperate = (ImageView) mActivity.findViewById(R.id.unit_voice_record_operate);
        mTVRecText = (TextView) mActivity.findViewById(R.id.unit_voice_record_text);
        mTVRecTime = (TextView) mActivity.findViewById(R.id.unit_voice_record_time);
        mVRecFinish = mActivity.findViewById(R.id.unit_voice_record_finish);

        mVPlaRoot = mActivity.findViewById(R.id.unit_voice_play_root);
        mIVPlaOperate = (ImageView) mActivity.findViewById(R.id.unit_voice_play_operate);
        mIVPlaStop = (ImageView) mActivity.findViewById(R.id.unit_voice_play_stop);
        mSBPlayProgress = (SeekBar) mActivity.findViewById(R.id.unit_voice_play_progress);
        mTVPlaCurrent = (TextView) mActivity.findViewById(R.id.unit_voice_play_current);
        mTVPlaTotal = (TextView) mActivity.findViewById(R.id.unit_voice_play_total);

        if (mIVRecOperate != null) {
            mIVRecOperate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mDeviceState == CommonDefine.MEDIA_STATE_RECORD_DOING) {
                        mDeviceState = CommonDefine.MEDIA_STATE_RECORD_PAUSE;
                        stopRecorder(mMediaRecorder, true);
                        mMediaRecorder = null;

                        mTVRecText.setText("已暂停");
                        mIVRecOperate.setImageResource(R.mipmap.voice_pause);
                    } else {
                        sessionRecord(false);
                    }
                }
            });
        }

        if (mVRecFinish != null) {
            mVRecFinish.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    clickRecordFinish();
                }
            });
        }

        if (mIVPlaOperate != null) {
            mIVPlaOperate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mDeviceState == CommonDefine.MEDIA_STATE_PLAY_DOING) {
                        mDeviceState = CommonDefine.MEDIA_STATE_PLAY_PAUSE;
                        pauseMedia(mMediaPlayer);
                        mIVPlaOperate.setImageResource(R.mipmap.voice_play);

                    } else if (mDeviceState == CommonDefine.MEDIA_STATE_PLAY_PAUSE) {
                        mDeviceState = CommonDefine.MEDIA_STATE_PLAY_DOING;
                        playMedia(mMediaPlayer);

                        mIVPlaOperate.setImageResource(R.mipmap.voice_pause);
                        mHandler.removeMessages(CommonDefine.MSG_TIME_INTERVAL);
                        mHandler.sendEmptyMessage(CommonDefine.MSG_TIME_INTERVAL);

                    } else if (mDeviceState == CommonDefine.MEDIA_STATE_PLAY_STOP) {
                        if (!TextUtils.isEmpty(mPlaFilePath)) {
                            sessionPlay(false, mPlaFilePath);
                        }
                    }
                }
            });
        }

        if (mIVPlaStop != null) {
            mIVPlaStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        mHandler.removeMessages(CommonDefine.MSG_TIME_INTERVAL);
                        mDeviceState = CommonDefine.MEDIA_STATE_PLAY_STOP;

                        mIVPlaOperate.setImageResource(R.mipmap.voice_play);
                        mTVPlaCurrent.setText("00:00:00");

                        stopMedia(mMediaPlayer, true);
                        mMediaPlayer = null;

                    } catch (Exception e) {

                    } finally {
                        mVPlaRoot.setVisibility(View.GONE);
                    }
                }
            });
        }

        if (mSBPlayProgress != null) {
            mSBPlayProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    mHandler.removeMessages(CommonDefine.MSG_TIME_INTERVAL);
                    mSavedState = mDeviceState;
                    if (mSavedState == CommonDefine.MEDIA_STATE_PLAY_DOING) {
                        pauseMedia(mMediaPlayer);
                    }
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mHandler.removeMessages(CommonDefine.MSG_TIME_INTERVAL);

                    TimeMethod ts = TimeMethod.timeSpanSecond(progress / 1000);
                    mTVPlaCurrent.setText(String.format("%02d:%02d:%02d",
                            ts.mSpanHour, ts.mSpanMinute, ts.mSpanSecond));
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    seektoMedia(mMediaPlayer, mSBPlayProgress.getProgress());

                    if (mSavedState == CommonDefine.MEDIA_STATE_PLAY_DOING) {
                        playMedia(mMediaPlayer);
                        mHandler.sendEmptyMessage(CommonDefine.MSG_TIME_INTERVAL);
                    }
                }
            });
        }
    }

    public void sessionRecord(boolean init) {
        if (!FileUtils.isSDCardAvailable()) return;
        if (init) {
            mRecTimeSum = 0;
            cleanFieArrayList(mRecList);
        }

        stopRecorder(mMediaRecorder, true);
        mMediaRecorder = null;

        stopMedia(mMediaPlayer, true);
        mMediaPlayer = null;

        if (mVPlaRoot != null) {
            mVPlaRoot.setVisibility(View.GONE);
        }
        mVRecRoot.setVisibility(View.VISIBLE);
        mIVRecOperate.setImageResource(R.mipmap.voice_record);

        mMediaRecorder = new MediaRecorder();
        File file = prepareRecorder(mMediaRecorder, true);
        if (file != null) {
            mTVRecText.setText("正在录音");
            mDeviceState = CommonDefine.MEDIA_STATE_RECORD_DOING;
            mRecTimePrev = TimeMethod.getTimeStrFromMillis(System.currentTimeMillis());
            mRecList.add(file);

            mHandler.removeMessages(CommonDefine.MSG_TIME_INTERVAL);
            mHandler.sendEmptyMessage(CommonDefine.MSG_TIME_INTERVAL);
        }
    }

    public void sessionRecordEx(boolean init, int resTimeTextId) {
        if (!FileUtils.isSDCardAvailable()) return;
        if (init) {
            mRecTimeSum = 0;
            cleanFieArrayList(mRecList);
        }

        mTVRecTime = (TextView) mActivity.findViewById(resTimeTextId);

        stopRecorder(mMediaRecorder, true);
        mMediaRecorder = null;

        stopMedia(mMediaPlayer, true);
        mMediaPlayer = null;

        if (mVPlaRoot != null) {
            mVPlaRoot.setVisibility(View.GONE);
        }

        mMediaRecorder = new MediaRecorder();
        File file = prepareRecorder(mMediaRecorder, true);
        if (file != null) {
            mDeviceState = CommonDefine.MEDIA_STATE_RECORD_DOING;
            mRecTimePrev = TimeMethod.getTimeStrFromMillis(System.currentTimeMillis());
            mRecList.add(file);

            mHandler.removeMessages(CommonDefine.MSG_TIME_INTERVAL);
            mHandler.sendEmptyMessage(CommonDefine.MSG_TIME_INTERVAL);
        }
    }

    /**
     * 完成录音
     */
    public void clickRecordFinish() {
        try {
            mHandler.removeMessages(CommonDefine.MSG_TIME_INTERVAL);
            mDeviceState = CommonDefine.MEDIA_STATE_RECORD_STOP;
            stopRecorder(mMediaRecorder, true);
            mMediaRecorder = null;

            if (mVRecRoot != null) {
                mVRecRoot.setVisibility(View.GONE);
            }
            if (TimeMethod.timeSpanSecond(mRecTimeSum).mSpanSecond == 0) {
                Toast.makeText(mActivity, "时间过短", Toast.LENGTH_SHORT).show();
            } else {

                File file = getOutputVoiceFile(mRecList);
                if (file != null && file.length() > 0) {
                    cleanFieArrayList(mRecList);
                    //TODO 这里可以返回数据 setResult
                    voiceCallBack.recFinish();
                    voiceCallBack.voicePath(file.getAbsolutePath());
                }
            }

        } catch (Exception e) {
        }
    }

    /**
     * 录音播放
     *
     * @param init
     * @param path
     */
    public void sessionPlay(boolean init, String path) {
        if (TextUtils.isEmpty(path)) return;
        mPlaFilePath = path;

        try {
            mVPlaRoot.setVisibility(View.VISIBLE);
            if (mVRecRoot != null) {
                mVRecRoot.setVisibility(View.GONE);
            }

            mIVPlaOperate.setImageResource(R.mipmap.voice_pause);
            mTVPlaCurrent.setText("00:00:00");

            stopRecorder(mMediaRecorder, true);
            mMediaRecorder = null;

            stopMedia(mMediaPlayer, true);
            mMediaPlayer = null;

            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(mPlayCompetedListener);

            if (prepareMedia(mMediaPlayer, path)) {
                mDeviceState = CommonDefine.MEDIA_STATE_PLAY_DOING;

                TimeMethod ts = TimeMethod.timeSpanSecond(mMediaPlayer.getDuration() / 1000);
                mTVPlaTotal.setText(String.format("%02d:%02d:%02d",
                        ts.mSpanHour, ts.mSpanMinute, ts.mSpanSecond));

                mSBPlayProgress.setMax(Math.max(1, mMediaPlayer.getDuration()));

                if (init) {
                    mSBPlayProgress.setProgress(0);
                    seektoMedia(mMediaPlayer, 0);
                } else {
                    seektoMedia(mMediaPlayer, mSBPlayProgress.getProgress());
                }

                if (playMedia(mMediaPlayer)) {
                    mHandler.removeMessages(CommonDefine.MSG_TIME_INTERVAL);
                    mHandler.sendEmptyMessage(CommonDefine.MSG_TIME_INTERVAL);
                }
            }
        } catch (Exception e) {
        }
    }

    /**
     * 合并录音
     *
     * @param list
     * @return
     */
    private File getOutputVoiceFile(ArrayList<File> list) {
        String mMinute1 = TimeMethod.getTime();
        String recFilePath = FileUtils.recAudioPath(filePath);
        File recDirFile = FileUtils.recAudioDir(recFilePath);

        // 创建音频文件,合并的文件放这里
        File resFile = new File(recDirFile, mMinute1 + ".amr");
        FileOutputStream fileOutputStream = null;

        if (!resFile.exists()) {
            try {
                resFile.createNewFile();
            } catch (IOException e) {
            }
        }
        try {
            fileOutputStream = new FileOutputStream(resFile);
        } catch (IOException e) {
        }
        // list里面为暂停录音 所产生的 几段录音文件的名字，中间几段文件的减去前面的6个字节头文件
        for (int i = 0; i < list.size(); i++) {
            File file = list.get(i);
            try {
                FileInputStream fileInputStream = new FileInputStream(file);
                byte[] myByte = new byte[fileInputStream.available()];
                // 文件长度
                int length = myByte.length;
                // 头文件
                if (i == 0) {
                    while (fileInputStream.read(myByte) != -1) {
                        fileOutputStream.write(myByte, 0, length);
                    }
                }
                // 之后的文件，去掉头文件就可以了
                else {
                    while (fileInputStream.read(myByte) != -1) {
                        fileOutputStream.write(myByte, 6, length - 6);
                    }
                }
                fileOutputStream.flush();
                fileInputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 结束后关闭流
        try {
            fileOutputStream.close();
        } catch (IOException e) {
        }

        return resFile;
    }

    private void cleanFieArrayList(ArrayList<File> list) {
        for (File file : list) {
            file.delete();
        }
        list.clear();
    }

    /**
     * 播放录音准备工作
     *
     * @param mp
     * @param file
     * @return
     */
    private boolean prepareMedia(MediaPlayer mp, String file) {
        boolean result = false;

        try {
            mp.setDataSource(file);
            mp.prepare();
            result = true;
        } catch (Exception e) {
        }

        return result;
    }

    /**
     * 播放录音开始
     *
     * @param mp
     * @return
     */
    private boolean playMedia(MediaPlayer mp) {
        boolean result = false;

        try {
            if (mp != null) {
                mp.start();
                result = true;
            }
        } catch (Exception e) {
        }

        return result;
    }

    /**
     * 拖动播放进度条
     *
     * @param mp
     * @param pos
     * @return
     */
    private boolean seektoMedia(MediaPlayer mp, int pos) {
        boolean result = false;

        try {
            if (mp != null && pos >= 0) {
                mp.seekTo(pos);
                result = true;
            }
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * 停止播放
     *
     * @param mp
     * @param release
     * @return
     */
    private boolean stopMedia(MediaPlayer mp, boolean release) {
        boolean result = false;

        try {
            if (mp != null) {
                mp.stop();

                if (release) {
                    mp.release();
                }
                result = true;
            }
        } catch (Exception e) {
        }

        return result;
    }

    /**
     * 暂停播放
     *
     * @param mp
     * @return
     */
    private boolean pauseMedia(MediaPlayer mp) {
        boolean result = false;

        try {
            if (mp != null) {
                mp.pause();
                result = true;
            }
        } catch (Exception e) {
        }

        return result;
    }

    /**
     * 停止录音
     *
     * @param mr
     * @param release
     * @return
     */
    private boolean stopRecorder(MediaRecorder mr, boolean release) {
        boolean result = false;

        try {
            if (mr != null) {
                mr.stop();
                if (release) {
                    mr.release();
                }
                result = true;
            }
        } catch (Exception e) {
        }

        return result;
    }

    /**
     * 录音准备工作 ，开始录音
     *
     * @param mr
     * @param start
     * @return
     */
    @SuppressWarnings("deprecation")
    private File prepareRecorder(MediaRecorder mr, boolean start) {
        File recFile = null;
        if (mr == null) return null;

        try {
            String path = FileUtils.recAudioPath(filePath);
            recFile = new File(path, TimeMethod.getTime() + ".amr");

            mr.setAudioSource(MediaRecorder.AudioSource.MIC);
            mr.setOutputFormat(MediaRecorder.OutputFormat.RAW_AMR);
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mr.setOutputFile(recFile.getAbsolutePath());
            mr.prepare();

            if (start) {
                mr.start();
            }
        } catch (Exception e) {
        }
        return recFile;
    }
}
