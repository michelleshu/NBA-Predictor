/** 
 * AdaBoostRT.java
 * An implementation of AdaBoostRT for regression. Based on the algorithm in 
 * Shrestha, D.L. and Solomatine, D.P. (2006). "Experiments with AdaBoost.RT,
 * an Improved Boosting Scheme for Regression". Neural Computation. 18: 1678-1710.
 * 
 * @author Michelle Shu
 * Last Updated February 23, 2013
 */

import java.util.ArrayList;

public class AdaBoostRT {
	private static final int MAX_WL = 100;      // Number of weak learners to recruit
	private static final int MAX_BAD_WL = 200;  // Quit upon exceeding this number
												// of bad learners
	
	/* wl_committee is the set of weak learners that have already been drafted 
	 * as part of this AdaBoostRT model */
	private ArrayList<WeakLearner> wl_committee;

	/* The training_set contains all training examples, who each hold their
	 * current weight */
	private ArrayList<TrainingExample> training_set;
	
	/* The test_set contains all test examples */
	public ArrayList<TrainingExample> test_set;

	/* If RELATIVE_ERR is true, measure error by relative value abs(error / target)
	 * If RELATIVE_ERR is false, measure error by absolute value abs(error) */
	private static final boolean RELATIVE_ERR = true;
	
	/* MAX_WL_ERROR defines our standard for accepting the weak learner into
	 * our committee. If the weak learner cannot meet this minimum standard,
	 * we discard it.
	 */
	private static final double MAX_WL_ERR = 0.495;

	private TrainingSetTransformer transformer = null;
	private static final boolean NORMALIZE_DATA = false;
	
	/* RT_BOOST_POWER is a power coefficient (e.g. linear, square or cubic)
	 * that determines boosting behavior.
	 */
	private static final int RT_BOOST_POWER = 2;

	/* A threshold for demarcation of correct and incorrect predictions. 
	 * In algorithm, this is denoted mathematically with PHI. The threshold 
	 * value must be strictly between 0 and 1 */
	private static final double RT_MAX_ERROR = 0.20; 
	
	/* 0 is difference, 1 is cumulative */
	private static final int BET_TYPE = 1;

	/** Constructor */
	public AdaBoostRT(ArrayList<TrainingExample> train_set) {
		// List of training examples
		this.training_set = train_set;

		// Recruited weak learners
		this.wl_committee = new ArrayList<WeakLearner>();

		System.out.println("AdaboostRT initialized.");
	}

	/*
	 * Training Phase:
	 * 1. Inputs: training set, weak learner algorithm and RT_MAX_ERROR 
	 * 		(a threshold for demarcation of correct and incorrect predictions)
	 * 2. Initialize: Even distribution of training examples, error rate = 0
	 * 3. Iterate:
	 * 		a. Train a weak learner with current training set distribution.
	 * 		b. Calculate absolute relative error (ARE) for each training example
	 * 		c. Set the error rate to the weighted sum of ARE
	 * 		d. Raise the error rate the the power specified by RT_BOOST_POWER to
	 * 			compute the beta term.
	 * 		e. Update the distribution by setting the multiplying the example's
	 * 			weight by beta if its ARE is <= RT_MAX_ERROR and 1 otherwise.
	 * 			Then set relative weights of examples so that they make a 
	 * 			proper probability distribution (i.e. add to 1)
	 */
	
	public void trainAdaBoostRT() {
		// Normalize the data with Transformer if that step is specified.
		if (NORMALIZE_DATA) {
			transformer = new TrainingSetTransformer(training_set);
			transformer.transform(training_set);
		}

		int wl_recruited = 0;	// Number of WL accepted into committee
		int wl_discarded = 0;	// Number of WL rejected
		initializeTrainingDistribution();
		
		// Keep going as long as we do not have too many weak learners 
		// recruited or too many weak learners rejected
		while (wl_recruited < MAX_WL && wl_discarded < MAX_BAD_WL) {
			WeakLearner wlt = new WeakLearner(training_set);
			wlt.train();

			// calculate error rate
			double[] errors = calcRTErrors(wlt);
			double errRate = 0.0;
			for (int i = 0; i < training_set.size(); i++) {
				if (errors[i] > RT_MAX_ERROR) {
					errRate += training_set.get(i).getRelativeWeight();
				}
			}
			if (0 == errRate) {
				System.out.println("No Errors.  Quit");
				break;
			}
			double beta = Math.pow(errRate, RT_BOOST_POWER);

			// collect WL and set weight for this weak learner only if errRate < 0.5
			if (errRate < MAX_WL_ERR) {
				wl_committee.add(wlt);
				wlt.setCombCoef(Math.log(1.0/beta));
				wl_recruited++;
				wl_discarded = 0;
			}
			else {
				wl_discarded++;
			}

			// set weight of samples for the next iteration
			double total_weight = 0;
			for (int i = 0; i < training_set.size(); i++) {
				TrainingExample sample = training_set.get(i);
				double weight = sample.getRelativeWeight();
				if (errors[i] <= RT_MAX_ERROR) {
					weight *= beta;
				}
				total_weight += weight;
				sample.setRelativeWeight(weight);  // not normalized yet
			}
			for (TrainingExample sample : training_set) {
				double weight = sample.getRelativeWeight() / total_weight;
				sample.setRelativeWeight(weight);  // normalized weight
			}

			// print out iteration result
			System.out.println("[" + wl_recruited + "] WeakLearnerError=" + getError(wlt) + " ErrorRate=" + errRate + " FinalError=" + getError());
		}
	}
	
