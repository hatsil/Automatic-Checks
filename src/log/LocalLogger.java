package log;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.io.output.WriterOutputStream;

import static global.Vars.*;

/**
 * @deprecated Do not use this logger, it causes deadlocks.
 * @author Tsahi Saporta
 *
 */
@Deprecated
public class LocalLogger extends AbstractLogger {
	private static final long COLSING_WINDOW_WAITING_DURATION = 5L;

	private PrintStream stdout;
	private PrintStream stderr;
	private TextWindow textWindow;
	private final String title;
	private MultiOutputStream errLog;
	private boolean encounteredError = false;

	public LocalLogger(String title) {
		super();
		this.title = title;
		new File(LOGS_DIR).mkdirs();
	}

	@Override
	public void beginLogSession(String outLogStr, String errLogStr)
			throws FileNotFoundException {
		super.beginLogSession(outLogStr, errLogStr);
		
		textWindow = new TextWindow(title);

		MultiOutputStream outLog = new MultiOutputStream(
				new CloseShieldOutputStream(System.out),
				new PrintStream(textWindow.out, true /* auto flash */),
				new PrintStream(
						new FileOutputStream(new File(outLogStr), false /* do not append */),
						true /* auto flash */));

		errLog = new MultiOutputStream(
				new CloseShieldOutputStream(System.err),
				new PrintStream(textWindow.err, true),
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
		System.out.println();
		
		//wait for the window to close
		textWindow.waitForClosing();
				
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

	private static class TextWindow extends JFrame {
		private static final long serialVersionUID = 1L;

		//MARK: public fields
		public OutputStream out = new WriterOutputStream(new TextWindowWriter(Color.black), Charset.defaultCharset());
		public OutputStream err = new WriterOutputStream(new TextWindowWriter(Color.red), Charset.defaultCharset());

		//MARK: private fields
		private Object monitor = new Object();
		private JTextPane txtLog;
		private Semaphore sem = new Semaphore(0);

		public TextWindow(String title) {
			super(title);

			//yaichs...
			addWindowListener(new WindowListener() {
				//private volatile boolean isActionRunning = false;
				private volatile JFrame popupDialog = null;
				
				@Override
				public void windowOpened(WindowEvent e) {}

				@Override
				public void windowIconified(WindowEvent e) {}

				@Override
				public void windowDeiconified(WindowEvent e) {}

				@Override
				public void windowDeactivated(WindowEvent e) {}

				@Override
				public void windowClosing(WindowEvent e) {
					//double check
					int defaultCloseOperation = getDefaultCloseOperation();
					if(defaultCloseOperation == DISPOSE_ON_CLOSE |
					defaultCloseOperation == EXIT_ON_CLOSE) {
						//releases the process of waiting for this window to close...
						sem.release();
						return;		
					}
					
					if(sem.getQueueLength() == 0) //the process is still active	
						establishPopupDialogWindow();
					else setDefaultCloseOperation(DISPOSE_ON_CLOSE);
					
					//double check
					defaultCloseOperation = getDefaultCloseOperation();
					if(defaultCloseOperation == DISPOSE_ON_CLOSE |
					defaultCloseOperation == EXIT_ON_CLOSE) {
						//releases the process of waiting for this window to close...
						sem.release();
					}
				}

				@Override
				public void windowClosed(WindowEvent e) {}

				@Override
				public void windowActivated(WindowEvent e) {}
				
				public void establishPopupDialogWindow() {
					class PopupDialogWindow extends JFrame {
						private static final long serialVersionUID = 1L;
						
						public PopupDialogWindow() {
							super("Closing Alert");
							JPanel contentPane = new JPanel();
							contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
							contentPane.setLayout(new GridLayout(2, 1, 10, 5));
							
							JPanel labelPane = new JPanel();
							contentPane.add(labelPane);
							
							labelPane.setBorder(new EmptyBorder(0, 0, 0, 0));
							labelPane.setLayout(new GridLayout(2, 1, 5, 5));
							labelPane.add(new JLabel("You are about to close the log window while the process is still running."));
							labelPane.add(new JLabel("What do you want to do?"));
							
							JPanel buttonsPane = new JPanel();
							contentPane.add(buttonsPane);
							
							buttonsPane.setBorder(new EmptyBorder(0, 0, 0, 0));
							buttonsPane.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
							
							JButton exitProcessButton = new JButton("Exit process");
							exitProcessButton.addActionListener(this::exitAction);
							buttonsPane.add(exitProcessButton);
							
							JButton closeWindowWitoutExit = new JButton("Close window witout exit");
							closeWindowWitoutExit.addActionListener(this::closeWithoutExitAction);
							buttonsPane.add(closeWindowWitoutExit);
							
							JButton cancelButton = new JButton("Cancel");
							cancelButton.addActionListener(this::cancelAction);
							buttonsPane.add(cancelButton);
							
							setContentPane(contentPane);
							
							addWindowListener(new WindowListener() {
								@Override
								public void windowOpened(WindowEvent e) {}

								@Override
								public void windowIconified(WindowEvent e) {}

								@Override
								public void windowDeiconified(WindowEvent e) {}

								@Override
								public void windowDeactivated(WindowEvent e) {}

								@Override
								public void windowClosing(WindowEvent e) {
									TextWindow.this.dispatchEvent(new WindowEvent(TextWindow.this, WindowEvent.WINDOW_CLOSING));
								}

								@Override
								public void windowClosed(WindowEvent e) {
									popupDialog = null;
								}

								@Override
								public void windowActivated(WindowEvent e) {}
							});
							
							setDefaultCloseOperation(DISPOSE_ON_CLOSE);
							pack();
							setVisible(true);
						}
						
						private void exitAction(ActionEvent e) {
							TextWindow.this.setDefaultCloseOperation(EXIT_ON_CLOSE);
							dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
						}
						
						private void closeWithoutExitAction(ActionEvent e) {
							TextWindow.this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
							dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
						}
						
						private void cancelAction(ActionEvent e) {
							TextWindow.this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
							dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
						}
					}
					
					try {
						if(popupDialog == null || !popupDialog.isVisible())
							popupDialog = new PopupDialogWindow();
					} catch(Exception e) {
						popupDialog = new PopupDialogWindow();
					}
				}
			});

			setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

			JPanel contentPane = new JPanel();
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			setContentPane(contentPane);
			contentPane.setLayout(new BorderLayout());
			JPanel textPanel = new JPanel();

			textPanel.setLayout(new BorderLayout());
			textPanel.setPreferredSize(new Dimension(600, 400));

			txtLog = new JTextPane(new DefaultStyledDocument()); 
			txtLog.setEditable(false);
			textPanel.add(new JScrollPane(txtLog));
			contentPane.add(textPanel, BorderLayout.CENTER);
			pack();
			setVisible(true);
		}

		public void waitForClosing() {
			if(!sem.tryAcquire()) {
				System.out.println("Please close the logger's window.");
				LocalTime timeOfClosing = LocalTime.now().plusMinutes(COLSING_WINDOW_WAITING_DURATION);
	
				DateTimeFormatter formatter = new DateTimeFormatterBuilder()
						.appendValue(HOUR_OF_DAY, 2)
						.appendLiteral(':')
						.appendValue(MINUTE_OF_HOUR, 2)
						.optionalStart()
						.appendLiteral(':')
						.appendValue(SECOND_OF_MINUTE, 2)
						.optionalStart()
						.toFormatter();
	
				System.out.println("The logger's window will close itself at " + timeOfClosing.format(formatter));
				
				try {
					//wait for 5 minute...
					sem.tryAcquire(COLSING_WINDOW_WAITING_DURATION, TimeUnit.SECONDS);
				} catch(InterruptedException e) {
					//if got interrupted, interrupt myself again to forward the interruption.
					Thread.currentThread().interrupt();
				} finally {
					if(isVisible()) {
						boolean interruptionStatus = Thread.interrupted();
						setDefaultCloseOperation(DISPOSE_ON_CLOSE);
						dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
						if(interruptionStatus)
							Thread.currentThread().interrupt();
					}
				}
			} 
		}

		private class TextWindowWriter extends Writer {
			private static final int BUFFER_SIZE = 1024;

			private Object monitor = new Object();
			private char[] buffer = new char[BUFFER_SIZE];
			private int nextIndex = 0;
			SimpleAttributeSet set = new SimpleAttributeSet();
			private boolean isClosed = false;

			public TextWindowWriter(Color color) {
				//set the text color. Black for stdout and Red for stderr.
				StyleConstants.setForeground(set, color);
			}

			@Override
			public void write(char[] cbuf, int off, int len) throws IOException {
				ensureOpen();
				synchronized(monitor) {
					for(int i = off; i < len; ++i) {
						if(nextIndex == BUFFER_SIZE)
							flush();
						buffer[nextIndex++] = cbuf[i];
					}
				}
			}

			@Override
			public void flush() throws IOException {
				ensureOpen();
				synchronized(monitor) {
					if(nextIndex > 0) {
						String text = new String(buffer, 0, nextIndex);
						nextIndex = 0;

						//place the text on the pane, synchronized to avoid disorder.
						synchronized(TextWindow.this.monitor) {
							StyledDocument doc = (StyledDocument)txtLog.getDocument();
							int length = doc.getLength();
							try {
								doc.insertString(length, text, null);
							} catch(BadLocationException e) {/* don't know what to do */}
							doc.setCharacterAttributes(length, text.length(), set, true);
						}
					}
				}
			}

			@Override
			public void close() throws IOException {
				flush();
				isClosed = true;
			}

			private void ensureOpen() throws IOException {
				if(isClosed)
					throw new IOException("The stream is closed");
			}
		}
	}
}
