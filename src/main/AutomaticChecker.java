package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import generator.AllTestsGenerator;
import log.Logger;
import log.RemoteLogger;
import serializable.AutomaticResults;
import serializable.JUnitResultWrapper;
import serializable.TestResult;
import serializable.statistics.AllFailuresStatistics;
import serializable.statistics.AutomaticTestsStatistics;
import test.Test;
import unzip.Unzip;

import static global.Vars.*;

/**
 * This class's {@code main(String[] args)} method runs the automatic tests on the submitted assignments.
 * @author Tsahi Saporta
 *
 */
public class AutomaticChecker {
	/**
	 * Runs the automatic tests on the submitted assignments.
	 * @param args can be "-remote" for {@link RemoteLogger}, or "-null" for {@link NullLogger},<br>or nothing for {@link LocalLogger}.
	 * @throws IOException when fails to backup the previous "system dir"'s content.
	 */
	public static void main(String... args)
			throws IOException {

		
		//backup old files and directories
		backup();

		Logger logger = new RemoteLogger();

		try {
			//begins the logging session
			logger.beginLogSession(AUTO_OUT_LOG, AUTO_ERR_LOG);

			//get all group IDs
			System.out.println("Get all groupIDs");
			List<String> groupIDs = getAllGroupIDs();

			//update statistics
			AutomaticTestsStatistics.numOfParticipants = groupIDs.size();

			//unzip all submissions from $SUBMISSIONS to $OUTPUT_FILES
			System.out.println("Unzip all submissions");
			unzipAll(groupIDs);

			//get all additional jars
			System.out.println("Get all jar paths");
			List<String> jars = getAllJars();

			//parses all tests
			System.out.println("Parse the tests.txt file");
			System.out.println();
			List<Test> tests = Test.parseTests(TESTS_DIR, TESTS_TXT);

			//create testsStats list
			List<AutomaticTestsStatistics> testsStats = new ArrayList<>(tests.size());

			//init the statistics list
			tests.forEach(test -> testsStats.add(new AutomaticTestsStatistics(test)));

			Set<String> errorsGroupID = new TreeSet<>();
			
			int count = 1;
			final int groupIDsSize = groupIDs.size();
			for(String groupID : groupIDs) {
				String percents = String.format("%.2f", ((100. * count) / groupIDsSize));
				System.out.println(count + " of " + groupIDsSize + ", " + percents + "%");
				++count;
				System.out.println(groupID + ": prepare");
				try {
					//copy tests from $TESTS to $OUTPUT_FILES/$groupID/$SRC
					System.out.println(groupID + ": copy tests files");
					String allTestsClassName = copyTests(groupID); //may throw an IOException
					System.out.println(groupID + ": allTestsClassName is: " + allTestsClassName);

					//Compile files from $OUTPUT_FILES/$groupID/$SRC to $OUTPUT_FILES/$groupID/$BIN.
					//Generates and compiles the $allTestsClassName.java file.
					//In a case of a compilation error a marker file
					//will be created in the $OUTPUT_FILES/$groupID directory.
					System.out.println(groupID + ": compile tests files");
					compile(jars, groupID, tests, allTestsClassName, errorsGroupID);

					//runs the tests
					//In a case of a runtime error a marker file
					//will be created in the $OUTPUT_FILES/$groupID directory.
					System.out.println(groupID +
							": begin running tests, wait to finish no longer than "
							+ RUNTIME_WAITING_DURATION + " minutes");
					List<TestResult> testsResults = runTests(jars, groupID, allTestsClassName, tests, errorsGroupID);

					System.out.println(groupID +
							": put tests results in: "
							+ AUTOMATIC_RESULTS + File.separator + groupID + JSON_SUFFIX);

					//writes results to $AUTOMATIC_RESULTS/$groupID.json
					putResults(testsResults, groupID, testsStats);
				} catch(InterruptedException e) {
					System.out.println("Main thread was interrupted. Exiting process.");
					System.err.println("Main thread was interrupted. Exiting process.");
					
					//interrupts main again
					Thread.currentThread().interrupt();
					return;
				} catch(Exception e) {
					System.err.println("Exception groupID: " + groupID);
					e.printStackTrace();
				} finally {
					//if there was an error the $OUTPUT_FILES/$groupID will remain
					if(!logger.encounteredError())
						clean(groupID);
					System.out.println();
					logger.printErrNewLineIfNeeded();
					logger.clearErrorStatus();
				}
			}

			//prints the statistics
			printStats(testsStats, new ArrayList<>(errorsGroupID));
		} catch(Exception e) {
			System.err.println("-----------------Caught an exception-----------------");
			e.printStackTrace();
		} finally {
			//cleanup
			File outputDir = new File(OUTPUT_FILES);
			File[] checkedSubmissions = outputDir.listFiles();
			if(checkedSubmissions == null || checkedSubmissions.length == 0) {
				System.out.println("Remove: " + OUTPUT_FILES);
				deleteFileRec(outputDir);
			}
			
			//ends the logging session
			logger.endLogSession();
		}
	}

