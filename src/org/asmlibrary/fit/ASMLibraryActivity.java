package org.asmlibrary.fit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.asmlibrary.fit.R;
import org.asmlibrary.fit.ASMFit;

//import com.jjoe64.graphview.*;

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;

import android.R.bool;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import libsvm.*;

public class ASMLibraryActivity extends Activity implements CvCameraViewListener2{
	
	final int FEATURE = 13;
    
	private static final String    TAG                 = "ASMLibraryDemo";
    
    private Mat                    	mRgba;
    private Mat                    	mGray;
    private Mat                    	mGray2;
    private Mat 					mRgbaT;
    private Mat						mGrayT;
    private double					ratioRes;
    private int						screenWidth;
    private int						screenHeight;
    private int						rotateCam=-1;
    private File                   	mCascadeFile;
    private File                   	mFastCascadeFile;
    private File                   	mModelFile;
    private ASMFit      		   	mASMFit;
    private long				   	mFrame;
    private boolean					mFlag;
    private boolean					mPortrait = false; //fit asm in portrait view
    private boolean					mFastDetect = true; //use fastDetect since beginning
    private Mat						mShape;
    private static final Scalar 	mColor = new Scalar(0, 255, 0);
    private MenuItem               	mHelpItem;
    private MenuItem               	mDetectItem;
    private MenuItem               	mOrieItem;
    private MenuItem				mCameraitem;
    private CameraBridgeViewBase   	mOpenCvCameraView;
    private int 					mCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
    
    public TextView emoDesc;
    private ImageView emo;
    //queue
    private int[]					tempGuess = new int[5];
    private int						cFrame;
    private int 					[]freq; 
    private int						prevEmo=0;
    //private TextView				guessResult;
    private GraphicalView			lineGraph;
    private RelativeLayout 			chartContainer;
    
    
    
    public ASMLibraryActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }
    
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
    	private File getSourceFile(int id, String name, String folder){
    		File file = null;
    		try {
	    		InputStream is = getResources().openRawResource(id);
	            File cascadeDir = getDir(folder, Context.MODE_PRIVATE);
	            file = new File(cascadeDir, name);
	            FileOutputStream os = new FileOutputStream(file);
	            
	            byte[] buffer = new byte[4096];
	            int bytesRead;
	            while ((bytesRead = is.read(buffer)) != -1) {
	                os.write(buffer, 0, bytesRead);
	            }
	            is.close();
	            os.close();
    		}catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to load file " + name + ". Exception thrown: " + e);
            }
	            
            return file;
    		
    	}
    	
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("asmlibrary");
                    System.loadLibrary("jni-asmlibrary");
                    
                    mASMFit = new ASMFit();

                    mModelFile = getSourceFile(R.raw.my68_1d, "my68_1d.amf", "model");
                    if(mModelFile != null)
                    	mASMFit.nativeReadModel(mModelFile.getAbsolutePath());
                    
                    
                    mCascadeFile = getSourceFile(R.raw.haarcascade_frontalface_alt2, 
                    		"haarcascade_frontalface_alt2.xml", "cascade");
                    if(mCascadeFile != null)
                    	mASMFit.nativeInitCascadeDetector(mCascadeFile.getAbsolutePath());

                    mFastCascadeFile = getSourceFile(R.raw.lbpcascade_frontalface, 
                    		"lbpcascade_frontalface.xml", "cascade");
                    if(mFastCascadeFile != null)
                    	mASMFit.nativeInitFastCascadeDetector(mFastCascadeFile.getAbsolutePath());
                    
                    //test image alignment
                    // load image file from application resources
                	File JPGFile = getSourceFile(R.raw.gump, "gump.jpg", "image");
                	
                	Mat image = Highgui.imread(JPGFile.getAbsolutePath(), Highgui.IMREAD_GRAYSCALE);
                    Mat shapes = new Mat();
                    
                    if(mASMFit.detectAll(image, shapes) == true)
        			{
                    	/*
                    	for(int i = 0; i < shapes.row(0).cols()/2; i++)
        				{
                        	Log.d(TAG, "before points:" + 
                        			shapes.get(0, 2*i)[0] +"," +shapes.get(0, 2*i+1)[0]);
        				}
        				*/
                    	
        				mASMFit.fitting(image, shapes);
        				
        				/*
        				for(int i = 0; i < shapes.row(0).cols()/2; i++)
        				{
                        	Log.d(TAG, "after points:" + 
                        			shapes.get(0, 2*i)[0] +"," +shapes.get(0, 2*i+1)[0]);
        				}
        				*/
        			}

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

	/** Called when the activity is first created. */
	@Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);
        emoDesc = (TextView) findViewById(R.id.emo_desc);
        emo = (ImageView) findViewById(R.id.emo);
        
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();
        
        //Log.d("CamView",Build.FINGERPRINT+" =P " + Build.MODEL);
        //get screen resolution
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        ratioRes = (double)displaymetrics.widthPixels/displaymetrics.heightPixels;
        //ratioRes = (double)mOpenCvCameraView.getWidth()/mOpenCvCameraView.getHeight();
        //04-07 14:45:06.839: D/CamView(1282): generic/vbox86p/vbox86p:4.3/JLS36G/eng.buildbot.20141001.104431:userdebug/test-keys =P Samsung Galaxy Note 3 - 4.3 - API 18 - 1080x1920

        //screenWidth = displaymetrics.widthPixels;
        //screenHeight = displaymetrics.heightPixels;
        //if (ratioRes>1) ratioRes=1/ratioRes; //i don't know if it helps. 
        
        mFrame = 0;
        mFlag = false;
        copyAssets(); 
        
        //guess 
        for(int i=0; i<tempGuess.length; i++) tempGuess[i]=-1;
        cFrame = 0;
        freq = new int [6];
        for(int i=0; i<freq.length; i++) freq[i]=0;
        
     // Getting a reference to LinearLayout of the MainActivity Layout
        chartContainer = (RelativeLayout) findViewById(R.id.lineGraph);
        openGraph(tempGuess);
        		
