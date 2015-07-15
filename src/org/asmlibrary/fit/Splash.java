package org.asmlibrary.fit;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Space;

public class Splash extends Activity {
	private int progress=0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		
		ImageView img = (ImageView)findViewById(R.id.facegif);
		 img.setBackgroundResource(R.drawable.spin_animation);

		 // Get the background, which has been compiled to an AnimationDrawable object.
		 AnimationDrawable frameAnimation = (AnimationDrawable) img.getBackground();

		 // Start the animation (looped playback by default).
		 frameAnimation.start();
		 
		 final Drawable draw=getResources().getDrawable(R.drawable.custom_progressbar);
		// set the drawable as progress drawable
		 final ProgressBar progressBar = (ProgressBar)findViewById(R.id.progressBar1);
		 progressBar.setProgressDrawable(draw);
		 
		 Timer timer = new Timer();
		 timer.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				progress++;
				
				progressBar.setProgress(progress);
				if(progress==100){
					startActivity(new Intent(Splash.this, MainActivity.class));
					finish();
				}
			}
		}, 0, 40);
		
	}



	
}
