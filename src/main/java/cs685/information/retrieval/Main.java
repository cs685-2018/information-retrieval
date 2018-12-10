package cs685.information.retrieval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;

import io.reflectoring.diffparser.api.DiffParser;
import io.reflectoring.diffparser.api.UnifiedDiffParser;
import io.reflectoring.diffparser.api.model.Diff;

public class Main {
	public static void main(String[] args) throws ParseException, IOException, InterruptedException {
		String projectName = "input";
		File root = new File("input");
		File diffFile = new File("input/test.diff");
		
		// Get the list of diffs
    	DiffParser parser = new UnifiedDiffParser();
        InputStream in;
		try {
			in = new FileInputStream(diffFile);
		} catch (FileNotFoundException e) {
			System.out.println("File " + diffFile.getName() + " not found at " + diffFile.getAbsolutePath());
			return;
		}
        List<Diff> diffs = parser.parse(in);
        
        InformationRetriever ir = new InformationRetriever(root, diffs, projectName);
		
		int n = 5;
		Set<String> testDocs = ir.getTestDocuments(n);
		System.out.println("Documents selected:");
		for (String doc : testDocs) {
			System.out.println("\t" + doc);
		}
	}
}
