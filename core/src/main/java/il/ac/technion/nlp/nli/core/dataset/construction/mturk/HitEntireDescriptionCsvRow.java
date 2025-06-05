package il.ac.technion.nlp.nli.core.dataset.construction.mturk;

import ofergivoli.olib.io.csv.supercsv.CsvSettings;
import ofergivoli.olib.io.csv.supercsv.CsvWriter;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class HitEntireDescriptionCsvRow {

	public String hitId;
	public String hitEntireDescription;
	
	public String getHitId() {
		return hitId;
	}

	public void setHitId(String hitId) {
		this.hitId = hitId;
	}

	public String getHitEntireDescription() {
		return hitEntireDescription;
	}

	public void setHitEntireDescription(String hitEntireDescription) {
		this.hitEntireDescription = hitEntireDescription;
	}

	public HitEntireDescriptionCsvRow(String hitId, String hitEntireDescription) {
		this.hitId = hitId;
		this.hitEntireDescription = hitEntireDescription;
	}

	public static void writeRowsToCsv(File outputCsv, List<HitEntireDescriptionCsvRow> rows) {
		CsvWriter.writeAllObjectsToFile(getCsvSettings(), outputCsv, rows);
	}

	private static CsvSettings getCsvSettings() {
		List<String> columns = Arrays.asList("hitId", "hitEntireDescription");
		return new CsvSettings(columns);
	}
}
