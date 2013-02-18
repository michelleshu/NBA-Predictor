/**
 * WeakLearner.java
 * A class to train weak learners to be used in AdaBoostR
 * Weak learners are trained with respect to a weighted distribution of 
 * training examples reflecting the current state of AdaBoostR at the time of
 * call.
 * 
 * @author Michelle Shu
 * Last Updated February 14, 2013
 */

import java.util.ArrayList;
import java.util.Random;

public class WeakLearner {
	private final static int SUBSET_SIZE = 8;   // Size of random subset of features
	private final static double LAMBDA = 0.0;	// Regularization term for LSR
	private final static double ALPHA = 0.1;	// Step size for LSR gradient descent
	private final static double TAU = 0.05;		// Stopping criterion for gradient
										        // descent convergence

	private final static boolean USE_TOP_FEATURES = false;
	private final static boolean USE_QUAD_BASIS = false;

	private double alpha;
	private ArrayList<TrainingExample> training_set;

	/* Theta is the parameter vector used by this learner after training to 
	 * make predictions. Theta has the same number of dimensions as the
	 * qyadratic basis vector of input. */
	private double[] theta;

	/* The indices of the features upon which this weak learner is trained */
	private Integer[] subset; 

	/* The combination coefficient (c) is a value chosen between 0 and 1 that
	 * takes into account the accuracy of this learner's hypothesis with
	 * respect to the current weighted distribution of training examples */
	private double combCoefficient;

	/** Setter for combination coefficient */
	public void setCombCoef(double cc) {
		this.combCoefficient = cc;
	}

	/** Get the combination coefficient of this weak learner. */
	public double getCombCoef() {
		return this.combCoefficient;
	}

	/** Constructor - Create a WeakLearner by training it on training set */
	public WeakLearner(ArrayList<TrainingExample> training_set) {
		this.training_set = training_set;

		/* 
		 * Take the training set given to the weak learner when it was created.
		 * Get a random subset of the feature dimensions of that training data,
 		 * and store them in variable subset.
		 */
		selectSubset(training_set.get(0).getInputVector().length);
	}

	/**
	 * Select feature indices for regression.
	 * If USE_TOP_FEATURES=false, use random selection.
	 * If USE_TOP_FEATURES=true, select feature by correlation coefficient.
	 * 
	 * @param rawFeatureCount total number of raw features
	 */
	private void selectSubset(int rawFeatureCount) {
		// estimate correlations between input features and the target
		TrainingSetTransformer.Correlation[] corr = TrainingSetTransformer.estimateCorrelations(training_set);

		if (USE_TOP_FEATURES) {
			// select feature data by its estimated coefficient with the target
			subset = new Integer[SUBSET_SIZE];
			for (int i= 0; i < SUBSET_SIZE; i++) {
				subset[i] = corr[i].getIndex();
 			}
		}
		else {
			// select randomly from the raw input up to selectedSize
			ArrayList<Integer> randomList = getRandomSubset(rawFeatureCount, SUBSET_SIZE);
			subset = new Integer[randomList.size()];
			randomList.toArray(subset);
		}

		// Use the first training sample to count the basis vector, which should be the same length as theta
		double[] features = selectFeatures(training_set.get(0).getInputVector());
		Double[] basis = getBasisVector(features);

		// guess initial theta values for linear terms 
		theta = new double[basis.length];
		TrainingSetTransformer.Correlation[] corrByIndex = new TrainingSetTransformer.Correlation[corr.length];
		for (TrainingSetTransformer.Correlation c : corr) {
			corrByIndex[c.getIndex()] = c;  // convert coefficient list corr into index list
		}
		theta[0] = 0;
		double total_weight = 0;
		for (int i = 1; i < features.length; i++) {
			double factor = corrByIndex[subset[i]].getFactor();
			total_weight += 1.0/factor;
			theta[0] += corrByIndex[subset[i]].getOffset() / factor;
		}
		theta[0] /= total_weight;
		for (int i = 1; i < features.length; i++) {
			theta[i] = 1.0/total_weight;
		}

		// set other terms of theta to 0
		if (basis.length > features.length + 1) {
			for (int i = features.length; i < basis.length; i++) {
				theta[i] = 0;
			}
		}
	}

