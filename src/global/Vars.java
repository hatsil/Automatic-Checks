package global;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This class has some public methods and holds public
 * variables, most of them are paths of the system files.
 * @author Tsahi Saporta
 *
 */
public class Vars {
	//MARK: epsilon
	public static final double EPSILON = 0.005;
	
	//MARK: directories
	private static final String SYS_DIR = "system dir" + File.separator;
	public static final String BACKUP = SYS_DIR + "backup";
	public static final String BACKUP_OLD = SYS_DIR + "backup old";
	public static final String SUBMISSIONS = SYS_DIR + "submissions";
	public static final String OUTPUT_FILES = SYS_DIR + "extracted files";
	public static final String TESTS_DIR = SYS_DIR + "tests";
	public static final String ADDITIONAL_JARS = SYS_DIR + "additional jars";
	public static final String CORE_JARS = SYS_DIR + "core jars";
	public static final String AUTOMATIC_RESULTS = SYS_DIR + "automatic results";
	public static final String MANUAL_RESULTS = SYS_DIR + "manual results";
	public static final String FINAL_RESULTS = SYS_DIR + "final results";
	public static final String STATISTICS = SYS_DIR + "statistics by failures";
	public static final String CHECKING_PLAGIARISM = SYS_DIR + "checking plagiarism";
	public static final String SKELETONS = SYS_DIR + "skeletons";
	public static final String PLAGIARISM_RESULTS = SYS_DIR + "plagiarism results";
	public static final String LOGS_DIR = SYS_DIR + "logs";

	//MARK: nested directories
	public static final String SRC = "src";
	public static final String BIN = "bin";
	public static final String COMP_LOG = "comp log";
	public static final String OTHER_ERRORS = "other errors";
	public static final String TEMPLATE = "template";

	//MARK: files' suffixes
	public static final String ZIP_SUFFIX = ".zip";
	public static final String JAR_SUFFIX = ".jar";
	public static final String JAVA_SUFFIX = ".java";
	public static final String JSON_SUFFIX = ".json";
	public static final String LOG_SUFFIX = ".log";
	public static final String TXT_SUFFIX = ".txt";
	public static final String CSV_SUFFIX = ".csv";
	public static final String HTML_SUFFIX = ".html";

	//MARK: files
	public static final String FAILURE_STATISTICS_LOG = SYS_DIR + "failure statistics" + LOG_SUFFIX;
	public static final String FAILURE_STATISTICS_JSON = SYS_DIR + "failure statistics" + JSON_SUFFIX;
	public static final String MANUAL_CSV = SYS_DIR + "manual" + CSV_SUFFIX;
	public static final String FINAL_GRADES_CSV = SYS_DIR + "final grades" + CSV_SUFFIX;
	public static final String WEB_ADDRESS_FILE = SYS_DIR + "web address" + TXT_SUFFIX;
	private static final String SYSTEM_TIMEOUT_FILE = SYS_DIR + "system timeout" + TXT_SUFFIX;
	private static final String JPLAG_EXCLUDED_FILES = SYS_DIR + "jplag excluded files" + TXT_SUFFIX;
	
	//MARK: system log files
	public static final String AUTO_OUT_LOG = LOGS_DIR + File.separator + "auto checks out" + LOG_SUFFIX;
	public static final String AUTO_ERR_LOG = LOGS_DIR + File.separator + "auto checks err" + LOG_SUFFIX;
	public static final String MANUAL_OUT_LOG = LOGS_DIR + File.separator + "manual checks out" + LOG_SUFFIX;
	public static final String MANUAL_ERR_LOG = LOGS_DIR + File.separator + "manual checks err" + LOG_SUFFIX;
	public static final String MERGED_OUT_LOG = LOGS_DIR + File.separator + "merged results out" + LOG_SUFFIX;
	public static final String MERGED_ERR_LOG = LOGS_DIR + File.separator + "merged results err" + LOG_SUFFIX;
	public static final String PLAGIARIZM_OUT_LOG = LOGS_DIR + File.separator + "plagiarism results out" + LOG_SUFFIX;
	public static final String PLAGIARIZM_ERR_LOG = LOGS_DIR + File.separator + "plagiarism results err" + LOG_SUFFIX;
	
	//MARK: nested files
	public static final String TESTS_TXT = "tests" + TXT_SUFFIX;
	
	//MARK: waiting time for compiler & test running duration in minutes
	public static final long RUNTIME_WAITING_DURATION = getRuntimeTimeout();
	public static final long COMPTIME_WAITING_DURATION = 3L;
	
	private static long getRuntimeTimeout() {
		try {
			List<String> lines = Files.readAllLines(new File(SYSTEM_TIMEOUT_FILE).toPath());
			return Long.parseLong(lines.get(0));
		} catch(Exception e) {
			return 15L;
		}
	}

