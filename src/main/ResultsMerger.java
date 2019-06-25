package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import com.google.gson.Gson;

import final_rersults.FinalResults;
import log.Logger;
import log.RemoteLogger;
import serializable.AutomaticResults;
import serializable.GroupIDHolder;
import serializable.ManualRecord;
import serializable.statistics.AllFailuresStatistics;

import static global.Vars.*;
import static final_rersults.FinalResults.setFinalMaxGrade;

/**
 * This class's {@code main(String[] args)} method merges the automatic checks results and the manual checks results to final results.<br>
 * And also creates failure statistic files.
 * @author Tsahi Saporta
 *
 */
public class ResultsMerger {
	
	/**
	 * Merges the automatic checks results and the manual checks results to final results.<br>
	 * Creates also failure statistic files.
	 * @param args can be "-remote" for {@link RemoteLogger}, or "-null" for {@link NullLogger},<br>or nothing for {@link LocalLogger}.
	 * @throws IOException
	 */
	public static void main(String... args)
			throws IOException {
		Logger logger = new RemoteLogger();

		try {
			logger.beginLogSession(MERGED_OUT_LOG, MERGED_ERR_LOG);

			//rename the files in $SUBMISSIONS directory
			getAllGroupIDs();

			//set max final grade
			setFinalMaxGrade();

			//get all automatic results
			Map<String, AutomaticResults> automaticResultsMap = getAutomaticResults();

			//get all manual records
			Map<String, ManualRecord> manualRecords = getManualRecords();

			//merge the results to final results
			List<FinalResults> finalResultsList = mergeResults(automaticResultsMap, manualRecords);

			//crate final results .csv file
			System.out.println("creates " + FINAL_GRADES_CSV + " file");
			putFinalResultsToCSV(finalResultsList);

			//write results to .html files
			System.out.println("creates final results .html files");
			putFinalResultsToHTML(finalResultsList);

			//optional
			System.out.println("creates failure statitiscis");
			createStatisticsFiles(finalResultsList);

			System.out.println("Done");
		} finally {
			logger.endLogSession();
		}
	}

	private static void createStatisticsFiles(List<FinalResults> finalResultsList) {
		try(BufferedReader bufferedReader = new BufferedReader(
				new FileReader(new File(FAILURE_STATISTICS_JSON)))) {

			File statsDir = new File(STATISTICS);
			if(!statsDir.exists())
				statsDir.mkdirs();

			//initiate a StringBuilder for the lines in the .json file 
			StringBuilder jsonStringBuilder = new StringBuilder("");

			//reads the .json file
			for(String line = null;
					(line = bufferedReader.readLine()) != null;
					jsonStringBuilder.append(line));

			//generates .json string
			String jsonStr = jsonStringBuilder.toString();

			Map<String, String> htmlNames = new TreeMap<>();
			finalResultsList.forEach(finalResults -> {
				htmlNames.put(finalResults.groupID, finalResults.htmlFileName);
			});

			AllFailuresStatistics allFailaStats = new Gson().fromJson(jsonStr, AllFailuresStatistics.class);
			allFailaStats.forEachFailureByTest((testName, groupID) -> {
				File testNameDir = new File(statsDir, testName);
				testNameDir.mkdirs();

				File groupIDDir = new File(testNameDir, groupID);
				groupIDDir.mkdirs();
				try {
					Files.copy(new File(SUBMISSIONS, groupID + ZIP_SUFFIX).toPath(),
							new File(groupIDDir, groupID + ZIP_SUFFIX).toPath());

					String htmlName = htmlNames.get(groupID);
					if(htmlName != null)
						Files.copy(new File(FINAL_RESULTS, htmlName + HTML_SUFFIX).toPath(),
								new File(groupIDDir, htmlName + HTML_SUFFIX).toPath());
				} catch(IOException e) {
					e.printStackTrace();
				}
			});

			allFailaStats.forEachOtherFailureByGroupID(groupID -> {
				File otherErrorsDir = new File(statsDir, OTHER_ERRORS);
				otherErrorsDir.mkdirs();

				File groupIDDir = new File(otherErrorsDir, groupID);
				groupIDDir.mkdirs();

				try {
					Files.copy(new File(SUBMISSIONS, groupID + ZIP_SUFFIX).toPath(),
							new File(groupIDDir, groupID + ZIP_SUFFIX).toPath());

					String htmlName = htmlNames.get(groupID);
					if(htmlName != null)
						Files.copy(new File(FINAL_RESULTS, htmlName + HTML_SUFFIX).toPath(),
								new File(groupIDDir, htmlName + HTML_SUFFIX).toPath());
				} catch(IOException e) {
					e.printStackTrace();
				}
			});

		} catch (IOException e) {
			System.err.println("Error while creating statistics");
			e.printStackTrace();
		}
	}

