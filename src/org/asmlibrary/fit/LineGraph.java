package org.asmlibrary.fit;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

public class LineGraph {
	public Intent getIntent(Context context){
		int []x={1,2,3,4,5};
		int []y={1,6,2,2,3};
		
		//convert to series
		TimeSeries series = new TimeSeries("Line1");
		for (int i=0; i<x.length; i++)
		{
			series.add(x[i], y[i]);
		}
		
		//draw the line - graph can have more series
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		dataset.addSeries(series);
		
		//give property of the line
		XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
		XYSeriesRenderer renderer = new XYSeriesRenderer();
		renderer.setColor(Color.BLUE);
		renderer.setPointStyle(PointStyle.DIAMOND);
		renderer.setFillPoints(true);
		
		
		
		mRenderer.addSeriesRenderer(renderer);
		
		//create intent 
		Intent intent = ChartFactory.getLineChartIntent(context, dataset, mRenderer,"Line graph title");
		
		
		
		return intent;
		
	}
}
