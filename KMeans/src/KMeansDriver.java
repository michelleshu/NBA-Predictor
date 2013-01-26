import java.util.ArrayList;

/**
 * KMeansDriver.java
 * Simple test for correctness of KMeans.java
 * 
 * @author Michelle Shu
 * January 25, 2013
 */

public class KMeansDriver {
	public static void main (String args[]) {
		// A collection of points
		ArrayList<Point> thePoints = new ArrayList<Point>();
		
		// Create points in positive 3-D space
		double[] p1 = {30.1, 30.5, 30.6};
		thePoints.add(new Point(p1));
		double[] p2 = {31.0, 30.5, 30.9};
		thePoints.add(new Point(p2));
		double[] p3 = {30.4, 31.0, 31.2};
		thePoints.add(new Point(p3));
		double[] p4 = {31.3, 30.7, 31.1};
		thePoints.add(new Point(p4));
		
		// Create points in negative 3-D space
		double[] p5 = {-45.8, -45.4, -46.3};
		thePoints.add(new Point(p5));
		double[] p6 = {-45.2, -46.0, -45.9};
		thePoints.add(new Point(p6));
		double[] p7 = {-44.9, -46.2, -45.4};
		thePoints.add(new Point(p7));
		double[] p8 = {-45.2, -46.1, -45.7};
		thePoints.add(new Point(p8));
		
		// Create KMeans problem to compute two clusters
		KMeans problem = new KMeans(thePoints, 2);
		problem.applyKMeans();
		
		// Print the clusters and centroids
		ArrayList<ArrayList<Point>> clusters = problem.getClusters();
		ArrayList<Point> centroids = problem.getCentroids();
		
		System.out.println("Number of clusters formed: " + clusters.size());
		System.out.println("Number of centroids formed: " + centroids.size());
		
		System.out.println("Cluster 1: ");
		int s1 = clusters.get(0).size();	// size of cluster 1
		for (int i = 0; i < s1; i++) {
			System.out.println(clusters.get(0).get(i));
		}
		
		System.out.println("Cluster 2: ");
		int s2 = clusters.get(1).size();	// size of cluster 2
		for (int i = 0; i < s2; i++) {
			System.out.println(clusters.get(1).get(i));
		}
		
		System.out.println("Centroids: ");
		for (int i = 0; i < centroids.size(); i++) {
			System.out.println(centroids.get(i));
		}
	}
}