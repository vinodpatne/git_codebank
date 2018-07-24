package com.barclays.iportal.cros;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.log4j.Logger;

import com.barclays.proxyserver.ProxyServer;

/**
 * Servlet implementation class CROSMediator
 */
public class CROSMediator extends HttpServlet {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(CROSMediator.class);

    private CommonHttpClientExecutor commonHttpClientExecutor = new CommonHttpClientExecutor();

    private static final long serialVersionUID = 1L;

    private static ProxyServer proxyServer = null;

    /**
     * Default constructor.
     */
    public CROSMediator() {
    }

    /**
     *
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
	super.init(config);
	// System.out.println("getContextPath=" + config.getServletContext().getContextPath());
	// proxyServer = new ProxyServer(getLocalHostName(), getInitParams(config));
	// proxyServer.startServer();
    }

    private String getLocalHostName() {
	String hostname = "localhost";
	java.net.InetAddress localMachine = null;
	try {
	    localMachine = java.net.InetAddress.getLocalHost();
	    hostname = localMachine.getHostName();
	    System.out.println("Hostname of local machine: " + hostname);
	} catch (UnknownHostException e) {
	    e.printStackTrace();
	}
	return hostname;
    }

    private Map<String, String> getInitParams(ServletConfig config) {
	Map<String, String> map = new HashMap<String, String>();
	Enumeration<String> enumParams = config.getInitParameterNames();

	String parameName = "";
	String parameValue = "";
	while (enumParams.hasMoreElements()) {
	    parameName = enumParams.nextElement();
	    parameValue = config.getInitParameter(parameName);
	    map.put(parameName.trim(), parameValue.trim());
	}
	return map;
    }

    /**
     * @see javax.servlet.GenericServlet#destroy()
     */
    @Override
    public void destroy() {
	proxyServer.stopServer();
	super.destroy();
    }

