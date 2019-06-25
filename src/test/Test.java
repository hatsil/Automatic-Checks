package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static global.Vars.*;

public class Test {
	public String name;
	public double score;
	public String task;
	public boolean status = true;
	public String failureReason = "";
	
	public Test(String line) {
		String[] splitted = line.split(";");
		name = splitted[0];
		score = Double.parseDouble(splitted[1]);
		task = splitted[2];
	}
	
	public void setFailure(String failureReason) {
		if(status | failureReason == COMP_FAILURE) {
			status = false;
			this.failureReason = failureReason;
		}
	}
	
	public void setSuccess() {
		status = true;
		failureReason = "";
	}

	public void reset() {
		setSuccess();
	}
	
	public static List<Test> parseTests(String testsDir, String testsFile)
			throws IOException {
		try(BufferedReader bufferedReader = new BufferedReader(
				new FileReader(new File(testsDir, testsFile)))) {

			List<Test> tests = new ArrayList<>();

			//make a new Test from each line in the tests.txt file
			String line = null;
			while((line = bufferedReader.readLine()) != null) {
				try {
					tests.add(new Test(line));
				} catch(Exception e) {
					//error while parsing the test.
					System.out.println("Error while parsing test line: " + line);
					System.err.println("Error while parsing test line: " + line);
				}
			}
			
			return tests;
		}
	}
}
