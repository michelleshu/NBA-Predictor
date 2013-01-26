/** 
 * KMeans.java
 * Performs k-means clustering on a set of n-dimensional data points with
 * real-valued entries
 * 
 * @author Michelle Shu
 * January 24, 2013
 */


import java.util.ArrayList;
import java.util.HashSet;
import java.lang.Math;

public class KMeans {
	/* Keep parallel array lists of clusters and corresponding centroids */
	private int numClusters;	// number of clusters
	private ArrayList<ArrayList<Point>> clusters;	// groups of close points
	private ArrayList<Point> centroids;		// means of clusters
	
	/** Constructor:
	 * Create a KMeans problem from a set of input points and the number of 
	 * clusters to be formed
	 */
	public KMeans(ArrayList<Point> inputPoints, int c) {
		this.numClusters = c;
		this.clusters = new ArrayList<ArrayList<Point>>(c);
		this.centroids = new ArrayList<Point>(c);
		
		// Initialize array lists for every cluster
		for (int i = 0; i < c; i++) {
			clusters.add(new ArrayList<Point>());
		}
		
		// Initialize placeholders for each centroid
		for (int i = 0; i < c; i++) {
			centroids.add(new Point());
		}
		
		// All points start in one cluster
		clusters.set(0, inputPoints);
		
		// Initialize centroids at random, unique points
		HashSet<Integer> alreadyUsed = new HashSet<Integer>();
		for (int i = 0; i < c; i++) {
			int rand = (int) (Math.random() * c);
			while (alreadyUsed.contains(rand)) {
				// Regenerate if that example is already used as a centroid
				rand = (int) (Math.random() * c);
			}
			alreadyUsed.add(rand);
			centroids.set(i, inputPoints.get(rand));
		}
	}
	
	/* Getters */
	/** Return clusters */
	public ArrayList<ArrayList<Point>> getClusters() {
		return this.clusters;
	}
	
	/** Return centroids */
	public ArrayList<Point> getCentroids() {
		return this.centroids;
	}
	
	
	/** Calculate the Euclidean distance between two points */
	private double getDistance(Point p1, Point p2) {
		
		// Error if points are different dimensions
		if (p1.getDimensions() != p2.getDimensions()) {
			System.err.println("Error: Dimensions of points do not match.");
			return -1;
		}
		
		// Otherwise, start by calculating the distance between the 0th
		// components of the two points.
		double dist = Math.abs(p1.getComponent(0) - p2.getComponent(0));
		
		// Factor in each component of the points, using the Pythagorean theorem
		int i = 1;
		while (i <= p1.getDimensions() - 1) {
			dist = Math.sqrt(Math.pow(dist, 2) +
					Math.pow((p1.getComponent(i) - p2.getComponent(i)), 2));
			i++;
		}
		return dist;
	}
	
	/** Returns the index of the centroid closest to the point */
	private int getClosestCentroidIndex(Point p) {
		int closestIndex = 0;
		double closestDistance = Double.MAX_VALUE;
		
		// Iterate through all centroids to find the one that is closest to p
		for (int i = 0; i < this.numClusters; i++) {
			double currentDistance = getDistance(p, centroids.get(i));
			if (currentDistance < closestDistance) {
				closestIndex = i;
				closestDistance = currentDistance;
			}
		}
		// closestIndex is the index in centroid list of the closest centroid
		return closestIndex;
	}
	
	/** Get the average of a set of points */
	private Point getAveragePoint(ArrayList<Point> points) {
		int dim = points.get(0).getDimensions(); // dimensions of point
		double[] average = new double[dim]; // where we tally average dims
		int size = points.size();	// size of input set
		
		// First sum up all the values of components of points
		for (int i = 0; i < size; i++) {
			for (int d = 0; d < dim; d++) {
				average[d] += points.get(i).getComponent(d);
			}
		}
		
		// Then divide by the number of inputs to obtain the average.
		for (int d = 0; d < dim; d++) {
			average[d] /= size;
		}
		
		// Create a new Point object with these dimensions and return it.
		Point averagePoint = new Point(average);
		return averagePoint;
	}
	
	/** Update centroid locations based on cluster membership. */
	private void updateCentroids() {
		// Create new centroid list
		ArrayList<Point> newCentroids = new ArrayList<Point>();
		
		// Iterate through clusters, calculating new centroids.
		for (int c = 0; c < this.numClusters; c++) {
			Point newCentroid = getAveragePoint(this.clusters.get(c));
			newCentroids.add(c, newCentroid);
		}
		
		// Update the centroid list to the new one.
		this.centroids = newCentroids;
	}
	
	
	/** Assign points to appropriate cluster given current set of centroids 
	 * Return true if any cluster's membership changed.
	 * Return false if all clusters remained the same. */
	private boolean reclusterPoints() {	
		// Use boolean variable changed to track whether or not clusters changed
		boolean changed = false;
		
		// Initialize new array list to store new clusters
		ArrayList<ArrayList<Point>> newClusters = 
				new ArrayList<ArrayList<Point>>();
		for (int i = 0; i < this.numClusters; i++) {
			newClusters.add(new ArrayList<Point>());
		}
		
		// Reassign points from current clusters to appropriate new cluster
		for (int cIndex = 0; cIndex < this.numClusters; cIndex++) {
			ArrayList<Point> currentCluster = this.clusters.get(cIndex);
			for (int pIndex = 0; pIndex < currentCluster.size(); pIndex++) {
				Point currentPoint = currentCluster.get(pIndex);
				
				// Find the appropriate cluster index for this point, based on
				// which centroid it is closest to.
				int updatedIndex = getClosestCentroidIndex(currentPoint);
				// Put the point in this cluster in the new configuration 
				newClusters.get(updatedIndex).add(currentPoint);
				
				// If this is not the point's original cluster, then update
				// changed to reflect the change.
				if (updatedIndex != cIndex) {
					changed = true;
				}
			}
		}
		
		// Update the problem's instance variable to reflect new clusters
		this.clusters = newClusters;
		// And return whether or not the clusters have changed
		return changed;
	}
	
	/** Apply the k-means algorithm to until system reaches equilibrium
	 * Equilibrium is reached when no points change cluster membership.
	 */
	public void applyKMeans() {
		// While points are changing between clusters, update centroids.
		// When points no longer change, we are done.
		while (reclusterPoints()) {
			updateCentroids();
		}
	}
}