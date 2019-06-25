package main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import java.util.List;

import static global.Vars.*;

/**
 * This class's {@code main(String[] args)} method fixes text files
 * {@code new line character} to windows format: "{@code \r\n}".
 * @author Tsahi Saporta
 *
 */
public class FixTextFilesToWindowsFormat {
	public static void main(String[] args) throws IOException {
		File fileToFix = new File(FAILURE_STATISTICS_LOG);
//		File fileToFix = new File("system dir\\system errors.log");
		List<String> allLines = Files.readAllLines(fileToFix.toPath());
		
		try(PrintWriter printWriter =
				new PrintWriter(
						new FileWriter(fileToFix, false /* don't append, truncates the file */))) {
			for(String line : allLines)
				printWriter.print(line + "\r\n");
		}
		
		System.out.println("Done");
	}
}
