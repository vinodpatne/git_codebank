package com.barclays.proxyserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;

public class ProxyThread extends Thread {
    private Socket socket = null;
    private static final int BUFFER_SIZE = 32768;
    private static String crossMediatorURL = "";

    private static Map configMap = null;

    public ProxyThread(Socket socket, String crossMediatorURL, Map configMap) {
	super("ProxyThread");
	this.socket = socket;
	this.configMap = configMap;
	this.crossMediatorURL = crossMediatorURL;
    }

    public void run() {
	// get input from user
	// send request to server
	// get response from server
	// send response to user

	try {
	    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
	    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

	    String inputLine;
	    int cnt = 0;
	    String httpMethod = ""; // GET , POST
	    String urlToCall = "";
	    // /////////////////////////////////
	    // begin get request from client
	    while ((inputLine = in.readLine()) != null) {
		try {
		    StringTokenizer tok = new StringTokenizer(inputLine);
		    tok.nextToken();
		} catch (Exception e) {
		    break;
		}
		// parse the first line of the request to find the url
		if (cnt == 0) {
		    String[] tokens = inputLine.split(" ");

		    for (int index = 0; index < tokens.length; index++) {
			System.out.println("tokens=>");
			System.out.println(tokens[index]);
			System.out.println();
		    }
		    httpMethod = tokens[0];
		    urlToCall = tokens[1];
		    // can redirect this to output log
		    System.out.println("Original Request for : " + urlToCall);
		}

		cnt++;
	    }
	    // end get request from client
	    // /////////////////////////////////

	    String requestedURI = "";
	    if (urlToCall.startsWith("http")) {
		int len = urlToCall.length();
		int lastIndex = -1;
		if (len > 10) {
		    lastIndex = urlToCall.indexOf("/", 10);
		}
		if (lastIndex == -1) {
		    lastIndex = len - 1;
		}
		requestedURI = urlToCall.substring(lastIndex);

		System.out.println("Requested URI=[" + requestedURI + "]");
	    } else {
		requestedURI = urlToCall;
	    }

	    int count = configMap.size();

	    for (int index = 1; index <= count; index++) {
		String key = "url-pattern-routing-" + index;
		String value = (String) configMap.get(key);
		if (value != null) {

		    String[] values = value.split(",");
		    String pattern = values[0];
		    String target = values[1];

		    if (requestedURI.startsWith(pattern)) {

			values = target.split("-");
			String targetHost = values[0];
			String targetPort = values[1];
			if (targetPort.equals("80")) {
			    urlToCall = "http://" + targetHost + "" + requestedURI;
			} else {
			    urlToCall = "http://" + targetHost + ":" + targetPort + "" + requestedURI;
			}
			break;
		    }
		}
	    }

	    // BufferedReader rd = null;
	    try {
		String newCrossMediatorURL = crossMediatorURL.replace("{formMethod}", httpMethod);
		System.out.println("sending request to real server (via crossmediator) for url: " + newCrossMediatorURL + urlToCall);
		// /////////////////////////////////
		// int relativeURLIndex = urlToCall.indexOf("/olb/portalidp");
		// if (relativeURLIndex >= 0) {
		// urlToCall = crossMediatorPrefix + urlToCall.substring(relativeURLIndex);
		// }
		// System.out.println("Request for : " + urlToCall);

		// begin send request to server, get response from server
		URL url = null;
		try {
		    url = new URL(newCrossMediatorURL + urlToCall);
		} catch (MalformedURLException e) {
		    System.out.println("Prefix http and retry");
		    urlToCall = "http://" + urlToCall;
		    url = new URL(newCrossMediatorURL + urlToCall);
		}

		HttpURLConnection conn = null;

		if (urlToCall.toLowerCase().contains("bdspuk") || urlToCall.contains("localhost")) {
		    conn = (HttpURLConnection) url.openConnection();
		} else {
		    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("webproxy.africa.barclays.org", 8080)); // webproxy.africa.barclays.org
		    conn = (HttpURLConnection) url.openConnection(proxy);
		    sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
		    String encoded = new String(encoder.encode("E20041629:01Jun2015".getBytes()));
		    conn.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
		}
		conn.setUseCaches(false);
		conn.setDoInput(true);
		// conn.setDoOutput(true);
		conn.connect();
		// not doing HTTP posts
		// conn.setDoOutput(false);

		// System.out.println("Type is: " + conn.getContentType());
		// System.out.println("content length: " + conn.getContentLength());
		// System.out.println("allowed user interaction: " + conn.getAllowUserInteraction());
		// System.out.println("content encoding: " + conn.getContentEncoding());
		// System.out.println("content type: " + conn.getContentType());

		// Get the response
		System.out.println("Get the response...");

		InputStream is = null;
		if (conn != null) {// conn.getContentLength() > 0) {
		    try {
			System.out.println("Get input stream." + conn.getResponseCode());
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
			    is = conn.getInputStream();
			} else {
			    /* error from server */
			    is = conn.getErrorStream();
			}
			// rd = new BufferedReader(new InputStreamReader(is));
		    } catch (IOException ioe) {
			System.out.println("********* IO EXCEPTION **********: " + ioe);
			ioe.printStackTrace();
		    }
		}
		// end send request to server, get response from server
		// /////////////////////////////////
		System.out.println("write headers.");
		out.write(getHeaders(conn).concat("\n").getBytes());

		// /////////////////////////////////
		// begin send response to client
		System.out.println("begin send response to client");

		out.write(IOUtils.toByteArray(is));

		if (is != null) {
		    is.close();
		}

		if (out != null) {
		    out.flush();
		}

		// if (stringBuilder.indexOf("ng-app") > 0) {
		// System.out.println("\n\n\n\n\n *********** Found ng-app in this Request : " + urlToCall + "\n\n\n");
		// }

		// end send response to client
		// /////////////////////////////////
	    } catch (Exception e) {
		// can redirect this to error log
		System.out.println("Encountered exception: " + e);
		e.printStackTrace();

		// encountered error - just send nothing back, so
		// processing can continue
		if (out != null) {
		    out.writeBytes("" + e.getMessage());
		}
	    } finally {
		if (out != null) {
		    out.close();
		}
		if (in != null) {
		    in.close();
		}
		// System.out.println("closing socket");
		if (socket != null) {
		    socket.close();
		}
		System.out.println("closed socket connection.");
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private static String getHeaders(HttpURLConnection conn) {
	StringBuilder buffer = new StringBuilder();
	// get all headers
	Map<String, List<String>> map = conn.getHeaderFields();
	for (Map.Entry<String, List<String>> entry : map.entrySet()) {
	    String key = entry.getKey();
	    String value = entry.getValue().toString().replace("[", "").replace("]", "");
	    // System.out.println("Key : " + key + " ,Value : " + value);
	    if (key == null) {
		buffer.append(value).append("\n");
	    } else {
		buffer.append(key).append(": ").append(value).append("\n");
	    }
	}
	return buffer.toString();

    }

}
