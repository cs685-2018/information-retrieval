package cs685.information.retrieval;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 
 * @author Ryan
 *
 */
public class IndexManager {
	private transient Indexer instance;
	private File root;
	private String projectName;
	private Set<String> filesToUpdate;
	
	/**
	 * 
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private synchronized Indexer getIndexer() throws IOException, InterruptedException {
		if (instance == null) {
			instance = new Indexer(root, projectName, filesToUpdate);
		}
		return instance;
	}
	
	/**
	 * 
	 * @param root
	 * @param projectName
	 * @param filesToUpdate
	 */
	public IndexManager(File root, String projectName, Set<String> filesToUpdate) {
		this.root = root;
		this.projectName = projectName;
		this.filesToUpdate = filesToUpdate;
	}
	
	/**
	 * 
	 * @param query
	 * @param n
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public List<TestCase> getHits(String query, int n) throws IOException, InterruptedException {
        List<TestCase> hits = getIndexer().getHits(query, n);
        return hits;
    }
	
	public void close() throws IOException, InterruptedException {
		getIndexer().close();
	}
}
