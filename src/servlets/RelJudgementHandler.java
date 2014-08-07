/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import yacql.ResultSet;
import yacql.ResultSets;

/**
 *
 * @author Debasis
 */
public class RelJudgementHandler extends HttpServlet {

    Properties prop;
    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            String propFile = config.getInitParameter("configFile");
            if (propFile == null) {
                System.err.println("Servlet Configuration file missing");
            }
            prop = new Properties();
            prop.load(new FileReader(propFile));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
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
            if (false) {
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet RelJudgementHandler</title>");            
            out.println("</head>");
            out.println("<body>");
            }
            
            String relFileName = prop.getProperty("rr_file");            
            ResultSets resultSets = ResultSets.load(relFileName);
            
            if (resultSets == null) {
                // No previously saved results
                resultSets = new ResultSets();
            }
            
            session = request.getSession();
            ResultSet rs = (ResultSet)session.getAttribute("results");
            if (rs == null) {
                out.println("ERROR: No resultset found in session data");
                return;
            }
                        
            int numRetrieved = rs.getNumRet();
            for (int i = 0; i < numRetrieved; i++) {
                int rel = Integer.parseInt(request.getParameter("relboard" + i));
                rs.setRel(i, rel);
            }
            
            // Finally save the resultset
            resultSets.addResultSet(rs);
            
            RequestDispatcher dispatcher = request.getRequestDispatcher("/index.jsp?displayAlert=true" );
            dispatcher.forward(request, response);
            
            //rs.computeAll();
            //out.println(rs.htmlEvalString());
            
            if (false) {
            out.println("</body>");
            out.println("</html>");
            out.flush();
            }
            
            resultSets.save(prop.getProperty("rr_file"));
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