//        mOpenCvCameraView.disableView();
//        mOpenCvCameraView.setCameraIndex(mCameraIndex);
//        mOpenCvCameraView.enableView();
        
        //Intent musicIntent = new Intent(this, MusicService.class);
        //startService(musicIntent);
  
    }
	
	@Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        mFrame = 0;
        mFlag = false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu"+mFastDetect);
        mCameraitem = menu.add("Toggle Front/Back");
        mOrieItem = menu.add("Toggle Portrait");
        if(mFastDetect == true)
        	mDetectItem = menu.add("CascadeDetector");
        else
        	mDetectItem = menu.add("FastCascadeDetector");
        mHelpItem = menu.add("About");
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        
        if (item == mHelpItem)
        	new AlertDialog.Builder(this).setTitle("About")
    		.setMessage("FYP - Analysis of mood using video\n"+
    				"FYP Student: Yean Seanglidet\n" +
    				"ASMLibrary Copyright (c) 2008-2011 by Yao Wei, all rights reserved.\n")
    				.setPositiveButton("OK", null).show();
        else if(item == mDetectItem)
        {
        	mFastDetect = !mFastDetect;
        }
        else if(item == mOrieItem)
        {
        	mPortrait = !mPortrait;
        }
        else if(item == mCameraitem)
        {
        	
        	if(mCameraIndex == CameraBridgeViewBase.CAMERA_ID_ANY ||
        			mCameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK)
        	{
        		mCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
        		rotateCam=-1;
        	}
        	else
        	{
        		mCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
        		rotateCam=1;
        	}
        	
        	
        	mOpenCvCameraView.disableView();
        	mOpenCvCameraView.setCameraIndex(mCameraIndex);
        	mOpenCvCameraView.enableView();
        	
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
        //Intent musicIntent = new Intent(this, MusicService.class);
        //stopService(musicIntent);
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mGray2 = new Mat();
        mRgba = new Mat();
        mRgbaT = new Mat();
        mGrayT = new Mat();
        mShape = new Mat();
        
        mFrame = 0;
        mFlag = false;
        
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mRgbaT.release();
        mGrayT.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	int curEmo;
//    	int cropHeight;
//	   	int cropWidth;
//	   	int cropX;
//	   	int cropY;
	   	
    	try {
			//trainWeka();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	//ratioRes = (double)mOpenCvCameraView.getWidth()/mOpenCvCameraView.getHeight();
    	//Log.d("CamView", mOpenCvCameraView.getWidth() + "  "+mOpenCvCameraView.getHeight());
    	//Log.d("CamView", mOpenCvCameraView.getMeasuredWidth() + "  "+mOpenCvCameraView.getMeasuredHeight()+ " lol");
    	 if (isPhone()){
    	 mRgba = inputFrame.rgba();
    	 mGray = inputFrame.gray();
   
    	 mRgbaT = mRgba.t();
    	 mGrayT = mGray.t();
    	 
    	 if(mPortrait ==true) 
         	Core.transpose(mGray, mGray2);
         else
         	mGray2 = mGray;
    	 
    	 //if(mRgba.width()>= mRgba.height()) rotateCam=-1;
    	 
    	 Core.flip(mRgba.t(), mRgbaT, rotateCam);
    	 Core.flip(mGray2.t(),mGrayT, rotateCam);
    	 
    	 
//    	 if (mGrayT.width()>= mGrayT.height()){
//    	 
//    		 cropHeight = mGrayT.height();
//    		 cropWidth = (int)(mGrayT.height()* ratioRes);
//    	     cropX = (mGrayT.width()-cropWidth)/2;
//    	     cropY = 0;
//    	 }
//    	 else{
//    		 
//    		 cropHeight = (int)(mGrayT.width()/ratioRes);
//    		 cropWidth = mGrayT.width();
//    	     cropX = 0;
//    	     cropY = (mGrayT.height()-cropHeight)/2;
//    	 }
    	 int cropWidth= mRgbaT.cols();
    	 int cropYposition = Math.abs(mRgbaT.rows()- mRgbaT.cols())/2; 
    	 Rect myROI = new Rect(0, cropYposition, cropWidth,(int)(cropWidth*ratioRes));
    	 //crop image
    	 //Rect myROI = new Rect(0, cropY , cropWidth,cropHeight);
    	
    	 mRgbaT = new Mat(mRgbaT,myROI);
    	 
    	 mGrayT=new Mat(mGrayT,myROI);
    	 
    	 
    	 //Log.d("mat", mRgba.size()+"  "+mRgbaT.size());
    	 Imgproc.resize(mRgbaT, mRgbaT,mRgba.size());
    	 Imgproc.resize(mGrayT, mGrayT,mRgba.size());
    	 
    	 }
    	 else{
    		 mRgbaT = inputFrame.rgba();
        	 mGray = inputFrame.gray();
        	 
        	 if(mPortrait ==true) 
              	Core.transpose(mGray, mGray2);
              else
              	mGray2 = mGray;
        	 
        	 mGrayT = mGray2;
    	 }
    	 
    	 
    	 
        //WindowManager manager = getWindowManager();
        //int width = manager.getDefaultDisplay().getWidth();
        //int height = manager.getDefaultDisplay().getHeight();
        //Log.d(TAG, "å±�å¹•å¤§å°�" + width + "x" + height);

        if(mFrame == 0 || mFlag == false)
		{
        	Mat detShape = new Mat();
			if(mFastDetect)
				//mFlag = mASMFit.fastDetectAll(mGray2, detShape);
				mFlag = mASMFit.fastDetectAll(mGrayT, detShape);
			else
				//mFlag = mASMFit.detectAll(mGray2, detShape);
				mFlag = mASMFit.detectAll(mGrayT, detShape);
			if(mFlag)	mShape = detShape.row(0);
		}
			
		if(mFlag) 
		{
			//mFlag = mASMFit.videoFitting(mGray2, mShape, mFrame);
			mFlag = mASMFit.videoFitting(mGrayT, mShape, mFrame);
		}
		
		if(mFlag)
		{
			//double[] test = new double[FEATURE];
			
			double fWidth = 0;
			if(mPortrait == true)
			{
				int nPoints = mShape.row(0).cols()/2;
				
				double[] xLeft={mShape.get(0, 2*0+1)[0],mShape.get(0, 2*1+1)[0],mShape.get(0, 2*2+1)[0],mShape.get(0, 2*3+1)[0],mShape.get(0, 2*4+1)[0]};
				double[] xRight={mShape.get(0, 2*14+1)[0],mShape.get(0, 2*13+1)[0],mShape.get(0, 2*12+1)[0],mShape.get(0, 2*11+1)[0],mShape.get(0, 2*10+1)[0]};
			    
				fWidth=getMaxValue(xRight)-getMinValue(xLeft); //face width
				
				xLeft=null;
				xRight=null;
				
				
						
				// creating a testing data
				// e.g., 1st testing sample in iris-test.txt
				//    0 1:0.555 2:-0.166 3:0.661 4:0.666
				//    true class value is 3		

				
				
				for(int i = 0; i < nPoints; i++)
				{ 
					double x = mShape.get(0, 2*i)[0];
					double y = mShape.get(0, 2*i+1)[0];
					Point pt = new Point(y, x);
					
					Core.circle(mRgbaT, pt, 5, mColor,-1);
				}		
				
			}
			else
			{
				int nPoints = mShape.row(0).cols()/2;
				
				double[] xLeft={mShape.get(0, 2*0)[0],mShape.get(0, 2*1)[0],mShape.get(0, 2*2)[0],mShape.get(0, 2*3)[0],mShape.get(0, 2*4)[0]};
				double[] xRight={mShape.get(0, 2*14)[0],mShape.get(0, 2*13)[0],mShape.get(0, 2*12)[0],mShape.get(0, 2*11)[0],mShape.get(0, 2*10)[0]};
			    
				fWidth=getMaxValue(xRight)-getMinValue(xLeft); //face width
				
				xLeft=null;
				xRight=null;
				
				for(int i = 0; i < nPoints; i++)
				{ 
					Point pt = new Point(mShape.get(0, 2*i)[0], mShape.get(0, 2*i+1)[0]);
					Core.circle(mRgbaT, pt, 5, mColor,-1);
					
				}

			}
			
			if (cFrame==3)
			{
				//make prediction.
				svm_model model = null;
				try {
					File arffFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android/data/"+getPackageName()+"/files/ckfe.txt.model");
					//Log.d("ARFF","File path: "+Environment.getExternalStorageDirectory().getAbsolutePath());
					Log.d("ARFF","File exist: "+arffFile.exists());
					model = svm.svm_load_model(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Android/data/"+getPackageName()+"/files/ckfe.txt.model");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				int NUM_ATTR = 13;
				svm_node[] test = new svm_node[NUM_ATTR];
	
				for(int i=0; i<NUM_ATTR; i++) {
					test[i] = new svm_node();
					test[i].index = i+1;
				}
				//--------------------------------------------------------------------------------------------------------------------------------------
				//getting feature points 
				//myfile << "	<eyebrow-eye>" <<endl;
				test[0].value= distanceP(mShape.get(0, 2*21)[0],mShape.get(0, 2*21+1)[0], mShape.get(0, 2*27)[0],mShape.get(0, 2*27+1)[0])/fWidth;
				test[1].value= distanceP(mShape.get(0, 2*24)[0],mShape.get(0, 2*24+1)[0], mShape.get(0, 2*29)[0],mShape.get(0, 2*29+1)[0])/fWidth;
				test[2].value= distanceP(mShape.get(0, 2*18)[0],mShape.get(0, 2*18+1)[0], mShape.get(0, 2*34)[0],mShape.get(0, 2*34+1)[0])/fWidth;
				test[3].value= distanceP(mShape.get(0, 2*15)[0],mShape.get(0, 2*15+1)[0], mShape.get(0, 2*32)[0],mShape.get(0, 2*32+1)[0])/fWidth;
	
				//myfile << "	<eye>" ;
				test[4].value= distanceP(mShape.get(0,2*27)[0],mShape.get(0,2*27+1)[0], mShape.get(0,2*29)[0],mShape.get(0,2*29+1)[0])/distanceP(mShape.get(0,2*28)[0], mShape.get(0,2*28+1)[0],mShape.get(0,2*30)[0],mShape.get(0,2*30+1)[0]);
				test[5].value= distanceP(mShape.get(0,2*34)[0],mShape.get(0,2*34+1)[0], mShape.get(0,2*32)[0],mShape.get(0,2*32+1)[0])/distanceP(mShape.get(0,2*33)[0], mShape.get(0,2*33+1)[0],mShape.get(0,2*35)[0],mShape.get(0,2*35+1)[0]);
				
				//myfile << "	<mouth>" ;
				test[6].value= distanceP(mShape.get(0,2*48)[0],mShape.get(0,2*48+1)[0], mShape.get(0,2*54)[0],mShape.get(0,2*54+1)[0])/distanceP(mShape.get(0,2*64)[0],mShape.get(0,2*64+1)[0], mShape.get(0,2*61)[0],mShape.get(0,2*61+1)[0]);
				
				//myfile << "	<eye-mouth>" ;
				test[7].value=distanceP(mShape.get(0,2*30)[0],mShape.get(0,2*30+1)[0], mShape.get(0,2*48)[0],mShape.get(0,2*48+1)[0])/fWidth;
				test[8].value=distanceP(mShape.get(0,2*35)[0],mShape.get(0,2*35+1)[0], mShape.get(0,2*54)[0],mShape.get(0,2*54+1)[0])/fWidth;
	
				//myfile << "	<eye-nose>" ;
				test[9].value=distanceP(mShape.get(0,2*30)[0],mShape.get(0,2*30+1)[0], mShape.get(0,2*40)[0],mShape.get(0,2*40+1)[0])/fWidth;
				test[10].value=distanceP(mShape.get(0,2*35)[0],mShape.get(0,2*35+1)[0],mShape.get(0,2*42)[0],mShape.get(0,2*42+1)[0])/fWidth;
	
				//myfile << "	nose-mouth" ;
				test[11].value=distanceP(mShape.get(0,2*40)[0],mShape.get(0,2*40+1)[0], mShape.get(0,2*48)[0],mShape.get(0,2*48+1)[0])/fWidth;
				test[12].value=distanceP(mShape.get(0,2*42)[0],mShape.get(0,2*42+1)[0], mShape.get(0,2*54)[0],mShape.get(0,2*54+1)[0])/fWidth;
	//--------------------------------------------------------------------------------------------------------------------------------------
				/*
				//debug log
				Log.d("ASM_Feature", "test 0: " + test[0]);
				Log.d("ASM_Feature", "test 1: " + test[1]);
				Log.d("ASM_Feature", "test 2: " + test[2]);
				Log.d("ASM_Feature", "test 3: " + test[3]);
				Log.d("ASM_Feature", "test 4: " + test[4]);
				Log.d("ASM_Feature", "test 5: " + test[5]);
				Log.d("ASM_Feature", "test 6: " + test[6]);
				Log.d("ASM_Feature", "test 7: " + test[7]);
				Log.d("ASM_Feature", "test 8: " + test[8]);	
				Log.d("ASM_Feature", "test 9: " + test[9]);
				Log.d("ASM_Feature", "test 10: " + test[10]);
				Log.d("ASM_Feature", "test 11: " + test[11]);
				Log.d("ASM_Feature", "test 12: " + test[12]);
				*/
				
				double v = svm.svm_predict(model,test);
				
				freq[(int)v-1]++; //change here
				//record the result
				int temp;
				
				temp = addQueue(tempGuess, (int)v);
				Log.d("SVM", "stat: " + tempGuess[0]+" "+tempGuess[1]+" "+tempGuess[2]+" "+tempGuess[3]+" "+tempGuess[4]);
				
				if (temp!=-1) freq[temp-1]--;
				
				cFrame = -1;	
				
				//guessResult.setText(""+v);
				curEmo = getMaxIndex(freq)+1;
				Log.d("SVM", "result: " + curEmo);
				
				//emoDesc = (TextView) findViewById(R.id.emo_desc);
				if (prevEmo != curEmo){  //update to new emo
					prevEmo = curEmo;
					showEmo(prevEmo);
				}
				showEmo(getMaxIndex(freq)+1);
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						// TODO Auto-generated method stub
						updateLineGraph(tempGuess);
					}
				});
				
			}
			cFrame ++;
			Log.d("cFrame", ""+cFrame);
			
		}
		
		mFrame ++;
		
        return mRgbaT;
		//return mGrayT;
    }
 
    //Line Graph
    public void openGraph(int []y){
    	int []x={1,2,3,4,5};
		//int []y={1,6,2,2,3};
		
		//convert to series
		TimeSeries series = new TimeSeries("Line1");
		for (int i=0; i<x.length; i++)
		{
			series.add(x[i], y[i]);
		}
		
		//draw the line - graph can have more series
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(series);
		
		//give properties of the line
		XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		renderer.setColor(Color.RED);
		renderer.setPointStyle(PointStyle.DIAMOND);
		renderer.setFillPoints(true);
		renderer.setLineWidth(3);
		
		mRenderer.addSeriesRenderer(renderer);
		mRenderer.setYAxisMin(0);
		mRenderer.setYAxisMax(6);
		mRenderer.setXAxisMax(5);
		mRenderer.setXAxisMin(0);
		mRenderer.setPointSize(3);
        // Creating a Line Chart
        lineGraph = ChartFactory.getLineChartView(getBaseContext(), dataset, mRenderer);
 
        // Adding the Line Chart to the LinearLayout
        chartContainer.addView(lineGraph);
    }
    
    public void updateLineGraph(int []y){
    	if (lineGraph!=null){
    		chartContainer.removeView(lineGraph);
    	}
    	
    	int []x={1,2,3,4,5};
		//int []y={1,6,2,2,3};
		
		//convert to series
		TimeSeries series = new TimeSeries("Line1");
		for (int i=0; i<x.length; i++)
		{
			series.add(x[i], y[i]);
		}
		
		//draw the line - graph can have more series
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(series);
		
		//give properties of the line
		XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		renderer.setColor(Color.RED);
		renderer.setPointStyle(PointStyle.DIAMOND);
		renderer.setFillPoints(true);
		renderer.setLineWidth(3);
		
		mRenderer.setYAxisMin(0);
		mRenderer.setYAxisMax(6);
		mRenderer.setXAxisMax(5);
		mRenderer.setXAxisMin(1);
		mRenderer.setPointSize(6);
		
		mRenderer.addSeriesRenderer(renderer);
 
        // Creating a Line Chart
        lineGraph = ChartFactory.getLineChartView(getBaseContext(), dataset, mRenderer);
 
        // Adding the Line Chart to the LinearLayout
        chartContainer.addView(lineGraph);
        
		lineGraph.repaint();

    	
    }
    
    //check Phone/Emulator
    boolean isPhone(){
    	if (Build.FINGERPRINT.contains("generic")){
    		return false;
    	}
    	return true;
    }
    
    //Show emo on screen
    void showEmo(final int intEmo){
    	//Intent musicBroadcast = new Intent(Const.ACTION_CHANGE_MUSIC);
    	//musicBroadcast.putExtra(Const.EMO_INDEX, intEmo-1);
    	//sendBroadcast(musicBroadcast);
    	
//    	if (intEmo==Const.EMO_SURPRISE){
//    		runOnUiThread(new Runnable() {					
//    			@Override
//    			public void run() {
//    				// TODO Auto-generated method stub
//    				//if (!((BitmapDrawable)emo.getDrawable()).getBitmap().isRecycled())
//    				//{
//    				((BitmapDrawable)emo.getDrawable()).getBitmap().recycle();
//    				
//    				//}
//    				emo.setImageResource(R.drawable.emo_surprised);
//    				emoDesc.setText("Surprise");
//    			}
//    		});
//    	}
//    	else if (intEmo==Const.EMO_FEAR){
//    		runOnUiThread(new Runnable() {					
//    			@Override
//    			public void run() {
//    				// TODO Auto-generated method stub
//    				((BitmapDrawable)emo.getDrawable()).getBitmap().recycle();
//    				emo.setImageResource(R.drawable.emo_fear);
//    				emoDesc.setText("Fear");
//    			}
//    		});
//    	}
//    	else if (intEmo==Const.EMO_HAPPY){
//    		runOnUiThread(new Runnable() {					
//    			@Override
//    			public void run() {
//    				// TODO Auto-generated method stub
//    				((BitmapDrawable)emo.getDrawable()).getBitmap().recycle();
//    				emo.setImageResource(R.drawable.emo_happy);
//    				emoDesc.setText("Happy");
//    			}
//    		});
//    	}
//    	else if (intEmo==Const.EMO_SAD){
//    		runOnUiThread(new Runnable() {					
//    			@Override
//    			public void run() {
//    				// TODO Auto-generated method stub
//    				((BitmapDrawable)emo.getDrawable()).getBitmap().recycle();
//    				emo.setImageResource(R.drawable.emo_sad);
//    				emoDesc.setText("Sad");
//    			}
//    		});
//    	}
//    	else if (intEmo==Const.EMO_SAD){
//    		runOnUiThread(new Runnable() {					
//    			@Override
//    			public void run() {
//    				// TODO Auto-generated method stub
//    				((BitmapDrawable)emo.getDrawable()).getBitmap().recycle();
//    				emo.setImageResource(R.drawable.emo_angry);
//    				emoDesc.setText("Anger");
//    			}
//    		});
//    	}
//    	else if (intEmo==6){
//    		runOnUiThread(new Runnable() {					
//    			@Override
//    			public void run() {
//    				// TODO Auto-generated method stub
//    				((BitmapDrawable)emo.getDrawable()).getBitmap().recycle();
//    				emo.setImageResource(R.drawable.emo_disgust);
//    				emoDesc.setText("Disgust");
//    			}
//    		});
//    	}
//    	else{
//    		runOnUiThread(new Runnable() {					
//    			@Override
//    			public void run() {
//    				// TODO Auto-generated method stub
//    				((BitmapDrawable)emo.getDrawable()).getBitmap().recycle();
//    				emo.setImageResource(R.drawable.ic_launcher);
//    				emoDesc.setText("Neutral");
//    			}
//    		});
//    	}
    	
    	runOnUiThread(new Runnable() {					
			@Override
			public void run() {
				emo.setImageResource(0);
				switch(intEmo){
					case Const.EMO_HAPPY:
						emo.setImageResource(R.drawable.emo_happy);
						emoDesc.setText("Happy");
						break;
					case Const.EMO_SAD:
						emo.setImageResource(R.drawable.emo_sad);
						emoDesc.setText("Sad");
						break;
					case Const.EMO_SURPRISE:
						emo.setImageResource(R.drawable.emo_surprised);
						emoDesc.setText("Surprise");
						break;
					case Const.EMO_ANGER:
						emo.setImageResource(R.drawable.emo_angry);
						emoDesc.setText("Angry");
						break;
					case Const.EMO_DISGUST:
						emo.setImageResource(R.drawable.emo_disgust);
						emoDesc.setText("Disgust");
						break;
					case Const.EMO_FEAR:
						emo.setImageResource(R.drawable.emo_fear);
						emoDesc.setText("Fear");
						break;
				}

				
			}
		});
    	
    	
    }
    
	public void trainWeka() throws Exception{
    	
		//File arffFile = new File(System.getProperty("user.dir")+"//res//raw//ckfe.arff");
		//FileInputStream fis = new FileInputStream("android.resource://org.asmlibrary.fit/asset/ckFE.arff");
		//File arffFile = new File("android.resource://org.asmlibrary.fit/assets/ckfe.arff");
		//BufferedReader bfr = new BufferedReader(new InputStreamReader(fis));
		//FileInputStream arffFile = new FileInputStream(R.raw.ckfe);
		//File arffFile = 
		//InputStream arffFile = getResources().openRawResource(R.raw.ckfe);
		//File arffFile = getAssets().open
		File arffFile = Environment.getExternalStorageDirectory().getAbsoluteFile();
		//InputStream arffFile = getAssets().open("ckfe.arff");
		Log.d("ARFF","File path: "+Environment.getExternalStorageDirectory().getAbsolutePath());
		Log.d("ARFF","File exist: "+arffFile.exists());
		//if (arffFile.available())
		//{
		   
	       //String fname = System.getProperty("user.dir")+"//res//raw//ckfe.arff";
	       //Log.d("ARFF","File path: "+ fname);
	       
			BufferedReader breader = null;
			breader = new BufferedReader(new FileReader(arffFile));
			
			
			if (breader!=null){
	    	
	    	Instances train = null;
			train = new Instances(breader);
			train.setClassIndex(train.numAttributes()-1);
	    	
	    	
			breader.close();
			
	    	
	    	NaiveBayes nB = new NaiveBayes();
	    	nB.buildClassifier(train);
	    	Evaluation eval = new Evaluation(train);
	    	eval.crossValidateModel(nB, train, 10,new Random(1));
	    	System.out.println(eval.toSummaryString("\nResults\n",true));
	    	System.out.println(eval.fMeasure(1) +" "+eval.precision(1)+" "+eval.recall(1));
			}
		//}
    }
	//add tail to linkedlist queue 
	public int addQueue(int []q,int tail){
		int head=q[0];
			
		for(int i=0; i < q.length-1; i++)
		{
			q[i]=q[i+1];
		}
		q[q.length-1]=tail;
		
		return head;
	}
    
    public static double distanceP(double p1x, double p1y, double p2x, double p2y){
    	double result;
    	result = Math.sqrt(((p1x-p2x)*(p1x-p2x)) + ((p1y-p2y)*(p1y-p2y)) );
    	return result;
    }
    
    // getting the miniumum value
    public static double getMinValue(double[] array){  
         double minValue = array[0];  
         for(int i=1;i<array.length;i++){  
	         if(array[i] < minValue){  
	        	 minValue = array[i];  
	         }  
         }  
        return minValue;  
    }
    
    // getting the maximum value
    public static double getMaxValue(double[] array){  
         double minValue = array[0];  
         for(int i=1;i<array.length;i++){  
	         if(array[i] > minValue){  
	        	 minValue = array[i];  
	         }  
         }  
        return minValue;  
    }
 
    public int getMaxIndex(int []array){
    	int result=0;
    	
    	for (int i=0; i<array.length; i++)
    	{
    		if (array[result] <= array[i]) result = i;  //tie breaker, choose the most recent one.
    	}
		return result;
    	
    }
    //----------------------------------------------------------------------
    //COPY Asset to Android data/data
    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        for(String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
              in = assetManager.open(filename);
              File outFile = new File(getExternalFilesDir(null), filename);
              out = new FileOutputStream(outFile);
              copyFile(in, out);
            } catch(IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }     
            finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // NOOP
                    }
                }
            }  
        }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
          out.write(buffer, 0, read);
        }
    }
    
}