package com.barclays.proxyserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

public class ProxyServer extends Thread {

    private String hostname = "localhost"; // default
    private int port = 10000; // default
    private boolean listening = false;
    private Map<String, String> configMap = null;
    private String crossMediatorURL;

    public ProxyServer(String hostname, Map<String, String> map) {
	this.hostname = hostname;
	this.configMap = map;
	setPort();
	setCrossMediatorURL();
    }

    public void startServer() {
	listening = true;
	this.start();
    }

    public void setPort() {
	String portStr = (String) configMap.remove("proxyServerPort");
	if (portStr != null) {
	    this.port = Integer.parseInt(portStr);
	}
    }

    public void setCrossMediatorURL() {
	String portStr = (String) configMap.remove("crossMediatorWebPort");
	this.crossMediatorURL = "http://" + hostname + ":" + portStr + "/crosmediator/call?formMethod={formMethod}&targetURL=";
	System.out.println("crossMediatorURL: " + crossMediatorURL);
    }

    @Override
    public void run() {
	ServerSocket serverSocket = null;

	try {
	    serverSocket = new ServerSocket(port);
	    System.out.println("Proxy Server started listening on port : " + port);
	} catch (IOException e) {
	    System.err.println("Could not listen on port: " + port + " due to - " + e.getMessage());
	    System.exit(-1);
	}

	try {
	    while (listening) {
		new ProxyThread(serverSocket.accept(), crossMediatorURL, configMap).start();
	    }
	    serverSocket.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void stopServer() {
	listening = false;
	try {
	    Thread.sleep(1000);
	    this.interrupt();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
