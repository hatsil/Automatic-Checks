package serializable;

import static j2html.TagCreator.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import j2html.tags.DomContent;
import j2html.tags.Text;
import j2html.tags.UnescapedText;
import test.Test;

import static global.Vars.*;

public class TestResult implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public String testName = "";
	public double testScore = 0;
	public String task = "";
	
	public boolean successionStatus = false;
	public List<String> failureMessages = new ArrayList<>();
	
	public TestResult() {}
	
	public TestResult(Test test) {
		testName = test.name;
		testScore = test.score;
		task = test.task;
	}
	
	public TestResult initTestStatus(boolean successionStatus) {
		this.successionStatus = successionStatus;
		return this;
	}

	public TestResult initFailures(List<String> failureMessages) {
		this.failureMessages = failureMessages;
		return this;
	}

	public TestResult addTestMessage(String testMessage) {
		failureMessages.add(testMessage);
		return this;
	}
	
	public double getGrade() {
		return successionStatus ? testScore : 0;
	}
	
	private boolean isPenalized() {
		return testScore < -EPSILON;
	}

	public DomContent toHTML() {
		return isPenalized() ? 
				div(p(u(new UnescapedText(task + "&ensp;-&ensp;" + testName)),
						new UnescapedText(":&ensp;(" + testScore + " points)")).withStyle("color:red;")) :
				
				div(p(u(new UnescapedText("Task&ensp;" + task + "&ensp;[" + testName + "]")),
				new UnescapedText(":&ensp;" + (successionStatus ? "passed":
						("failed&ensp;(-" + testScore + " points)")))),
				iff(!successionStatus, div(dl(
						dt(u("Reason"), new Text(":")),
						dd(pre(each(failureMessages, failureMessage ->
							new UnescapedText(failureMessage + "\n"))))
						), pre("\n"))));
	}
}
