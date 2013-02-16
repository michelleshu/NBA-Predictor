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
	private final int SUBSET_SIZE = 3;  // Size of random subset of features
	private final double LAMBDA = 0.01;	// Regularization term for LSR
	private final double ALPHA = 0.01;	// Step size for LSR gradient descent
	private final double TAU = 0.01;	// Stopping criterion for gradient
										// descent convergence
	
	private ArrayList<TrainingExample> training_set;
	private int n;	 // Size of training set
	private int dim; // Dimensions of training set
	private int quad_dim; // Dimensions of quadratic basis vector
	
	/* Theta is the parameter vector used by this learner after training to 
	 * make predictions. Theta has the same number of dimensions as the
	 * qyadratic basis vector of input. */
	private double[] theta;
	
	/* The indices of the features upon which this weak learner is trained */
	private ArrayList<Integer> subset; 
	
	/* The quadratic basis form of the training_set input vectors */
	private ArrayList<ArrayList<Double>> training_quad_basis;
	
	/* The combination coefficient (c) is a value chosen between 0 and 1 that
	 * takes into account the accuracy of this learner's hypothesis with
	 * respect to the current weighted distribution of training examples */
	private double combCoefficient;
	
	/** Constructor - Create a WeakLearner by training it on training set */
	public WeakLearner(ArrayList<TrainingExample> training_set) {
		this.training_set = training_set;
		this.n = this.training_set.size();
		this.dim = this.training_set.get(0).getInputDim();
		this.quad_dim = ((this.dim) * (this.dim + 1) / 2) + 1; // quadratic 
		this.theta = new double[this.quad_dim]; // quadratic theta
		for (int i = 0; i < this.theta.length; i++) {
			this.theta[i] = 0; // Initialize theta entries to 0.
		}
		this.training_quad_basis = new ArrayList<ArrayList<Double>>(this.n);
	}
	
	/** Setter for combination coefficient */
	public void setCombCoef(double cc) {
		this.combCoefficient = cc;
	}
	
	/** Getters */
	/** Get the combination coefficient of this weak learner. */
	public double getCombCoef() {
		return this.combCoefficient;
	}
	
	/** Get the subset of feature dimensions selected */
	public ArrayList<Integer> getSubset() {
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
		 * Take the training set given to the weak learner when it was created.
		 * Get a random subset of the feature dimensions of that training data,
		 * generate quadratic basis vectors for all examples in the training
		 * set, and store those quadratic basis vectors as the instance
		 * variable training_quad_basis of the weak learner.
		 */
		setTrainingSetQuadBasis();
		
		/*
		 * Now we have the quadratic representation of all inputs. Use them to
		 * generate a quadratic parameter vector theta by gradient descent, 
		 * which will define this weak learner.
		 */
		batchGradientDescent();
	}
	
	/** Set the training_quad_basis instance variable to the quadratic bases
	 * generated from input vectors based on a subset of the feature space.
	 */
	private void setTrainingSetQuadBasis() {
		this.subset = getRandomSubset();
		// Get the quadratic basis vectors for all training examples.
		double[] input;
		ArrayList<Double> quad;
		for (int i = 0; i < n; i++) {
			input = training_set.get(i).getInputVector();
			quad = getSubsetQuadBasis(input);
			this.training_quad_basis.add(i, quad);
		}
	}
	
	/** Get a random subset of feature indices */
	private ArrayList<Integer> getRandomSubset(){
		// Start with an ArrayList that contains each possible index as an
		// element.
		ArrayList<Integer> possibilities = new ArrayList<Integer>(dim);
		for (int i = 1; i <= dim; i++) {
			possibilities.add(i);
		}
		
		/* Choose indices for the subset randomly from possibilities */
		Random rand = new Random();
		int options_remaining = dim;
		ArrayList<Integer> random_subset = new ArrayList<Integer>(SUBSET_SIZE);
		for (int subset_index = 0; subset_index < SUBSET_SIZE; subset_index++) {
			int selected = rand.nextInt(options_remaining);
			random_subset.add(possibilities.remove(selected));
			options_remaining--;
		}
		return random_subset;
	}
	
	/** Generate a quadratic basis vector for the subset of data features of
	 * the input vector which were selected. Includes bias term.
	 */
	private ArrayList<Double> getSubsetQuadBasis(double[] input) {
		ArrayList<Double> qb = new ArrayList<Double>(
				(input.length * (input.length + 1) / 2) + 1);
		qb.add(0, 1.0);	// bias term
		int qb_index = 1;
		for (int i = 0; i < input.length; i++) {
			for (int j = i; j < input.length; j++) {
				// offset index by 1 because Java counts starting from 0, while
				// our subset starts at 1
				if (subset.contains(i + 1) && subset.contains(j + 1)) {
					qb.add(qb_index, input[i] * input[j]);
				}
				else {
					qb.add(qb_index, 0.0);
				}
				qb_index++;
			}
		}
		return qb;
	}
	
	/** Get prediction based on raw input vector */
	public double getHypothesis(double[] input) {
		// First must convert input to a quadratic basis vector.
		ArrayList<Double> quad_input = getSubsetQuadBasis(input);
		
		// Then get prediction from input and theta dot product
		return getHypothesisQuad(quad_input);
	}
	
	/** Get prediction based on quadratic theta vector and an input vector 
	 * passed in its quadratic basis form */
	private double getHypothesisQuad(ArrayList<Double> quad_input) {
		// The prediction is the sum of each feature with its corresponding
		// theta parameter.
		double prediction = 0;
		for (int i = 0; i < dim; i++) {
			prediction += quad_input.get(i) * theta[i];
		}
		return prediction;
	}
	
	private void batchGradientDescent() {
		// Converges on minimum when gradient step of error is less than TAU 
		// Iterate until convergence for every feature of every example 
		double step_magnitude = Double.MAX_VALUE;
		while (step_magnitude > TAU) {
			step_magnitude = 0; // holds magnitude of gradient
			// theta_change will hold the values by which each dimension of
			// theta will change with this iteration.
			double[] theta_change = new double[quad_dim];
			
			for (int j = 0; j < quad_dim; j++) { // for each feature
				ArrayList<Double> quad_input;	// quadratic basis for input
				double target;					// target value of input
				
				// Add up the contribution of each example to theta_j.
				for (int i = 0; i < n; i++) { 
					quad_input = training_quad_basis.get(i);
					target = training_set.get(i).getTarget();
					
					/* Theta changes by one step down the gradient:
					 * ALPHA * prediction error * jth component of quad input
					 * 
					 * There are also 2 adjustments: The training example weight
					 * reflects the importance of this example at the current
					 * stage in AdaBoost and the LAMBDA term is a regularizer to
					 * prevent overfitting with large theta values.
					 */
					
//					System.out.println("Example # " + i + ", Dimension " + j);
//					System.out.println("Error: " + 
//							(getHypothesisQuad(quad_input) - target));
//					System.out.println("xj: " + training_quad_basis.get(i).get(j));
					
					theta_change[j] = ALPHA * (getHypothesisQuad(quad_input) - 
							target) * training_quad_basis.get(i).get(j) * 
							training_set.get(i).getRelativeWeight();
					
					// Regularize all terms but the bias.
					if (j != 0) {
							theta_change[j] += LAMBDA * theta[j];
					}
				}
				
				// Change all theta values according to gradient
				for (int k = 0; k < quad_dim; k++) {
					theta[k] -= theta_change[k];
				}
				
				// Add change along this dimension to total magnitude of 
				// gradient step
				step_magnitude = Math.sqrt(Math.pow(step_magnitude, 2) + 
						Math.pow(theta_change[j], 2));
			}
		}
	}
}