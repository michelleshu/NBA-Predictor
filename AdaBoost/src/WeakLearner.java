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

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class WeakLearner {
	private static int SUBSET_SIZE = 8;         // Size of random subset of features
	private final static double LAMBDA = 0.0;	// Regularization term for LSR
	private final static double ALPHA = 0.1;	// Initial Step size for LSR gradient descent
	private final static double TAU = 0.01;		// Stopping criterion for gradient
										        // descent convergence

	private static boolean USE_QUAD_BASIS = true;  // include quadratic terms if it is true

	public static void configWeakLearner(int subsetSize, boolean useQuad) {
		SUBSET_SIZE = subsetSize;
		USE_QUAD_BASIS = useQuad;
	}

	private double alpha;    // step size for gradient descent, it changes during iterations
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

	public ArrayList<TrainingExample> getTrainingSet() {
		return training_set;
	}

	/**
	 * Select feature indices for regression.
	 * If USE_TOP_FEATURES=false, use random selection.
	 * If USE_TOP_FEATURES=true, select feature by correlation coefficient.
	 * 
	 * @param rawFeatureCount total number of raw features
	 */
	private void selectSubset(int rawFeatureCount) {
		if (SUBSET_SIZE >= rawFeatureCount) {
			// select all features
			subset = new Integer[rawFeatureCount];
			for (int i= 0; i < rawFeatureCount; i++) {
				subset[i] = i;
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

		for (int i = 0; i < basis.length; i++) {
			theta[i] = 0;
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
		StringBuffer buff = new StringBuffer("Theta = ");
		for (int i = 0; i < theta.length; i++) {
			buff.append(theta[i] + " ");
		}
		LogHelper.logln("DEBUG", buff.toString());
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
		for (TrainingExample sample : training_set) {
			double target = sample.getTarget();
			double[] features = selectFeatures(sample.getInputVector());
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
			double weight = sample.getRelativeWeight();
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
		int iter = 0;
		while (thetaNotChanged < 10) {
			iter++;
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
				saveIteration();
				thetaChange = Math.sqrt(thetaChange);
				thetaSize = Math.sqrt(thetaSize);
				if (thetaChange < TAU * thetaSize) {
					thetaNotChanged += 1;
				}
				else {
					thetaNotChanged = 0;
				}
				error = newError;
				gradient = calcGradient();
				alpha *= 1.2;
				printTheta();
				LogHelper.logln("DEBUG", "Iteration=" + iter + " Alpha=" + alpha + " ThetaChange=" + thetaChange + " ThetaSize=" + thetaSize + " Error=" + error);
			}
			else {
				alpha *= 0.5;
			}
		}
	}

	// helper for charting iteration results
	ArrayList<double[]> thetaList = new ArrayList<double[]>();

	// save theta of the current iteration, so it can be used for print out intermediate results
	private void saveIteration() {
		double[] newTheta = new double[theta.length];
		for (int i = 0; i < theta.length; i++) {
			newTheta[i] = theta[i];
		}
		thetaList.add(newTheta);
	}

	/**
	 * write iteration results to a file
	 * @param filename
	 */
	public void writeIterationResults(String filename, boolean append) {
		FileWriter out = null;
		try {
			out = new FileWriter(filename, append);
			int majorBasis = getMajorBasis();
			if (majorBasis == 0)
				return;
			int[] featureIndices = getFeatureIndices(majorBasis);

			// sort samples according to target value
			Collections.sort(training_set, new Comparator<TrainingExample>() {
				public int compare(TrainingExample t1, TrainingExample t2) {
					if (t1.getTarget() < t2.getTarget()) {
						return -1;
					} else if (t1.getTarget() > t2.getTarget()) {
						return 1;
					} else {
						return 0;
					}
				}
			});

			// write output file
			for (TrainingExample sample : training_set) {
				// calculate value of basis for the sample
				double[] input = sample.getInputVector();
				double[] features = selectFeatures(input);
				Double[] basis = getBasisVector(features);
				double majorValue = basis[majorBasis];
				double target = sample.getTarget();

				// comma-delimited prediction errors for each iteration
				StringBuffer buff = new StringBuffer();
				buff.append(featureIndices[0]).append(',');
				buff.append(featureIndices[1]).append(',');
				buff.append(majorValue).append(',');
				buff.append(target).append(',');
				double prediction = 0;
				for (int j = 0; j < thetaList.size()-10; j++) {
					double[] ths = thetaList.get(j);
					prediction = 0;
					for (int i = 0; i < ths.length; i++) {
						prediction += ths[i] * basis[i];
					}
					buff.append(prediction - target).append(',');
				}
				buff.append(prediction);
				out.write(buff.toString());
				out.append('\n');
				out.flush();
			}
		} catch (Exception e) {
			LogHelper.logln("Failed to write iteration result " + e.getMessage());
		} finally {
			try {
				out.close();
			} catch (Exception ex) {}
		}
	}

	// find the index of basis that contribute the most to the final result
	private int getMajorBasis() {
		// calculate values of individual basis features
		double[] contributors = null;
		for (TrainingExample sample : training_set) {
			double[] features = selectFeatures(sample.getInputVector());
			Double[] basis = getBasisVector(features);
			if (null == contributors) {
				contributors = new double[basis.length];
			}
			for (int i = 0; i < basis.length; i++) {
				contributors[i] += basis[i] * theta[i];
			}
		}

		// find the basis that contributed most to the target
		double maxValue = 0;
		int maxIndex = -1;
		for (int i = 0; i < contributors.length; i++) {
			if (contributors[i] > maxValue) {
				maxValue = contributors[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	// convert a index of a theta basis into feature indices (2 indices only if quad terms are included)
	private int[] getFeatureIndices(int basisIndex) {
		int[] indices = new int[]{-1, -1};
		if (basisIndex == 0) {
			// this is the bias term
			return indices;
		}
		else if (basisIndex < subset.length) {
			// this is a linear term
			indices[0] = subset[basisIndex-1];
		}
		else {
			// this is a quad term
			int idx = subset.length;
			for (int i = 0; i < subset.length; i++) {
				for (int j = i; j < subset.length; j++) {
					idx++;
					if (idx == basisIndex) {
						indices[0] = subset[i];
						indices[1] = subset[j];
					}
				}
			}
		}
		return indices;
	}

	public static void main(String args[]) {
		LogHelper.initialize("", true);

		// config parser to parse all input columns in the new NBA stats
		int[] features = new int[36];
		for (int i = 0; i < 36; i++) {
			features[i] = i;
		}
		DataParser.conigParser(",", 37, features);

		// read sample data
		DataParser.processFile("C:/work/workspace/NBAStatFetch/data/SEASON-2007.csv");
		ArrayList<TrainingExample> training_set = DataParser.getData();

		// config WeakLearner to use specified number of features and quad terms
		configWeakLearner(9, true);

		// setup initial weight
		int sampleSize = training_set.size();
		for (TrainingExample sample : training_set) {
			sample.setRelativeWeight(1.0/sampleSize);
		}

/*		// setup transformer
		TrainingSetTransformer transformer = new TrainingSetTransformer(training_set);
		for (int i = 0; i < transformer.inputOffset.length; i++) {
			LogHelper.logln("DEBUG", "Transformer [" + i + "] = " + transformer.inputOffset[i] + " + " + transformer.inputScale[i] + " v");
		}
		LogHelper.logln("DEBUG", "Transformer target = " + transformer.targetOffset + " + " + transformer.targetScale + " t");

		// convert samples to use normalized feature values
		transformer.transform(training_set);
*/
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

		// write iteration results for chart
		wl.writeIterationResults("C:/temp/wldata.csv", false);

		// test Hypothesis
		double predicted = wl.getHypothesis(wl.training_set.get(1).getInputVector());
		double actual = training_set.get(1).getTarget();
		LogHelper.logln("Predicted=" + predicted + " Actual=" + actual);
	}
}