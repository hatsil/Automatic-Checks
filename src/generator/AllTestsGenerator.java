package generator;

import java.io.File;
import java.util.List;

import test.Test;

import static global.Vars.*;

/**
 * This class's {@link AllTestsGenerator#generate(String, List, String, String)} method
 * generates a tester class, in the Java programming language, that runs all the given
 * assignment's tested tests.
 * @author Tsahi Saporta
 *
 */
public class AllTestsGenerator {

	/**
	 * This method generates a tester class, in the Java programming language,
	 * that runs all the given assignment's tested tests, then writes the results
	 * as a {@code json} format to the standard output.
	 * 
	 * @param className is the name of the generated class.
	 * @param tests are the tests to run.
	 * @param pathToCompilerLogDir is the path to the compiler log directory.
	 * @param groupID is the submission group.
	 * @return A String with the code of the generated class in Java programming language.
	 */
	public static String generate(
			String className,
			List<Test> tests,
			String pathToCompilerLogDir,
			String groupID) {
		
		//Windows operating system causes troubles
		if(File.separator.equals("\\"))
			pathToCompilerLogDir = adjustPathToWindowsOS(pathToCompilerLogDir);
			
		return	"import java.io.BufferedReader;\n" + 
				"import java.io.File;\n" + 
				"import java.io.FileNotFoundException;\n" + 
				"import java.io.FileReader;\n" + 
				"import java.io.IOException;\n" + 
				"import java.io.InputStream;\n" + 
				"import java.io.OutputStream;\n" + 
				"import java.io.PrintStream;\n" + 
				"import java.io.PrintWriter;\n" + 
				"import java.io.Serializable;\n" + 
				"import java.io.StringReader;\n" + 
				"import java.io.StringWriter;\n" + 
				"import java.util.ArrayList;\n" + 
				"import java.util.LinkedList;\n" + 
				"import java.util.List;\n" + 
				"\n" + 
				"import org.junit.runner.JUnitCore;\n" + 
				"import org.junit.runner.notification.Failure;\n" + 
				"import org.junit.runners.model.TestTimedOutException;\n" + 
				"\n" + 
				"import com.google.gson.Gson;\n" +
				"\n" +
				"public class " + className + " {\n" + 
				"	private static final String COMP_DIR_PATH = " + "\"" + pathToCompilerLogDir + "\";\n" +
				"	private static final String GROUP_ID = \"" + groupID + "\";\n" +
				"\n" +
				"	//saves the system stdin and stderr\n" + 
				"	private static final InputStream stdin = System.in;\n" + 
				"	private static final PrintStream stderr = System.err;\n" +
				"\n" +
				"	public static void main(String[] args)\n" +
				"			throws IllegalAccessException,\n" +
				"			FileNotFoundException,\n" + 
				"			IllegalArgumentException {\n" +
				"		//Makes it as a local variable.\n" +
				"		//I will not let these vile, villanious, vicious students (not really)\n" +
				"		//to reach my sweet little precious stdout and pollute their test results,\n" +
				"		//not even by using reflection.\n" +
				"		final PrintStream stdout = System.out;\n" +
				"\n" +
				"		//creates a list of results\n" + 
				"		List<Result> results = new ArrayList<>(" + tests.size() + "); //" + tests.size() + " is the number of tests.\n" + 
				"\n" + 
				"		//creates JUnitCore object\n" + 
				"		JUnitCore core = new JUnitCore();\n" + 
				"\n" + 
				"		//run the tests add tests results to the list\n" + 
				"		//sets new stdin, stdout and stderr before each running\n" + 
				testsMaker(tests) +
				"		//writes the tests results to stdout as a json format\n" + 
				"		stdout.print(new Gson().toJson(results));\n" + 
				"		stdout.flush();\n" +
				"\n" +
				"		//exits the process\n" +
				"		System.exit(0);\n" +
				"	}\n" +
				"\n" + 
				"	private static class NullInputStream extends InputStream {\n" + 
				"		private boolean isClosed = false;\n" + 
				"		\n" + 
				"		@Override\n" + 
				"		public int read() throws IOException {\n" + 
				"			ensureOpen();\n" + 
				"			return -1;\n" + 
				"		}\n" + 
				"\n" + 
				"		@Override\n" + 
				"		public void close() throws IOException {\n" + 
				"			ensureOpen();\n" + 
				"			isClosed = true;\n" + 
				"		}\n" + 
				"		\n" + 
				"		private void ensureOpen() throws IOException {\n" + 
				"			if(isClosed)\n" + 
				"				throw new IOException(\"The stream is closed.\");\n" + 
				"		}\n" + 
				"		\n" + 
				"	}\n" + 
				"	\n" + 
				"	private static class NullOutputStream extends OutputStream {\n" + 
				"		private boolean isClosed = false;\n" + 
				"\n" + 
				"		@Override\n" + 
				"		public void write(int b) throws IOException {\n" + 
				"			ensureOpen();\n" + 
				"		}\n" + 
				"		\n" + 
				"		@Override\n" + 
				"		public void close() throws IOException {\n" + 
				"			ensureOpen();\n" + 
				"			isClosed = true;\n" + 
				"		}\n" + 
				"\n" + 
				"		private void ensureOpen() throws IOException {\n" + 
				"			if(isClosed)\n" + 
				"				throw new IOException(\"The stream is closed.\");\n" + 
				"		}\n" + 
				"	}\n" + 
				"\n" + 
				"	public static class Result implements Serializable {\n" + 
				"		private static final long serialVersionUID = 1L;\n" + 
				"\n" + 
				"		public boolean successionStatus = false;\n" + 
				"		public List<String> failureMessages = new ArrayList<>();\n" + 
				"\n" + 
				"		public Result(org.junit.runner.Result result, String testName) {\n" + 
				"			if(!(successionStatus = result.wasSuccessful())) {\n" + 
				"				for(Failure failure : result.getFailures()) {\n" + 
				"					String failureMessage = failure.getMessage();\n" + 
				"					if(!(failure.getException() instanceof AssertionError) &&\n" + 
				"						!(failure.getException() instanceof TestTimedOutException)) {\n" + 
				"						//missed an exception or something else...\n" + 
				"						stderr.println(GROUP_ID + \": runtime error in test \" + testName);\n" + 
				"						stderr.flush();\n" + 
				"						\n" + 
				"						final String nl =  System.lineSeparator();\n" + 
				"						\n" + 
				"						Throwable throwable = failure.getException();\n" + 
				"						failureMessage = \"Some serious exception of type: \"\n" + 
				"						+ throwable.getClass().getName()\n" + 
				"						+ \" has been caught.\" + nl\n" + 
				"						+ \"Exception message: \" + throwable.getMessage() + nl\n" + 
				"						+ \"Stack trace:\" + nl;\n" + 
				"						\n" + 
				"						if(!(throwable instanceof StackOverflowError)) {\n" + 
				"							StringWriter stringWriter = new StringWriter();\n" + 
				"							PrintWriter printWriter = new PrintWriter(stringWriter);\n" + 
				"							throwable.printStackTrace(printWriter);\n" + 
				"							printWriter.flush();\n" + 
				"							failureMessage += stringWriter;\n" + 
				"						} else { //StackOverflowError trace takes lots of lines\n" + 
				"							failureMessage += throwable + nl;\n" + 
				"							StackTraceElement[] trace = throwable.getStackTrace();\n" + 
				"							StackTraceElement prevElement = null;\n" + 
				"							for(StackTraceElement traceElement : trace) {\n" +
				"								if(traceElement == null)\n" + 
				"									break;\n" +
				"								\n" +
				"								if(traceElement.equals(prevElement)) {\n" + 
				"									failureMessage += \"\\tat \" + traceElement + nl;\n" + 
				"									failureMessage += \"\\t...\" + nl;\n" + 
				"									break;\n" + 
				"								}\n" + 
				"								failureMessage += \"\\tat \" + traceElement + nl;\n" + 
				"								prevElement = traceElement;\n" + 
				"							}\n" + 
				"						}\n" + 
				"					}\n" + 
				"					\n" + 
				"					if(failureMessage != null) addFailureMessage(failureMessage);\n" + 
				"				}\n" + 
				"			}\n" + 
				"		}\n" +
				"\n" + 
				"		public Result() {}\n" + 
				"\n" +
				"		public Result(String testName) { //compilation error\n" + 
				"			addFailureMessage(\"Compilation error:\");\n" + 
				"			try(BufferedReader reader = new BufferedReader(\n" + 
				"					new FileReader(new File(COMP_DIR_PATH, testName + \".log\")))) {\n" + 
				"				\n" + 
				"				//adds the compiler log\n" + 
				"				for(String line = null;\n" + 
				"						(line = reader.readLine()) != null;\n" + 
				"						addFailureMessage(line));\n" + 
				"				\n" + 
				"			} catch (IOException e) {\n" + 
				"				stderr.println(GROUP_ID + \": IOException of type: \" + e.getClass().getName());\n" + 
				"				stderr.println(\"Stack trace:\");\n" + 
				"				e.printStackTrace(stderr);\n" +
				"				stderr.flush();\n" +
				"			}\n" + 
				"		}\n" +
				"\n" +
				"		public Result addFailureMessage(String failureMessage) {\n" + 
				"			failureMessages.addAll(parseMessage(failureMessage));\n" + 
				"			return this;\n" + 
				"		}\n" + 
				"\n" + 
				"		private static List<String> parseMessage(String msg) {\n" + 
				"			List<String> msgs = new LinkedList<>();\n" + 
				"			try(BufferedReader reader = new BufferedReader(\n" + 
				"					new StringReader(msg))) {\n" + 
				"\n" + 
				"				for(String line = null;\n" + 
				"						(line = reader.readLine()) != null;\n" + 
				"						msgs.add(line));\n" + 
				"\n" + 
				"			} catch(IOException neverThrows) {}\n" + 
				"\n" + 
				"			return msgs;\n" + 
				"		}\n" + 
				"	}\n" +
				"}\n";
	}
	
