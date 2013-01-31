/**
 * DataParser.java
 * Reads in data from semicolon-delimited text file and initializes instances
 * of TrainingExample class based on data.
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
		String[] lineComponents = line.split(";");
		int size = lineComponents.length;
		
		// input will store feature vector x
		double[] input = new double[size - 1];
		// target will store the target value y
		double target = Double.parseDouble(lineComponents[size - 1]);
		
		for (int i = 0; i < (size - 1); i++) {
			input[i] = Double.parseDouble(lineComponents[i]);
		}
		
		addExample(input, target);
	}
	
	/** Read entire file, processing line by line */
	public static void processFile(String filepath) {
		try {	
			// Open input streams
			FileInputStream fstream = new FileInputStream(filepath);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			
			// Pass line by line to parseLine
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				parseLine(line);
			}
			
			// Close input streams
			reader.close();
			in.close();
			fstream.close();
			
		} catch (Exception exc) {
			System.err.println("Error: Unreadable file.");
		}
	}
	
	public static void main(String [] args) {
		processFile("/Users/michelleshu/Documents/2013/CS74/Workspace/AdaBoost/src/data1.txt");
		
	}
}