package com.barclays.iportal.cros;

import org.apache.http.Header;

public class HTTPClientResponse {

    Header[] headers;
    byte[] responseBody;

    /**
     * @param responseBody
     */
    public HTTPClientResponse(Header[] headers, byte[] responseBody) {
	this.headers = headers;
	this.responseBody = responseBody;
    }

    /**
     * @return the responseBody
     */
    public final byte[] getResponseBody() {
	return responseBody;
    }

    /**
     * @param responseBody
     *            the responseBody to set
     */
    public final void setResponseBody(byte[] responseBody) {
	this.responseBody = responseBody;
    }

    /**
     * @return the headers
     */
    public Header[] getHeaders() {
	return headers;
    }

    /**
     * @param headers
     *            the headers to set
     */
    public void setHeaders(Header[] headers) {
	this.headers = headers;
    }

}