	private static void backup() throws IOException {
		File backupDir = new File(BACKUP);
		List<File> filesToBackup = filesToBackup();

		if(filesToBackup.isEmpty() | (filesToBackup.size() == 1 && backupDir.exists()))
			return;
		
		System.out.println("Backup old files");

		if(backupDir.exists()) {
			backupDir.renameTo(new File(BACKUP_OLD));
			backupDir = new File(BACKUP);
		}

		filesToBackup = filesToBackup();
		backupDir.mkdir();

		for(File file : filesToBackup)
			Files.move(file.toPath(), new File(backupDir.getPath(), file.getName()).toPath());
	}

	private static void printStats(List<AutomaticTestsStatistics> testsStats, List<String> errorsGroupIDAsList)
			throws FileNotFoundException {
		try(PrintStream statsOut = new PrintStream(
				new FileOutputStream(
						new File(FAILURE_STATISTICS_LOG)));
				PrintWriter printWriterForJson = new PrintWriter(FAILURE_STATISTICS_JSON)) {

			//creats AllFailuresStatistics object
			AllFailuresStatistics allStats = new AllFailuresStatistics()
					.withFailedByTest(testsStats)
					.withOtherFailuresByGroupID(errorsGroupIDAsList);

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			String jsonString = gson.toJson(allStats);

			//write the .json file
			printWriterForJson.print(jsonString);

			//print log statistics file
			testsStats.forEach(automaticTestsStatistics -> {
				if(!automaticTestsStatistics.failedGroupIDs.isEmpty())
					statsOut.println(automaticTestsStatistics);
			});

			//prints other errors report
			if(!errorsGroupIDAsList.isEmpty()) {
				statsOut.println();
				statsOut.println("Other errors - compilation, runtime etc... " +
				errorsGroupIDAsList.size() + " of " + AutomaticTestsStatistics.numOfParticipants);
				statsOut.println("GroupIDs:");
				statsOut.print("\t");
				statsOut.println(errorsGroupIDAsList);
			}
		}
	}

	private static void unzipAll(List<String> groupIDs) throws IOException {
		//creates $OUTPUT_FILES if not existed
		File extractedFilesDir = new File(OUTPUT_FILES);
		if(!extractedFilesDir.exists())
			extractedFilesDir.mkdirs();

		for(String groupID : groupIDs) {
			String inputZipFile = SUBMISSIONS + File.separator + groupID + ZIP_SUFFIX;
			String srcDir = getSrcDir(groupID);
			Unzip.extractJavaFiles(inputZipFile, srcDir);
		}
	}

	private static String getGroupIDDir(String groupID) {
		return OUTPUT_FILES + File.separator + groupID + File.separator;
	}

	private static String getSrcDir(String groupID) {
		return getGroupIDDir(groupID) + SRC;
	}	

	private static String getBinDir(String groupID) {
		return getGroupIDDir(groupID) + BIN;
	}

	private static String getCompilerLogDir(String groupID) {
		return getGroupIDDir(groupID) + COMP_LOG;
	}

	private static List<String> getAllJars() {
		//add additional jar files
		File[] jarFiles = listFiles(ADDITIONAL_JARS, JAR_SUFFIX);

		List<String> jars = new ArrayList<>(jarFiles.length + 3); // +3 for JUint's files and Gson  
		for(File jarFile : jarFiles)
			jars.add(jarFile.getPath());

		//add JUnit jar files
		File[] juintJarFiles = listFiles(CORE_JARS, JAR_SUFFIX);

		for(File juintJarFile : juintJarFiles)
			jars.add(juintJarFile.getPath());

		return jars;
	}

