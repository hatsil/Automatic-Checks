package main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import log.Logger;
import log.RemoteLogger;
import serializable.ManualRecord;
import serializable.ManualRecord.Format;
import test.Test;

import static global.Vars.*;

public class ManualChecker {

	public static void main(String... args)
			throws FileNotFoundException, IOException {

		Logger logger = new RemoteLogger();
		
		try {
			logger.beginLogSession(MANUAL_OUT_LOG, MANUAL_ERR_LOG);
	
			//creates results directory
			File resultsDir = new File(MANUAL_RESULTS);
			if(!resultsDir.exists())
				resultsDir.mkdirs();
	
			putManualResults();
		} finally {
			logger.endLogSession();
		}
	}

	private static void putManualResults() throws FileNotFoundException, IOException {
		try(CSVParser csvParser = CSVParser.parse(new FileReader(MANUAL_CSV), CSVFormat.EXCEL)) {
			List<CSVRecord> records = csvParser.getRecords();	
			//converts the records into a LinkedList
			LinkedList<CSVRecord> subRecords = records.stream()
					.collect(Collectors.toCollection(LinkedList::new));

			//removes the header
			CSVRecord header = subRecords.removeFirst();

			LinkedList<String> headerColumns = new LinkedList<>();
			//adds the header columns into a LinkedList
			header.forEach(headerColumns::add);
			headerColumns.removeFirst(); //removes the group id column;
			headerColumns.removeLast(); //removes the general notes column;
			
			//creates Gson object
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			//creates a ManualRecord.Format object from the columns of the header
			System.out.println("Parsing manual record columns");
			ManualRecord.Format format = ManualRecord.Format.makeFromCSV(headerColumns);
			
			//checks the header format
			assertHeaderFormat(format);
			
			//iterates over the rows of the .csv file
			int line = 2;
			for(CSVRecord csvRecord : subRecords) {
				try {
					//creates ManualRecord object that stores the student's manual check results 
					ManualRecord manualRecord = new ManualRecord().withFormat(format);
					manualRecord.parseCSVRecord(csvRecord);

					//convert it to a .json format
					String jsonString = gson.toJson(manualRecord);

					String groupID = manualRecord.groupID;
					System.out.println(groupID);
					try(PrintWriter printWriter = new PrintWriter(
							MANUAL_RESULTS + File.separator + groupID + JSON_SUFFIX)) {
						printWriter.print(jsonString);
					} catch(FileNotFoundException e) {
						e.printStackTrace();
					}
				} catch(Exception e) {
					System.out.println("Error in reading line: " + line + " of the excel file.");
					System.err.println("Error in reading line: " + line + " of the excel file.");
					e.printStackTrace();
				} finally {
					++line;
				}
			}
		}
	}
	
	private static void assertHeaderFormat(Format format) throws IOException {
		List<Test> tests = Test.parseTests(TESTS_DIR, TESTS_TXT);
		Set<String> tasks = new TreeSet<>();
		tests.forEach(test -> tasks.add(test.task));
		Set<String> unmatchingTasks = new TreeSet<>();
		for(String task : format.tasks())
			if(!tasks.contains(task))
				unmatchingTasks.add(task);
		
		if(!unmatchingTasks.isEmpty()) {
			final String nl = System.lineSeparator();
			String msg = "Tasks: " + unmatchingTasks + nl
			+ "are not listed in the tests.txt file." + nl
			+ "pleas check the manual.csv file or the tests.txt file or" + nl
			+ "the implementation of ManualRecord.Format.makeFromCSV(List<String>) method.";
			throw new AssertionError(msg);
		}
	}

	public static void putManualFakeRasults() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		List<String> groupIDs = getAllGroupIDs();
		for(String groupID : groupIDs) {
			System.out.println(groupID);
			String jsonString = gson.toJson(new ManualRecord().withGroupID(groupID));
			try(PrintWriter printWriter = new PrintWriter(
					MANUAL_RESULTS + File.separator + groupID + JSON_SUFFIX)) {
				printWriter.print(jsonString);
			} catch(FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