	/**
	 * Get a random subset of feature indices
	 * 
	 * @param featureSize number of features in the sample
	 * @param subsetSize number of features 
	 * @return
	 */
	private ArrayList<Integer> getRandomSubset(int featureSize, int subsetSize) {
		// Start with an ArrayList that contains all possible feature index
		ArrayList<Integer> possibilities = new ArrayList<Integer>();
		for (int i = 0; i < featureSize; i++) {
			possibilities.add(i);
		}

		/* Randomly choose indices from possibilities */
		Random rand = new Random();
		ArrayList<Integer> random_subset = new ArrayList<Integer>();
		while (random_subset.size() < subsetSize && possibilities.size() > 0) {
			int selected = rand.nextInt(possibilities.size());
			random_subset.add(possibilities.remove(selected));
		}
		return random_subset;
	}

	/** Get the subset of feature dimensions selected */
	public Integer[] getSubset() {
		return this.subset;
	}

	/** Get the theta parameters for this weak learner */
	public double[] getTheta() {
		return this.theta;
	}

	/* Training process for the weak learner:
	 * 1. Choose random subset of features St such that |St| = some constant k.
	 * 2. For each example x(i), build a quadratic basis vector phi using St.
	 * 3. Use regularized least square regression to compute learning hypothesis.
	 */
	public void train() {
		/*
		 * Now we have the quadratic representation of all inputs. Use them to
		 * generate a quadratic parameter vector theta by gradient descent, 
		 * which will define this weak learner.
		 */
		alpha = ALPHA;
		batchGradientDescent();
	}

	/**
	 * Get prediction based on raw input vector
	 * @param rawInput
	 * @return
	 */
	public double getHypothesis(double[] rawInput) {
		// Convert raw input to the basis vector
		double[] features = selectFeatures(rawInput);
		Double[] basis = getBasisVector(features);

		// Get prediction from basis and theta dot product
		return getPrediction(basis);
	}

	/**
	 * Select subset feature values from raw input features.
	 * 
	 * @param rawInput array of all raw feature data
	 * @return
	 */
	private double[] selectFeatures(double[] rawInput) {
		double[] features = new double[subset.length];
		for (int i = 0; i < subset.length; i++) {
			features[i] = rawInput[subset[i]];
		}
		return features;
	}

	/**
	 * Generate basis vector from a subset of feature values from a training sample.
	 * Includes bias term.
	 * 
	 * @param input list of selected feature values from a sample
	 * @return coefficient vector for theta iteration in regression
	 */
	private Double[] getBasisVector(double[] input) {
		ArrayList<Double> terms = new ArrayList<Double>();
		terms.add(1.0);		// bias term

		// Add all linear terms.
		for (int i = 0; i < input.length; i++) {
			terms.add(input[i]);
		}

		if (USE_QUAD_BASIS) {
			// Add all quadratic terms.
			for (int i = 0; i < input.length; i++) {
				for (int j = i; j < input.length; j++) {
					terms.add(input[i]*input[j]);
				}
			}
		}
		Double[] array = new Double[terms.size()];
		return terms.toArray(array);
	}

	private double getPrediction(Double[] basisVector) {
		double prediction = 0;
		for (int i = 0; i < basisVector.length; i++) {
			prediction += basisVector[i] * theta[i];
		}
		return prediction;
	}

	public void printTheta() {
		System.out.print("Theta = ");
		for (int i = 0; i < theta.length; i++) {
			System.out.print(theta[i] + " ");
		}
		System.out.print("\n");
	}
	
	/**
	 * Calculate the value of the error function, which we are minimizing.
	 * If gradient is null, return the error value at the current theta;
	 * otherwise, return the error at the next step of theta
	 * 
	 * @param gradient g[j] = sum of (diff * basis[j] * weight) for all samples at current theta
	 * @return
	 */
	private double calcError(double[] gradient) {
		double error = 0.0;
		for (int i = 0; i < training_set.size(); i++) {
			double target = training_set.get(i).getTarget();
			double[] features = selectFeatures(training_set.get(i).getInputVector());
			Double[] basis = getBasisVector(features);
			double prediction = 0;
			for (int j = 0; j < basis.length; j++) {
				double currTheta = theta[j];
				if (gradient != null) {
					currTheta -= alpha * gradient[j];
				}
				prediction += basis[j] * currTheta;
			}
			double diff = prediction - target;
			double weight = training_set.get(i).getRelativeWeight();
			error += 0.5 * diff * diff * weight;
		}
		return error;
	}

