package log;

import java.io.FileNotFoundException;

/**
 * This is an interface that represents the functionality of a logger system.
 * @author Tsahi Saporta
 */
public interface Logger {
	
	/**
	 * This method begins the log session. It is supposed to extend the {@link System#out}
	 * and the {@link System#err} standard streams with a given paths of log files.
	 * @param outLogStr the path to the output log file.
	 * @param errLogStr the path to the error log file.
	 * @throws FileNotFoundException If the given paths can't be open for writing.
	 * @throws IllegalStateException If the session has already begun.
	 */
	public void beginLogSession(String outLogStr, String errLogStr)
			throws FileNotFoundException;
	
	/**
	 * This method ends the log session and recover the previous {@link System#out} and
	 * {@link System#err} standard streams.
	 * @throws IllegalStateException If the session hasn't begun yet, or if
	 * the session has already ended.
	 */
	public void endLogSession();
	
	/**
	 * This method prints a new line to the extended error stream if an error has been
	 * occurred and won't print another new line until the next error occurs.
	 * @throws IllegalStateException If the session hasn't begun yet, or if
	 * the session has already ended.
	 */
	public void printErrNewLineIfNeeded();
	
	/**
	 * This method returns {@code true} if and only if the {@link System#err} stream has
	 * been used during the current log session. 
	 * @return A {@code boolean} that represents the answer.
	 * @throws IllegalStateException If the session hasn't begun yet, or if
	 * the session has already ended.
	 */
	public boolean encounteredError();
	
	/**
	 * This method clears the error status.<br>
	 * Right immediately after calling this method, the method {@link Logger#encounteredError()}
	 * will return {@code false} and the method {@link Logger#printErrNewLineIfNeeded()} will do nothing.
	 * This status remains the same until the next time {@link System#err} stream is going to be used
	 * during the current log session.
	 * @throws IllegalStateException If the session hasn't begun yet, or if
	 * the session has already ended.
	 */
	public void clearErrorStatus();
	
	/**
	 * An {@code enum} that represents the {@link Logger}'s session state.
	 * @author Tsahi Saporta
	 *
	 */
	public static enum State {
		DID_NOT_START, ACTIVE, ENDED
	}
}
