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

public class AdaBoostR {
	/* Demarcation threshold tau. Roughly reflects the maximum averaged squared
	 * error we are willing to accept to recruit that learner */
	private final double TAU = 0.4;

	private final int MAX_WL = 100;             // Number of weak learners to recruit
	private static final int MAX_BAD_WL = 200;  // quit searching if exceeds this number of bad WL

	/* wl_committee is the set of weak learners that have already been drafted 
	 * as part of this AdaBoost predictive model */
	private ArrayList<WeakLearner> wl_committee = new ArrayList<WeakLearner>();

	/* The training_set contains all training examples, who each hold their
	 * current weight */
	private ArrayList<TrainingExample> training_set;
	private int N;	// Number of training examples

	// parameters for data transformer
	private TrainingSetTransformer transformer = null;
	private static final boolean NORMALIZE_DATA = true;

	/** Constructor */
	public AdaBoostR(ArrayList<TrainingExample> train_set) {
		// List of training examples
		this.training_set = train_set;

		// We make a new empty array for weak learner committee.
		//this.wl_committee = new ArrayList<WeakLearner>();

		this.N = this.training_set.size();
		System.out.println("Adaboost init");
		System.out.println("N = " + N);

		if (NORMALIZE_DATA) {
			// setup transformer
			transformer = new TrainingSetTransformer(training_set);
			for (int i = 0; i < transformer.inputOffset.length; i++) {
				LogHelper.logln("DEBUG", "Transformer [" + i + "] = " + transformer.inputOffset[i] + " + " + transformer.inputScale[i] + " v");
			}
			LogHelper.logln("DEBUG", "Transformer target = " + transformer.targetOffset + " + " + transformer.targetScale + " t");

			// convert samples to use normalized feature values
			transformer.transform(training_set);
		}
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
		int iter = 1;
		while (iter <= MAX_WL) {
			WeakLearner wlt;
			double epsilon = 0.0;
			int wl_tried = 0;

			// Accept the weak learner if it passes the demarcation test.
			// Otherwise, train a new weak learner.
			do {
				wlt = new WeakLearner(training_set);
				wlt.train();
				epsilon = demarcationValue(wlt);
				wl_tried++;
				System.out.println("Tried " + wl_tried + " WL.  epsilon = " + epsilon);
			} while (epsilon >= 1.0 && wl_tried <= MAX_BAD_WL);

			if (wl_tried > MAX_BAD_WL) {
				break;  // quit if cannot find more qualified WL
			}

			wl_committee.add(wlt);

			// Set the combination coefficient of the learner.
			minimizeCost(wlt);
			// Update training distribution.
			updateTrainingDistribution(wlt);

			// Print result of adding wlt to console.
			System.out.println("[" + iter + "] WL Error = " + getWLError(wlt) + 
					" WL Weight = " + wlt.getCombCoef() +
					" AdaBoostR Error = " + getCommitteeError());
			iter++;
		}
		System.out.println("Training phase complete.");
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
	 * Measure the error of a single weak learner in terms of average
	 * relative error in predictions (error / true value) over all training 
	 * examples
	 */
	public double getWLError(WeakLearner wl) {
		double error_sum = 0.0;
		for (TrainingExample example : training_set) {
			double target = example.getTarget();
			double prediction = wl.getHypothesis(example.getInputVector());
			if (transformer != null) {
				// convert to real value from the normalized data
				target = transformer.toRealTarget(target);
				prediction = transformer.toRealTarget(prediction);
			}
			error_sum += Math.abs((prediction - target) / target);
		}
		return error_sum / training_set.size();
	}
	
	/**
	 * Measure the error of the prediction due to entire committee of weak 
	 * learners, just as we did for getWLError above.
	 */
	public double getCommitteeError() {
		double error_sum = 0.0;
		for (TrainingExample example : training_set) {
			double target = example.getTarget();
			double prediction = getPrediction(example.getInputVector());
			if (transformer != null) {
				// convert to real value from the normalized data
				target = transformer.toRealTarget(target);
				prediction = transformer.toRealTarget(prediction);
			}
			error_sum += Math.abs((prediction - target) / target);
		}
		return error_sum / training_set.size();
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
	 * The demarcation threshold is a marker by which to judge whether the
	 * performance of a new weak learner on the current training distribution
	 * warrants its inclusion.
	 * 
	 * A weak learner passes if its epsilon value is less than 1.
	 * epsilon = SUM (over i examples) { p(i) * exp[(f(x(i)) - y(i))^2 - TAU] },
	 * where p represents the relative weight of training example i.
	 */
	private double demarcationValue(WeakLearner wl) {
		double epsilon = 0;
		for (int i = 0; i < N; i++) {
			TrainingExample example = training_set.get(i);
			double exp_term = Math.exp(squaredError(wl, example) - TAU);
			epsilon += example.getRelativeWeight() * exp_term;
		}
		return epsilon;
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
	 * Use a type of line search called golden section search to find the ct 
	 * which leads to the minimum cost Jt.
	 * 
	 * Return the value of the cost at the optimal ct
	 */
	private double minimizeCost(WeakLearner wlt) {
		double a = 0.01;
		double c = 1.0;
		double phi = (1.0 + Math.sqrt(5)) / 2.0;
		double b = (2.0 - phi) * (c + phi * a);
		double cost_b = evaluateCost(wlt, b);
		double ct =  goldenSectionSearch(a, b, c, cost_b, wlt);
		
		// Set the combination coefficient of wlt to this optimal value.
		wlt.setCombCoef(ct);
		return ct;
	}
	
	/**
	 * Golden section search to find ct for minimizing the cost function.
	 * http://en.wikipedia.org/wiki/Golden_section_search
	 * 
	 * @param a  lower bound of ct for the search
	 * @param b upper golden mid-point of ct for the search
	 * @param c upper bound of ct for the search
	 * @param cost_b cached cost value at the mid-point
	 * @param wlt WeakLearner required to calculate the cost
	 * @return the ct value where cost is minimized between a and c
	 */
	private double goldenSectionSearch(double a, double b, double c, double cost_b, WeakLearner wlt) {
		double phi = (1.0 + Math.sqrt(5)) / 2.0;
		double resphi = 2.0 - phi;
		double tau = 0.01;
		double x;
		if (c - b > b - a) {
			x = b + resphi * (c - b);
		} else {
			x = b - resphi * (b - a);
		}
		if (Math.abs(c - a) < tau * (Math.abs(b) + Math.abs(x))) {
			return 0.5 * (c + a);
		}
		double cost_x = evaluateCost(wlt, x);
		if (cost_x < cost_b) {
			if (c - b > b - a) {
				return goldenSectionSearch(b, x, c, cost_x, wlt);
			} else {
				return goldenSectionSearch(a, x, b, cost_x, wlt);
			}
		} else {
			if (c - b > b - a) {
				return goldenSectionSearch(a, b, x, cost_b, wlt);
			} else {
				return goldenSectionSearch(x, b, x, cost_b, wlt);
			}
		}
	}
	
	/**
	 * Minimize the cost Jt analytically with respect to the 
	 * combination coefficient, ct, of the newly recruited weak learner.
	 * The paper said that this method should not be used, that line search 
	 * should work better.
	 * 
	 * Minimizing Jt is the same as minimizing ln(Jt).
	 * Taking the derivative of ln(Jt) and setting it equal to zero gives us
	 * that the optimal ct is: n / { 2 * SUM (ft(x(i)) - y(i))^2 }
	 * 
	 * @return optimal ct for wlt
	 */
	private double minimizeCostAnalytical(WeakLearner wlt) {
		// Get sum of squared error from examples.
		double sumSquaredError = 0;
		for (int i = 0; i < N; i++) {
			TrainingExample example = training_set.get(i);
			sumSquaredError += squaredError(wlt, example);
		}

		double ct = N / (2 * sumSquaredError);

		// ct must be less than or equal to 1. Correct if it is too high.
		if (ct > 1.0) {
			wlt.setCombCoef(1.0);
			return 1.0;
		} else { 
			wlt.setCombCoef(ct);
			return ct; }
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
		LogHelper.initialize("AdaBoost", false);

		// config parser to parse new NBA stats
		int[] features = new int[36];
		for (int i = 0; i < 36; i++) {
			features[i] = i;
		}
		DataParser.conigParser(",", 37, features);

		DataParser.processFile("C:/work/workspace/NBAStatFetch/data/SEASON-2007.csv");
//		DataParser.processFile("/Users/michelleshu/Documents/2013/CS74/Workspace/AdaBoost/src/test.txt");
		ArrayList<TrainingExample> training_set = DataParser.getData();

		AdaBoostR ada = new AdaBoostR(training_set);

		// Train AdaBoostR
		ada.trainAdaBoostR();

	}
}