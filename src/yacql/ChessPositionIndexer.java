package yacql;

import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.Properties;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import pgnparse.*;

public class ChessPositionIndexer {
    
    private Analyzer analyzer;
    Properties prop;
    int gameId;
    int luceneDocId;
    
    public static final String LUCENE_DOC_ID = "docid";
    public static final String GAME_ID_LABEL = "id";
    public static final String GAME_INFO_LABEL = "info";
    public static final String GAME_STATE_ID_LABEL = "posid";
    public static final String GAME_POS_LABEL = "pos";
    public static final String GAME_FEN_LABEL = "fen";
    
    public ChessPositionIndexer(String propFile) throws Exception {
        analyzer = new PayloadAnalyzer(); //WhitespaceAnalyzer(Version.LUCENE_CURRENT);
        prop = new Properties();
        prop.load(new FileReader(propFile));
        gameId = 1;
        luceneDocId = 1;
    }
    
    public void indexAll() {
        String index_dir, data_dir = null;
        data_dir = prop.getProperty("coll");
        index_dir = prop.getProperty("index");
        
    	IndexWriter writer = null;
        try {
            File dataDir = new File(data_dir);
            File indexDir = new File(index_dir);
            
            IndexWriterConfig iwcfg = new IndexWriterConfig(Version.LUCENE_CURRENT, analyzer);
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            writer = new IndexWriter(FSDirectory.open(indexDir), iwcfg);
            
            indexDirectory(writer, dataDir);

            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
	
    private void indexDirectory(IndexWriter writer, File dir) 
        throws Exception {
        File[] files = dir.listFiles();
        for (int i=0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                System.out.println("Indexing directory " + f.getName());
                indexDirectory(writer, f);  // recurse
            } else { 
                indexFile(writer, f);
            }
        }
    }

    private void indexFile(IndexWriter writer, File f) throws Exception {
        
        String name = f.getName();
        
        if (name.charAt(0) == '.')
            return;
        if (name.charAt(name.length()-1) == '~')
            return;
        
        System.out.println("Indexing PGN file " + name);
        String content = null;

        Document doc = null;
        StringBuffer buff = null;
        String fen = null;
        
        // Create an instance of the PGN parser
        PGNSource source = new PGNSource(f);
        PGNGame game = null;
        PGNMove move = null;
        
        Iterator<PGNGame> gameIterator = source.listGames().iterator();
        int gameCount = 1;
        int numSkip = Integer.parseInt(prop.getProperty("numskip", "12"));
        
        while (gameIterator.hasNext()) {
            
            game = gameIterator.next();
             
            // For now store the player names and result as
            // meta-data. We are not going to search on the metadata
            buff = new StringBuffer();
            buff.append(game.getTag("White"));
            buff.append(":");
            buff.append(game.getTag("Black"));
            buff.append(":");
            buff.append(game.getTag("Result"));
                        
            Iterator<PGNMove> movesIterator = game.getMovesIterator();
            int num = -1;
            numSkip = numSkip - 1;
            
            while (movesIterator.hasNext()) {
                move = movesIterator.next();
                ///System.out.println(move.getMove());
                ///move.printBoard();
                
                fen = move.getFEN();
                ///System.out.println(fen);
                
                num++;
                
                // Do not index the initial 'numskip' board positions
                // because these will amount to very large inverted lists.
                // A reasonable value of 'numskip' is higher than 12.
                if ( (num>>1) < numSkip ) {
                    ///System.out.println("Ignoring move: " + (num>>1) + "." + num%2);
                    continue;
                }
                
                if (move.isEndGameMarked())
                    break;                
                
                doc = new Document();
                content = move.graphEncoding();
                
                ///System.out.println("content:");
                ///System.out.println(content);
                
                doc.add(new Field(LUCENE_DOC_ID, String.valueOf(luceneDocId++), Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field(GAME_ID_LABEL, String.valueOf(gameId), Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field(GAME_STATE_ID_LABEL, String.valueOf(num), Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field(GAME_INFO_LABEL, buff.toString(), Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field(GAME_FEN_LABEL, fen, Field.Store.YES, Field.Index.NOT_ANALYZED));
                doc.add(new Field(GAME_POS_LABEL, content, Field.Store.YES, Field.Index.ANALYZED));
                
		writer.addDocument(doc);
		///System.out.println("Added move " + (num>>1) + "." + num%2 + "  of game " + gameCount + " to index");
            }
            gameCount++;
            gameId++;
        }
    }
    
    public static void main(String[] args) {
        String propFile = "web/init.properties";
        if (args.length > 0) {
            propFile = args[0];
        }
        
        try {
            ChessPositionIndexer chessPosIndexer = new ChessPositionIndexer(propFile);
            chessPosIndexer.indexAll();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
} 

