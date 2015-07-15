package org.asmlibrary.fit;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.media.MediaPlayer;

public class MusicPlayerCrossFade {
	MediaPlayer mp[]= new MediaPlayer[2];
	Context context;
	int volume[] = new int[2];
	
	
	
	public MusicPlayerCrossFade(Context context ){
		this.context = context;
		volume[0]=volume[1]=0;
		
	}
	public void play(int emoIndex){
		if(mp[0]== null || (mp[1]!=null && mp[1].isPlaying())){
			startMusic(0,emoIndex);
			
		}
		else if(mp[0] !=null && mp[0].isPlaying()){
			startMusic(1,emoIndex);
		}
		
		
		//mp2.start();
	}
	void startMusic(int index, int emoIndex){
		if(mp[index] != null ){
			mp[index].stop();
			mp[index].release();
		}
		switch(emoIndex){
			case Const.EMO_HAPPY:
				mp[index] = MediaPlayer.create(context, R.raw.emo_3_happy);
				break;
			case Const.EMO_SURPRISE:
				mp[index] = MediaPlayer.create(context, R.raw.emo_1_surprise);
				break;
			case Const.EMO_ANGER:
				mp[index] = MediaPlayer.create(context, R.raw.emo_6_anger);
				break;
			case Const.EMO_DISGUST:
				mp[index] = MediaPlayer.create(context, R.raw.emo_5_digust);
				break;
			case Const.EMO_FEAR:
				mp[index] = MediaPlayer.create(context, R.raw.emo_2_fear);
				break;
			case Const.EMO_SAD:
				mp[index] = MediaPlayer.create(context, R.raw.emo_4_sad);
				break;
		}
		if(!mp[index].isPlaying()){
			mp[index].start();
			fadePlay(index);
		}
		
	}
	
	public void fadePlay(final int index){
		final Timer timer = new Timer(true);
		TimerTask timerTask = new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				//updateVolume(1);
				
				if(mp[index]!= null && mp[index].isPlaying()){
					volume[index] += 5;
					if(volume[index]>=100){
						volume[index]=100;
						timer.cancel();
						timer.purge();
					}
					mp[index].setVolume(((float)volume[index])/(float)100, ((float)volume[index])/(float)100);
				}
				
				if(mp[1-index]!= null && mp[1-index].isPlaying()){
					volume[1-index] -= 5;
					if(volume[1-index]<=0){
						volume[1-index]=0;
						mp[1-index].stop();
					}
					mp[1-index].setVolume(((float)volume[1-index])/(float)100, ((float)volume[1-index])/(float)100);
				}
				
				
				
			}	
		};
		timer.scheduleAtFixedRate(timerTask, 0, 100);
	}
	public void stop(){
		mp[0].stop();
		mp[1].stop();
	}
}
