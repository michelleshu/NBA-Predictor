/**
 * WeakLearnerDriver.java
 * 
 * Tests functionality of the WeakLearner class.
 * 
 * @author Michelle Shu
 * February 14, 2013
 */

import java.util.ArrayList;

public class WeakLearnerDriver {
	public static void main(String args[]) {
		
		/*
		 * Let's create a set of 10 training points that lie exactly on the
		 * line y = 2x1 + x2 - x3. We'll set all of their weights to 1.
		 */
		ArrayList<TrainingExample> training_set = 
				new ArrayList<TrainingExample>(10);
		double[] i1 = {3.1, 4.0, 2.2};
		double t1 = 8.0;
		TrainingExample ex1 = new TrainingExample(i1, t1, 1);
		training_set.add(ex1);
		ex1.setRelativeWeight(0.1);
		
		double[] i2 = {1.5, 2.5, 3.0};
		double t2 = 2.5;
		TrainingExample ex2 = new TrainingExample(i2, t2, 1);
		training_set.add(ex2);
		ex2.setRelativeWeight(0.1);
		
		double[] i3 = {1.0, 9.2, 1.5};
		double t3 = 9.7;
		TrainingExample ex3 = new TrainingExample(i3, t3, 1);
		training_set.add(ex3);
		ex3.setRelativeWeight(0.1);
		
		double[] i4 = {4.4, 4.2, 0.6}; 
		double t4 = 12.4;
		TrainingExample ex4 = new TrainingExample(i4, t4, 1);
		training_set.add(ex4);
		ex4.setRelativeWeight(0.1);
		
		double[] i5 = {1.2, 10.6, 10.2};
		double t5 = 2.8;
		TrainingExample ex5 = new TrainingExample(i5, t5, 1);
		training_set.add(ex5);
		ex5.setRelativeWeight(0.1);
		
		double[] i6 = {3.0, 5.0, 14.2};
		double t6 = -3.2;
		TrainingExample ex6 = new TrainingExample(i6, t6, 1);
		training_set.add(ex6);
		ex6.setRelativeWeight(0.1);
		
		double[] i7 = {2.2, 1.9, 5.5}; 
		double t7 = 0.8;
		TrainingExample ex7 = new TrainingExample(i7, t7, 1);
		training_set.add(ex7);
		ex7.setRelativeWeight(0.1);
		
		double[] i8 = {10.2, 1.1, 1.3};
		double t8 = 20.2;
		TrainingExample ex8 = new TrainingExample(i8, t8, 1);
		training_set.add(ex8);
		ex8.setRelativeWeight(0.1);
		
		double[] i9 = {1.1, 4.5, 7.8};
		double t9 = -1.1;
		TrainingExample ex9 = new TrainingExample(i9, t9, 1);
		training_set.add(ex9);
		ex9.setRelativeWeight(0.1);
		
		double[] i10 = {-1.9, -4.5, 5.0};
		double t10 = -13.4;
		TrainingExample ex10 = new TrainingExample(i10, t10, 1);
		training_set.add(ex10);
		ex10.setRelativeWeight(0.1);
		
		// Now train a weak learner on this training set.
		 
		WeakLearner wl = new WeakLearner(training_set);
		wl.train();
		
		
		// Here are the theta parameters learned for these dimensions:
		System.out.println("Theta: (" + wl.getTheta()[0] + ", " + 
							wl.getTheta()[1] + ", " + wl.getTheta()[2] + ", " +
							wl.getTheta()[3] + ", " + wl.getTheta()[4] + ", " +
							wl.getTheta()[5] + ", " + wl.getTheta()[6] + ")");
		
		// Now let's try a test input on the model learned by the weak learner:
		double[] test_input = { 2.0, 3.2, 1.7 };
		System.out.print("Hypothesis for input (2.0, 3.2, 1.7): ");
		System.out.println(wl.getHypothesis(test_input));
	}
}