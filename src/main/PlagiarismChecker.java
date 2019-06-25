package main;

import static global.Vars.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import log.Logger;
import log.RemoteLogger;
import unzip.Unzip;

public class PlagiarismChecker {
	
	public static void main(String[] args) throws IOException {
		Logger logger = new RemoteLogger();
		try {
			logger.beginLogSession(PLAGIARIZM_OUT_LOG, PLAGIARIZM_ERR_LOG);		
			
			Set<String> excludedFileNames = getJPlageExcludedFileNames();
			if(!excludedFileNames.isEmpty()) {
				System.out.println("Excluded files from checking (lower case no suffix):");
				excludedFileNames.forEach(System.out::println);
			} else {
				System.out.println("No files are excluded from checking.");
			}
			
			System.out.println("unzips all submissions.");
			System.out.println();
			unzipAll(getAllGroupIDs(), excludedFileNames);
			
			System.out.println();
			System.out.println("done unzipping.");
			
			System.out.println();
			System.out.println("copying skeleton files");
			copySkeletonsToTemplate(excludedFileNames);
			System.out.println("done copying skeleton files");
			
			System.out.println("runs JPlag.");
			try {
				runJPlag();
			} catch(InterruptedException e) {
				System.out.println("Main thread was interrupted. Exiting process.");
				System.err.println("Main thread was interrupted. Exiting process."); 
				
				Thread.currentThread().interrupt();
				
				return;
			}
		} finally { //cleanup
			System.out.println();
			System.out.println("removes JPlag's checking directory");
			deleteFileRec(new File(CHECKING_PLAGIARISM));
			System.out.println();
			
			System.out.println("DONE");
			logger.endLogSession();
		}
	}
	
	private static void copySkeletonsToTemplate(Set<String> excludedFileNames) throws IOException {
		File[] skeletons = listFiles(SKELETONS, JAVA_SUFFIX);
		if(skeletons != null) {
			File templateDir = new File(CHECKING_PLAGIARISM + File.separator + TEMPLATE);
			templateDir.mkdirs();
			for(File skeletonFile : skeletons)
				if(!excludedFileNames.contains(skeletonFile.getName())) {
					File newLocationOfTemplate = new File(templateDir, skeletonFile.getName());
					Files.copy(skeletonFile.toPath(), newLocationOfTemplate.toPath());
				}
		}
	}

	private static void unzipAll(List<String> groupIDs, Set<String> excludedFileNames) throws IOException {
		//creates $OUTPUT_FILES if not existed
		File extractedFilesDir = new File(CHECKING_PLAGIARISM);
		if(!extractedFilesDir.exists())
			extractedFilesDir.mkdirs();
		
		for(String groupID : groupIDs) {
			String inputZipFile = SUBMISSIONS + File.separator + groupID + ZIP_SUFFIX;
			Unzip.extractJavaFiles(inputZipFile, CHECKING_PLAGIARISM + File.separator + groupID);
			
			//filter files
			for(String fileNameToDelete : excludedFileNames) {
				File fileToDelete = new File(CHECKING_PLAGIARISM + File.separator + groupID + File.separator + fileNameToDelete);
				fileToDelete.delete();
			}
			
			System.out.println(groupID + ": unzipped");
		}
	}
	
	private static void runJPlag()
			throws IOException, InterruptedException {
		File plagResDir = new File(PLAGIARISM_RESULTS);
		if(!plagResDir.exists())
			plagResDir.mkdirs();
		
		ProcessBuilder JPlagPB = new ProcessBuilder(
				"java",
				"-jar", "jplag-2.11.8-SNAPSHOT-jar-with-dependencies.jar",
				"-vl", //verbose mode: long, very detailed
				"-m", "100", //max comparisons to show
				"-bc", TEMPLATE, //the skeletons
				"-l", "java17", //the language is java 7, no support for java 8 :(
				"-r", PLAGIARISM_RESULTS, //output results
				CHECKING_PLAGIARISM //this is always the last argument - the submission directory for JPlag
				);

	
		Process JPlag = JPlagPB.start(); //may throw an IOException
		
		Thread outputStreamListener = new Thread(() -> {
			InputStream outStream = JPlag.getInputStream();
			@SuppressWarnings("resource")
			Scanner sc = new Scanner(outStream);
			while(sc.hasNextLine())
				System.out.println(sc.nextLine());
		});
		
		outputStreamListener.start();
		
		JPlag.waitFor(); //waiting for the process to end
		closeStreams(JPlag);
		
		outputStreamListener.join(); //waiting for the output stream listener thread to finish
	}
}
