/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package yacql;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import org.apache.lucene.search.ScoreDoc;



/**
 *
 * @author Debasis
 * The class for storing the retrieved results and the relevance values
 */
class ScoreDocRel implements Serializable, Comparable {
    int      doc;
    float    score;
    int      rel;   // 5 point relevance

    public ScoreDocRel(ScoreDoc sd) {
        this.doc = sd.doc;
        this.score = sd.score; 
        this.rel = 0;
    }

    @Override
    public int compareTo(Object t) {
        ScoreDocRel that = (ScoreDocRel)t;
        return this.rel < that.rel? 1 : this.rel == that.rel? 0 : -1;
    }
}

public class ResultSet implements Serializable {
    String query;
    int    nretr;
    List<ScoreDocRel> hits;
    
    transient float ndcg;
    transient float map;
    transient float pAt5;
    transient int   numRel;
    
    public ResultSet(String query, ScoreDoc[] scoreDocs) {
        this.query = query;
        hits = new Vector<ScoreDocRel>(scoreDocs.length);
        
        for (int i = 0; i < scoreDocs.length; i++) {
            ScoreDoc thisScoreDoc = scoreDocs[i];
            hits.add(new ScoreDocRel(thisScoreDoc));
        }
        nretr = scoreDocs.length;
    }
    
    public int getNumRet() {
        return nretr;
    }
    
    public void setRel(int offset, int rel) {
        ScoreDocRel sdRel = hits.get(offset);
        if (sdRel != null) {
            sdRel.rel = rel;
        }
    }

    public String getQuery() {
        return this.query;
    }
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < hits.size(); i++) {
            ScoreDocRel sdrel = this.hits.get(i);
            buff.append(this.query).append("\t");
            buff.append(sdrel.doc).append("\t");
            buff.append(sdrel.rel).append("\t");
            buff.append(sdrel.score).append("\n");
        }
        return buff.toString();
    }

    public void computeAll() {
        for (int i = 0; i < this.hits.size(); i++) {
            ScoreDocRel thisRcd = this.hits.get(i);
            if (thisRcd.rel > 0)
                numRel++;
        }
        this.map = computeMAP();
        this.pAt5 = computePAt5();
        this.ndcg = computeNDCG();
    }
    
    float computeMAP() {
        int numRelSeen = 0;
        float prec = 0;
        for (int i = 0; i < this.hits.size(); i++) {
            ScoreDocRel thisRcd = this.hits.get(i);
            if (thisRcd.rel == 0)
                continue;
            numRelSeen++;
            prec = numRelSeen/(float)(i+1);
        }
        prec /= (float)numRel;        
        return prec;
    }
    
    float computeDCG(List<ScoreDocRel> hits) {
        float dcgSum = 0;
        for (int i = 0; i < hits.size(); i++) {
            ScoreDocRel thisRcd = hits.get(i);
            int twoPowerRel = 1<<(thisRcd.rel);
            float dcg = (twoPowerRel - 1)/(float)(Math.log(i+2)/Math.log(2));
            dcgSum += dcg;
        }
        return dcgSum;
    }
    
    float computeNDCG() {
        float dcg = computeDCG(hits);
        
        Vector<ScoreDocRel> idealSDRels = new Vector<ScoreDocRel>(hits);
        Collections.sort(idealSDRels);
                
        float idcg = computeDCG(idealSDRels);
        
        return dcg/idcg;
    }
    
    public String htmlEvalString() {
        StringBuffer buff = new StringBuffer();
        buff.append("<table> <tr><th>MAP</th> <th>P@5</th> <th>NDCG</th> </tr>"); 
        buff.append("<tr>");
        buff.append("<td>").append(this.map).append("</td>");
        buff.append("<td>").append(this.pAt5).append("</td>");
        buff.append("<td>").append(this.ndcg).append("</td>");
        buff.append("</tr></table>");
        return buff.toString();
    }
    
    float computePAt5() {
        int numRelSeen = 0;
        float prec = 0;
        int nretr = Math.min(hits.size(), 5);
        for (int i = 0; i < nretr; i++) {
            ScoreDocRel thisRcd = this.hits.get(i);
            if (thisRcd.rel == 0)
                continue;
            numRelSeen++;
        }
        prec = numRelSeen/(float)nretr;        
        return prec;
    }
    
}
