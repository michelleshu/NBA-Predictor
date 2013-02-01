/**
 * TrainingExample.java
 * Consists of input feature vector (x) and target value (y)
 * 
 * @author Michelle Shu
 * January 25, 2013
 */

public class TrainingExample {
	private double[] input;		// Feature vector x
	private int dim;			// Number of components of input vector
	private double target;		// Target value y
	private double weight;		// Weight (w) of this example during training
								// (will change with each iteration)
	
	// Also, keep an instance variable for this example's relative weight (p),
	// i.e. (this weight) / (sum of all examples' weights)
	private double relative_weight;
	
	/* Constructors */
	public TrainingExample(double[] i, double t, double w) {
		this.input = i;
		this.dim = i.length;
		this.target = t;
		this.weight = w;
		this.relative_weight = 0;
	}
	
	public TrainingExample(double[] i, double t) {
		this.input = i;
		this.dim = i.length;
		this.target = t;
		this.weight = 0;
		this.relative_weight = 0;
	}
	
	/* Getters */
	/** Get entire input vector */
	public double[] getInputVector() {
		return this.input;
	}
	
	/** Get one component of input vector 
	 * @param component: The index of the dimension we want */
	public double getInputComp(int component) {
		return this.input[component];
	}
	
	/** Get the number of components of input vector */
	public int getInputDim() {
		return this.dim;
	}
	
	/** Get target value */
	public double getTarget() {
		return this.target;
	}
	
	/** Get weight of this training example */
	public double getWeight() {
		return this.weight;
	}
	
	/** Get relative weight of this training example */
	public double getRelativeWeight() {
		return this.relative_weight;
	}
	
	/* Setters */
	/** Set weight of this training example */
	public void setWeight(double w) {
		this.weight = w;
	}
	
	/** Set the relative weight of this training example */
	public void setRelativeWeight(double rw) {
		this.relative_weight = rw;
	}
}