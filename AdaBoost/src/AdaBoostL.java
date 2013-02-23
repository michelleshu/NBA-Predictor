/** 
 * AdaBoostR.java
 * An implementation of AdaBoost for regression. Based on the algorithm in 
 * Zemel, R. S., & Pitassi, T. (2001). A gradient-based boosting algorithm for 
 * regression problems. Advances in neural information processing systems, 696-702.
 * 
 * @author Michelle Shu
 * Last Updated February 1, 2013
 */

import java.util.ArrayList;

public class AdaBoostL {
	private final int MAX_WL = 100; // Number of weak learners to recruit
	
	/* wl_committee is the set of weak learners that have already been drafted 
	 * as part of this AdaBoost predictive model */
	private ArrayList<WeakLearner> wl_committee = new ArrayList<WeakLearner>();
	
	/* The training_set contains all training examples, who each hold their
	 * current weight */
	private ArrayList<TrainingExample> training_set;

	// calculate relative errors, instead of absolute errors abs((f-y)/y)
	private static final boolean RELATIVE_ERR = true;
	
	// parameters for AdaBoostL
	private static final double L_MAX_ERROR = 25;
	private static final double L_MIN_ERROR = 5;
	private static final int L_MAX_BAD_WL = 200;  // quit searching if exceeds this number of bad WL
	private static final double L_MAX_WL_ERR = 0.495;
	
	/** Constructor */
	public AdaBoostL(ArrayList<TrainingExample> train_set) {
		// List of training examples
		this.training_set = train_set;

		// We make a new empty array for weak learner committee.
		//this.wl_committee = new ArrayList<WeakLearner>();

		System.out.println("Adaboost init");
	}

	/**
	 * My own boost with linear function
	 */
	public void trainAdaBoostL() {
		int wl_collected = 0;
		int wl_discarded = 0;
		initializeTrainingDistribution();
		while (wl_collected < MAX_WL && wl_discarded < L_MAX_BAD_WL) {
			WeakLearner wlt = new WeakLearner(training_set);
			wlt.train();

			// calculate error rate
			double[] errors = calcLErrors(wlt);
			double errRate = 0.0;
			for (int i = 0; i < training_set.size(); i++) {
				errRate += training_set.get(i).getRelativeWeight() * errors[i];
			}
			if (0 == errRate) {
				LogHelper.log("No Errors.  Quit");
				break;
			}
			double beta = (1.0 - errRate) / errRate;

			// collect WL and set weight for this weak learner only if errRate < 0.5
			if (errRate < L_MAX_WL_ERR) {
				wl_committee.add(wlt);
				wlt.setCombCoef(Math.log(beta));
				wl_collected++;
				wl_discarded = 0;
			}
			else {
				wl_discarded++;
				LogHelper.logln("discard " + wl_discarded + " failed weak learners with error rate " + errRate);
				continue;
			}

			// set weight of samples for the next iteration
			double total_weight = 0;
			for (int i = 0; i < training_set.size(); i++) {
				TrainingExample sample = training_set.get(i);
				double weight = sample.getRelativeWeight();
				weight *= Math.pow(beta, errors[i] - 0.5);
				total_weight += weight;
				sample.setRelativeWeight(weight);  // not normalized yet
			}
			for (TrainingExample sample : training_set) {
				double weight = sample.getRelativeWeight() / total_weight;
				sample.setRelativeWeight(weight);  // normalized weight
			}

			// print out iteration result
			System.out.println("[" + wl_collected + "] WeakLearnerError=" + getError(wlt) + " ErrorRate=" + errRate + " FinalError=" + getError());
		}
	}

	/**
	 * Use linear function between L_MIN_ERROR and L_MAX_ERROR to determine the error penalty.
	 * This is a straight extension of AdaBoost step function I(f != y)
	 * 
	 * @param wlt
	 * @return
	 */
	private double[] calcLErrors(WeakLearner wlt) {
		double[] errors = new double[training_set.size()];
		for (int i = 0; i < training_set.size(); i++) {
			double target = training_set.get(i).getTarget();
			double predicted = wlt.getHypothesis(training_set.get(i).getInputVector());
			double error = Math.abs(predicted-target);
			if (error <= L_MIN_ERROR) {
				errors[i] = 0.0;
			} else if (error >= L_MAX_ERROR) {
				errors[i] = 1.0;
			} else {
				errors[i] = (error - L_MIN_ERROR) / (L_MAX_ERROR - L_MIN_ERROR);
			}
		}
		return errors;
	}

	/* Prediction Phase:
	 * ONLY CALL THIS FUNCTION AFTER TRAINING IS COMPLETE.
	 * 
	 * After the AdaBoostR model is trained, it should be able to utilize the
	 * collective knowledge of the weak learner committee to make predictions
	 * about the target values of new input vectors.
	 * 
	 * The predicted output is given by the weighted sum of individual 
	 * learners' predictions. The weight of each learner is proportional to its
	 * combination coefficient.
	 * 
	 * predicted y = SUM { ct * ft(x) } / SUM { ct }
	 */
	public double getPrediction(double [] input) {
		if (wl_committee.size() == 0) return 0;
		double weighted_prediction = 0;
		double sumOfWeights = 0;
		for (WeakLearner wlt : wl_committee) {
			weighted_prediction += (wlt.getCombCoef() * 
									wlt.getHypothesis(input));
			sumOfWeights += wlt.getCombCoef();
		}
		return weighted_prediction / sumOfWeights;
	}

	/**
	 * Return relative error of committee of weak learners
	 * @return
	 */
	public double getError() {
		double error = 0.0;
		for (TrainingExample sample : training_set) {
			double target = sample.getTarget();
			double prediction = getPrediction(sample.getInputVector());
			if (RELATIVE_ERR) {
				error += Math.abs((prediction-target)/target);
			} else {
				error += Math.abs(prediction-target);
			}
		}
		return error/training_set.size();
	}

	/**
	 * Return relative error of a weak learner
	 * 
	 * @param wl the weak learner
	 * @return
	 */
	public double getError(WeakLearner wl) {
		double error = 0.0;
		for (TrainingExample sample : training_set) {
			double target = sample.getTarget();
			double prediction = wl.getHypothesis(sample.getInputVector());
			if (RELATIVE_ERR) {
				error += Math.abs((prediction-target)/target);
			} else {
				error += Math.abs(prediction-target);
			}
		}
		return error/training_set.size();
	}

	/** Initialize training set distribution */
	private void initializeTrainingDistribution() {
		// Initialize all weights and relative weights of examples to 1/N.
		int size = training_set.size();
		for (int i = 0; i < size; i++) {
			training_set.get(i).setWeight(1.0/size);
			training_set.get(i).setRelativeWeight(1.0/size);
		}
	}

	public static void main(String[] args) {
		LogHelper.initialize("AdaBoost", false);

		// config parser to parse new NBA stats
		int[] features = new int[36];
		for (int i = 0; i < 36; i++) {
			features[i] = i;
		}
		DataParser.conigParser(",", 37, features);

		DataParser.processFile("C:/work/workspace/NBAStatFetch/data/SEASON-2007.csv");
		ArrayList<TrainingExample> training_set = DataParser.getData();
		AdaBoostL ada = new AdaBoostL(training_set);

		// call AdaBoostL with linear error function
		ada.trainAdaBoostL();
	}
}