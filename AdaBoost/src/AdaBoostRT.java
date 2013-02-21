/** 
 * AdaBoostRT
 * UNDER CONSTRUCTION
 */

import java.util.ArrayList;

public class AdaBoostRT {
	/* Demarcation threshold tau. Roughly reflects the maximum averaged squared
	 * error we are willing to accept to recruit that learner */
	private final int TAU = 100;
	
	private final int MAX_WL = 100; // Number of weak learners to recruit
	
	/* wl_committee is the set of weak learners that have already been drafted 
	 * as part of this AdaBoost predictive model */
	private ArrayList<WeakLearner> wl_committee = new ArrayList<WeakLearner>();
	private int T = 0;	// number of weak learners in committee
	
	/* The training_set contains all training examples, who each hold their
	 * current weight */
	private ArrayList<TrainingExample> training_set;
	private int N;	// Number of training examples
	
	// parameters for AdaBoostRT
//	private static final boolean NORMALIZE_DATA = false;
//	private static final int RT_BOOST_POWER = 2;
//	private static final double RT_MAX_ERROR = 0.2;
//	private static final boolean RT_RELATIVE_ERR = true;
	
	/** Constructor */
	public AdaBoostR(ArrayList<TrainingExample> train_set) {
		// List of training examples
		this.training_set = train_set;

		// We make a new empty array for weak learner committee.
		//this.wl_committee = new ArrayList<WeakLearner>();

		this.N = this.training_set.size();
		System.out.println("Adaboost init");
		System.out.println("N = " + N);
	}
	
