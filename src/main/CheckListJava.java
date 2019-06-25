package main;

import static global.Vars.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import unzip.Unzip;

public class CheckListJava {
	public static void main(String[] args) throws IOException {
		List<String> groupIDs = getAllGroupIDs();
		unzipAll(groupIDs);
		Set<String> marked = new TreeSet<>();
		for(String groupID : groupIDs) {
			String srcDir = getSrcDir(groupID);
			File[] javaFiles = listFiles(srcDir, JAVA_SUFFIX);
			for(File javaFile : javaFiles) {
				List<String> lines = Files.readAllLines(javaFile.toPath(), Charset.forName("ISO-8859-1"));
				for(String line : lines)
					if(line.contains("java.util.List"))
						marked.add(groupID);
			}
		}
		System.out.println(marked);
		System.out.println("DONE");
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
	
	private static String getSrcDir(String groupID) {
		return getGroupIDDir(groupID) + SRC;
	}
	
	private static String getGroupIDDir(String groupID) {
		return OUTPUT_FILES + File.separator + groupID + File.separator;
	}
}
