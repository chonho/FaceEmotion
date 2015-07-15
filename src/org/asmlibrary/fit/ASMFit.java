package org.asmlibrary.fit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;

import android.util.Log;

public class ASMFit {
	
	public ASMFit(){}
	
	/**
	 * This function can only be used after nativeInitFastCascadeDetector()
	 * @param imageGray original gray image 
	 * @param faces all faces' feature points 
	 * @return true if found faces, false otherwise
	 */
	public boolean fastDetectAll(Mat imageGray, Mat faces){
		return nativeFastDetectAll(imageGray.getNativeObjAddr(), 
				faces.getNativeObjAddr());
	}
	
	/**
	 * This function can only be used after nativeInitCascadeDetector()
	 * @param imageGray original gray image 
	 * @param face all faces' feature points 
	 * @return true if found faces, false otherwise
	 */
	public boolean detectAll(Mat imageGray, Mat faces){
		return nativeDetectAll(imageGray.getNativeObjAddr(), 
				faces.getNativeObjAddr());
	}
	
	/**
	 * This function can only be used after nativeInitCascadeDetector()
	 * @param imageGray original gray image 
	 * @param faces only one face's feature points 
	 * @return true if found faces, false otherwise
	 */
	public boolean detectOne(Mat imageGray, Mat face){
		return nativeDetectOne(imageGray.getNativeObjAddr(), 
				face.getNativeObjAddr());
	}
	
	public void fitting(Mat imageGray, Mat shapes){
		nativeFitting(imageGray.getNativeObjAddr(), 
				shapes.getNativeObjAddr());
	}
	
	public boolean videoFitting(Mat imageGray, Mat shape, long frame){
		return nativeVideoFitting(imageGray.getNativeObjAddr(),
				shape.getNativeObjAddr(), frame);
	}
	
	public static native boolean nativeReadModel(String modelName);

	/**
	 * @param cascadeName could be haarcascade_frontalface_alt2.xml 
	 * @return
	 */
	public static native boolean nativeInitCascadeDetector(String cascadeName);
	public static native void nativeDestroyCascadeDetector();
	
	/**
	 * @param cascadeName could be lbpcascade_frontalface.xml 
	 * @return
	 */
	public static native boolean nativeInitFastCascadeDetector(String cascadeName);
	public static native void nativeDestroyFastCascadeDetector();
	
	public static native void nativeInitShape(long faces);
	
	private static native boolean nativeDetectAll(long inputImage, long faces);
	private static native boolean nativeDetectOne(long inputImage, long face);
	private static native boolean nativeFastDetectAll(long inputImage, long faces);
	
	private static native void nativeFitting(long inputImage, long shapes);
	private static native boolean nativeVideoFitting(long inputImage, long shape, long frame);

}
