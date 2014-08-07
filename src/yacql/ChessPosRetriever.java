/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package yacql;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import pgnparse.PGNGame;
import pgnparse.PGNMove;
import pgnparse.PGNParseException;
import pgnparse.PGNSource;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.payloads.AveragePayloadFunction;
import org.apache.lucene.search.payloads.PayloadFunction;
import org.apache.lucene.search.payloads.PayloadTermQuery;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.util.Version;


/**
 *
 * @author dganguly
 */

class ScoreComparator implements Comparator {

    @Override
    public int compare(Object t, Object t1) {
        ScoreDoc thisScoreDoc = (ScoreDoc)t;
        ScoreDoc thatScoreDoc = (ScoreDoc)t1;
        return thisScoreDoc.score < thatScoreDoc.score? 1 : thisScoreDoc.score == thatScoreDoc.score? 0 : -1;
    }    
}

public class ChessPosRetriever {

    IndexSearcher searcher;
    Properties prop;
    int numWanted;
    PGNMove query;
    
    public ChessPosRetriever(String propFile) throws Exception {
        String index_dir = null;
        prop = new Properties();
        prop.load(new FileReader(propFile));
        index_dir = prop.getProperty("index");
        
        try {
            File indexDir = new File(index_dir);
            IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir));
            searcher = new IndexSearcher(reader);
            ///float lambda = Float.parseFloat(prop.getProperty("lambda", "0.9"));
            ///searcher.setSimilarity(new LMJelinekMercerSimilarity(lambda));
            searcher.setSimilarity(new BM25PayloadSimilarity());
            numWanted = Integer.parseInt(prop.getProperty("num_wanted", "1000"));    
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public IndexSearcher getSearcher() {
        return searcher;
    }
    
    BooleanQuery constructQuery(String boardState) {
        BooleanQuery q = new BooleanQuery();
        StringTokenizer st = new StringTokenizer(boardState, " \t\r\n");
        PayloadFunction pf = new AveragePayloadFunction();
        
        while (st.hasMoreTokens()) {
            String thisToken = st.nextToken();
            Term thisTerm = new Term(ChessPositionIndexer.GAME_POS_LABEL, thisToken);
            q.add(new PayloadTermQuery(thisTerm, pf), BooleanClause.Occur.SHOULD);            
        }        
        return q;
    }
    
    // Construct queries from the PGN query file
    public List<Query> constructQueries() throws Exception {
        // Create an instance of the PGN parser
        String pgnQryFile = prop.getProperty("query_file");
        PGNSource source = new PGNSource(new File(pgnQryFile));
        PGNGame game = null;
        PGNMove move = null;
        String boardState = null;
        List<Query> queryList = new LinkedList<Query>();
        
        Iterator<PGNGame> gameIterator = source.listGames().iterator();
        
        while (gameIterator.hasNext()) {            
            game = gameIterator.next();
            Iterator<PGNMove> movesIterator = game.getMovesIterator();
            while (movesIterator.hasNext()) {
                move = movesIterator.next();
            }
            // add the board state resulting from the last move
            // as a query
            boardState = move.graphEncodingForQuery();
            ///System.out.println(boardState);
            queryList.add(constructQuery(boardState));
        }
        return queryList;
    }
    
    public ScoreDoc[] filter(ScoreDoc[] docs) throws Exception {
        HashMap<Integer, ScoreDoc> scoreMap = new HashMap<Integer, ScoreDoc>();
        ScoreDoc[] newDocs;
        for (int i = 0; i < docs.length; i++) {
            int docId = docs[i].doc;
            Document d = searcher.doc(docId);
            int gameId = Integer.parseInt(d.get(ChessPositionIndexer.GAME_ID_LABEL));
            if (scoreMap.get(gameId) == null) {
                scoreMap.put(gameId, docs[i]);
            }
        }

        newDocs = scoreMap.values().toArray(new ScoreDoc[0]);
        Arrays.sort(newDocs, new ScoreComparator());
        return newDocs;
    }
    
    public ScoreDoc[] retrievePGNQuery(String pgnQuery) throws Exception {
        TopScoreDocCollector collector = TopScoreDocCollector.create(numWanted, true);
        ScoreDoc[] hits = null, uniqueGameHits = null;
        TopDocs topDocs = null;
        PGNSource source = new PGNSource(pgnQuery + "\n1/2-1/2\n");
        PGNGame game = source.listGames().get(0);
        PGNMove move = null;
        String boardState = null;
        
        Iterator<PGNMove> movesIterator = game.getMovesIterator();
        while (movesIterator.hasNext()) {
            move = movesIterator.next();
        }
        
        boardState = move.graphEncodingForQuery();
        query = move;
        Query query = constructQuery(boardState);
        
        searcher.search(query, collector);
        topDocs = collector.topDocs();
        hits = topDocs.scoreDocs;

        uniqueGameHits = filter(hits);

        System.out.println("Found " + uniqueGameHits.length + " hits.");
        for(int i = 0; i < uniqueGameHits.length; ++i) {
            int docId = uniqueGameHits[i].doc;
            Document d = searcher.doc(docId);
            System.out.println((i + 1) + "\t" +
                    d.get(ChessPositionIndexer.LUCENE_DOC_ID) + "\t" +
                    d.get(ChessPositionIndexer.GAME_INFO_LABEL) + "\t" +
                    d.get(ChessPositionIndexer.GAME_ID_LABEL) + "\t" +
                    d.get(ChessPositionIndexer.GAME_STATE_ID_LABEL) + "\t" +
                    d.get(ChessPositionIndexer.GAME_FEN_LABEL) + "\t" +
                    uniqueGameHits[i].score);
        }
        return uniqueGameHits;
    }

    public PGNMove getQuery() {
        return query;
    }
    
    /* Decode a given chess position into a Lucene query and retrieve results */
    public void retrieveAll() throws Exception {
        TopScoreDocCollector collector = TopScoreDocCollector.create(numWanted, true);
        ScoreDoc[] hits = null, uniqueGameHits = null;
        TopDocs topDocs = null;
        int qcount = 0;
        
        for (Query query : constructQueries()) {
            System.out.println("Query: " + (++qcount));
            ///System.out.println(query);
            searcher.search(query, collector);
            topDocs = collector.topDocs();
            hits = topDocs.scoreDocs;
        
            uniqueGameHits = filter(hits);
            
            System.out.println("Found " + uniqueGameHits.length + " hits.");
            for(int i = 0; i < uniqueGameHits.length; ++i) {
                int docId = uniqueGameHits[i].doc;
                Document d = searcher.doc(docId);
                System.out.println((i + 1) + "\t" +
                        d.get(ChessPositionIndexer.LUCENE_DOC_ID) + "\t" +
                        d.get(ChessPositionIndexer.GAME_INFO_LABEL) + "\t" +
                        d.get(ChessPositionIndexer.GAME_ID_LABEL) + "\t" +
                        d.get(ChessPositionIndexer.GAME_STATE_ID_LABEL) + "\t" +
                        d.get(ChessPositionIndexer.GAME_FEN_LABEL) + "\t" +
                        uniqueGameHits[i].score);
            }        
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String propFile = "web/init.properties";
        if (args.length > 0) {
            propFile = args[0];
        }
        
        try {
            ChessPosRetriever chessPosRetriever = new ChessPosRetriever(propFile);
            chessPosRetriever.retrieveAll();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
