/**
 * DataParser.java
 * Reads in data from semicolon-delimited text file and initializes instances
 * of TrainingExample class based on data.
 * To use: Pass processFile the FULL path (from root directory) of the txt file
 * to read
 * 
 * @author Michelle Shu, with major parts copied from Gediminas Bertasius
 * January 30, 2013
 */

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DataParser {
	static String DELIMITER = ",";
	static int TARGET_COL = 37;  // controls which column is the target
	static int[] FEATURE_COL;
	static {
		FEATURE_COL = new int[36];
		for (int i = 0; i < 36; i++) {
			FEATURE_COL[i] = i;
		}
	}
	static int[] BET_COL = {41, 42};
	public static ArrayList<TrainingExample> examplesIn = 
			new ArrayList<TrainingExample>();

	public static void clear() {
		examplesIn = new ArrayList<TrainingExample>();
	}
	
	public static void configParser(String delim, int target, int[] cols) {
		DELIMITER = delim;
		TARGET_COL = target;
		FEATURE_COL = cols;
	}

	/** Get all examples that have already been read. */
	public static ArrayList<TrainingExample> getData() {
		return examplesIn;
	}

	 /** Construct new TrainingExample from input and add it to examplesIn */
	private static void addExample(double[] input, double target) {
		TrainingExample newExample = new TrainingExample(input, target);
		examplesIn.add(newExample);
	}
	
	private static void addExample(double[] input, double target, 
			double[] betting_cutoffs) {
		TrainingExample newExample = new TrainingExample(input, target, 
				betting_cutoffs);
		examplesIn.add(newExample);
	}

	/** Take one line from file, break it into components, pass to addExample 
	 *  Use this case for training example. */
	private static void parseLineTrain(String line) {
		String[] lineComponents = line.split(DELIMITER);

		// input will store feature vector x
		double[] input = new double[FEATURE_COL.length];
		// target will store the target value y
		double target = Double.parseDouble(lineComponents[TARGET_COL]);

		for (int i = 0; i < FEATURE_COL.length; i++) {
			input[i] = Double.parseDouble(lineComponents[FEATURE_COL[i]]);
		}

		addExample(input, target);
	}
	
	/** Take one line from file, break it into components, pass to addExample 
	 *  Use this case for testing example. (include betting data) */
	private static void parseLineTest(String line) {
		String[] lineComponents = line.split(DELIMITER);

		// input will store feature vector x
		double[] input = new double[FEATURE_COL.length];
		// target will store the target value y
		double target = Double.parseDouble(lineComponents[TARGET_COL]);

		for (int i = 0; i < FEATURE_COL.length; i++) {
			input[i] = Double.parseDouble(lineComponents[FEATURE_COL[i]]);
		}
		// betting cutoffs stores the cutoffs for cumulative and difference bets
		// from OddShark.com
		double[] betting_cutoffs = new double[BET_COL.length];
		betting_cutoffs[0] = Double.parseDouble(lineComponents[BET_COL[0]]);
		betting_cutoffs[1] = Double.parseDouble(lineComponents[BET_COL[1]]);

		addExample(input, target, betting_cutoffs);
	}

	/** Read entire file, processing line by line 
	 * Test is true if it is a test example, in which case we store betting
	 * cutoff data. */
	public static void processFile(String filepath, boolean test) {
		try {	
			// Open input streams
			FileInputStream fstream = new FileInputStream(filepath);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			if (! test) {
				// Pass line by line to parseLineTrain
				String line;
				while ((line = reader.readLine()) != null) {
					parseLineTrain(line);
				}
			}
			
			if (test) {
				String line;
				while ((line = reader.readLine()) != null) {
					parseLineTest(line);
				}
			}

			// Close input streams
			reader.close();
			in.close();
			fstream.close();

		} catch (Exception exc) {
			System.err.println("Error reading file.");
		}
	}
	
	public static void main(String[] args) {
		processFile("data/SEASON-2012-TEST.csv", true);
		ArrayList<TrainingExample> data = getData();
		System.out.println(data.size());
		System.out.println(data.get(87).getBetCutoff(0));
	}
}