	/**
	 * AdaBoostRT, D.L. Shrestha & D.P. Solomatine, November 9, 2005 (Neural Computaion)
	 * Experiments with AdaBoost.RT, an Improved Boosting Scheme for Regression
	 */
	public void trainAdaBoostRT() {
		int iter = 0;
		initializeTrainingDistribution();
		while (iter < MAX_WL) {
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
				LogHelper.log("No Errors.  Quit");
				break;
			}
			double beta = Math.pow(errRate, RT_BOOST_POWER);

			// collect WL and set weight for this weak learner only if errRate < 0.5
//			if (errRate < 0.49) {
				wl_committee.add(wlt);
				wlt.setCombCoef(Math.log(1.0/beta));
				iter++;
//			}
//			else {
//				LogHelper.logln("discard failed weak learner with error rate " + errRate);
//			}

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
			System.out.println("[" + iter + "] WeakLearnerError=" + getError(wlt) + " ErrorRate=" + errRate + " FinalError=" + getError());
		}
	}

	private double[] calcRTErrors(WeakLearner wlt) {
		double[] errors = new double[training_set.size()];
		for (int i = 0; i < training_set.size(); i++) {
			double target = training_set.get(i).getTarget();
			double predicted = wlt.getHypothesis(training_set.get(i).getInputVector());
			errors[i] = Math.abs(predicted-target);
			if (RT_RELATIVE_ERR) {
				errors[i] = errors[i]/Math.abs(target);
			}
		}
		return errors;
	}

	/* Training Phase:
	 * 
	 * 1. Setup
	 * 		Use training set examples (x1, y1)... (xn, yn)
	 * 		Train many weak learners who produce hypothesis f(x) that 
	 * 			approximates y and whose performance can be evaluated by cost 
	 * 			function J
	 * 		Set initial weights of all training inputs: p(xi) = 1/n
	 * 
	 * 2. Iterate (t is round of iteration):
	 * 		Train a new weak learner, wlt.
	 * 		Recruit wlt iff its performance on current training distribution 
	 * 			passes the demarcation threshold. Otherwise, train another wlt.
	 * 		Given the current distribution of training examples, use line search
	 * 			to set 0 <= ct <= 1, the combination coefficient of the new 
	 * 			learner to minimize the cost function Jt.
	 * 		Update the training distribution based on new ct and error.
	 */
	
	public void trainAdaBoostR() {
		// Set a uniform training example distribution to start.
		initializeTrainingDistribution();
		System.out.println("Training distribution set");
		
		// Train MAX_WL weak learners.
		while (T <= MAX_WL) {
			WeakLearner wlt = new WeakLearner(training_set);
			wl_committee.add(wlt);
			wlt.train();
			System.out.println("Weak learner " + T + " trained");
			T++;
			// Set the combination coefficient of the learner.
			minimizeCost(wlt);
			// Update training distribution.
			updateTrainingDistribution(wlt);
		}
		System.out.println("Training phase complete.");
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
			if (RT_RELATIVE_ERR) {
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
			if (RT_RELATIVE_ERR) {
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
		for (int i = 0; i < N; i++) {
			training_set.get(i).setWeight(1.0/N);
			training_set.get(i).setRelativeWeight(1.0/N);
		}
	}
	
	/**
	 * The demarcation threshold is a marker by which to judge whether the
	 * performance of a new weak learner on the current training distribution
	 * warrants its inclusion.
	 * 
	 * A weak learner passes if its epsilon value is less than 1.
	 * epsilon = SUM (over i examples) { p(i) * exp[(f(x(i)) - y(i))^2 - TAU] },
	 * where p represents the relative weight of training example i.
	 */
	private boolean passDemarcationTest(WeakLearner wl) {
		double epsilon = 0;
		for (int i = 0; i < N; i++) {
			TrainingExample example = training_set.get(i);
			double exp_term = Math.exp(squaredError(wl, example) - TAU);
			epsilon += example.getRelativeWeight() * exp_term;
		}
		return (epsilon < 1);
	}
	
	/**
	 * Return the squared error of a weak learner on a training example.
	 * @return squared error value
	 */
	private double squaredError(WeakLearner wl, TrainingExample example) {
		return Math.pow(wl.getHypothesis(example.getInputVector()) - 
				example.getTarget(), 2);
	}
	
	/** 
	 * The learning objective/ cost function is multiplicative in the
	 * wl_committee hypotheses' costs.
	 * The target outputs are not altered after each stage. Rather the objective
	 * for each hypothesis is formed by re-weighting the training distribution.
	 * 
	 * Here, the cost J in a given stage is proportional to the exponentiated 
	 * squared error of the new learner on a training distribution weighted
	 * according to the performance of all previously recruited learners.
	 * 
	 * evaluateCost returns the value of the equation:
	 * Jt = SUM(i = 1 to n) { wt(i) * ct^(-1/2) * exp[ct * (ft(x(i)) - y(i))^2]
	 * where t is the new weak learner and i is a training example.
	 * 
	 * @return cost Jt with addition of wlt
	 */
	private double evaluateCost(WeakLearner wlt, double ct) {
		double cost = 0;
		
		// Update by summing to cost one example at a time
		for (int i = 0; i < N; i++) {
			TrainingExample example = training_set.get(i);
			double exp_term = Math.exp(ct * squaredError(wlt, example));
			double wt = example.getWeight();
			cost += wt * Math.pow(ct, -0.5) * exp_term;
		}	
		return cost;
	}
	
	/**
	 * Minimize the cost Jt (as computed in evaluateCost) with respect to the 
	 * combination coefficient, ct, of the newly recruited weak learner.
	 * 
	 * Minimizing Jt is the same as minimizing ln(Jt).
	 * Taking the derivative of ln(Jt) and setting it equal to zero gives us
	 * that the optimal ct is: n / { 2 * SUM (ft(x(i)) - y(i))^2 }
	 * 
	 * @return optimal ct for wlt
	 */
	private double minimizeCost(WeakLearner wlt) {
		// Get sum of squared error from examples.
		double sumSquaredError = 0;
		for (int i = 0; i < N; i++) {
			TrainingExample example = training_set.get(i);
			sumSquaredError += squaredError(wlt, example);
		}
		
		double ct = N / (2 * sumSquaredError);
		
		wlt.setCombCoef(ct);
		return ct; 
	}
	
	/** Update the weight of a single training example with respect to the
	 * performance of the newly added learner. Then return the updated weight.
	 * Do not call this function directly. It is called from the function
	 * updateTrainingDistribution.
	 * 
	 * The new weight after iteration t is:
	 * w(t + 1) = wt * ct^(-1/2) * exp[ct * (ft(x(i) - y(i))^2]
	 * @return updated weight of example after new learner
	 */
	private double updateSingleExampleWeight(WeakLearner wlt, 
			TrainingExample example) {
		double wt = example.getWeight();	// weight of example before update
		double ct = wlt.getCombCoef();
		double exp_term = Math.exp(ct * squaredError(wlt, example));
		double newWeight = wt * (Math.pow(ct, -0.5)) * exp_term;
		
		example.setWeight(newWeight);
		return newWeight;
	}
	
	/** Update the weights of each training example in the training
	 * distribution with respect to performance of newly added learner.
	 * Then update their relative weights.
	 */
	private void updateTrainingDistribution(WeakLearner wlt) {
		double sumOfWeights = 0;
		
		// Update weights of each individual example, while tallying new weights
		// in sumOfWeights
		for (int i = 0; i < N; i++) {
			TrainingExample example = training_set.get(i);
			sumOfWeights += updateSingleExampleWeight(wlt, example);
		}

		// Now set relative weights of all examples using sumOfWeights.
		for (int i = 0; i < N; i++) {
			TrainingExample example = training_set.get(i);
			example.setRelativeWeight(example.getWeight() / sumOfWeights);
		}
	}

	public static void main(String[] args) {
		DataParser.processFile("C:/work/workspace/AdaBoost/src/data1.txt");
		ArrayList<TrainingExample> training_set = DataParser.getData();
		AdaBoostR ada = new AdaBoostR(training_set);

		// call AdaBoostRT
		ada.trainAdaBoostRT();

//		ada.trainAdaBoostR();
//		System.out.println("Prediction for sample 1: " + ada.getPrediction(training_set.get(0).getInputVector()));
//		System.out.println("True target for sample 1: " + training_set.get(0).getTarget());
//		System.out.println("Prediction for sample 2: " + ada.getPrediction(training_set.get(1).getInputVector()));
//		System.out.println("True target for sample 2: " + training_set.get(1).getTarget());
//		System.out.println("Prediction for sample 3: " + ada.getPrediction(training_set.get(2).getInputVector()));
//		System.out.println("True target for sample 3: " + training_set.get(2).getTarget());
//		System.out.println("Prediction for sample 4: " + ada.getPrediction(training_set.get(3).getInputVector()));
//		System.out.println("True target for sample 4: " + training_set.get(3).getTarget());
//		System.out.println("Prediction for sample 5: " + ada.getPrediction(training_set.get(4).getInputVector()));
//		System.out.println("True target for sample 5: " + training_set.get(4).getTarget());
	}
}