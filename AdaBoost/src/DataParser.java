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
	static String DELIMITER = ";";
	static int TARGET_COL = 33;  // controls which column is the target
	static int[] FEATURE_COL;
	static {
		FEATURE_COL = new int[31];
		for (int i = 0; i < 31; i++) {
			FEATURE_COL[i] = i;
		}
	}

	public static void conigParser(String delim, int target, int[] cols) {
		DELIMITER = delim;
		TARGET_COL = target;
		FEATURE_COL = cols;
	}

	public static ArrayList<TrainingExample> examplesIn;

	/** Get all examples that have already been read. */
	public static ArrayList<TrainingExample> getData() {
		return examplesIn;
	}

	 /** Construct new TrainingExample from input and add it to examplesIn */
	private static void addExample(double[] input, double target) {
		TrainingExample newExample = new TrainingExample(input, target);
		examplesIn.add(newExample);
	}

	/** Take one line from file, break it into components, pass to addExample */
	private static void parseLine(String line) {
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

	/** Read entire file, processing line by line */
	public static void processFile(String filepath) {
		examplesIn = new ArrayList<TrainingExample>();
		try {	
			// Open input streams
			FileInputStream fstream = new FileInputStream(filepath);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));

			// Pass line by line to parseLine
			String line;
			while ((line = reader.readLine()) != null) {
				parseLine(line);
			}

			// Close input streams
			reader.close();
			in.close();
			fstream.close();

		} catch (Exception exc) {
			System.err.println("Error reading file.");
		}
	}

	public static void main(String [] args) {
		processFile("/Users/michelleshu/Documents/2013/CS74/Workspace/AdaBoost/src/data1.txt");
	}
}