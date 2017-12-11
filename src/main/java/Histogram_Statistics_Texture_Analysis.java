import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;

import java.lang.Math;
import java.awt.Rectangle;

import ij.measure.ResultsTable;


/**
 * Compute statistics on histogram for texture analysis.
 * @author Julien Pontabry
 */
public class Histogram_Statistics_Texture_Analysis implements PlugInFilter {
	/**
	 * Name of the image.
	 */
	String m_name;
	
	/**
	 * Setup method
	 * @param arg Arguments of the plugin.
	 * @param imp Image on which the plugin will be processed.
	 */
	public int setup(String arg, ImagePlus imp)
	{
		// Check if there is an image
		if(imp == null)
		{
			IJ.error("Histogram statistics texture analysis", "There is no image");
			return DONE;
		}
		
		// Get the name of the image
		m_name = imp.getTitle();

		return DOES_8G+DOES_16+NO_CHANGES;
	}
	
	/**
	 * Actually run the plugin processing.
	 * @param ip Image processor on which processing is done.
	 */
	public void run(ImageProcessor ip)
	{
		///////////////////////////////////////////////////
		// Get ROI histogram
		//
		
		int[] histogram = this.computeHistogramFromROI(ip);


		///////////////////////////////////////////////////
		// Normalize histogram
		//
		
		// Sum up the histogram
		double sumHistogram = 0.0;
		
		for(int i = 0; i < histogram.length; i++)
		{
			sumHistogram += histogram[i];
		}
		
		// Divide the histogram by its sum to normalize it
		double[] normalizedHistogram = new double[histogram.length];
		
		for(int i = 0; i < normalizedHistogram.length; i++)
		{
			normalizedHistogram[i] = (double)histogram[i] / sumHistogram;
		}
		
		
		///////////////////////////////////////////////////
		// Compute statistics on normalized histogram
		//
		
		// Compute the mean
		double mean = this.computeMean(normalizedHistogram);
		
		// Compute the variance
		double variance = this.computeNthMoment(normalizedHistogram, 2);
		
		// Compute the standard deviation
		double stdDeviation = Math.sqrt(variance);
		
		// Normalize the variance
		double normalizedVariance = variance / ( (normalizedHistogram.length-1) * (normalizedHistogram.length-1) );
		
		// Compute the relative smoothness coefficient
		double relativeSmoothness = this.computeRelativeSmoothness(normalizedVariance);
		
		// Compute the skewness of the histogram
		double skewness = this.computeNthMoment(normalizedHistogram, 3) / Math.pow(stdDeviation, 3);
		
		// Compute the relative flatness of the histogram
		double kurtosis = this.computeNthMoment(normalizedHistogram, 4) / Math.pow(stdDeviation, 4) - 3.0;
		
		// Compute the uniformity coefficient
		double uniformity = this.computeUniformity(normalizedHistogram);
		
		// Compute the entropy
		double entropy = this.computeEntropy(normalizedHistogram);
		entropy /= Math.log(normalizedHistogram.length) / Math.log(2.0);
		
		
		///////////////////////////////////////////////////
		// Display values in a table
		//
		
		// Create a new result table
		ResultsTable results = ResultsTable.getResultsTable();
		
		// Add measurements to the table
		results.incrementCounter();
		results.addLabel(m_name);
		
		results.addValue("Mean", mean);
		results.addValue("Std deviation", stdDeviation);
		results.addValue("Relative Smoothness", relativeSmoothness);
		results.addValue("Skewness", skewness);
		results.addValue("Kurtosis", kurtosis);
		results.addValue("Uniformity", uniformity);
		results.addValue("Entropy", entropy);
		
		// Display table
		results.show("Results");
	}
	
	/**
	 * Compute the histogram of a ROI defined in image (or entire image if there is no defined ROI).
	 * @param ip The image processor of the image to process.
	 * @return The histogram of the defined ROI (or of the entire image if there is no defined ROI).
	 */
	public int[] computeHistogramFromROI(ImageProcessor ip)
	{
		// Get the bounding box and the mask of the ROI (if any)
		Rectangle boundingBox = ip.getRoi();
		ImageProcessor   mask = ip.getMask();
		
		// Compute histogram inside ROI
		int[] histogram = new int[(int)Math.pow(2,ip.getBitDepth())];
		
		for(int y = 0; y < ip.getHeight(); y++)
		{
			if(y >= boundingBox.y && y < boundingBox.y + boundingBox.height) // inside bounding box on y axis
			{
				for(int x = 0; x < ip.getWidth(); x++)
				{
					if(x >= boundingBox.x && x < boundingBox.x + boundingBox.width) // inside bounding box on x axis
					{
						if(mask == null || mask.get(x-boundingBox.x,y-boundingBox.y) > (byte)0) // inside mask of ROI
						{
							histogram[ip.get(x,y)]++;
						} // outside mask of ROI
					} // outside bounding box of ROI on x axis
				} // for each column
			} // outside bounding box of ROI on y axis
		} // for each row
		
		return histogram;
	}
	
	/**
	 * Compute the mean of the normalized histogram.
	 * @param histogram Normalized histogram.
	 * @return Mean of the normalized histogram.
	 */
	public double computeMean(double[] histogram)
	{
		double mean = 0.0;
		
		for(int i = 0; i < histogram.length; i++)
		{
			mean += i * histogram[i];
		}
		
		return mean;
	}
	
	/**
	 * Compute the nth moment of the normalized histogram.
	 * @param histogram Normalized histogram.
	 * @param moment Moment order to compute (it must be a positive integer).
	 * @return The value of the nth moment of the histogram.
	 */
	public double computeNthMoment(double[] histogram, int moment)
	{
		double      mean = this.computeMean(histogram);
		double nthMoment = 0.0;
		
		for(int i = 0; i < histogram.length; i++)
		{
			nthMoment += Math.pow((double)i-mean, moment) * histogram[i];
		}
		
		return nthMoment;
	}
	
	/**
	 * Compute the relative smoothness coefficient (R).
	 * @param variance Normalized variance (variance divided by (maxIntensity-1)^2.
	 * @return The relative smoothness coefficient.
	 */
	public double computeRelativeSmoothness(double variance)
	{
		return 1.0 - (1.0 / (1.0+variance));
	}
	
	/**
	 * Compute the uniformity coefficient (U).
	 * @param histogram Normalized histogram.
	 * @return The uniformity coefficient.
	 */
	public double computeUniformity(double[] histogram)
	{
		double uniformity = 0.0;
		
		for(int i = 0; i < histogram.length; i++)
		{
			uniformity += histogram[i]*histogram[i];
		}
		
		return uniformity;
	}
	
	/**
	 * Compute the entropy of the normalized histogram.
	 * @param histogram Normalized histogram.
	 * @return The entropy of the normalized histogram.
	 */
	public double computeEntropy(double[] histogram)
	{
		double entropy = 0.0;
		
		for(int i = 0; i < histogram.length; i++)
		{
			if(histogram[i] > 0.0)
			{
				entropy += histogram[i] * (Math.log(histogram[i]) / Math.log(2.0));
			}
		}
		
		return -entropy;
	}
	
	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Histogram_Statistics_Texture_Analysis.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();
	}
}
