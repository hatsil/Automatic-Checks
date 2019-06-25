package log;

import java.io.FileNotFoundException;

import static log.Logger.State.*;

public abstract class AbstractLogger implements Logger {
	protected volatile State sessionState = DID_NOT_START;
	protected volatile State progressBarState = DID_NOT_START;
	
	private Object monitor = new Object();
	
	@Override
	public void beginLogSession(String outLogStr, String errLogStr) throws FileNotFoundException {
		synchronized(monitor) {
			if(sessionState != DID_NOT_START)
				throw new IllegalStateException("The logger session has already started");
			
			sessionState = ACTIVE;
		}
	}

	@Override
	public void endLogSession() {
		synchronized(monitor) {
			if(sessionState != ACTIVE)
				throw new IllegalStateException("The logger session isn't active");
			
			sessionState = ENDED;
		}
	}

	@Override
	public void printErrNewLineIfNeeded() {
		if(sessionState != ACTIVE)
			throw new IllegalStateException("The logger session isn't active");
	}

	@Override
	public boolean encounteredError() {
		if(sessionState != ACTIVE)
				throw new IllegalStateException("The logger session isn't active");
			
		return false;
	}

	@Override
	public void clearErrorStatus() {
		if(sessionState != ACTIVE)
			throw new IllegalStateException("The logger session isn't active");
	}
}