	//MARK: test failure reasons
	public static final String COMP_FAILURE = "compilation error";
	public static final String COMP_TIMEOUT_FAILURE = "compilation error - timeout.";
	public static final String COMP_ERROR_UNKNOWN = "compilation error - the compilation process was terminated for an unknown reason.";
	public static final String RUNTIME_FAILURE = "an unknown runtime error occurred.";
	public static final String RUNTIME_TIMEOUT =
			"your program has been aborted, it took more than " + RUNTIME_WAITING_DURATION + " minutes to run.";
	
	private static class ExcludedFilesHolder {
		private static final Set<File> excludedFiles;
		
		static {
			excludedFiles = new HashSet<>();
			excludedFiles.addAll(Arrays.asList(
					SUBMISSIONS,
					TESTS_DIR,
					ADDITIONAL_JARS,
					MANUAL_CSV,
					CORE_JARS,
					WEB_ADDRESS_FILE,
					SYSTEM_TIMEOUT_FILE,
					SKELETONS,
					JPLAG_EXCLUDED_FILES,
					PLAGIARISM_RESULTS)
					.stream().map(File::new).collect(Collectors.toList()));
		}
	}
	
	/**
	 * Returns a list of all the files inside a given directory filtered by the given suffix. 
	 * @param dir is the path to the directory as {@link String}.
	 * @param suffix is the suffix of the file as {@link String}.
	 * @return A filtered array of {@link File} objects inside the given directory.
	 */
	public static File[] listFiles(String dir, String suffix) {
		return new File(dir).listFiles(
				file -> file.isFile() && file.getName().toLowerCase().endsWith(suffix)); 
	}
	
	/**
	 * @return A {@link List} of {@link File}s inside the "system dir" directory for backup.
	 */
	public static List<File> filesToBackup() {
		File[] listOfFiles = new File(SYS_DIR).listFiles();
		return Arrays.asList(listOfFiles)
				.stream().filter(file -> {
					return !ExcludedFilesHolder.excludedFiles.contains(file) && !file.isHidden();
				}).collect(Collectors.toCollection(LinkedList<File>::new));
	}
	
	/**
	 * Renames all the .zip files in the $SUBMISSIONS directory.
	 * 
	 * @return A {@link List} of {@link String}s that contains the submission groups.
	 */
	public static List<String> getAllGroupIDs() {
		//read all zip files in $SUBMISSIONS directory
		File[] submissionZipFiles = listFiles(SUBMISSIONS, ZIP_SUFFIX);

		List<String> groupIDs = new ArrayList<>(submissionZipFiles.length);
		for(File submissionZipFile : submissionZipFiles) {
			String zipName = submissionZipFile.getName();

			//extract the groupID from .zip file
			int end = 0;
			for(; zipName.charAt(end) >= '0' & zipName.charAt(end) <= '9'; ++end);

			String groupID = zipName.substring(0, end);
			groupIDs.add(groupID);

			//rename the file to $groupID.zip
			submissionZipFile.renameTo(new File(SUBMISSIONS, groupID + ZIP_SUFFIX));
		}

		return groupIDs;
	}
	
	/**
	 * Closes the input and output streams of a {@link Process}.
	 * @param p is the {@link Process}.
	 */
	public static void closeStreams(Process p) {
		try {
			p.getErrorStream().close();
		} catch(IOException e) {
			System.out.println("fail to close error stream");
		} finally {
			try {
				p.getInputStream().close();
			} catch(IOException e) {
				System.out.println("fail to close input stream");
			} finally {
				try {
					p.getOutputStream().close();
				} catch(IOException e) {
					System.out.println("fail to close output stream");
				}
			}
		}
	}
	
	/**
	 * Deletes a file recursively.<br>
	 * If the file is a directory it removes its contents and then removes the directory itself.
	 * @param file is the path of the file to remove. 
	 */
	public static void deleteFileRec(File file) {
		if(Files.isDirectory(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
			File[] dirContent = file.listFiles();
			if(dirContent != null) {
				for(File fileInDir : dirContent)
					deleteFileRec(fileInDir);
			}
		}
		file.delete();
	}
	
	/**
	 * @return A list of {@link String}s containing the file names to exclude from checking
	 */
	public static Set<String> getJPlageExcludedFileNames() {
		Set<String> ans = new TreeSet<>();
		try {
			List<String> excludedFiles = Files.readAllLines(new File(JPLAG_EXCLUDED_FILES).toPath(), Charset.forName("ISO-8859-1"));
			for(String excludedFile : excludedFiles) {
				String cleanedName = excludedFile.replaceAll("\\s+", "");
				if(!cleanedName.equals(""))
					ans.add(cleanedName + JAVA_SUFFIX);
			}
		} catch(IOException e) {/* do nothing */}
		
		return ans;
	}
}
