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
	
	/* wl_candidates is a pool of weak learners that we can recruit from */
	private ArrayList<WeakLearner> wl_candidates;
	
	/* wl_committee is the set of weak learners that have already been drafted 
	 * as part of this AdaBoost predictive model */
	private ArrayList<WeakLearner> wl_committee;
	
	/* Demarcation threshold tau. Roughly reflects the maximum averaged squared
	 * error we are willing to accept to recruit that learner */
	private final int TAU = 100;
	
	/* The training_set contains all training examples, who each hold their
	 * current weight */
	private ArrayList<TrainingExample> training_set;
	private int N;	// Number of training examples
	
	/** Constructor - need to fill this in */
	public AdaBoostR(ArrayList<WeakLearner> wl_set, 
			ArrayList<TrainingExample> train_set) {
		// Two things needed: list of weak learner candidates and list of
		// training examples.
		this.wl_candidates = wl_set;
		this.training_set = train_set;
		
		// We make a new empty array for weak learner committee.
		this.wl_committee = new ArrayList<WeakLearner>();
		
		this.N = this.training_set.size();
	}
	
	/** Training Phase:
	 * 
	 * 1. Setup
	 * 		Use training set examples (x1, y1)... (xn, yn)
	 * 		Train many weak learners who produce hypothesis f(x) that 
	 * 			approximates y and whose performance can be evaluated by cost 
	 * 			function J
	 * 		Set initial weights of all training inputs: p(xi) = 1/n
	 * 
	 * 2. Iterate (t is round of iteration):
	 * 		Find a new weak learner, wlt.
	 * 		Recruit wlt iff its performance on current training distribution 
	 * 			passes the demarcation threshold. Otherwise, pick another wlt.
	 * 		Given the current distribution of training examples, use line search
	 * 			to set 0 <= ct <= 1, the combination coefficient of the new 
	 * 			learner to minimize the cost function Jt.
	 * 		Update the training distribution based on new ct and error.
	 */
	
	public void trainAdaBoostR() {
		// Set even training distribution to start.
		initializeTrainingDistribution();
		
		// Choose the first weak learner, if possible.
		WeakLearner wlt = recruitLearner();
		if (wlt == null) {
			System.err.println("Error: No suitable learners available." + 
					"Please adjust tau or add new learners.");
		} else {
			// As long as we have a suitable weak learner to add, continue
			// training the model by adding the learner.
			while (wlt != null) {
				// Set the combination coefficient of the learner.
				minimizeCost(wlt);
				// Update training distribution.
				updateTrainingDistribution(wlt);
				// Recruit a new learner.
				wlt = recruitLearner();
			}
			System.out.println("Training phase complete.");
		}
	}
	
	/** Prediction Phase:
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
		int T = wl_committee.size(); // number of weak learners in committee
		double weighted_prediction = 0;
		double sumOfWeights = 0;
		for (int i = 0; i < T; i++) {
			WeakLearner wlt = wl_committee.get(i);
			weighted_prediction += (wlt.combCoefficient * 
									wlt.getHypothesis(input));
			sumOfWeights += wlt.combCoefficient;
		}
		return weighted_prediction / sumOfWeights;
	}
	
	
	/** Initialize training set distribution */
	private void initializeTrainingDistribution() {
		// Initialize all weights and relative weights of examples to 1/N.
		for (int i = 0; i < N; i++) {
			training_set.get(i).setWeight(1/N);
			training_set.get(i).setRelativeWeight(1/N);
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
	
	/** Recruit learner from candidates and return it. If there are no
	 * satisfactory candidates left in the pool, return null. */
	private WeakLearner recruitLearner() {
		boolean wlFound = false;
		int index = 0;
		while ((! wlFound) && (index < wl_candidates.size())) {
			WeakLearner wl = wl_candidates.get(index);
			// Accept the weak learner if it passes the demarcation test
			if (passDemarcationTest(wl)) {
				wlFound = true;
				wl_candidates.remove(index);
				wl_committee.add(wl);
				return wl;
			} else {
				index++;
			}
		}
		
		// If no weak learner in candidate pool is satisfactory, return null.
		return null;
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
		
		// ct must be less than or equal to 1. Correct if it is too high.
		if (ct > 1.0) {
			return 1.0;
		} else { return ct; }
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
		double ct = wlt.combCoefficient;
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
}