	private static Map<String, AutomaticResults> getAutomaticResults()
			throws FileNotFoundException, IOException {
		return getMapFromJsonFiles(AUTOMATIC_RESULTS, AutomaticResults.class);
	}

	private static Map<String, ManualRecord> getManualRecords()
			throws FileNotFoundException, IOException {
		return getMapFromJsonFiles(MANUAL_RESULTS, ManualRecord.class);
	}

	private static <T extends GroupIDHolder> Map<String, T> getMapFromJsonFiles(
			String jsonsDir, Class<T> clazz)
					throws FileNotFoundException, IOException {
		//get all .json files from directory: &jsonsDir
		File[] jsonFiles = listFiles(jsonsDir, JSON_SUFFIX);

		//results array of the parsed .json objects
		Map<String, T> parsedObjects = new TreeMap<>();

		//creates Gson object
		Gson gson = new Gson();

		for(File jsonFile : jsonFiles) {
			try(BufferedReader bufferedReader = new BufferedReader(
					new FileReader(jsonFile))) {
				//initiate a StringBuilder for the lines in the .json file 
				StringBuilder jsonStringBuilder = new StringBuilder("");

				//reads the .json file
				for(String line = null;
						(line = bufferedReader.readLine()) != null;
						jsonStringBuilder.append(line));

				//generates a json string
				String json = jsonStringBuilder.toString();

				//converts the json object into a T type
				T parsedObject = gson.fromJson(json, clazz);
				parsedObjects.put(parsedObject.getGroupID(), parsedObject);
			}
		}

		return parsedObjects;
	}

	private static List<FinalResults> mergeResults(
			Map<String, AutomaticResults> automaticResultsSet,
			Map<String, ManualRecord> manualRecords) {
		List<FinalResults> mergedResults = new LinkedList<>();

		automaticResultsSet.forEach((groupID, automaticResults) -> {
			ManualRecord manualRecord = manualRecords.get(groupID);
			if(manualRecord == null) {
				System.out.println(groupID + ": is missing in the excel file.");
				System.err.println(groupID + ": is missing in the excel file.");
			} else {
				System.out.println(groupID + ": merged.");
				mergedResults.add(new FinalResults(automaticResults, manualRecord));
			}
		});

		return mergedResults;
	}

	private static void putFinalResultsToCSV(List<FinalResults> finalResultsList)
			throws IOException {
		try(CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(new File(FINAL_GRADES_CSV)), CSVFormat.EXCEL)) {
			String webAddress = getWebAddress();
			finalResultsList.forEach(finalResults -> {
				try {
					csvPrinter.printRecord(finalResults.groupID,
							finalResults.grade,
							"Further information can be found here:\n" +
									webAddress + finalResults.htmlFileName + HTML_SUFFIX);
				} catch(IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	private static String getWebAddress() throws IOException {
		return Files.readAllLines(new File(WEB_ADDRESS_FILE).toPath()).get(0);
	}

	private static void putFinalResultsToHTML(List<FinalResults> finalResultsList) {
		File finalResultsDir = new File(FINAL_RESULTS);
		if(!finalResultsDir.exists())
			finalResultsDir.mkdirs();

		finalResultsList.forEach(finalResults -> {
			String htmlStr = finalResults.toHTML();
			try(FileWriter fileWriter = 
					new FileWriter(
							new File(FINAL_RESULTS,
									finalResults.htmlFileName + HTML_SUFFIX))) {
				fileWriter.write(htmlStr);
			} catch(IOException e) {
				e.printStackTrace();
			}
		});
	}
}