	private static String copyTests(String groupID) throws IOException {
		File[] testJavaFiles = listFiles(TESTS_DIR, JAVA_SUFFIX);
		String srcDir = getSrcDir(groupID);

		//copy the test .java files to the groupID directory - may throw an IOException
		for(File testJavaFileSrc : testJavaFiles) {
			String javaFileName = testJavaFileSrc.getName();
			File testJavaFileDest = new File(srcDir, javaFileName);
			Files.copy(testJavaFileSrc.toPath(), testJavaFileDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		//put all .java files' names in a set - for uniqueness
		File[] allJavaFiles = listFiles(srcDir, JAVA_SUFFIX);
		Set<String> classesNames = new TreeSet<>();
		for(File javaFile : allJavaFiles) {
			String className = javaFile.getName();
			className = className.substring(0, className.length()-JAVA_SUFFIX.length());
			classesNames.add(className);
		}

		//generates a unique name of a new class for running all the tests
		String allTestsClassName = "AllTests_A";
		while(classesNames.contains(allTestsClassName))
			allTestsClassName += "A";

		return allTestsClassName;
	}

	private static void compile(
			List<String> jars,
			String groupID,
			List<Test> tests,
			String allTestsClassName,
			Set<String> errorsGroupID)
					throws IOException, InterruptedException {
		//create bin directory
		String binDir = getBinDir(groupID);
		new File(binDir).mkdirs();

		//sets paths needed for the compilation process
		//concatenate .jar paths
		String jarPaths = "";
		for(String jar : jars)
			jarPaths += "." + File.separator + jar + File.pathSeparator;

		String binDirPath = "." + File.separator + binDir;
		String srcDirPath = "." + File.separator + getSrcDir(groupID);

		//create compiler log directory
		String compLogDir = getCompilerLogDir(groupID);
		new File(compLogDir).mkdirs();

		//reset all tests
		tests.forEach(test -> test.reset());

		//try to compile &allTestsClassName.java before
		System.out.println(groupID +
				": try to compile allTestsClassName: "
				+ allTestsClassName + JAVA_SUFFIX);

		if(!compileAllTestsJavaFile(
				groupID,
				allTestsClassName,
				tests,
				jarPaths,
				errorsGroupID,
				false)) {//failed to compile all tests at once

			//compiles each test separately
			for(Test test : tests) {
				System.out.println(groupID + ": compile test file: " + test.name + JAVA_SUFFIX);
				File testJavaFile = new File(getSrcDir(groupID), test.name + JAVA_SUFFIX);

				//sets variables for the compilation, "javac", process
				ProcessBuilder javacPB = new ProcessBuilder(
						"javac",
						"-g",
						"-encoding", "ISO-8859-1",
						"-d", binDirPath,
						"-sourcepath", srcDirPath,
						"-cp", jarPaths,
						"." + File.separator + testJavaFile.getPath())
						.redirectError(new File(compLogDir, test.name + LOG_SUFFIX));

				//starts the compilation process
				Process javacSingle = javacPB.start(); //may throw an IOException
				LocalTime waitUntil = LocalTime.now().plusMinutes(COMPTIME_WAITING_DURATION);
				for(;;) {
					//waiting for process to finish, checks for exit status
					//may throw an InterruptedException
					Duration waitingDuration = Duration.between(LocalTime.now(), waitUntil);
					boolean finshedNicely = javacSingle.waitFor(waitingDuration.toMillis(), TimeUnit.MILLISECONDS); //waiting for process to end
					if(!finshedNicely || javacSingle.exitValue() != 0) {
						if(!finshedNicely) {
							javacSingle.destroy();
							javacSingle.waitFor(); //waiting for the process to die
						}

						//compilation error, the "javac" process's exit value isn't 0
						test.setFailure(finshedNicely ? COMP_FAILURE : COMP_TIMEOUT_FAILURE);
						System.out.println(groupID + ": compilation error test: " + test.name + JAVA_SUFFIX);
						System.err.println(groupID + ": compilation error test: " + test.name + JAVA_SUFFIX);

						//mark this groupID with a compilation error;
						errorsGroupID.add(groupID);
					} else {
						test.setSuccess();
						System.out.println(groupID + ": done compiling test: " + test.name + JAVA_SUFFIX);
					}

					closeStreams(javacSingle);
					break; //breaks the endless loop

				}
			}

			System.out.println(groupID +
					": done compiling tests, compile allTestsClassName: "
					+ allTestsClassName + JAVA_SUFFIX);

			//try to compile all tests again
			compileAllTestsJavaFile(
					groupID,
					allTestsClassName,
					tests,
					jarPaths,
					errorsGroupID,
					true);
		}

		System.out.println(groupID +
				": done compiling allTestsClassName: "
				+ allTestsClassName + JAVA_SUFFIX);
	}

	private static boolean compileAllTestsJavaFile(
			String groupID,
			String allTestsClassName,
			List<Test> tests,
			String jarPaths,
			Set<String> errorsGroupID,
			boolean tryAgain)
					throws IOException, InterruptedException {
		String binDirPath = "." + File.separator + getBinDir(groupID);
		String srcDirPath = "." + File.separator + getSrcDir(groupID);
		outer:
			for(;;) {
				//generates the tests class
				String classCode = AllTestsGenerator.generate(
						allTestsClassName,
						tests,
						getCompilerLogDir(groupID),
						groupID);

				//creates $srcDir/$allTestsClassName.java File object
				File allTestsJavaFile = new File(getSrcDir(groupID), allTestsClassName + JAVA_SUFFIX);
				//delete the old file
				allTestsJavaFile.delete();

				//write the generated class code to $srcDir/$allTestsClassName.java file
				try(PrintWriter printWriter = new PrintWriter(allTestsJavaFile)) {
					printWriter.print(classCode);
				}

				//compiles the $srcDir/$allTestsClassName.java file
				//sets variables for compilation "javac" process
				ProcessBuilder javacPB = new ProcessBuilder(
						"javac",
						"-g",
						"-Xlint:none",
						"-Xmaxerrs", "1",
						"-encoding", "ISO-8859-1",
						"-d", binDirPath,
						"-sourcepath", srcDirPath,
						"-cp", jarPaths,
						"." + File.separator + allTestsJavaFile.getPath());

				//starts the compilation process
				Process javacAll = javacPB.start(); //may throw an IOException
				LocalTime waitUntil = LocalTime.now().plusMinutes(COMPTIME_WAITING_DURATION);
				for(;;) {
					Duration waitingDuration = Duration.between(LocalTime.now(), waitUntil);
					boolean finshedNicely = javacAll.waitFor(waitingDuration.toMillis(), TimeUnit.MILLISECONDS); //waiting for process to end
					if(!finshedNicely || javacAll.exitValue() != 0) {
						System.out.println(groupID + ": failed to complie: " + allTestsClassName + JAVA_SUFFIX);
						System.err.println(groupID + ": failed to complie: " + allTestsClassName + JAVA_SUFFIX);

						if(!finshedNicely) {
							javacAll.destroy();
							javacAll.waitFor(); //waiting for the process to die.
						}

						if(!tryAgain) {
							closeStreams(javacAll);
							return false;
						}

						closeStreams(javacAll);
						System.out.println(groupID + ": marks all tests as failures...");

						//mark this groupID with a compilation error;
						errorsGroupID.add(groupID);

						//marks all tests as failure
						tests.forEach(test -> test.setFailure(finshedNicely ? COMP_ERROR_UNKNOWN : COMP_TIMEOUT_FAILURE));

						continue outer;
					}

					closeStreams(javacAll);
					break outer;
				}
			}
		return true;
	}

	private static List<TestResult> runTests(
			List<String> jars,
			String groupID,
			String allTestsClassName,
			List<Test> tests,
			Set<String> errorsGroupID) throws IOException, InterruptedException {

		String classPaths = "." + File.separator + getBinDir(groupID) + File.pathSeparator;

		//concatenate .jar paths to the src class path
		String jarPaths = "";
		for(String jar : jars)
			jarPaths += "." + File.separator + jar + File.pathSeparator;

		classPaths += jarPaths;

		ProcessBuilder javaPB = new ProcessBuilder(
				"java",
				"-cp", classPaths,
				allTestsClassName);

		//runs $allTestsClassName.main(String[])
		Process java = javaPB.start(); //may throw an IOException

		StringBuilder jsonStringBuilder = new StringBuilder();
		AtomicBoolean errorFlag = new AtomicBoolean(false);
		
		Thread streamsListener = new Thread(() -> {
			Thread errThread = new Thread(() -> {
				InputStream errorStream = java.getErrorStream();
				@SuppressWarnings("resource")
				Scanner sc = new Scanner(errorStream);
				while(sc.hasNextLine()) {
					String line = sc.nextLine();
					System.err.println(line);
					if(!line.equals(""))
						errorFlag.set(true);
				}
			});

			errThread.start();

			InputStream outStream = java.getInputStream();
			@SuppressWarnings("resource")
			Scanner sc = new Scanner(outStream);
			while(sc.hasNextLine())
				jsonStringBuilder.append(sc.nextLine());
			
			boolean interrupted = Thread.currentThread().isInterrupted();
			for(;;) {
				try {
					errThread.join();
					break;
				} catch(InterruptedException e) {
					//continue waiting
					interrupted = true;
				}
			}
			
			if(interrupted) //recover the interruption status
				Thread.currentThread().interrupt();
		});

		streamsListener.start();

		//waits for no long than $RUNTIME_WAITING_DURATION minutes
		LocalTime waitUntil = LocalTime.now().plusMinutes(RUNTIME_WAITING_DURATION);
		for(;;) {
			Duration waitingDuration = Duration.between(LocalTime.now(), waitUntil);
			boolean isFinished = java.waitFor(waitingDuration.toMillis(), TimeUnit.MILLISECONDS); //waiting for process to end

			boolean runAgain = false;
			String failureReason = "";

			if(!isFinished) {
				//if the process didn't finish running kills it forcibly 
				java.destroy();
				java.waitFor(); //waiting for the process to die.
				
				streamsListener.join();
				closeStreams(java);
				System.out.println(groupID + ": run tests timout");
				System.err.println(groupID + ": run tests timout");
				runAgain = true;
				failureReason = RUNTIME_TIMEOUT;
			} else if(java.exitValue() != 0) { //some uncaught exception...
				streamsListener.join();
				closeStreams(java);
				System.out.println(groupID + ": an error occurred while running the tests.");
				System.err.println(groupID + ": an error occurred while running the tests.");
				runAgain = true;
				failureReason = RUNTIME_FAILURE;
			}
			for(;;) {
				if(runAgain) {
					//mark this groupID with a runtime error;
					errorsGroupID.add(groupID);

					//marks all tests as failure
					System.out.println(groupID + ": marks all tests as failures...");
					String failureReason2BecauseJavaIsAStupidLanguage = failureReason;
					tests.forEach(test -> test.setFailure(failureReason2BecauseJavaIsAStupidLanguage));

					//recompile $allTestsClassName.java
					compileAllTestsJavaFile(
							groupID,
							allTestsClassName,
							tests,
							jarPaths,
							errorsGroupID,
							false);

					//run tests again
					return runTests(jars, groupID, allTestsClassName, tests, errorsGroupID);
				}

				streamsListener.join();
				if(errorFlag.get())
					errorsGroupID.add(groupID);
					
				closeStreams(java);
				System.out.println(groupID + ": done running tests begin to summarize results");
				List<TestResult> testsResults = sumResults(groupID, tests, jsonStringBuilder.toString());
				if(testsResults == null) { //error in parsing the json string
					System.out.println(groupID + ": some error occurred while parsing the json string");
					System.err.println(groupID + ": some error occurred while parsing the json string");
					runAgain = true;
					failureReason = RUNTIME_FAILURE;
				} else return testsResults;
			}
		}
	}

	private static List<TestResult> sumResults(
			String groupID,
			List<Test> tests,
			String json) {		
		try {
			//converts the json object into a List<JUnitResultWrapper>
			Type listType = new TypeToken<List<JUnitResultWrapper>>(){}.getType();
			List<JUnitResultWrapper> JUnitResults = new Gson().fromJson(json, listType);

			if(JUnitResults.size() != tests.size()) //the json string was too short (or too long?!)
				return null;

			//summarizes the JUnit tests results
			int i = 0;
			List<TestResult> testsResults = new ArrayList<>(tests.size());
			for(JUnitResultWrapper JUnitResult : JUnitResults) {
				Test test = tests.get(i++);
				TestResult testResult = new TestResult(test)
						.initTestStatus(JUnitResult.successionStatus)
						.initFailures(JUnitResult.failureMessages);

				testsResults.add(testResult);
			}

			return testsResults;

		} catch(Exception e) {
			//some error: like the the student exit the program by using System.exit(int),
			//or the process was killed by the system.
			return null;
		}
	}

	private static void putResults(
			List<TestResult> testsResults,
			String groupID,
			List<AutomaticTestsStatistics> testsStats)
					throws IOException {

		//update statistics
		int i = 0;
		for(TestResult testResult : testsResults) {
			AutomaticTestsStatistics testsStat = testsStats.get(i++);
			if(!testResult.successionStatus) //the test failed
				testsStat.addFailue(groupID);
		}

		//creates results directory
		File resultsDir = new File(AUTOMATIC_RESULTS);
		if(!resultsDir.exists())
			resultsDir.mkdirs();

		//creates AutomaticResults object
		AutomaticResults automaticResults = new AutomaticResults()
				.withGroupID(groupID)
				.withTestsResults(testsResults);

		//serialize automaticResults to .json format 
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonString = gson.toJson(automaticResults);

		//write the .json file
		try(PrintWriter printWriter = new PrintWriter(
				AUTOMATIC_RESULTS + File.separator + groupID + JSON_SUFFIX)) {
			printWriter.print(jsonString);
		}
	}

	private static void clean(String groupID) {
		System.out.println(groupID + ": clean");
		File outputFilesDir = new File(getGroupIDDir(groupID));
		//delete extracted files directory
		if(outputFilesDir.exists())
			deleteFileRec(outputFilesDir);
	}

}
