/** 
 * AdaBoostR.java
 * An implementation of AdaBoost for regression. Based on the algorithm in 
 * Zemel, R. S., & Pitassi, T. (2001). A gradient-based boosting algorithm for 
 * regression problems. Advances in neural information processing systems, 696-702.
 * 
 * @author Michelle Shu
 * January 25, 2012
 */

/* The Overall Strategy:
 * 
 * 1. Setup
 * 		Use training set examples (x1, y1)... (xn, yn)
 * 		Train many weak learners who produce hypothesis f(x) that approximates y
 * 			and whose performance can be evaluated by cost function J
 * 		Set initial weights of all training inputs: p(xi) = 1/n
 * 
 * 2. Iterate (t is round of iteration):
 * 		Recruit a new weak learner, wlt.
 * 		Given the current distribution of training examples, use line search
 * 			to set 0 <= ct <= 1, the combination coefficient of the new learner 
 * 			to minimize the cost function Jt.
 * 		Update the training distribution based on new ct and error.
 * 
 */

import java.util.ArrayList;

public class AdaBoostR {
	
	/* wl_candidates is a pool of weak learners that we can recruit from */
	private ArrayList<WeakLearner> wl_candidates;
	private int candidates_remaining; // Number of candidates remaining in pool
	
	/* wl_committee is the set of weak learners that have already been drafted 
	 * as part of this AdaBoost predictive model */
	private ArrayList<WeakLearner> wl_committee;
	
	/* The training_set contains all training examples, who each hold their
	 * current weight */
	private ArrayList<TrainingExample> training_set;
	private int N;	// Number of training examples
	
	/** Constructor - need to fill this in */
	public AdaBoostR() {
		// FILL THIS IN
		
		this.candidates_remaining = this.wl_candidates.size();
		this.N = this.training_set.size();
	}
	
	/** Initialize training set distribution - need to fill this in */
	public void init() {
		// FILL THIS IN
		
		// Initialize all weights and relative weights of examples to 1/N.
		for (int i = 0; i < N; i++) {
			training_set.get(i).setWeight(1/N);
			training_set.get(i).setRelativeWeight(1/N);
		}
	}
	
	/** Recruit learner from candidates and return it */
	private WeakLearner recruitLearner() {
		// Pluck off the last learner in the candidate list and add him to the
		// committee list.
		WeakLearner wl = wl_candidates.get(candidates_remaining - 1);
		wl_committee.add(wl);
		
		// Remove from candidates list and decrement candidates_remaining
		wl_candidates.remove(--candidates_remaining);
		
		return wl;
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
			double squaredError = 
					Math.pow(wlt.getHypothesis(example.getInputVector()) - 
						example.getTarget(), 2);
			double exp_term = Math.exp(ct * squaredError);
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
			sumSquaredError += 
					Math.pow(wlt.getHypothesis(example.getInputVector()) - 
							example.getTarget(), 2);
		}
		
		double ct = N / (2 * sumSquaredError);
		
		// ct must be less than or equal to 1. Correct if it is too high.
		if (ct > 1.0) {
			return 1.0;
		} else { return ct; }
	}
}