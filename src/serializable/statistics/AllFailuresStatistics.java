package serializable.statistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AllFailuresStatistics implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public List<AutomaticTestsStatistics> failedByTest = new ArrayList<>();
	public List<String> otherFailuresByGroupID = new ArrayList<>();
	
	public AllFailuresStatistics() {}
	
	public AllFailuresStatistics withFailedByTest(List<AutomaticTestsStatistics> failedByTest) {
		this.failedByTest = failedByTest;
		return this;
	}
	
	public AllFailuresStatistics withOtherFailuresByGroupID(List<String> otherFailuresByGroupID) {
		this.otherFailuresByGroupID = otherFailuresByGroupID;
		return this;
	}
	
	public void forEachFailureByTest(BiConsumer<String, String> action) {
		failedByTest.forEach(autoStat -> {
			autoStat.failedGroupIDs.forEach(groupID -> {
				action.accept(autoStat.testName, groupID);
			});
		});
	}
	
	public void forEachOtherFailureByGroupID(Consumer<String> action) {
		otherFailuresByGroupID.forEach(action::accept);
	}
	
}
