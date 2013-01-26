/** 
 * AdaBoostR.java
 * An implementation of AdaBoost for regression. Based on the algorithm in 
 * Zemel, R. S., & Pitassi, T. (2001). A gradient-based boosting algorithm for 
 * regression problems. Advances in neural information processing systems, 696-702.
 * 
 * @author Michelle Shu
 * January 25, 2012
 */

/* Plan:
 * 
 * 1. Input
 * 	Use training set examples (x1, y1)... (xn, yn)
 * 	Train many weak learners who produce hypothesis f(x) that approximates y
 * 		and whose performance can be evaluated by cost function J
 * 
 * 2. Set initial weights of all training inputs: p(xi) = 1/n
 * 
 * 3. Iterate:
 * 		Call WeakLearner - minimize cost by reweighting distribution p
 * 		(Test by acceptance criteria whether WeakLearner is good enough)
 * 		Use gradient descent to set 0 <= c <= 1, the combination coefficients
 * 			of recruited learners
 * 		Update the training distribution
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
	
	
}