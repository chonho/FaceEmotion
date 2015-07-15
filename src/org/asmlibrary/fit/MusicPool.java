package org.asmlibrary.fit;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;


public final class MusicPool {

	public static final int TOTAL_BUTTONS = 3 * 2 * 2;
	private int soundIndex = 0;
	
	
	public interface Listener {
		void buttonStateChanged(int index);

		void multipleButtonStateChanged();
	}
	
	private boolean[] buttonPressMap = new boolean[TOTAL_BUTTONS];
	
	private List<Listener> listeners = new ArrayList<Listener>();
	
	private SoundPool[] soundPool = new SoundPool[2];
	
	private int[] soundIds = new int[TOTAL_BUTTONS];
	
	private int[] streamIds = new int[TOTAL_BUTTONS];
	private int[] volume = new int [2];
	
	private int streamId1;
	private int streamId2;
	private int soundId1;
	private int soundId2;
	
	public MusicPool(Context context) {
		for (int i = 0; i < TOTAL_BUTTONS; ++i) {
			buttonPressMap[i] = false;
		}
//        AudioAttributes attributes = new AudioAttributes.Builder()
//                .setUsage(AudioAttributes.USAGE_GAME)
//                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                .build();
//        soundPool = new SoundPool.Builder()
//                .setAudioAttributes(attributes)
//                .build();



		soundPool[0] = new SoundPool(TOTAL_BUTTONS, AudioManager.STREAM_MUSIC, 0);
		soundPool[1] = new SoundPool(TOTAL_BUTTONS, AudioManager.STREAM_MUSIC, 0);
		
		//soundPool[0] load music
		soundIds[0] = soundPool[0].load(context, R.raw.emo_1_surprise, 1);
        soundIds[1] = soundPool[0].load(context, R.raw.emo_2_fear, 1);
        soundIds[2] = soundPool[0].load(context, R.raw.emo_3_happy, 1);
		soundIds[3] = soundPool[0].load(context, R.raw.emo_4_sad, 1);
		soundIds[4] = soundPool[0].load(context, R.raw.emo_5_digust, 1);
		soundIds[5] = soundPool[0].load(context, R.raw.emo_6_anger, 1);
		
		//soundPool[1] load music
		soundIds[6] = soundPool[1].load(context, R.raw.emo_1_surprise, 1);
        soundIds[7] = soundPool[1].load(context, R.raw.emo_2_fear, 1);
        soundIds[8] = soundPool[1].load(context, R.raw.emo_3_happy, 1);
		soundIds[9] = soundPool[1].load(context, R.raw.emo_4_sad, 1);
		soundIds[10] = soundPool[1].load(context, R.raw.emo_5_digust, 1);
		soundIds[11] = soundPool[1].load(context, R.raw.emo_6_anger, 1);
		volume[0]=volume[1]=0;
	}
	
	public void play(int index){
		if (soundIndex ==0)
		{ 
			fadeInPlay(soundIndex, index);
			soundIndex =1;
		}
//		else if(soundIndex ==1)
//		{ 
//			fadeIn(soundIndex, index, 1);
//			soundIndex =2;
//		}
//		else if(soundIndex ==2){
//			fadeIn(soundIndex, index, 0);
//			soundIndex =1;
//			
//		}
		
	
	}
	public void fadeInPlay(final int soundIndex, final int index)
	{
		if (index >= 0 && index < TOTAL_BUTTONS) {
			
			int soundId = soundIds[index];
			if (soundId != 0) {
				streamIds[index] = soundPool[0].play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
				Log.d("MusicService", "start streamId: "+streamIds[index]);
			
			}	
		}
		
//		final Timer timer = new Timer(true);
//		TimerTask timerTask = new TimerTask() {
//			
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				//updateVolume(1);
//				Log.d("MusicService", "volume 0: "+volume[0]);
//				volume[0] += 5;
//
//				if(volume[0]>=100){
//					volume[0]=100;
//					timer.cancel();
//					timer.purge();
//				}
//				
//				soundPool[0].setVolume(streamIds[index], (float)volume[0]/100.0f,(float)volume[0]/100.0f);
//				Log.d("MusicService", "streamId: "+streamIds[index]);
//				
//			}	
//		};
//		timer.scheduleAtFixedRate(timerTask, 0, 100);
		
		
	}
	
	
	public void fadeIn(final int soundIndex, final int index, final int poolIndex)
	{
		
		final Timer timer = new Timer(true);
		TimerTask timerTask = new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				//updateVolume(1);
				volume[poolIndex] += 5;
				if(soundIndex==0) volume[1-poolIndex] -= 5;
				
				if(volume[poolIndex]>=100){
					volume[poolIndex]=100;
					timer.cancel();
					timer.purge();
				}
				if((soundIndex==0) && volume[1-poolIndex]<=0){
					volume[1-poolIndex]=0;
				}
				soundPool[poolIndex].setVolume(streamIds[(poolIndex*6)+index], volume[poolIndex]/100, volume[poolIndex]/100);
				if(soundIndex==0) soundPool[1-poolIndex].setVolume(streamIds[(1-poolIndex)*6 + index], volume[1-poolIndex]/100, volume[1-poolIndex]/100);
			}	
		};
		timer.scheduleAtFixedRate(timerTask, 0, 100);
		
		if (index >= 0 && index < TOTAL_BUTTONS) {
			
			int soundId = soundIds[index];
			if (soundId != 0) {
				streamIds[(poolIndex*6)+index] = soundPool[poolIndex].play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
				//streamId1 = streamIds[index];
			
			}	
		}
	}
	/*
	//public void 
	public void pressButton(int index) { //index starts from 0
	
			if (index >= 0 && index < TOTAL_BUTTONS) {
				if (buttonPressMap[index] == false) {
					buttonPressMap[index] = true;
					
					int soundId = soundIds[index];
					if (soundId != 0) {
						streamIds[index] = soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
					
						
					}
					
	//				for (Listener listener : listeners) {
	//					listener.buttonStateChanged(index);
	//				}
				}
			}
			
	}
	
	
	public void releaseButton(int index) {
		if (index >= 0 && index < TOTAL_BUTTONS) {
			if (buttonPressMap[index] == true) {
				buttonPressMap[index] = false;
	
				int streamId = streamIds[index];
				if (streamId != 0) {
					soundPool.stop(streamId);
					streamIds[index] = 0;
				}

//				for (Listener listener : listeners) {
//					listener.buttonStateChanged(index);
//				}
			}
		}
	}
	
	*/
	public boolean isButtonPressed(int index) {
		if (index < 0 || index > TOTAL_BUTTONS) {
			return false;
		} else {
			return buttonPressMap[index];
		}
	}
	
	public void addListener(Listener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public void releaseAllButtons() {
		for (int i = 0; i < buttonPressMap.length; ++i) {
			buttonPressMap[i] = false;
		}
		for (Listener listener : listeners) {
			listener.multipleButtonStateChanged();
		}
	}
	
	public void dispose() {
		
	}
}
