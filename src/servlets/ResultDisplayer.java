/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import yacql.ChessPosRetriever;
import yacql.ChessPositionIndexer;
import static yacql.ChessPositionIndexer.GAME_STATE_ID_LABEL;
import yacql.ResultSet;

/**
 *
 * @author Debasis
 */
public class ResultDisplayer extends HttpServlet {

    ChessPosRetriever retriever;
    String propFile;
    static final String[] relevanceLabels = {"Extremely Relevant", "Highly Relevant",
                    "Relevant", "Somewhat Relevant", "Not Relevant"};
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            propFile = config.getInitParameter("configFile");
            if (propFile == null) {
                System.err.println("Servlet Configuration file missing");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private String resultSetToHTML(ScoreDoc[] rs) throws Exception {
        StringBuffer buff = new StringBuffer();
        IndexSearcher searcher = retriever.getSearcher();
        String queryFen = retriever.getQuery().getFEN();
        
        // the query board position
        if (false) {
        buff.append("<table><tr><td align=\"center\">");
        buff.append("Query").append("</td><td>");
        buff.append("<div id=\"qboard\"></div></td></tr></table><hr><br>");
        }
        
        buff.append("<table border=\"1\">");
        
        for (int i = 0; i < rs.length; ++i) {
            int docId = rs[i].doc;
            Document d = searcher.doc(docId);
            buff.append("<tr><td>");
            buff.append("The Query Position: <br>");
            buff.append("<div id=\"qboard" + i + "\"></div></td><td align=\"center\">");
            buff.append(d.get(ChessPositionIndexer.GAME_INFO_LABEL));
            buff.append(" (Pos#: ").append(d.get(ChessPositionIndexer.GAME_STATE_ID_LABEL)).append(")");
            //buff.append("<br>");
            //buff.append(d.get(ChessPositionIndexer.GAME_FEN_LABEL));
            buff.append("</td><td>");
            buff.append("Position Retrieved @ Rank : ").append(i+1).append("<br>");
            buff.append("<div id=\"board").append(i).append("\"></div>");
            buff.append("</td>").append("<td>");
            for (int j = 0; j < relevanceLabels.length; j++) {
                buff.append("<input type=\"radio\" name=\"relboard")
                        .append(i).append("\" value=\"")
                        .append(relevanceLabels.length - j - 1).append("\">")
                        .append(relevanceLabels[j]).append("<br>");
            }
            buff.append("</td>").append("</tr>");
        }
        buff.append("</table>");

        buff.append("<input type=\"button\" value=\"Submit Relevance Judgements\" onclick=\"validate()\">");
        buff.append("</form>\n");
        
        buff.append("<script type=\"text/javascript\">")
        .append("var chessObj = new DHTMLGoodies.ChessFen();")
        .append("chessObj.setSquareSize(30);");

        // Query board
        if (false) {
        buff.append("chessObj.loadFen('")
            .append(queryFen)
            .append("', ")
            .append("'qboard');\n");
        }
        
        for (int i = 0; i < rs.length; ++i) {
            int docId = rs[i].doc;
            Document d = searcher.doc(docId);
            buff.append("chessObj.loadFen('")
                .append(queryFen)
                .append("', ")
                .append("'qboard")
                .append(i)
                .append("');")
                .append("\n");            
            buff.append("chessObj.loadFen('")
                .append(d.get(ChessPositionIndexer.GAME_FEN_LABEL))
                .append("', ")
                .append("'board")
                .append(i)
                .append("');")
                .append("\n");            
        }
        
        return buff.toString();
    }
    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        HttpSession session;
        
        try {
            /* TODO output your page here. You may use following sample code. */
            String pgnQuery = request.getParameter("PgnMoveText");
            ScoreDoc[] resultSet = null;
        
            session = request.getSession();
            
            out.println("<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"css/chess.css\"></link>");
            out.println("<script type=\"text/javascript\" src=\"js/ChessFen.js\"></script>");
            out.println("</head> <body>");
            out.println("<form name=\"relform\" action=\"RelJudgementHandler\">");
            
            retriever = new ChessPosRetriever(propFile);
            resultSet = retriever.retrievePGNQuery(pgnQuery);
            
            // Store the results and the query in this session
            ResultSet rs = new ResultSet(pgnQuery, retriever.getQuery().getFEN(), resultSet);
            session.setAttribute("results", rs);
            
            String html = resultSetToHTML(resultSet);
            
            out.println(html);
            
            out.println("function validate() {");
	    out.println("var valid = false;");
	    out.println("for (var i = 0; i < " + resultSet.length + "; i++) {");
            out.println("valid = false;");
            out.println("var chkObj = document.getElementsByName(\"relboard\" + i);");
	    out.println("for (var j=0; j < chkObj.length; j++) {");
	    out.println("if (chkObj[j].checked) {");
	    out.println("valid = true; break; } }");
	    out.println("if (!valid) {break;}}");
	    out.println("if (valid) {document.relform.submit();} " +
                    "else {alert(\"Can't submit incomplete relevance judgement\")} }");

            out.println("</script>");        
            out.println("</body></html>");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finally {            
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
