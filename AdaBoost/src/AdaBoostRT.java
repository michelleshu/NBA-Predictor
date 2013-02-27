/** 
 * AdaBoostR.java
 * An implementation of AdaBoost for regression. Based on the algorithm in 
 * Zemel, R. S., & Pitassi, T. (2001). A gradient-based boosting algorithm for 
 * regression problems. Advances in neural information processing systems, 696-702.
 * 
 * @author Michelle Shu
 * Last Updated February 1, 2013
 */

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AdaBoostRT {
	private static final int MAX_WL = 100;      // Number of weak learners to recruit
	private static final int MAX_BAD_WL = 200;  // quit searching if exceeds this number of bad WL
	
	/* wl_committee is the set of weak learners that have already been drafted 
	 * as part of this AdaBoost predictive model */
	private ArrayList<WeakLearner> wl_committee;

	/* The training_set contains all training examples, who each hold their
	 * current weight */
	private ArrayList<TrainingExample> training_set;

	// calculate relative errors, instead of absolute errors abs((f-y)/y)
	private static final boolean RELATIVE_ERR = true;
	private static final double MAX_WL_ERR = 0.495;

	// parameters for AdaBoostRT
	private TrainingSetTransformer transformer = null;
	private static final boolean NORMALIZE_DATA = false;
	private static final int RT_BOOST_POWER = 2;

	// threshold to consider a saple prediction correct.  Result is sensitive to this parameter
	private static final double RT_MAX_ERROR = 0.19;  

	/** Constructor */
	public AdaBoostRT(ArrayList<TrainingExample> train_set) {
		// List of training examples
		this.training_set = train_set;

		// We make a new empty array for weak learner committee.
		this.wl_committee = new ArrayList<WeakLearner>();

		System.out.println("Adaboost init");
	}

	/**
	 * AdaBoostRT, D.L. Shrestha & D.P. Solomatine, November 9, 2005 (Neural Computaion)
	 * Experiments with AdaBoost.RT, an Improved Boosting Scheme for Regression
	 */
	public void trainAdaBoostRT() {
		// normalize data set if required
		if (NORMALIZE_DATA) {
			transformer = new TrainingSetTransformer(training_set);
			transformer.transform(training_set);
		}

		int wl_collected = 0;
		int wl_discarded = 0;
		initializeTrainingDistribution();
		while (wl_collected < MAX_WL && wl_discarded < MAX_BAD_WL) {
			WeakLearner wlt = new WeakLearner(training_set);
			wlt.train();

			// calculate error rate
			double[] errors = calcRTErrors(wlt);
			double errRate = 0.0;
			for (int i = 0; i < training_set.size(); i++) {
				if (errors[i] > RT_MAX_ERROR) {
					errRate += training_set.get(i).getRelativeWeight();
				}
			}
			if (0 == errRate) {
				LogHelper.log("No Errors.  Quit");
				break;
			}
			double beta = Math.pow(errRate, RT_BOOST_POWER);

			// collect WL and set weight for this weak learner only if errRate < 0.5
			if (errRate < MAX_WL_ERR) {
				wl_committee.add(wlt);
				wlt.setCombCoef(Math.log(1.0/beta));
				wl_collected++;
				wl_discarded = 0;
			}
			else {
				wl_discarded++;
				LogHelper.logln("discard " + wl_discarded + " failed weak learner with error rate " + errRate);
				continue;
			}

			// set weight of samples for the next iteration
			double total_weight = 0;
			for (int i = 0; i < training_set.size(); i++) {
				TrainingExample sample = training_set.get(i);
				double weight = sample.getRelativeWeight();
				if (errors[i] <= RT_MAX_ERROR) {
					weight *= beta;
				}
				total_weight += weight;
				sample.setRelativeWeight(weight);  // not normalized yet
			}
			for (TrainingExample sample : training_set) {
				double weight = sample.getRelativeWeight() / total_weight;
				sample.setRelativeWeight(weight);  // normalized weight
			}

			// print out iteration result
			System.out.println("[" + wl_collected + "] WeakLearnerError=" + getError(wlt) + " ErrorRate=" + errRate + " FinalError=" + getError(training_set));
		}
	}

	private double[] calcRTErrors(WeakLearner wlt) {
		double[] errors = new double[training_set.size()];
		for (int i = 0; i < training_set.size(); i++) {
			double target = training_set.get(i).getTarget();
			double predicted = wlt.getHypothesis(training_set.get(i).getInputVector());
			errors[i] = Math.abs(predicted-target);
			if (RELATIVE_ERR) {
				errors[i] = errors[i]/Math.abs(target);
			}
		}
		return errors;
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
	 * Return relative error of committee of weak learners
	 * @return
	 */
	public double getError(ArrayList<TrainingExample> testSet) {
		double error = 0.0;
		for (TrainingExample sample : testSet) {
			double target = sample.getTarget();
			double prediction = getPrediction(sample.getInputVector());
			if (RELATIVE_ERR) {
				error += Math.abs((prediction-target)/target);
			} else {
				error += Math.abs(prediction-target);
			}
		}
		return error/testSet.size();
	}

	/**
	 * Return relative error of a weak learner
	 * 
	 * @param wl the weak learner
	 * @return
	 */
	public double getError(WeakLearner wl) {
		double error = 0.0;
		for (TrainingExample sample : training_set) {
			double target = sample.getTarget();
			double prediction = wl.getHypothesis(sample.getInputVector());
			if (RELATIVE_ERR) {
				error += Math.abs((prediction-target)/target);
			} else {
				error += Math.abs(prediction-target);
			}
		}
		return error/training_set.size();
	}

	/** Initialize training set distribution */
	private void initializeTrainingDistribution() {
		// Initialize all weights and relative weights of examples to 1/N.
		int size = training_set.size();
		for (TrainingExample sample : training_set) {
			sample.setWeight(1.0/size);
			sample.setRelativeWeight(1.0/size);
		}
	}

	/**
	 * write iteration results to a file
	 * @param filename
	 */
	public void writeBoostResults(String filename) {
		FileWriter out = null;
		try {
			// sort samples according to target value
			Collections.sort(training_set, new Comparator<TrainingExample>() {
				public int compare(TrainingExample t1, TrainingExample t2) {
					if (t1.getTarget() < t2.getTarget()) {
						return -1;
					} else if (t1.getTarget() > t2.getTarget()) {
						return 1;
					} else {
						return 0;
					}
				}
			});

			// write output file
			out = new FileWriter(filename, true);
			for (TrainingExample sample : training_set) {
				double target = sample.getTarget();
				double[] input = sample.getInputVector();

				// comma-delimited prediction errors for each iteration
				StringBuffer buff = new StringBuffer();
				buff.append(target).append(',');
				double prediction = 0;
				double sumOfWeights = 0;
				for (WeakLearner wlt : wl_committee) {
					prediction += (wlt.getCombCoef() * wlt.getHypothesis(input));
					sumOfWeights += wlt.getCombCoef();
					buff.append(prediction / sumOfWeights - target).append(',');
				}
				buff.append(prediction / sumOfWeights);
				out.write(buff.toString());
				out.append('\n');
				out.flush();
			}
		} catch (Exception e) {
			LogHelper.logln("Failed to write iteration result " + e.getMessage());
		} finally {
			try {
				out.close();
			} catch (Exception ex) {}
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

		// config WeakLearner to use specified number of features and quad terms
		WeakLearner.configWeakLearner(8, true);

		DataParser.processFile("C:/work/workspace/NBAStatFetch/data/SEASON-2007.csv");
		ArrayList<TrainingExample> training_set = DataParser.getData();
		AdaBoostRT ada = new AdaBoostRT(training_set);

		// call AdaBoostL with linear error function
		ada.trainAdaBoostRT();

		// write iteration results
		ada.writeBoostResults("c:/temp/boost.csv");

		// check new test samples
		for (int i = 2008; i < 2012; i++) {
			DataParser.processFile("C:/work/workspace/NBAStatFetch/data/SEASON-" + i + ".csv");
			System.out.println("SEASON-" + i + " Error: " + ada.getError(DataParser.getData()));
		}

	}
}