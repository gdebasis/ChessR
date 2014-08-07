/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package yacql;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author Debasis
 */
public class ResultSets implements Serializable {
    HashMap<String, ResultSet> rmap;    

    public ResultSets() {
        rmap = new HashMap<String, ResultSet>(100);
    }
    
    public void addResultSet(ResultSet rs) {
        rmap.put(rs.query, rs);
    }

    public ResultSet get(String query) {
        return rmap.get(query);
    }
    
    public static ResultSets load(String fileName) {
        ResultSets rsets = null;
        try {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            rsets = (ResultSets) in.readObject();
            in.close();
            fileIn.close();
        }
        catch (Exception i) {
            i.printStackTrace();
        }
        return rsets;
    }
    
    public void save(String fileName) {
        try {
            FileOutputStream fileOut = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
        }
        catch (Exception i) {
            i.printStackTrace();
        }        
    }
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (Map.Entry<String, ResultSet> entry : rmap.entrySet()) {
            buff.append(entry.getValue().toString()).append("\n");
        }
        return buff.toString();
    }
    
    public void evaluate() {
        float n = rmap.entrySet().size();
        float map = 0, pAt5 = 0, ndcg = 0;        
        for (Map.Entry<String, ResultSet> entry : rmap.entrySet()) {
            ResultSet rs = entry.getValue();
            rs.computeAll();
            map += rs.map;
            pAt5 += rs.pAt5;
            ndcg += rs.ndcg;
            System.out.println(rs.map + ", " + rs.pAt5 + ", " + rs.ndcg);
        }
        System.out.println("---");
        System.out.println(map/n + ", " + pAt5/n + ", " + ndcg/n);        
    }
    
    public static void main(String[] args) {
        String propFile = "web/init.properties";
        if (args.length > 0) {
            propFile = args[0];
        }
        try {
            Properties prop = new Properties();
            prop.load(new FileReader(propFile));
            String fileName = prop.getProperty("rr_file");
            ResultSets rsets = ResultSets.load(fileName);
            System.out.println(rsets.toString());
            rsets.evaluate();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
