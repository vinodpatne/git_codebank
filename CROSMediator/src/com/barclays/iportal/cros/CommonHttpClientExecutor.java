package com.barclays.iportal.cros;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.ConnectException;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParamBean;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.log4j.Logger;

/**
 * The Class CommonHttpClientExecuter.
 */
public class CommonHttpClientExecutor {

    private static final String CHARSET = "UTF-8";

    private static final String JSESSIONID = "SESSION";

    private static final String NV = "NV";

    /** The Constant LOGGER. */
    private static final Logger LOGGER = Logger.getLogger(CommonHttpClientExecutor.class);

    private static final HashMap<String, HttpContext> sessionMap = new HashMap<String, HttpContext>();

    /** The conn manager. */
    private PoolingClientConnectionManager connManager;

    /** The http client. */
    private DefaultHttpClient httpClient;

    /** The host scheme. */
    private String hostScheme;

    /** The host scheme. */
    private String hostName;

    /** The host scheme. */
    private String hostPath;

    /** The max total connections. */
    private int maxTotalConnections = 10;

    /** The host config port. */
    private int hostConfigPort;

    /** The default max per route. */
    private int defaultMaxPerRoute = 10;

    /**
     * Gets the host scheme.
     *
     * @return the host scheme
     */
    public String getHostScheme() {
	return hostScheme;
    }

    /**
     * Sets the host scheme.
     *
     * @param hostScheme
     *            the new host scheme
     */
    public void setHostScheme(final String hostScheme) {
	this.hostScheme = hostScheme;
    }

    /**
     * Gets the max total connections.
     *
     * @return the max total connections
     */
    public int getMaxTotalConnections() {
	return maxTotalConnections;
    }

    /**
     * Sets the max total connections.
     *
     * @param maxTotalConnections
     *            the new max total connections
     */
    public void setMaxTotalConnections(final int maxTotalConnections) {
	this.maxTotalConnections = maxTotalConnections;
    }

    /**
     * Gets the host config port.
     *
     * @return the host config port
     */
    public int getHostConfigPort() {
	return hostConfigPort;
    }

    /**
     * Sets the host config port.
     *
     * @param hostConfigPort
     *            the new host config port
     */
    public void setHostConfigPort(final int hostConfigPort) {
	this.hostConfigPort = hostConfigPort;
    }

    public boolean cleanSession(final String ipAddress, final String tartgetHost) {
	if (tartgetHost == null || tartgetHost.length() == 0) {
	    return cleanSession(ipAddress);
	} else {
	    String key = ipAddress + "_" + tartgetHost;
	    if (this.sessionMap.containsKey(key)) {
		this.sessionMap.remove(key);
		System.out.println("Removed key = " + key);
		return true;
	    } else {
		return false;
	    }
	}
    }

