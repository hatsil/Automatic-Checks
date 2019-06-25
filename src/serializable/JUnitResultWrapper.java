package serializable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import generator.AllTestsGenerator;

/**
 * This class is identical to the class "Result" that is generated in<br>
 * {@link AllTestsGenerator#generate(String, List, String, String)}.
 * @author Tsahi Saporta
 *
 */
public class JUnitResultWrapper implements Serializable {
	private static final long serialVersionUID = 1L;

	public boolean successionStatus;
	public List<String> failureMessages;

	public JUnitResultWrapper() {
		successionStatus = false;
		failureMessages = new ArrayList<>();
	}
}