	/** Initialize training set distribution */
	private void initializeTrainingDistribution() {
		// Initialize all weights and relative weights of examples to 1/N.
		int size = training_set.size();
		for (TrainingExample sample : training_set) {
			sample.setWeight(1.0/size);
			sample.setRelativeWeight(1.0/size);
		}
	}

	private double[] calcRTErrors(WeakLearner wlt) {
		double[] errors = new double[training_set.size()];
		for (int i = 0; i < training_set.size(); i++) {
			double target = training_set.get(i).getTarget();
			double predicted = wlt.getHypothesis(training_set.get(i).getInputVector());
			errors[i] = Math.abs(predicted-target);
			if (RELATIVE_ERR) {
				errors[i] = errors[i]/Math.abs(target);
			}
		}
		return errors;
	}


	/* Prediction Phase:
	 * ONLY CALL THIS FUNCTION AFTER TRAINING IS COMPLETE.
	 * 
	 * After the AdaBoostRT model is trained, it should be able to utilize the
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
	 * Return relative error of committee of weak learners on training data
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
	 * Return average absolute error of committee of weak learners on test data
	 */
	public double getAvAbsError(ArrayList<TrainingExample> examples) {
		double error = 0.0;
		for (TrainingExample example : examples) {
			double target = example.getTarget();
			double prediction = getPrediction(example.getInputVector());
			error += Math.abs(prediction-target);
		}
		return error/examples.size();
	}
	
	/**
	 * Return average squared error of committee of weak learners on test data
	 */
	public double getAvSquaredError(ArrayList<TrainingExample> examples) {
		double squared_error = 0.0;
		for (TrainingExample example : examples) {
			double target = example.getTarget();
			double prediction = getPrediction(example.getInputVector());
			squared_error += Math.pow(prediction - target, 2);
		}
		return squared_error/examples.size();
	}
	
	/**
	 * Return root mean squared error of committee of weak learners on test data
	 */
	public double getRMSError(ArrayList<TrainingExample> examples) {
		double squared_error = 0.0;
		for (TrainingExample example : examples) {
			double target = example.getTarget();
			double prediction = getPrediction(example.getInputVector());
			squared_error += Math.pow(prediction - target, 2);
		}
		return Math.sqrt(squared_error/examples.size());
	}

	/**
	 * Return relative error of a weak learner
	 */
	public double getError(WeakLearner wl) {
		double error = 0.0;
		for (TrainingExample example : training_set) {
			double target = example.getTarget();
			double prediction = wl.getHypothesis(example.getInputVector());
			if (RELATIVE_ERR) {
				error += Math.abs((prediction-target)/target);
			} else {
				error += Math.abs(prediction-target);
			}
		}
		return error/training_set.size();
	}
	
	/**
	 * Return the bet accuracy of the AdaBoostRT model as percentage
	 */
	public double getBetAccuracy() {
		int num_correct = 0;
		for (TrainingExample example : test_set) {
			double target = example.getTarget();
			double prediction = this.getPrediction(example.getInputVector());
			double cutoff = example.getBetCutoff(BET_TYPE);
			// If the target and prediction are on the same side of the cutoff,
			// the bet prediction is correct.
			if ((target <= cutoff && prediction <= cutoff) ||
					(target >= cutoff && prediction >= cutoff)) {
				num_correct++;
			}
		}
		return ((double) num_correct) / test_set.size();
	}
	


	public static void main(String[] args) {

		// Get training set
		DataParser.processFile("data/2010-SEASON.csv", false);
		ArrayList<TrainingExample> training_set = DataParser.getData();
		System.out.println("Training Set N = " + training_set.size());
		System.out.println(training_set.get(100).getTarget());
		AdaBoostRT ada = new AdaBoostRT(training_set);
		
		// Train AdaBoostRT on training set.
		ada.trainAdaBoostRT();
		System.out.println();
		
		// Test AdaBoostRT model on training set
		System.out.println("Training Set N = " + ada.training_set.size());
		System.out.println("Training Set Average Absolute Error = " + 
				ada.getAvAbsError(ada.training_set));
		System.out.println("Training Set Average Squared Error = " + 
				ada.getAvSquaredError(ada.training_set));
		System.out.println("Training Set RMS Error = " + 
				ada.getRMSError(ada.training_set));
		System.out.println();
		
		// Get test set and test AdaBoostRT model on test data
		DataParser.clear();
		DataParser.processFile("data/2012-SEASON-TEST.csv", true);
		ada.test_set = DataParser.getData();
		System.out.println("Test Set N = " + ada.test_set.size());
		System.out.println("Test Set Average Absolute Error = " + 
				ada.getAvAbsError(ada.test_set));
		System.out.println("Test Set Average Squared Error = " + 
				ada.getAvSquaredError(ada.test_set));
		System.out.println("Test Set RMS Error = " + 
				ada.getRMSError(ada.test_set));
		System.out.println("Bet Accuracy: " + ada.getBetAccuracy());
	}
}