package serializable.statistics;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import test.Test;

public class AutomaticTestsStatistics implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static int numOfParticipants = 0;
	static String nl = System.lineSeparator();
	
	public String testName = "";
	public List<String> failedGroupIDs = new LinkedList<>();
	
	public AutomaticTestsStatistics() {}
	
	public AutomaticTestsStatistics(Test test) {
		testName = test.name;
	}
	
	public void addFailue(String groupID) {
		failedGroupIDs.add(groupID);
	}
	
	@Override
	public String toString() {
		return "Test:   " + testName + "   " + failedGroupIDs.size() + " of " + numOfParticipants + " failed." + nl
				+ "GroupIDs:" + nl + "\t" + failedGroupIDs + nl + nl;
	}
}