	/**
	 * This method generates the code in the Java programming language of the tested tests.
	 * @param tests the tested tests.
	 * @return A {@link String} in the Java programming language.
	 */
	private static String testsMaker(List<Test> tests) {
		StringBuilder stringBuilder = new StringBuilder("");

		for(Test test : tests) {
			if(test.status)
				stringBuilder.append(
					"		System.setIn(new NullInputStream());\n" +
					"		System.setOut(new PrintStream(new NullOutputStream()));\n" + 
					"		System.setErr(new PrintStream(new NullOutputStream()));\n" +
					"		results.add(new Result(core.run(" + test.name + ".class), \"" + test.name + "\"));\n" +
					"\n");
			else if(test.failureReason != COMP_FAILURE)
				stringBuilder.append(
					"		results.add(new Result().addFailureMessage(\"" + test.name + 
					" failed: " + test.failureReason + "\"));\n" + 
					"\n");
			else stringBuilder.append(
						"		results.add(new Result(\"" + test.name + "\"));\n" + 
						"\n");
		}

		return stringBuilder.toString();
	}
	
	/**
	 * This method converts a {@link String} path to a file in the Windows operating system
	 * to a {@link String} in the Java programming language's format.
	 * @param path the given file's path as a {@link String}.
	 * @return A {@link String} in the Java programming language's format.
	 */
	private static String adjustPathToWindowsOS(String path) {
		String newPath = "";
		for(int i = 0; i < path.length(); ++i) {
			char c = path.charAt(i);
			newPath += c;
			if(c == '\\')
				newPath += c;
		}
		return newPath;
	}
}
