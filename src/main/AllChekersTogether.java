package main;

import static global.Vars.*;

import java.io.File;

/**
 * This class's {@code main(String[] args)} method runs automatic checks, manual checks and then merges their results.
 * @deprecated Do not run this class's main method. Run each checker separately.
 * @author Tsahi Saporta
 * 
 */

@Deprecated
public class AllChekersTogether {
	
	/**
	 * Runs automatic checks, manual checks and then merges their results.
	 * @param args can be "-remote" for {@link RemoteLogger}, or "-null" for {@link NullLogger},
	 * or nothing for {@link LocalLogger}.
	 */
	public static void main(String... args) {
		try {
			printBefore("Automatic Checker Log");
			AutomaticChecker.main();
			
			if(Thread.interrupted())
				return;
			
			printAfter();
			
			printBefore("Manual Checker Log");
			ManualChecker.main();
			
			if(Thread.interrupted())
				return;
			
			printAfter();
			
			printBefore("Results Merger Log");
			ResultsMerger.main();
			printAfter();
			
		} catch(Throwable e) {
			if(!(e instanceof InterruptedException))
				e.printStackTrace();
		} finally {
			cleanGarbage();
		}	
	}
	
	private static void cleanGarbage() {
		deleteFileRec(new File(OUTPUT_FILES));
		deleteFileRec(new File(AUTOMATIC_RESULTS));
		deleteFileRec(new File(MANUAL_RESULTS));
		deleteFileRec(new File(STATISTICS));
		deleteFileRec(new File(FAILURE_STATISTICS_JSON));
	}

	private static void printBefore(String title) {
		System.out.println("************** " + title + " **************");
		System.err.println("************** " + title + " **************");
	}
	
	private static void printAfter() {
		System.out.println();
		System.out.println();
		System.err.println();
		System.err.println();
	}
}
