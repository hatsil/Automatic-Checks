package serializable;

import static j2html.TagCreator.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import j2html.tags.DomContent;

public class AutomaticResults implements
GroupIDHolder, Serializable {
	private static final long serialVersionUID = 1L;
	
	//MARK: fields
	public String groupID = "";
	public List<TestResult> testsResults = new ArrayList<>();
	
	public AutomaticResults() {}
	
	public AutomaticResults withGroupID(String groupID) {
		this.groupID = groupID;
		return this;
	}
	
	public AutomaticResults withTestsResults(List<TestResult> testsResults) {
		this.testsResults = testsResults;
		return this;
	}
	
	@Override
	public String getGroupID() {
		return groupID;
	}

	public DomContent toHTML() {
		return div(
				h2(u("Automatic check report")).withStyle("text-align:center;"),
				h3(each(testsResults, testResult -> testResult.toHTML())));
	}

	public Map<String, Double> getScores() {
		Map<String, Double> scores = new TreeMap<>();
		testsResults.forEach(testResult -> {
			String task = testResult.task;
			Double score = scores.get(task);
			
			if(score == null)
				score = testResult.getGrade();
			else score += testResult.getGrade();
			
			scores.put(task, score);
		});
		return scores;
	}
}
