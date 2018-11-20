package cs685.information.retrieval;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class InformationRetrieval 
{
    public static void main(String[] args) throws URISyntaxException, IOException, ParseException {
    	Indexer indexer = new Indexer();
    	String[] queries = { "Hello world", "test", "said", "great", "blah", "how is you?" };
    	for (String query : queries) {
    		TopDocs results = indexer.query(query, 5);
    		System.out.println("\""+query+"\" results: " + results.totalHits);
    		for (ScoreDoc doc : results.scoreDocs) {
    			System.out.println("\t" + doc);
    		}
    	}
    	indexer.close();
    }
}
