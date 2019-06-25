package final_rersults;

import static global.Vars.*;
import static j2html.TagCreator.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import j2html.tags.DomContent;
import j2html.tags.Text;
import serializable.AutomaticResults;
import serializable.ManualRecord;
import test.Test;

/**
 * This class represents the final results of a submission group.
 * This class's instance can be encoded as an {@code html} file.
 * @author Tsahi Saporta
 *
 */
public class FinalResults {
	
	//MARK: class fields
	private static int MAX_GRADE = 100;
	private static final Random rndGen = new Random();
	
	//MARK: fields
	
	/**
	 * The submission group.
	 */
	public final String groupID;
	
	/**
	 * The output {@code html} file's name.
	 */
	public final String htmlFileName;
	
	/**
	 * The final grade.
	 */
	public long grade = 0;
	
	/**
	 * The automatic checks results.
	 */
	private final AutomaticResults automaticResults;
	
	/**
	 * The manual checks results.
	 */
	private final ManualRecord manualRecord;
	
	/**
	 * This method calculates the maximal grade of the tested assignment.
	 * @see FinalResults#MAX_GRADE
	 * @throws IOException If the {@link Test#parseTests(String, String)} fails to parse the tests.
	 */
	public static void setFinalMaxGrade() throws IOException {
		List<Test> tests = Test.parseTests(TESTS_DIR, TESTS_TXT);
		
		double maxGrade = FinalResults.MAX_GRADE = 0;
		
		for(Test test : tests)
			maxGrade += test.score;
		
		FinalResults.MAX_GRADE = (int)Math.round(maxGrade);
	}
	
	/**
	 * Constructor.
	 * @param automaticResults are the automatic checks result.
	 * @param manualRecord are the manual checks results.
	 * @see {@link AutomaticResults}<br>
	 * {@link ManualRecord}
	 */
	public FinalResults(AutomaticResults automaticResults, ManualRecord manualRecord) {
		this.automaticResults = automaticResults;
		this.manualRecord = manualRecord;
		groupID = manualRecord.groupID;
		htmlFileName = manualRecord.groupID + "0x" + Integer.toHexString(rndGen.nextInt());
		claculateFinalGrade();
	}
	
	/**
	 * This method calculates the final grade of a submission group,
	 * combining the automatic results and the manual results.
	 */
	private void claculateFinalGrade() {
		Map<String, Double> automaticScores = automaticResults.getScores();
		Map<String, Double> manualGargeLosses = manualRecord.getGradeLosses();
		double[] grade = {0};
		
		automaticScores.forEach((task, score) -> {
			Double gradeLoss = manualGargeLosses.get(task);
			if(gradeLoss == null)
				gradeLoss = 0.;
			grade[0] += Math.min(Math.max(score - gradeLoss, 0), score); //if the score is negative - for penalty.
		});
		
		this.grade = Math.max(Math.min(Math.round(grade[0]), MAX_GRADE), 0);
	}
	
	/**
	 * This method encodes the final records to an {@code html} format {@link String}. 
	 * @return A {@link String} representing the {@code html} file. 
	 */
	public String toHTML() {
		return html(
			body(
				header(h1("Grade summary for group " + groupID),
						h1("Grade: " + grade)).withStyle("text-align:center;background-color:LightSteelBlue"),
				main(howToReadJUnitReport(),
					manualRecord.toHTML(),
					automaticResults.toHTML())
			).withStyle("background-color:AliceBlue;")
		).render();
	}
	
	/**
	 * This method returns an explanation, for the students in an {@code html} format,
	 * of how to read the {@code JUnit}'s tests results.
	 * @return A {@link DomContent} object that can be encoded to an {@code html} format.
	 */
	private static DomContent howToReadJUnitReport() {
		return div(
				h3("How to read automatic log:"),
					p(new Text("Several automated tests are run on the submitted assignment."), br(),
					new Text("The result for each test is presented as following:"), br(),
					new Text("- if the test is passed successfully, the message "),
					new Text("\""), b(u(new Text("Task <checked task> [<test name>]")), new Text(": passed")), new Text("\""),
					new Text(" is printed to this log. Example: "),
					new Text("\""), b(u(new Text("Task 3a [Test_3a]")), new Text(": passed")), new Text("\""), br(),
					
					new Text("- if the test fails, the message "),
					new Text("\""), b(u(new Text("Task <checked task> [<test name>]")), new Text(": faliled (-<number> points)")), new Text("\""),
					new Text(" is printed to this log. Example: "),
					new Text("\""), b(u(new Text("Task 3a [Test_3a]")), new Text(": falied (-3.0 points)")), new Text("\""),
					br(), br(),
					new Text("A detailed explanation about the failed test is given below the message in the following format:"), br(),
					new Text("1) Test input and a short explanation about the test"), br(), br(),
					new Text("If the test was executed without any runtime error (java Exception) and generated some output"), br(),
					new Text("2) Expected output and the actual output received from the tested assignment:"), br(),
					b("expected:<...[...]...> but was:<...[...]...>"), br(),
					new Text("where <...[...]...> is the text printed to the standard output by the tested"), br(),
					new Text("assignment and [...] denotes the first place where expected and actual output differ."), br(), br(),
					new Text("Example:"), br(),
					b(u(new Text("Task 3a [Test_3a]")), new Text(": falied (-3.0 points)"))),
					dl(
						dt(b(u("Reason"), new Text(":"))),
						dd(pre("Wrong answer in Task1 on the input 1,3,2.\n" +
						"expected:<[1 \n2 \n3]> but was: <[3 \n2 \n1]>\n")))
				);
	}
}