    public boolean cleanSession(final String ipAddress) {

	final Iterator<Map.Entry<String, HttpContext>> entries = sessionMap.entrySet().iterator();
	int counter = 0;
	while (entries.hasNext()) {
	    final Entry<String, HttpContext> entry = entries.next();
	    String key = entry.getKey();
	    System.out.println("Key = " + key + ", Value = " + entry.getValue());
	    if (key.startsWith(ipAddress + "_")) {
		this.sessionMap.remove(ipAddress);
		System.out.println("Removed matched key = " + key);
		counter++;
	    }
	}

	if (counter > 0) {
	    this.sessionMap.remove(ipAddress);
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * postHttpRequest.
     *
     * @param url
     *            the url
     * @param localContext
     *            the local context
     * @param queryParamMap
     *            the query param map
     * @return the string
     * @throws Exception
     *             the uSSD blocking exception
     */
    public HTTPClientResponse processHttpRequest(final String url, final String method, final Map<String, String> queryParamMap) throws Exception {

	System.out.println("processHttpRequest queryParamMap=" + queryParamMap);

	HTTPClientResponse response = null;

	try {
	    int startIndex = -1, endIndex = -1;

	    hostScheme = url.startsWith("https") ? "https" : "http";
	    startIndex = url.indexOf(":");
	    startIndex = url.indexOf(":", startIndex + 1);
	    if (startIndex > 0) {
		hostName = url.substring(hostScheme.length() + 3, startIndex);
		endIndex = url.indexOf("/", startIndex);
		hostConfigPort = Integer.parseInt(url.substring(startIndex + 1, endIndex));
	    } else {
		endIndex = url.indexOf("/", hostScheme.length() + 3);
		hostName = url.substring(hostScheme.length() + 3, endIndex);
		hostConfigPort = 80;
	    }
	    hostPath = url.substring(endIndex);

	    // LOGGER.debug("hostScheme=" + hostScheme);
	    // LOGGER.debug("hostName=" + hostName);
	    // LOGGER.debug("hostConfigPort=" + hostConfigPort);
	    // LOGGER.debug("hostConfigPort=" + hostPath);

	    if (LOGGER.isDebugEnabled()) {
		LOGGER.debug("set properties for " + hostName + " - Done");
	    }

	    this.afterPropertiesSet();

	    // LOGGER.debug("afterPropertiesSet - Done");

	    HttpContext localContext = null;
	    String ipAddress = queryParamMap.remove("ipAddress");
	    // queryParamMap.remove("ipAddress");
	    String tartgetHost = queryParamMap.remove("tartgetHost");
	    // queryParamMap.remove("tartgetHost");

	    localContext = sessionMap.get(ipAddress + "_" + tartgetHost);

	    if (localContext == null) {
		System.out.println("localContext not Found.");
		localContext = this.createLocalContext();
		sessionMap.put(ipAddress + "_" + tartgetHost, localContext);
	    } else {
		System.out.println("localContext Found.");
	    }
	    // LOGGER.debug("createLocalContext - Done");

	    response = httpCall(url, method, httpClient, localContext, queryParamMap);

	} catch (final Exception e) {
	    LOGGER.error(e.getMessage(), e);
	    throw e;
	}
	return response;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    private void afterPropertiesSet() throws Exception {
	// LOGGER.debug("Create PoolingClientConnectionManager object.");
	connManager = new PoolingClientConnectionManager();
	// LOGGER.debug("Create PoolingClientConnectionManager object - Done");

	connManager.setMaxTotal(maxTotalConnections);
	connManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
	try {
	    setSchemeRegistry();
	} catch (final KeyManagementException e1) {
	    LOGGER.error(e1.getMessage(), e1);
	} catch (final NoSuchAlgorithmException e1) {
	    LOGGER.error(e1.getMessage(), e1);
	}
	httpClient = new DefaultHttpClient();
	httpClient = (DefaultHttpClient) sslTrust(httpClient, connManager);
	// setProxy();

	httpClient.setRedirectStrategy(new DefaultRedirectStrategy() {
	    @Override
	    public boolean isRedirected(final HttpRequest request, final HttpResponse response, final HttpContext context) {
		boolean isRedirect = false;
		try {
		    isRedirect = super.isRedirected(request, response, context);
		} catch (final ProtocolException e) {
		    LOGGER.error(e.getMessage(), e);
		}
		if (!isRedirect) {
		    final int responseCode = response.getStatusLine().getStatusCode();
		    if (responseCode == 301 || responseCode == 302) {
			System.out.println("Redirect.....");
			return true;
		    }
		}
		return isRedirect;
	    }
	});
	httpClient.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
	httpClient.setReuseStrategy(new DefaultConnectionReuseStrategy());
	httpClient.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy());
	setProtocolParam();
    }

    // public static void main(String[] args) {
    // String url = "http://localhost:8080/ussd-war/selcom";
    //
    // String context ="/ussd-war/";
    // String requestURL = url;
    // int strIndex = requestURL.indexOf(context);
    // String contextPath = requestURL.substring(0, strIndex+context.length());
    // LOGGER.debug("contextPath=" + contextPath);
    //
    // try {
    // int startIndex = -1, endIndex = -1;
    // String hostName = "", hostPath = "";
    // int hostConfigPort = 0;
    //
    // String hostScheme = url.startsWith("https") ? "https" : "http";
    // startIndex = url.indexOf(":");
    // // startIndex = url.indexOf(":");
    // startIndex = url.indexOf(":", startIndex + 1);
    // if (startIndex > 0) {
    // hostName = url.substring(hostScheme.length() + 3, startIndex);
    // endIndex = url.indexOf("/", startIndex);
    // hostConfigPort = Integer.parseInt(url.substring(startIndex + 1, endIndex));
    // } else {
    // endIndex = url.indexOf("/", hostScheme.length() + 3);
    // hostName = url.substring(hostScheme.length() + 3, endIndex);
    // hostConfigPort = 80;
    // }
    // hostPath = url.substring(endIndex);
    //
    // LOGGER.debug("hostScheme=" + hostScheme);
    // LOGGER.debug("hostName=" + hostName);
    // LOGGER.debug("hostConfigPort=" + hostConfigPort);
    // LOGGER.debug("hostConfigPort=" + hostPath);
    //
    // } catch (final Exception e) {
    // LOGGER.error(e.getMessage(), e);
    // }
    //
    // }

    /**
     * Http call.
     *
     * @param url
     *            the url
     * @param httpClient
     *            the http client
     * @param httpContext
     *            the http context
     * @param queryParamMap
     *            the query param map
     * @return the string
     * @throws InterruptedException
     *             the interrupted exception
     * @throws Exception
     *             the uSSD blocking exception
     */
    private HTTPClientResponse httpCall(String url, final String method, DefaultHttpClient httpClient, HttpContext httpContext,
	    Map<String, String> queryParamMap) throws Exception {

	Header[] headers = null;
	byte[] responseBody;
	HttpPost postRequest = null;
	HttpGet getRequest = null;
	BufferedReader br = null;
	// String jSessionCookieName = "";
	// String jSessionId = "";
	// String nonceValue = "";

	try {
	    final HttpParams httpParameters = new BasicHttpParams();
	    // Set the timeout in milliseconds until a connection is established.
	    final int connectionTimeout = 300000;// ConfigurationManager.getInt(USSDConstants.HTTP_CONECTION_TIMEOUT);

	    HttpConnectionParams.setConnectionTimeout(httpParameters, connectionTimeout);

	    // Set the default socket timeout (SO_TIMEOUT) in milliseconds which
	    // is the timeout for waiting for data.
	    final int socketTimeout = 300000; // ConfigurationManager.getInt(USSDConstants.HTTP_SOCKET_TIMEOUT);
	    HttpConnectionParams.setSoTimeout(httpParameters, socketTimeout);
	    System.out.println("target URL =[" + url + "] , method=[" + method + "]");

	    // Fetch cookies from HTTP context
	    final CookieStore cookieStore = (CookieStore) httpContext.getAttribute(ClientContext.COOKIE_STORE);
	    System.out.println("--------------Sending Cookies in request --------------------------");
	    List<Cookie> cookies = cookieStore.getCookies();
	    for (int i = 0; i < cookies.size(); i++) {
		System.out.println("\t " + cookies.get(i).getName() + "=" + cookies.get(i).getValue());
	    }
	    System.out.println("----------------------------------------");

	    HttpResponse response = null;
	    if ("POST".equals(method)) {
		postRequest = new HttpPost(url);
		postRequest = preparePostBody(postRequest, queryParamMap);

		// return executePostRequest(queryParamMap);
		System.out.println("Sending the request.");
		response = httpClient.execute(postRequest, httpContext);

	    } else {
		// getRequest = new HttpGet(url);
		getRequest = prepareGetRequest(queryParamMap);
		LOGGER.info("Requesting the Hello Money URL.");
		response = httpClient.execute(getRequest, httpContext);
	    }

	    System.out.println("Received response from application.");
	    System.out.println("response - " + response.getStatusLine().getStatusCode());
	    System.out.println("response - " + response.getStatusLine().getReasonPhrase());

	    if (response.getStatusLine().getStatusCode() != 200) {
		String error = "[" + response.getStatusLine().getStatusCode() + "] - " + response.getStatusLine().getReasonPhrase();
		System.out.println(error);
		// throw new Exception(error);
	    }

	    // String cookieValue = "";
	    headers = response.getAllHeaders();
	    for (int index = 0; index < headers.length; index++) {
		Header header = headers[index];
		String name = header.getName();
		String value = header.getValue();

		if (name.equalsIgnoreCase("Set-Cookie")) {
		    if (value.contains(JSESSIONID)) {
			System.out.println("\n\n\n\n\n\n New session.....\n\n\n\n\n\n");
		    }
		}
		System.out.println("response header => " + name + "=" + value);
	    }

	    // if (name.equalsIgnoreCase("Set-Cookie")) {
	    //
	    // // cookieValue = cookieValue + "|" + name + "=>" + value;
	    // if (value.contains(JSESSIONID)) {
	    // int startIndex = value.indexOf("=");
	    // int endIndex = value.indexOf(";");
	    // jSessionCookieName = value.substring(0, startIndex);
	    // if (endIndex <= 0) {
	    // endIndex = value.length();
	    // }
	    // jSessionId = value.substring(startIndex + 1, endIndex);
	    //
	    // } else if (value.contains(NV)) {
	    // int startIndex = value.indexOf("=");
	    // int endIndex = value.indexOf(";");
	    // if (endIndex <= 0) {
	    // endIndex = value.length();
	    // }
	    // nonceValue = value.substring(startIndex + 1, endIndex);
	    // }
	    // }
	    // }
	    // LOGGER.debug("Sesstion Cookie - " + jSessionCookieName + "=" + jSessionId);
	    // LOGGER.debug("NV=" + nonceValue);

	    // String displayText = "";
	    responseBody = IOUtils.toByteArray(response.getEntity().getContent());
	    // br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), CHARSET));
	    //
	    // while (displayText != null) {
	    // displayText = br.readLine();
	    // responseString.append(displayText != null ? displayText : "");
	    // }
	    // }

	} catch (final ConnectException e) {
	    LOGGER.fatal("ConnectException while connecting to Server", e);
	    throw e;
	} catch (final ClientProtocolException e) {
	    if (getRequest != null) {
		getRequest.abort();
	    } else {
		postRequest.abort();
	    }
	    LOGGER.fatal("ClientProtocolException while connecting to Server", e);
	    throw e;
	} catch (final Exception e) {
	    LOGGER.fatal("Exception while connecting to Server", e);
	    throw e;
	} finally {
	    if (getRequest != null) {
		getRequest.releaseConnection();
	    } else if (postRequest != null) {
		postRequest.releaseConnection();
	    }
	}

	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Response from Server: " + responseString.toString());
	// // LOGGER.debug("jSessionCookieName: " + jSessionCookieName);
	// LOGGER.debug("jSessionCookieValue: " + jSessionId);
	// }
	return new HTTPClientResponse(headers, responseBody);
    }

    /**
     * Ssl trust.
     *
     * @param httpClient2
     *            the http client2
     * @param connManager2
     *            the conn manager2
     * @return the http client
     */
    @SuppressWarnings("deprecation")
    private HttpClient sslTrust(final HttpClient httpClient2, final PoolingClientConnectionManager connManager2) {
	// Create a trust manager that does not validate certificate chains
	// Install the all-trusting trust manager
	SSLContext sc = null;

	try {
	    sc = SSLContext.getInstance("TLS");
	    final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
		    return new X509Certificate[0];
		}

		public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
		}

		public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
		}
	    } };

	    sc.init(null, trustAllCerts, null);
	    final SSLSocketFactory ssf = new SSLSocketFactory(sc);
	    ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
	    final SchemeRegistry sr = connManager2.getSchemeRegistry();
	    sr.register(new Scheme("https", 443, ssf));
	    /*
	     * sc.init(null, trustAllCerts, new java.security.SecureRandom()); HttpsURLConnection .setDefaultSSLSocketFactory(sc.getSocketFactory());
	     */

	} catch (final NoSuchAlgorithmException e) {
	    LOGGER.fatal("NoSuchAlgorithmException while connecting to Server", e);
	} catch (final KeyManagementException e) {
	    LOGGER.fatal("KeyManagementException while connecting to Server", e);
	}
	// Create all-trusting host name verifier

	final HostnameVerifier allHostsValid = new HostnameVerifier() {
	    public boolean verify(final String hostname, final SSLSession session) {
		return true;
	    }
	};

	// Install the all-trusting host verifier
	HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	return new DefaultHttpClient(connManager2, httpClient2.getParams());
    }

    // private String prepareGetURL(final String url, final Map<String, String> queryParamMap) throws
    // UnsupportedEncodingException {
    // // postRequest.addHeader("accept", "application/json");
    // StringBuilder strURL = new StringBuilder(url);
    //
    // if (!queryParamMap.isEmpty()) {
    // strURL.append("?");
    // final Iterator<Map.Entry<String, String>> entries = queryParamMap.entrySet().iterator();
    // int counter = 0;
    // while (entries.hasNext()) {
    // final Entry<String, String> entry = entries.next();
    // if (LOGGER.isDebugEnabled()) {
    // LOGGER.debug("Key = " + entry.getKey() + ", Value = " + entry.getValue());
    // }
    // String key = entry.getKey();
    // if (!key.equalsIgnoreCase("targetURL")) {
    // if (counter > 0) {
    // strURL.append("&");
    // }
    // strURL.append(entry.getKey());
    // strURL.append("=");
    // strURL.append(entry.getValue());
    // counter++;
    // }
    // }
    // }
    // return strURL.toString();
    // }

    /**
     * Prepare post body.
     *
     * @param postRequest
     *            the post request
     * @param queryParamMap
     *            the query param map
     * @return the http post
     * @throws UnsupportedEncodingException
     *             the unsupported encoding exception
     */
    private HttpGet prepareGetRequest(final Map<String, String> queryParamMap) throws Exception {
	// postRequest.addHeader("accept", "application/json");

	URIBuilder builder = new URIBuilder();
	builder.setScheme(this.getHostScheme());
	builder.setHost(this.getHostName());
	builder.setPort(this.getHostConfigPort());
	builder.setPath(this.getHostPath());

	if (!queryParamMap.isEmpty()) {
	    final Iterator<Map.Entry<String, String>> entries = queryParamMap.entrySet().iterator();
	    while (entries.hasNext()) {
		final Entry<String, String> entry = entries.next();
		String key = entry.getKey();
		if (LOGGER.isDebugEnabled()) {
		    LOGGER.debug("Key = " + key + ", Value = " + entry.getValue());
		}
		if (!key.equalsIgnoreCase("targetURL") && !key.equalsIgnoreCase("formMethod") && !key.equalsIgnoreCase("POSTDATA")) {
		    builder.setParameter(key, entry.getValue());
		}
	    }
	}

	URI uri = builder.build();
	HttpGet httpGet = new HttpGet(uri);
	if (LOGGER.isDebugEnabled()) {
	    LOGGER.debug("URI=" + httpGet.getURI());
	}

	return httpGet;
    }

    /**
     * Prepare post body.
     *
     * @param postRequest
     *            the post request
     * @param queryParamMap
     *            the query param map
     * @return the http post
     * @throws UnsupportedEncodingException
     *             the unsupported encoding exception
     */
    private HttpPost preparePostBody(final HttpPost postRequest, final Map<String, String> queryParamMap) throws UnsupportedEncodingException {
	// postRequest.addHeader("accept", "application/json");

	// if (!queryParamMap.isEmpty()) {
	// final List<NameValuePair> nvps = new ArrayList<NameValuePair>();
	// final Iterator<Map.Entry<String, String>> entries = queryParamMap.entrySet().iterator();
	//
	// while (entries.hasNext()) {
	// final Entry<String, String> entry = entries.next();
	// String key = entry.getKey();
	// if (LOGGER.isDebugEnabled()) {
	// LOGGER.debug("Key = " + key + ", Value = " + entry.getValue());
	// }
	// if (!key.equalsIgnoreCase("targetURL") && !key.equalsIgnoreCase("formMethod")) {
	// nvps.add(new BasicNameValuePair(key, entry.getValue()));
	// }
	//
	// }
	// }

	String postBodyString = null;

	String jsonid = queryParamMap.get("jsonid");
	String jsonValue = queryParamMap.get("jsonValue");
	if (jsonid != null && jsonid.length() > 0) {
	    postBodyString = "{\"" + jsonid + "\":\"" + jsonValue + "\"}";
	    System.out.println("updated=>" + postBodyString);
	}
	if (postBodyString == null) {
	    System.out.println("Get POSTDATA in request.");
	    postBodyString = queryParamMap.get("POSTDATA");
	}

	// build JSON using query parameters
	if (postBodyString == null) {
	    System.out.println("build JSON using query parameters");
	    StringBuilder postBodyBuffer = new StringBuilder();

	    if (!queryParamMap.isEmpty()) {
		final Iterator<Map.Entry<String, String>> entries = queryParamMap.entrySet().iterator();
		while (entries.hasNext()) {
		    final Entry<String, String> entry = entries.next();
		    String key = entry.getKey();
		    if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Key = " + key + ", Value = " + entry.getValue());
		    }
		    if (!key.equalsIgnoreCase("targetURL") && !key.equalsIgnoreCase("formMethod") && !key.equalsIgnoreCase("POSTDATA")) {
			if (postBodyBuffer.length() > 0) {
			    postBodyBuffer.append(", ");
			}
			postBodyBuffer.append("\"").append(key).append("\": \"").append(entry.getValue()).append("\"");

		    }
		}
	    }
	    if (postBodyBuffer.length() > 0) {
		postBodyString = "{" + postBodyBuffer.toString() + "}";
	    }
	}

	System.out.println("POSTDATA=[" + postBodyString + "]");
	if (postBodyString != null) {
	    InputStream inputStream = new ByteArrayInputStream(postBodyString.getBytes(CHARSET));
	    InputStreamEntity inputStreamEntity = new InputStreamEntity(inputStream, postBodyString.length());

	    inputStreamEntity.setContentType("application/json;charset=utf-8");

	    // inputStreamEntity.setChunked(true);

	    postRequest.setEntity(inputStreamEntity);
	}

	return postRequest;
    }

    /**
     * Creates the local context.
     *
     * @return the http context
     */
    public HttpContext createLocalContext() {
	// Create a local instance of cookie store
	final CookieStore cookieStore = new BasicCookieStore();
	// Cookie cookie = new BasicClientCookie(sessionCookieName, sessionCookieValue);
	// cookieStore.addCookie(cookie);
	// Create local HTTP context
	final HttpContext localContext = new BasicHttpContext();
	localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	return localContext;
    }

    /**
     * Sets the scheme registry.
     *
     * @throws NoSuchAlgorithmException
     *             the no such algorithm exception
     * @throws KeyManagementException
     *             the key management exception
     */
    public void setSchemeRegistry() throws NoSuchAlgorithmException, KeyManagementException {
	final SchemeRegistry sr = new SchemeRegistry();

	if (hostScheme.equals("http")) {
	    final Scheme http = new Scheme("http", hostConfigPort, PlainSocketFactory.getSocketFactory());
	    sr.register(http);
	} else {
	    final SSLContext sslcontext = SSLContext.getInstance("TLS");
	    sslcontext.init(null, null, null);
	    final SSLSocketFactory sf = new SSLSocketFactory(sslcontext, SSLSocketFactory.STRICT_HOSTNAME_VERIFIER);
	    final Scheme https = new Scheme("https", hostConfigPort, sf);
	    sr.register(https);
	}

    }

    /**
     * Sets the protocol param.
     */
    public void setProtocolParam() {
	final HttpParams params = new BasicHttpParams();
	final HttpProtocolParamBean paramsBean = new HttpProtocolParamBean(params);
	paramsBean.setVersion(HttpVersion.HTTP_1_1);
	paramsBean.setUserAgent("HttpComponents/1.1");
	paramsBean.setContentCharset(CHARSET);
	paramsBean.setUseExpectContinue(true);
    }

    /**
     * Sets the proxy.
     */
    public void setProxy() {
	final HttpHost proxy = new HttpHost("webproxy.africa.barclays.org", 8080);
	httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	Authenticator authenticator = new Authenticator() {

	    public PasswordAuthentication getPasswordAuthentication() {
		return (new PasswordAuthentication("E20041629", "01Jun2015".toCharArray()));
	    }
	};
	Authenticator.setDefault(authenticator);
    }

    /**
     * Gets the http client.
     *
     * @return the http client
     */
    public DefaultHttpClient getHttpClient() {
	return httpClient;
    }

    /**
     * Sets the http client.
     *
     * @param httpClient
     *            the new http client
     */
    public void setHttpClient(final DefaultHttpClient httpClient) {
	this.httpClient = httpClient;
    }

    /**
     * Sets the default max per route.
     *
     * @param defaultMaxPerRoute
     *            the new default max per route
     */
    public void setDefaultMaxPerRoute(final int defaultMaxPerRoute) {
	this.defaultMaxPerRoute = defaultMaxPerRoute;
    }

    /**
     * Gets the default max per route.
     *
     * @return the default max per route
     */
    public int getDefaultMaxPerRoute() {
	return defaultMaxPerRoute;
    }

    /**
     * Destroy.
     */
    public void destroy() {
	connManager.shutdown();
    }

    /**
     * @return the hostName
     */
    public final String getHostName() {
	return hostName;
    }

    /**
     * @param hostName
     *            the hostName to set
     */
    public final void setHostName(String hostName) {
	this.hostName = hostName;
    }

    /**
     * @return the hostPath
     */
    public final String getHostPath() {
	return hostPath;
    }

    /**
     * @param hostPath
     *            the hostPath to set
     */
    public final void setHostPath(String hostPath) {
	this.hostPath = hostPath;
    }
}
