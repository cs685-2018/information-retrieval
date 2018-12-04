package cs685.information.retrieval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import io.reflectoring.diffparser.api.DiffParser;
import io.reflectoring.diffparser.api.UnifiedDiffParser;
import io.reflectoring.diffparser.api.model.Diff;
import io.reflectoring.diffparser.api.model.Hunk;
import io.reflectoring.diffparser.api.model.Line;
import io.reflectoring.diffparser.api.model.Range;

public class InformationRetrieval {
	private static Set<String> stopwords;
	private static Set<String> keywords;

	private static class Query {
		private String classes;
		private String methods;
		private String query;

		public Query(String classes, String methods, String query) {
			this.classes = classes;
			this.methods = methods;
			this.query = query;
		}

		public String getClasses() {
			return this.classes;
		}

		public String getMethods() {
			return this.methods;
		}

		public String getQuery() {
			return this.query;
		}

		public String getCoverages() {
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			sb.append(this.classes);
			sb.append(")#(");
			sb.append(this.methods);
			sb.append(")");
			return sb.toString();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.getCoverages());
			sb.append(" covered by query: [");
			sb.append(this.query);
			sb.append("]");
			return sb.toString();
		}
	}

	public static String removeStopwords(String s) {
		StringBuilder modifiedStr = new StringBuilder();
		for (String part : s.split("\\s+")) {
			if (!stopwords.contains(part) && part.length() > 1) {
				modifiedStr.append(part);
				modifiedStr.append(" ");
			}
		}
		return modifiedStr.toString();
	}

	public static String removeStopwordsAndKeywords(String s) {
		StringBuilder modifiedStr = new StringBuilder();
		for (String part : s.split("\\s+")) {
			// Check if len > 1 to avoid variables named "i", etc.
			if (!stopwords.contains(part) && !keywords.contains(part) && part.length() > 1) {
				modifiedStr.append(part);
				modifiedStr.append(" ");
			}
		}
		return modifiedStr.toString();
	}

	public static void main(String[] args) throws URISyntaxException, IOException, ParseException {
		// Load stopwords and keywords
		stopwords = new HashSet<String>();
		keywords = new HashSet<String>();
		File stopwordsFile = new File("stopwords.txt");
		File keywordsFile = new File("keywords.txt");
		try (BufferedReader br = new BufferedReader(new FileReader(stopwordsFile))) {
			for (String line; (line = br.readLine()) != null;) {
				stopwords.add(line.replaceAll("\\'", ""));
			}
		}
		try (BufferedReader br = new BufferedReader(new FileReader(keywordsFile))) {
			for (String line; (line = br.readLine()) != null;) {
				keywords.add(line);
			}
		}

		// Parse the diff
		DiffParser parser = new UnifiedDiffParser();
		InputStream in = new FileInputStream(
				"C:\\Users\\smoke\\Documents\\CS 685\\cs685-2018\\information-retrieval\\input\\test.diff");
		List<Diff> diffs = parser.parse(in);
		List<Query> queries = new ArrayList<Query>();
		for (Diff diff : diffs) {
			Map<String, List<Range>> classToRange = new HashMap<String, List<Range>>();
			Map<String, List<Range>> methodToRange = new HashMap<String, List<Range>>();
			String toFile = diff.getToFileName();
			System.out.println("Found diff at " + toFile);
			// Replace the prepended "b/" from with "input"/:
			List<String> fileSplit = Arrays.asList(toFile.split("/"));
			// Only parse Java files
			List<String> extensionSplit = Arrays.asList(toFile.split("\\."));
			if (extensionSplit.size() == 0
					|| !extensionSplit.get(extensionSplit.size() - 1).toLowerCase().equals("java")) {
				continue;
			}
			fileSplit.set(0, "input");
			toFile = String.join("/", fileSplit);
			System.out.println("Parsing ToFile at: [" + toFile + "]");
			// Parse the file with JavaParser
			File file = new File(toFile);
			CompilationUnit cu = JavaParser.parse(file);

			// Find line number ranges of all declared classes
			System.out.println("Classes:");
			List<ClassOrInterfaceDeclaration> classDelcarations = cu.findAll(ClassOrInterfaceDeclaration.class);
			for (ClassOrInterfaceDeclaration classDeclaration : classDelcarations) {
				System.out.println("\t" + classDeclaration.getName().asString());
				Optional<com.github.javaparser.Range> lineNumberRange = classDeclaration.getRange();
				if (lineNumberRange.isPresent()) {
					System.out.println("\t\tLine number range: " + lineNumberRange.get().begin + " to "
							+ lineNumberRange.get().end);
					String className = classDeclaration.getName().asString();
					Range range = new Range(lineNumberRange.get().begin.line,
							lineNumberRange.get().end.line - lineNumberRange.get().begin.line);
					if (!classToRange.containsKey(className)) {
						classToRange.put(className, new ArrayList<Range>());
					}
					classToRange.get(className).add(range);
				} else {
					System.out.println("\t\tLine number range does not exist");
				}
			}

			// Find line number ranges of all declared methods
			System.out.println("\nMethods:");
			List<MethodDeclaration> methodDeclarations = cu.findAll(MethodDeclaration.class);
			for (MethodDeclaration method : methodDeclarations) {
				System.out.println("\t" + method.getName().asString());
				// Line number
				Optional<com.github.javaparser.Range> lineNumberRange = method.getRange();
				if (lineNumberRange.isPresent()) {
					System.out.println("\t\tLine number range: " + lineNumberRange.get().begin + " to "
							+ lineNumberRange.get().end);
					String methodName = method.getName().asString();
					Range range = new Range(lineNumberRange.get().begin.line,
							lineNumberRange.get().end.line - lineNumberRange.get().begin.line);
					if (!methodToRange.containsKey(methodName)) {
						methodToRange.put(methodName, new ArrayList<Range>());
					}
					methodToRange.get(methodName).add(range);
				} else {
					System.out.println("\t\tLine number range does not exist");
				}
			}

			// Get all the different hunks in the diff for the current file
			for (Hunk hunk : diff.getHunks()) {
				// Determine the line number range of the hunk
				io.reflectoring.diffparser.api.model.Range lineRange = hunk.getToFileRange();
				int startLine = lineRange.getLineStart();
				int endLine = startLine + lineRange.getLineCount();
				System.out.println("Hunk start=" + startLine + ", end=" + endLine);
				// Find all classes and methods contained in these lines
				Set<String> classes = new HashSet<String>();
				Set<String> methods = new HashSet<String>();
				Set<String> unparsedClasses = new HashSet<String>();
				Set<String> unparsedMethods = new HashSet<String>();
				for (String className : classToRange.keySet()) {
					for (Range r : classToRange.get(className)) {
						int begin = r.getLineStart();
						int end = begin + r.getLineCount();
						System.out.println("Method=" + className + " (" + begin + ", " + end + ")");
						// Find if our hunk block intersects with this class's block
						if ((startLine >= begin && startLine <= end) || (endLine >= begin && endLine <= end)) {
							unparsedClasses.add(className);
							String parsedClassName = Indexer.parseCamelCase(className).toLowerCase();
							classes.add(removeStopwords(parsedClassName));
						}
					}
				}
				for (String methodName : methodToRange.keySet()) {
					for (Range r : methodToRange.get(methodName)) {
						int begin = r.getLineStart();
						int end = begin + r.getLineCount();
						System.out.println("Method=" + methodName + " (" + begin + ", " + end + ")");
						// Find if our hunk block intersects with this class's block
						if ((startLine >= begin && startLine <= end) || (endLine >= begin && endLine <= end)) {
							unparsedMethods.add(methodName);
							String parsedMethodName = Indexer.parseCamelCase(methodName).toLowerCase();
							methods.add(removeStopwords(parsedMethodName));
						}
					}
				}
				System.out.println("Hunk intersects with classes: " + classes.toString());
				System.out.println("Hunk intersects with methods: " + methods.toString());
				// Create a query based on the hunk (hunk's content, methods hunk is in, classes
				// hunk is in)
				StringBuilder query = new StringBuilder();
				query.append(String.join(" ", classes));
				query.append(String.join(" ", methods));
				List<String> toLines = new ArrayList<String>();
				List<String> fromLines = new ArrayList<String>();
				// Find all TO and FROM lines in the hunk
				for (Line line : hunk.getLines()) {
					if (line.getLineType() == Line.LineType.TO || line.getLineType() == Line.LineType.FROM) {
						if (line.getLineType() == Line.LineType.TO) {
							toLines.add(line.getContent());
						} else {
							fromLines.add(line.getContent());
						}
					}
				}
				// Add all TO lines to the query
				for (String line : toLines) {
					// Remove punctuation
					line = line.replaceAll("[^A-Za-z\\s]", " ");
					line = Indexer.parseCamelCase(line.trim()).toLowerCase();
					// Parse out stopwords and Java keywords
					query.append(removeStopwordsAndKeywords(line));
				}
				queries.add(new Query(String.join(", ", unparsedClasses), String.join(", ", unparsedMethods),
						query.toString()));
			}
		}
		System.out.println("Queries:");
		for (Query query : queries) {
			System.out.println("\t" + query);
		}
		// Loop over all our queries
		Indexer indexer = new Indexer();
		for (Query query : queries) {
			// Find top n documents
			TopDocs results = indexer.query(query.getQuery(), 5);
			System.out.println("Query: [" + query.getQuery() + "]");
			System.out.println("\tCovers: " + query.getCoverages());
			System.out.println("\tResults: " + results.totalHits);
			for (ScoreDoc doc : results.scoreDocs) {
				System.out.println("\t" + doc);
				String docName = indexer.getDocumentById(doc.doc);
				System.out.println("\tName=[" + docName + "]");
			}
		}
		indexer.close();
	}
}
