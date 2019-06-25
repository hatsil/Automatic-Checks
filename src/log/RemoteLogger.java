package log;

import static global.Vars.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.commons.io.output.CloseShieldOutputStream;

public class RemoteLogger extends AbstractLogger {
	private PrintStream stdout;
	private PrintStream stderr;
	private MultiOutputStream errLog;
	private boolean encounteredError = false;
	
	public RemoteLogger() {
		super();
		new File(LOGS_DIR).mkdirs();
	}
	
	@Override
	public void beginLogSession(String outLogStr, String errLogStr)
			throws FileNotFoundException {
		super.beginLogSession(outLogStr, errLogStr);
		
		MultiOutputStream outLog = new MultiOutputStream(
				new CloseShieldOutputStream(System.out),
				new PrintStream(
						new FileOutputStream(new File(outLogStr), false /* do not append */),
						true /* auto flash */));

		errLog = new MultiOutputStream(
				new CloseShieldOutputStream(System.err),
				new PrintStream(
						new FileOutputStream(new File(errLogStr), false /* do not append */),
						true /* auto flash */));

		//saves stdout and stderr
		stdout = System.out;
		stderr = System.err;

		//sets new out and err streams
		System.setOut(new PrintStream(outLog, true /* auto flash */));
		System.setErr(new PrintStream(errLog, true /* auto flash */));

		System.out.println("Begins log session...");
	}
	
	@Override
	public void endLogSession() {
		super.endLogSession();
		
		System.out.println("Ends log session...");
		//closes the log streams
		try(OutputStream out = System.out;
			OutputStream err = System.err) {
			//restores stdout and stderr
			System.setOut(stdout);
			System.setErr(stderr);
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void printErrNewLineIfNeeded() {
		super.printErrNewLineIfNeeded();
		
		try {
			if(errLog.gotUsed()) {
				System.err.println();
				errLog.clearUseageStatus();
				encounteredError = true;
			}
		} catch(Exception e) {}
	}
	
	@Override
	public boolean encounteredError() {
		super.encounteredError();
		
		return encounteredError || errLog.gotUsed();
	}
	
	@Override
	public void clearErrorStatus() {
		super.clearErrorStatus();
		
		encounteredError = false;
		errLog.clearUseageStatus();
	}
}