	private double[] calcGradient() {
		// gradient holds delta for calculating theta changes in a gradient descent iteration.
		double[] gradient = new double[theta.length];

		for (int j = 0; j < theta.length; j++) { // for each theta
			// Add up the contribution of each example to theta_j.
			for (int i = 0; i < training_set.size(); i++) { 
				double[] features = selectFeatures(training_set.get(i).getInputVector());
				double target = training_set.get(i).getTarget();
				Double[] basis = getBasisVector(features);

				// weight reflects the importance of this sample at the current stage in AdaBoost
				double weight = training_set.get(i).getRelativeWeight();
				gradient[j] += (getPrediction(basis) - target) * basis[j] * weight;
			}
			// Regularize all terms but the bias to prevent overfitting with large theta.
			if (j != 0) {
				gradient[j] += LAMBDA * theta[j];
			}
		}

		// normalize gradient vector
		double g = 0;
		for (int i = 0; i < gradient.length; i++) {
			g += gradient[i] * gradient[i];
		}
		g = Math.sqrt(g);

		for (int i = 0; i < gradient.length; i++) {
			gradient[i] /= g;
		}
		return gradient;
	}

	private void batchGradientDescent() {
		// initialize starting value for error function
		double error = calcError(null);
		// initialize gradient at the starting point
		double[] gradient = calcGradient();

		// repeat until 10 iterations without significant change of theta
		int thetaNotChanged = 0;
		while (thetaNotChanged < 10) {
			// calculate the error at the next theta
			double newError = calcError(gradient);
			if (newError < error) {
				// Update all theta values according to gradient
				double thetaChange = 0.0;
				double thetaSize = 0.0;
				for (int i = 0; i < theta.length; i++) {
					double delta = alpha * gradient[i];
					theta[i] -= delta;
					thetaChange += delta*delta;
					thetaSize += theta[i]*theta[i];
				}
				thetaChange = Math.sqrt(thetaChange);
				if (thetaChange < TAU * thetaSize) {
					thetaNotChanged += 1;
				}
				else {
					thetaNotChanged = 0;
				}
				error = newError;
				gradient = calcGradient();
				alpha *= 1.2;
			}
			else {
				alpha *= 0.5;
			}
		}
	}

	public static void main(String args[]) {
		// read sample data
		DataParser.processFile("/Users/michelleshu/Documents/2013/CS74/Workspace/AdaBoost/src/data1.txt");
		ArrayList<TrainingExample> training_set = DataParser.getData();
		// setup initial weight
		int sampleSize = training_set.size();
		for (int i = 1; i < sampleSize; i++) {
			training_set.get(i).setRelativeWeight(1.0/sampleSize);
		}

		// setup transformer
		TrainingSetTransformer transformer = new TrainingSetTransformer(training_set);
//		for (int i = 0; i < transformer.inputOffset.length; i++) {
//			System.out.println("Transformer [" + i + "] = " + transformer.inputOffset[i] + " + " + transformer.inputScale[i] + " v");
//		}
//		System.out.println("Transformer target = " + transformer.targetOffset + " + " + transformer.targetScale + " t");

		// convert samples to use normalized feature values
		transformer.transform(training_set);
/*
		// test target conversion
		double normalized = training_set.get(0).getTarget();
		System.out.println("Convert normalized target " + normalized + "->" + transformer.toRealTarget(normalized));

		// test estimated normalized correlations
		TrainingSetTransformer.Correlation[] corrArray = TrainingSetTransformer.estimateCorrelations(training_set);
		for (TrainingSetTransformer.Correlation corr : corrArray) {
			System.out.println("Correlation [" + corr.getIndex() + "] = " + corr.getOffset() + " + " + corr.getFactor() + " x");
		}
*/
		WeakLearner wl = new WeakLearner(training_set);
		wl.train();

		// test Hypothesis
//		double normalized = wl.getHypothesis(wl.training_set.get(1).getInputVector());
//		System.out.println("Calculated: Convert normalized target " + normalized + "->" + transformer.toRealTarget(normalized));
//		normalized = training_set.get(1).getTarget();
//		System.out.println("Sample: Convert normalized target " + normalized + "->" + transformer.toRealTarget(normalized));
	}
}