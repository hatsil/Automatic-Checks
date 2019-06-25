package serializable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.apache.commons.csv.CSVRecord;

import j2html.tags.DomContent;
import j2html.tags.Text;
import j2html.tags.UnescapedText;
import static j2html.TagCreator.*;

import static global.Vars.*;

public class ManualRecord implements
GroupIDHolder, Serializable {
	private static final long serialVersionUID = 1L;

	public String groupID = "";
	public List<Entry> entries = new ArrayList<>();
	public List<String> generalNotes = new ArrayList<>();

	private transient boolean areEntriesInitialized = false;

	public ManualRecord() {}

	public ManualRecord withFormat(Format format) {
		entries = format.entries;
		areEntriesInitialized = true;
		return this;
	}

	public ManualRecord withGroupID(String groupID) {
		this.groupID = groupID;
		return this;
	}

	public void parseCSVRecord(CSVRecord csvRecord) {
		if(!areEntriesInitialized)
			throw new IllegalStateException("Run withFormat(Format) first");

		LinkedList<String> columns = new LinkedList<>();
		csvRecord.forEach(columns::add);

		String groupIDDirty = columns.removeFirst();
		if(groupIDDirty == null)
			throw new IllegalArgumentException("Error in parsing the csv record. Excel file is corrupted.");

		groupID = groupIDDirty.replaceAll("\\s+", ""); //clean white spaces
		if(groupID.isEmpty())
			throw new IllegalArgumentException("Error in parsing the csv record. Excel file is corrupted.");

		generalNotes = parseNotes(columns.removeLast());

		//convert columns to ArrayList
		List<String> columnsAsArrayList = new ArrayList<>(columns);

		//initializes entries with grade & notes information 
		for(int i = 0; i < entries.size(); ++i) {
			Entry entry = entries.get(i);
			String notes = columnsAsArrayList.get(i*2);
			String gradeLoss = columnsAsArrayList.get(i*2 + 1);
			try {
				entry.initInfo(notes, gradeLoss);
			} catch(IllegalArgumentException e) {
				System.out.println(groupID + ": Error in parsing the csv record. Excel file is corrupted."); 
				System.err.println(groupID + ": Error in parsing the csv record. Excel file is corrupted.");
			}
		}
	}

	public static List<String> parseNotes(String notesStr) {
		List<String> notes = new LinkedList<>();
		if(notesStr == null)
			return notes;

		try(BufferedReader reader = new BufferedReader(
				new StringReader(notesStr))) {

			for(String line = null;
					(line = reader.readLine()) != null;
					notes.add(line));

		} catch(IOException neverThrows) {}

		return notes;
	}

	@Override
	public String getGroupID() {
		return groupID;
	}

	//MARK: ManualRecord.Format
	public static class Format {
		public List<Entry> entries = new ArrayList<>();

		/**
		 * @deprecated Use {@link Format#makeFromCSV(List)} method.
		 */
		@Deprecated
		public Format() {}

		public static Format makeFromCSV(List<String> columns) {
			List<String> columnsAsArrayList = new ArrayList<>(columns);

			List<Entry> entries = new ArrayList<>(columnsAsArrayList.size() / 2); //each couple is an entry
			for(int i = 0; i < columnsAsArrayList.size(); i+=2) {
				String[] splitted = columnsAsArrayList.get(i).split(",", 3);

				//strips leading an trailing white spaces from task's name
				String task = splitted[1].replaceAll("^\\s+|\\s+$", "");
				String specification = "";

				//the task has a specification
				if(splitted.length == 3)
					specification = splitted[2];

				entries.add(new Entry()
						.withTask(task)
						.withSpecification(specification));
			}

			return new Format().withEntries(entries);
		}

		public Format withEntries(List<Entry> entries) {
			this.entries = entries instanceof ArrayList<?> ? entries : new ArrayList<>(entries);
			return this;
		}

		public Iterable<String> tasks() {
			return () -> 
			new Iterator<String>() {
				private Iterator<Entry> it = entries.iterator();

				@Override
				public boolean hasNext() {
					return it.hasNext();
				}

				@Override
				public String next() {
					if(!it.hasNext())
						throw new NoSuchElementException();

					return it.next().task;
				}
			};
		}
	}

	//MARK: ManualRecord.Entry
	public static class Entry implements Serializable {
		private static final long serialVersionUID = 1L;

		//MARK: Entry's fields
		public String task = "";
		public String specification = "";
		public List<String> notes = new ArrayList<>();
		public double gradeLoss = 0;
		public boolean didLosePoints = false;

		public Entry() {}

		public Entry withTask(String task) {
			this.task = task;
			return this;
		}

		public Entry withSpecification(String specification) {
			this.specification = specification;
			return this;
		}

		public Entry withNotes(List<String> notes) {
			this.notes = notes;
			return this;
		}

		public Entry withGradeLoss(double gradeLoss) {
			this.gradeLoss = gradeLoss;
			return this;
		}

		public Entry withDidLosePoints(boolean didLosePoints) {
			this.didLosePoints = didLosePoints;
			return this;
		}

		private void initInfo(String notes, String gradeLossStr) {
			withNotes(parseNotes(notes == null ? "" : notes));

			//clean white spaces
			gradeLossStr = gradeLossStr == null ? "" : gradeLossStr.replaceAll("\\s+", "");
			double gradeLoss = 0;

			if(gradeLossStr.isEmpty())
				withDidLosePoints(false)
				.withGradeLoss(0);
			else {
				try {
					gradeLoss = Double.parseDouble(gradeLossStr);//may throw NumberFormatException which extends IllegalArgumentException.

					if(gradeLoss < -EPSILON) //error in the excel file the grade must be non-negative
						throw new IllegalArgumentException("" + gradeLoss);

					if(gradeLoss > EPSILON)
						withGradeLoss(gradeLoss)
						.withDidLosePoints(true);
					else
						withDidLosePoints(false)
						.withGradeLoss(0);

				} catch(IllegalArgumentException e) {
					withDidLosePoints(false)
					.withGradeLoss(0);

					throw e;
				}	
			}
		}

		public DomContent toHTML() {
			if(!didLosePoints & notes.isEmpty())
				return null;

			DomContent gradeLossHTML = !didLosePoints ? null :
				new UnescapedText(":&ensp;-" + gradeLoss + " ponits");

			DomContent firstLineHTML = p(u("Task " + task + (!specification.isEmpty() ? " [" + specification + "]" : "")),
					gradeLossHTML);

			DomContent notesHTML = notes.isEmpty() ? null :
				p(u("Notes"), new Text(": ")
						, each(notes, note -> new UnescapedText(new Text(note).render() + br().render())));

			return div(firstLineHTML, notesHTML, br());
		}

		public double getGradeLoss() {
			return didLosePoints ? gradeLoss : 0;
		}
	}

	public DomContent toHTML() {
		return div(
				h2(u("Manual check report")).withStyle("text-align:center;"),
				iff(!generalNotes.isEmpty(), h3(p(u("General notes"),
						new Text(": "),
						each(generalNotes, generalNote ->
						new UnescapedText(new Text(generalNote).render() + br().render()))))),
				h3(each(entries, entry -> entry.toHTML())));
	}

	public Map<String, Double> getGradeLosses() {
		Map<String, Double> gradeLosses = new TreeMap<>();

		entries.forEach(entry -> {
			String task = entry.task;
			double gradeLoss = entry.getGradeLoss();
			Double mapGradeLoss = gradeLosses.get(task);

			if(mapGradeLoss == null)
				mapGradeLoss = gradeLoss;
			else mapGradeLoss += gradeLoss;

			gradeLosses.put(task, mapGradeLoss);
		});

		return gradeLosses;
	}
}
