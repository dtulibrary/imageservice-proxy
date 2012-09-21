package dk.dtu.dtic.gazo;

import java.io.IOException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.commons.io.IOUtils;

/**
 * Servlet implementation class GazoProxyServlet
 */
public class GazoProxyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	private static String gazoRootUrl;
	private static String gazoApiKey;
	private static HttpClient httpClient;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GazoProxyServlet() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    public void init() throws ServletException {
    	super.init();
    	try {
    		Context ctx = (Context) new InitialContext().lookup("java:comp/env");
    		gazoRootUrl = (String) ctx.lookup("dadscis.imageServiceUrl");
    		gazoApiKey    = (String) ctx.lookup("dadscis.apiKey");
    		PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
    		cm.setMaxTotal(100);
    		httpClient = new DefaultHttpClient(cm);//[configure HttpClient with PoolingClientConnectionManager]
    	} catch (NamingException e) {
			e.printStackTrace();
		}
    }

    public void destroy() {
        //[shutdown PoolingClientConnectionManager]
    	httpClient.getConnectionManager().shutdown();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
	    //String servletPath = request.getServletPath();   // /servlet/MyServlet
	    String pathInfo = request.getPathInfo();         // /a/b;c=123
	    //String gazoUrl = gazoRootUrl+servletPath+"/"+gazoApiKey+pathInfo;//{gazoRootUrl + gazoApiKey + path and querystring from request}
	    String gazoUrl = gazoRootUrl+"/"+gazoApiKey+pathInfo;//{gazoRootUrl + gazoApiKey + path and querystring from request}
	    //System.out.println("Constructed url: "+gazoUrl);
      	HttpResponse gazoResponse;
		try {
			gazoResponse = httpClient.execute(new HttpGet(gazoUrl));
			//System.out.println("Executed url: "+gazoUrl);
	      	//[copy response code, content type, cache-control headers, other relevant headers from gazoResponse to response]
	      	response.setContentLength((int)gazoResponse.getEntity().getContentLength());
	      	response.setContentType(gazoResponse.getEntity().getContentType().getValue());
	      	response.setStatus(gazoResponse.getStatusLine().getStatusCode());
	      	IOUtils.copy(gazoResponse.getEntity().getContent(), response.getOutputStream());
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	*/
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request,response);
	} 
	
}
