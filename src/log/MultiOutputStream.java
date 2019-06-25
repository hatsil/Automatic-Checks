package log;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MultiOutputStream extends OutputStream {
	boolean isClosed = false;
	ExecutorService executor;
	List<OutputStream> streams;
	private volatile boolean usageStatus = false;
	
	public MultiOutputStream(OutputStream[] streams) {
		Objects.requireNonNull(streams, "streams array can not be null");
		for(OutputStream stream : streams)
			Objects.requireNonNull(stream, "streams array can not contain null objects");
		
		int numStreams = streams.length;
		int numProcessors = Runtime.getRuntime().availableProcessors();
		executor = Executors.newFixedThreadPool(Math.max(Math.min(numStreams, numProcessors), 1));
		this.streams = Arrays.asList(streams);
	}
	
	public MultiOutputStream(OutputStream first, OutputStream... rest) {
		this(joinStreams(first, rest));
	}
	
	private static OutputStream[] joinStreams(OutputStream first, OutputStream[] rest) {
		OutputStream[] streams = new OutputStream[1 + (rest != null ? rest.length : 0)];
		
		streams[0] = first;
		
		if(rest != null)
			for(int i = 1; i < streams.length; ++i)
				streams[i] = rest[i-1];
		
		return streams;
	}
	
	@FunctionalInterface
	private interface OutFunc {
		public void apply(OutputStream out) throws IOException;
	}
	
	private void forEach(OutFunc func) {
		List<Future<Void>> futures = new LinkedList<>();
		
		streams.forEach(out -> {
			futures.add(executor.submit(() -> {
				try {
					func.apply(out);
				} catch(IOException e) {
					throw new RuntimeException(e); //don't really know what to do...
				}
			}, (Void)null /* The answer is null (of type Void) */));
		});
		
		wait(futures);
	}
	
	private static void wait(List<Future<Void>> futures) {
		boolean interrupted = false;
		for(Future<Void> future : futures) {
			for(;;) {
				try {
					future.get();
					break;
				} catch(InterruptedException e) {
					interrupted = true;
				} catch(ExecutionException e) {
					break; //don't really know what to do;
				}
			}
		}
		
		if(interrupted) {
			//if got interrupted, interrupt myself again to forward the interruption.
			Thread.currentThread().interrupt();
		}
	}
	
	private void ensureOpen() throws IOException {
		usageStatus = true;
		if(isClosed)
			throw new IOException("The stream is closed");
	}
	
	@Override
	public void write(int b) throws IOException {
		ensureOpen();
		forEach(out -> out.write(b));
	}
	
	@Override
	public void close() throws IOException {
		ensureOpen();
		isClosed = true;
		
		forEach(out -> {
			out.flush();
			out.close();
		});
		
		executor.shutdown();
		
		boolean interrupted = false;
		
		for(;;) {
			try {
				if(executor.awaitTermination(1, TimeUnit.SECONDS))
					break;
			} catch(InterruptedException e) {
				interrupted = true;
			}
		}
		
		if(interrupted) //if got interrupted, interrupt myself again to forward the interruption.
			Thread.currentThread().interrupt();
	}
	
	@Override
	public void flush() throws IOException {
		ensureOpen();
		forEach(out -> out.flush());
	}
	
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ensureOpen();
		forEach(out -> out.write(b.clone(), off, len));
	}

	public boolean gotUsed() {
		return usageStatus;
	}

	public void clearUseageStatus() {
		usageStatus = false;
	}
}
