/**
 * Point.java
 * A class to represent a point in n-dimensional space
 * 
 * @author Michelle Shu
 * January 24, 2013
 */

public class Point {
	private int dim;			// number of dimensions
	private double[] values;	// components of point
	
	/* Constructors */
	
	// Create point from its component values
	public Point(double[] inputValues) {
		this.dim = inputValues.length;
		this.values = inputValues;
	}
	
	// Create an empty n-dimensional point
	public Point(int n) {
		this.dim = n;
		this.values = new double[n];
	}
	
	/* Getter and Setters */
	
	// Get all component values
	public double[] getValues() {
		return this.values;
	}
	
	// Get magnitude of one component
	public double getComponent(int comp) {
		return this.values[comp];
	}
	
	// Get number of dimensions of point
	public int getDimensions() {
		return this.dim;
	}
	
	// Set value of single component
	// comp_number: which component it is, comp_value: value to set it to
	public void setComponent(int comp_number, double comp_value) {
		this.values[comp_number] = comp_value;
	}
	
}
	