package org.asmlibrary.fit;

import java.util.ArrayList;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.media.AudioManager;
import android.media.MediaPlayer;

public class MusicService extends Service
{
	private MediaPlayer player;
	//Broadcast receiver and intent filter are used for communicating between service and activity
    private BroadcastReceiver receiver;
    private IntentFilter filter;
    private ArrayList<Long> songIdList;
    private int emoIndex=-1;
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		initMusicPlayer();
		getSongList();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void initMusicPlayer(){
		  //set player properties
		player = new MediaPlayer();
		player.setWakeMode(getApplicationContext(),
				  PowerManager.PARTIAL_WAKE_LOCK);
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				mp.start();
			}
		});
		player.setVolume(1, 1);
		
		filter = new IntentFilter();
		filter.addAction(Const.ACTION_CHANGE_MUSIC);
		receiver = new BroadcastReceiver(){
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals(Const.ACTION_CHANGE_MUSIC)){
					
					int emo = intent.getIntExtra(Const.EMO_INDEX, 0);
					if(emoIndex!=emo){
						emoIndex = emo;
						playSong(songIdList.get(emo));
					}
//					switch (emo){
//					case Const.EMO_HAPPY:
//						break;
//					case Const.EMO_SAD:
//						break;
//					case Const.EMO_SURPRISE:
//						break;
//						
//					}
					
				}
			};
		};
		registerReceiver(receiver, filter);
	}
	
	public void getSongList() {
		songIdList = new ArrayList<Long>();
		ContentResolver musicResolver = getContentResolver();
		Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
		if(musicCursor!=null && musicCursor.moveToFirst()){
			  //get columns
			  int titleColumn = musicCursor.getColumnIndex
			    (android.provider.MediaStore.Audio.Media.TITLE);
			  int idColumn = musicCursor.getColumnIndex
			    (android.provider.MediaStore.Audio.Media._ID);
			  //add songs to list
			  do {
			    long thisId = musicCursor.getLong(idColumn);
			    String thisTitle = musicCursor.getString(titleColumn);
			    Log.d("MusicService", thisTitle);
			    songIdList.add(thisId);
			  }
			  while (musicCursor.moveToNext());
			}
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		player.stop();
		unregisterReceiver(receiver);
	}
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void playSong(Long songIndex){
		player.reset();
	
		Uri trackUri = ContentUris.withAppendedId(
		  android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
		  songIndex);
		try{
			  player.setDataSource(getApplicationContext(), trackUri);
			}
			catch(Exception e){
			  Log.e("MUSIC SERVICE", "Error setting data source", e);
			}
		player.prepareAsync();
	}

	
	 
}
