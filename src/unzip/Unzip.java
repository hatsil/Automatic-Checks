package unzip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static global.Vars.*;

public class Unzip {
	/**
	 * Extracts all java files from a .zip file to a given destination path.
	 * @param srcZip - Given zip path.
	 * @param dest - Destination directory path.
	 * @throws FileNotFoundException 
	 * @throws IOException
	 */
	public static void extractJavaFiles(String srcZip, String dest)
			throws FileNotFoundException, IOException {
		File destDir = new File(dest);

		//create destination directory
		if(!destDir.exists())
			destDir.mkdirs();

		byte[] buffer = new byte[1024];
		//create zip input stream
		try(ZipInputStream zis = new ZipInputStream(new FileInputStream(srcZip), Charset.forName("ISO-8859-1"))) {
			for(ZipEntry zipEntry = null;
					(zipEntry = zis.getNextEntry()) != null;
					zis.closeEntry()) {		

				if(!zipEntry.isDirectory()) { //the entry isn't a directory
					File javaFile = getJavaFile(destDir, zipEntry);
					if(javaFile != null) {
						//write the file's content
						try(FileOutputStream fos = new FileOutputStream(javaFile)) {
							for(int len = 0;
									(len = zis.read(buffer)) > 0;
									fos.write(buffer, 0, len));
						}
					}
				}
			}
		}
	}

	private static File getJavaFile(File destinationDir, ZipEntry zipEntry) {
		if(!zipEntry.getName().toLowerCase().endsWith(JAVA_SUFFIX))
			return null;

		String[] splitted = zipEntry.getName().split("/");

		//creates a java file with suffix in lower case
		String javaFileName = splitted[splitted.length-1];
		javaFileName = javaFileName.substring(0, javaFileName.length()-JAVA_SUFFIX.length());
		javaFileName += JAVA_SUFFIX;
		return new File(destinationDir, javaFileName);
	}
}