    /**
     * @see javax.servlet.http.HttpServlet#doOptions(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	// super.doOptions(request, response);
	// response.setHeader("Access-Control-Allow-Origin", getOrigin(request));
	response.setHeader("Access-Control-Allow-Origin", "*");
	response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
	response.setHeader("Access-Control-Max-Age", "86400");
	// response.setHeader("Access-Control-Allow-Credentials", "true");
	response.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	Map<String, String> requestParameterMap = new HashMap<String, String>();
	processRequest(request, response, requestParameterMap);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response, Map<String, String> requestParameterMap)
	    throws ServletException, IOException {
	String httpMethod = "POST"; // default
	byte[] responseBody = null;

	// Get the IP address of client machine.
	String ipAddress = request.getRemoteAddr();
	// Log the IP address and current timestamp.
	System.out.println("\n\n\n\n\nRequesting machine IP " + ipAddress);
	requestParameterMap.put("ipAddress", ipAddress);

	// String buffer = request.getQueryString();
	// System.out.println(buffer.toString());
	// String targetURLTemp = buffer.substring(buffer.indexOf("targetURL") + "targetURL".length() + 1);
	// System.out.println("targetURLTemp=" + targetURLTemp);

	System.out.println("Print request params =>");
	StringBuilder extraParams = new StringBuilder();

	Enumeration<String> paramNamesEnum = request.getParameterNames();
	while (paramNamesEnum != null && paramNamesEnum.hasMoreElements()) {
	    String param = paramNamesEnum.nextElement();
	    if (param != null && param.length() > 0) {
		param = param.trim();
		// String requestBody = getBody(request);
		if (param.equals("POSTDATA") && requestParameterMap.containsKey("POSTDATA")) {
		    // ignore value passed in the URL
		    continue;
		}
		String paramVal = request.getParameter(param);
		System.out.println(param + "=" + paramVal);
		requestParameterMap.put(param, paramVal);

		if (param.equals("targetURL")) {
		    int qIndex = paramVal.indexOf("?");
		    if (qIndex > 0) {
			String url = paramVal.substring(0, qIndex);
			requestParameterMap.put(param, url);

			String params = paramVal.substring(qIndex + 1);
			String[] paramArr = params.split(",");
			for (int index = 0; index < paramArr.length; index++) {
			    String[] keyValue = paramArr[index].split("=");
			    requestParameterMap.put(keyValue[0], keyValue[1]);
			}

		    } else {
			requestParameterMap.put(param, paramVal);
		    }
		}

		// if (!param.equals("POSTDATA") && !param.equals("targetURL") && !param.equals("formMethod")) {
		// extraParams.append("&").append(param).append("=").append(paramVal);
		// }
	    }
	}
	String targetURL = requestParameterMap.get("targetURL");
	targetURL = targetURL.concat(extraParams.toString());

	HTTPClientResponse clientResponse = null;

	// URLEncoder.encode(targetURL);

	// override the targetURL
	requestParameterMap.put("targetURL", targetURL);
	httpMethod = requestParameterMap.remove("formMethod");

	System.out.println(httpMethod + " -> Print request params =>" + requestParameterMap);

	// LOGGER.debug("\n\nrequest Parameter Map=" + requestParameterMap);

	String tartgetHost = null;
	// System.out.println("targetURL=" + targetURL);
	if (targetURL != null && targetURL.length() > 0) {
	    requestParameterMap.remove("targetURL");
	    int len = targetURL.length();
	    int lastIndex = -1;
	    if (len > 10) {
		lastIndex = targetURL.indexOf("/", 10);
	    }
	    if (lastIndex == -1) {
		lastIndex = len - 1;
	    }
	    tartgetHost = targetURL.substring(0, lastIndex);
	    System.out.println("Requested tartgetHost=[" + tartgetHost + "]");
	}

	String action = requestParameterMap.remove("action");
	if (action != null && action.equals("clean")) {
	    boolean flag = commonHttpClientExecutor.cleanSession(ipAddress, tartgetHost);
	    if (flag) {
		responseBody = "Your session cleaned successfully.".getBytes();
	    } else {
		responseBody = "Your session not found.".getBytes();
	    }
	    System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n NEW SESSION \n\n\n\n\n\n\n\n\n\n");
	} else {

	    // httpMethod = request.getMethod();

	    requestParameterMap.put("tartgetHost", tartgetHost);

	    // LOGGER.debug("nonceValue=" + nonceValue);
	    // LOGGER.debug("targetURL=" + targetURL);
	    // LOGGER.debug("httpPostMethod=" + httpPostMethod);

	    // Set the response message's MIME type
	    // response.setContentType("text/html;charset=UTF-8");

	    try {
		clientResponse = commonHttpClientExecutor.processHttpRequest(targetURL, httpMethod, requestParameterMap);

	    } catch (Exception e) {
		e.printStackTrace(System.err);
		LOGGER.error(e);
		String error = "Exception Occurred => " + getStackTraceString(e);
		responseBody = error.getBytes();
	    }
	}

	// Allocate a output writer to write the response message into the network socket
	OutputStream outputStream = response.getOutputStream();
	// PrintWriter out = response.getWriter();
	// Write the response message, in an HTML page
	try {
	    boolean isHTMLText = false;
	    if (clientResponse != null) {
		responseBody = clientResponse.getResponseBody();

		Header[] headers = clientResponse.getHeaders();
		for (int index = 0; index < headers.length; index++) {
		    Header header = headers[index];
		    String name = header.getName();
		    String value = header.getValue();
		    // System.out.println("response header => " + name + "=" + value);
		    response.setHeader(name, value);
		    if (name.equals("Content-Type") && name.contains("text/html")) {
			isHTMLText = true;
		    }
		}
	    }

	    response.setHeader("Access-Control-Allow-Origin", getOrigin(request));
	    // response.setHeader("Access-Control-Allow-Origin", "*");
	    response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS"); // HEAD,GET,PUT,POST,DELETE,OPTIONS,TRACE
	    response.setHeader("Access-Control-Max-Age", "3600");
	    response.setHeader("Access-Control-Allow-Headers", "Content-Type");

	    if (isHTMLText) {
		String responseBodyStr = new String(responseBody);
		responseBodyStr = responseBodyStr.replace("href=\"/", "href=\"" + tartgetHost + "/");
		responseBodyStr = responseBodyStr.replace("src=\"/", "src=\"" + tartgetHost + "/");
		responseBody = responseBodyStr.getBytes();
		response.setHeader("Content-Length", String.valueOf(responseBody.length));
	    }

	    // System.out.println("responseString=>>>>>>>>> " + new String(responseBody));

	    outputStream.write(responseBody);
	    outputStream.flush();
	    // out.println(responseBody);

	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	    outputStream.close(); // Always close the output writer
	    outputStream = null;
	}
    }

    private String getOrigin(HttpServletRequest request) {
	String origin = request.getHeader("Origin");
	System.out.println("origin=[" + origin + "]");

	if (origin == null || origin.length() == 0) {
	    String referer = request.getHeader("Referer");
	    System.out.println("referer=[" + referer + "]");
	    if (referer != null && referer.length() > 10) {
		String clientDomain = referer.substring(0, referer.indexOf("/", 10));
		System.out.println("clientDomain=[" + clientDomain + "]");
		origin = clientDomain;
	    }
	}

	if (origin == null || origin.length() == 0) {
	    origin = "*";
	}
	System.out.println("FINAL origin=[" + origin + "]");
	return origin;
    }

    private String getStackTraceString(Exception ex) {
	StringWriter errors = new StringWriter();
	ex.printStackTrace(new PrintWriter(errors));
	return errors.toString();
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	Map<String, String> requestParameterMap = new HashMap<String, String>();
	String requestBody = getBody(request);
	if (requestBody != null && requestBody.length() == 0) {
	    requestParameterMap.put("POSTDATA", requestBody);
	}
	System.out.println("POST -> Print request params =>" + requestParameterMap);

	processRequest(request, response, requestParameterMap);
    }

    /**
     * @param request
     * @return
     * @throws IOException
     */
    public static String getBody(HttpServletRequest request) throws IOException {
	StringBuilder stringBuilder = new StringBuilder();
	BufferedReader bufferedReader = null;

	try {
	    InputStream inputStream = request.getInputStream();
	    if (inputStream != null) {
		bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		char[] charBuffer = new char[128];
		int bytesRead = -1;
		while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
		    stringBuilder.append(charBuffer, 0, bytesRead);
		}
	    } else {
		stringBuilder.append("");
	    }
	} catch (IOException ex) {
	    // throw ex;
	} finally {
	    if (bufferedReader != null) {
		try {
		    bufferedReader.close();
		} catch (IOException ex) {
		    throw ex;
		}
	    }
	}

	return stringBuilder.toString();
    }
}
