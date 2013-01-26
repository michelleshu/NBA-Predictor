/**
 * WeakLearner.java
 * An interface for weak learners to be used in AdaBoost
 * 
 * @author Michelle Shu
 * January 25, 2013
 */

import java.util.ArrayList;

public interface WeakLearner {
	/** Given input data (x), return predicted output (y) */
	double getHypothesis(double[] input_data);
	
}