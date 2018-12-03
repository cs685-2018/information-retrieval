package cs685.information.retrieval;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

public class Indexer {
	private IndexWriter writer;
	private IndexReader reader;
	private IndexSearcher searcher;
	private QueryParser parser;
	//private StandardAnalyzer standardAnalyzer;
	private Map<Integer, String> testDocuments;
	
	public Indexer() throws IOException {
		// Create a map of document ID to test method name
		testDocuments = new HashMap<Integer, String>();
		// New index
		StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
		File inputDir = new File("C:\\Users\\smoke\\Documents\\CS 685\\cs685-2018\\information-retrieval\\input");
        String outputDir = "C:\\Users\\smoke\\Documents\\CS 685\\cs685-2018\\information-retrieval\\output";
 
        Directory directory = FSDirectory.open(Paths.get(outputDir));
        IndexWriterConfig config = new IndexWriterConfig(standardAnalyzer);
        // Create a writer
        writer = new IndexWriter(directory, config);
        
        config.setOpenMode(OpenMode.CREATE);
        // Iterate over the entire directory
        Stack<File> inputFiles = new Stack<File>();
        inputFiles.addAll(Arrays.asList(inputDir.listFiles()));
        int i = 0;
        while (!inputFiles.isEmpty()) {
        	File path = inputFiles.pop();
            if (path.isDirectory()) {
            	// If directory, add all content within it to the stack
                inputFiles.addAll(Arrays.asList(path.listFiles()));
            } else if (path.getName().endsWith(".java")) {
            	// If a java file, parse it.
            	CompilationUnit cu = JavaParser.parse(path);
            	// Get the class name
            	List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);
            	List<String> classNames = new ArrayList<String>();
            	for (ClassOrInterfaceDeclaration classDelcaration : classes) {
            		String className = classDelcaration.getName().asString();
            		classNames.add(parseCamelCase(className).toLowerCase());
            	}
            	String classNamesContent = String.join(" ", classNames);
            	// Build a document for each method (TODO: if not @Before, etc)
            	List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);
            	// For now, just adding to indexer if beginning with "test.."
            	for (MethodDeclaration method : methods) {
            		if (method.getName().asString().matches("^test.*")) {
            			StringBuilder methodContent = new StringBuilder();
            			// Get the method's class name
            			methodContent.append(classNamesContent + "\n");
            			// Get method's name
            			String methodName = method.getName().asString();
            			methodContent.append(parseCamelCase(methodName).toLowerCase() + "\n");
            			// Get method's parameters
            			for (Parameter param : method.getParameters()) {
            				String params = param.toString().replaceAll("[^A-Za-z ]", " ").trim();
                            methodContent.append(parseCamelCase(params).toLowerCase() + " ");
                        }
            			// Get method's documentation
            			Optional<Comment> javadocComment = method.getComment();
            			if (javadocComment.isPresent()) {
            				String javadoc = javadocComment.get().getContent();
            				javadoc = javadoc.replaceAll("[^A-Za-z ]", " ").trim();
            				// TODO: build method to parse out camelcase
            				methodContent.append(parseCamelCase(javadoc).toLowerCase() + "\n");
            			}
            			// Get method's content
            			Optional<BlockStmt> methodBlock = method.getBody();
            			if (methodBlock.isPresent()) {
            				for (Statement statement : methodBlock.get().getStatements()) {
            					String statementContent = statement.toString().replaceAll("[^A-Za-z ]", " ").trim();
            					methodContent.append(parseCamelCase(statementContent).toLowerCase() + "\n");
            				}
            			}
            			// Add the method's document to the writer
        				Document document = new Document();
        				//document.add(new TextField(method.getName().asString() + " content", new StringReader(methodContent.toString())));
        				document.add(new TextField("content", new StringReader(methodContent.toString())));
        				System.out.println("Doc=" + i + ", name=" + method.getName().asString());
        				writer.addDocument(document);
        				if (testDocuments.containsValue(methodName)) {
        					System.out.println("***WARNING***: Found an overloaded test method: [" + methodName + "]");
        				}
        				testDocuments.put(i, methodName);
        				i++;
            		}
            	}
            }
            	
        }

        
        try {
        	File[] directoryListing = inputDir.listFiles();
    		if (directoryListing != null) {
    			for (File child : directoryListing) {
    				
    			}
    		} else {
    			System.out.println(inputDir.getPath() + " is not a directory!");
    			return;
    		}
        	writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
 
        // Now let's try to search for Hello
        reader = DirectoryReader.open(directory);
        searcher = new IndexSearcher(reader);
        parser = new QueryParser("content", standardAnalyzer);
	}
	
	public TopDocs query(String queryString, int numResults) throws ParseException, IOException {
        Query query = parser.parse(queryString);
        return searcher.search(query, numResults);
	}
	
	public String getDocumentById(int id) {
		return testDocuments.get(id);
	}
	
	public void close() throws IOException { 
		writer.close();
		reader.close();
	}
	
	public static String parseCamelCase(String s) {
		StringBuilder result = new StringBuilder();
		// Regex from https://stackoverflow.com/questions/7593969/regex-to-split-camelcase-or-titlecase-advanced
		for (String w : s.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
	        result.append(w);
	        result.append(" ");
	    }
		return result.toString();
	}
}
