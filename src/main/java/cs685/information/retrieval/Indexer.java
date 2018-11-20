package cs685.information.retrieval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

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

public class Indexer {
	private IndexWriter writer;
	private IndexReader reader;
	private IndexSearcher searcher;
	private QueryParser parser;
	//private StandardAnalyzer standardAnalyzer;
	
	public Indexer() throws IOException {
		// New index
		StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
		File inputDir = new File("C:\\Users\\smoke\\Documents\\CS 685\\cs685-2018\\information-retrieval\\input");
        String outputDir = "C:\\Users\\smoke\\Documents\\CS 685\\cs685-2018\\information-retrieval\\output";
 
        Directory directory = FSDirectory.open(Paths.get(outputDir));
        IndexWriterConfig config = new IndexWriterConfig(standardAnalyzer);
        config.setOpenMode(OpenMode.CREATE);
        
        // Create a writer
        writer = new IndexWriter(directory, config);
        try {
        	File[] directoryListing = inputDir.listFiles();
    		if (directoryListing != null) {
    			for (File child : directoryListing) {
    				BufferedReader br = new BufferedReader(new FileReader(child));
    				Document document = new Document();
    				document.add(new TextField("content", br));
    				writer.addDocument(document);
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
	
	public void close() throws IOException { 
		writer.close();
		reader.close();
	}
}
