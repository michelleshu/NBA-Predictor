/**
 * WeakLearner.java
 * An abstract class for weak learners to be used in AdaBoost
 * 
 * @author Michelle Shu
 * January 25, 2013
 */

public abstract class WeakLearner {
	
	/* The combination coefficient (c) is a value chosen between 0 and 1 that
	 * takes into account the accuracy of this learner's hypothesis with
	 * respect to the current weighted distribution of training examples */
	public double combCoefficient;
	
	/** Setter for combCoefficient */
	public void setCombCoef(double cc) {
		this.combCoefficient = cc;
	}
	
	/** Given input data (x), return predicted output (y) */
	abstract double getHypothesis(double[] input_data);
	
}