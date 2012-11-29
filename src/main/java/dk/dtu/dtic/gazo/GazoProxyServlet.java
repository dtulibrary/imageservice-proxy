package dk.dtu.dtic.gazo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

/**
 * Servlet implementation class GazoProxyServlet
 */
public class GazoProxyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static String gazoRootUrl;
	private static String gazoApiKey;
	private static HttpClient httpClient;
	private static String noImagePath;
	private static String noImageContentType;
	private static boolean useNoImage;

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
			gazoRootUrl = (String) ctx.lookup("dadscis.image.serviceUrl");
			gazoApiKey    = (String) ctx.lookup("dadscis.image.apiKey");
			String uni    = (String) ctx.lookup("dadscis.image.useNoImage");
			useNoImage = (uni.toLowerCase().equals("yes")) ? true : false;
			if(useNoImage == true){
				noImagePath    = (String) ctx.lookup("dadscis.image.noImage");
				noImageContentType = (String) ctx.lookup("dadscis.image.noImageContentType");
			} else {
				noImagePath = "";
				noImageContentType = "";
			}
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
		String pathInfo = request.getPathInfo();
		String gazoUrl = gazoRootUrl+"/"+gazoApiKey+pathInfo;
		HttpResponse gazoResponse = null;
		try {
			boolean connectException = false;
			try{
				gazoResponse = httpClient.execute(new HttpGet(gazoUrl));
			} catch(ConnectException e){
				connectException = true;
			}
			//[copy response code, content type, cache-control headers, other relevant headers from gazoResponse to response]		
			if(gazoResponse != null){
				System.out.println("Got request! Status = "+gazoResponse.getStatusLine().getStatusCode());
			}
			if(useNoImage == true && (connectException || gazoResponse == null || (gazoResponse.getStatusLine().getStatusCode() == 404))){
				try {
					// Read from a file
				    BufferedImage bufferedImage = ImageIO.read(new File(noImagePath));
				    ByteArrayOutputStream baos = new ByteArrayOutputStream();
				    ImageIO.write(bufferedImage, "png", baos);
				    InputStream is = new ByteArrayInputStream(baos.toByteArray());
					response.setContentLength(baos.toByteArray().length);
					response.setContentType(noImageContentType);
					response.setStatus(200);
					IOUtils.copy(is, response.getOutputStream());
				} catch (IOException e) {
					response.setStatus(404);
				}
			} else {
				response.setContentLength((int)gazoResponse.getEntity().getContentLength());
				response.setContentType(gazoResponse.getEntity().getContentType().getValue());
				response.setStatus(gazoResponse.getStatusLine().getStatusCode());
				IOUtils.copy(gazoResponse.getEntity().getContent(), response.getOutputStream());
			}
		} catch (ClientProtocolException e) {
			
		} catch (IOException e) {
			
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request,response);
	} 

}
