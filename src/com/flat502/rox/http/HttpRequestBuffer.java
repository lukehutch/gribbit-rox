package com.flat502.rox.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.encoding.EncodingMap;
import com.flat502.rox.http.exception.ExcessiveContentException;
import com.flat502.rox.http.exception.HttpBufferException;
import com.flat502.rox.http.exception.HttpResponseException;
import com.flat502.rox.http.exception.InvalidHeaderException;
import com.flat502.rox.http.exception.MissingHeaderException;
import com.flat502.rox.http.exception.UnsupportedHeaderException;
import com.flat502.rox.server.HttpServer;
import com.flat502.rox.utils.Utils;

/**
 * This class represents a buffer built up from one or more network messages, and containing an HTTP request.
 */
public class HttpRequestBuffer extends HttpMessageBuffer {
    private static final Comparator<Float> QVALUE_CMP = new Comparator<Float>() {
        @Override
        public int compare(Float qvalue1, Float qvalue2) {
            if (qvalue1 == null && qvalue2 == null) {
                return 0;
            }
            if (qvalue1 == null) {
                // a > b
                return 1;
            }
            if (qvalue2 == null) {
                // a < b
                return -1;
            }
            return qvalue1.compareTo(qvalue2);
        }
    };

    // private static final Pattern REQUEST_LINE = Pattern.compile("(\\S+) (\\S+) (\\S+)");

    private String method;
    private String rawUri;
    private String uri;
    private double httpVersion;
    private String httpVersionString;

    private EncodingMap encodingMap;
    private Encoding encoding;

    private Map<String, Float> acceptedEncodings;

    public HttpRequestBuffer(HttpServer server, Socket socket) {
        this(server, socket, null);
    }

    /**
     * Construct a new buffer for requests on the given socket.
     *
     * @param server
     * @param socket
     *            The socket from which data for this buffer will be gathered.
     * @param encodingMap
     *            An {@link EncodingMap} for mapping <code>Content-Encoding</code> header values to an appropriate
     *            {@link Encoding}. May be <code>null</code>.
     */
    public HttpRequestBuffer(HttpServer server, Socket socket, EncodingMap encodingMap) {
        super(server, socket);
        this.encodingMap = encodingMap;
    }

    public String getMethod() {
        return this.method;
    }

    public String getURI() {
        return this.uri;
    }

    @Override
    public boolean isComplete() throws Exception {
        try {
            return super.isComplete();
        } catch (MissingHeaderException e) {
            throw new HttpResponseException(HttpConstants.StatusCodes._412_PRECONDITION_FAILED,
                    "Precondition Failed (Missing " + e.getHeaderName() + " Header)", e);
        } catch (UnsupportedHeaderException e) {
            if (e.getHeaderName().equals(HttpConstants.Headers.CONTENT_ENCODING)) {
                throw new HttpResponseException(HttpConstants.StatusCodes._415_UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported Media Type (Bad " + e.getHeaderName() + ": " + e.getHeaderValue() + ")", e);
            }
            throw new HttpResponseException(HttpConstants.StatusCodes._400_BAD_REQUEST,
                    "Bad Request (Unsupported: " + e.getHeaderName() + "=" + e.getHeaderValue() + ")", e);
        } catch (ExcessiveContentException e) {
            throw new HttpResponseException(HttpConstants.StatusCodes._400_BAD_REQUEST,
                    "Bad Request (excessive content)", e);
        }
    }

    /**
     * Return a list of acceptable encodings (as specified by the client).
     * <p>
     * The <code>Accept-Content</code> header is interpreted as per <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.3">section 14.3</a> of RFC 2616 are
     * stripped off and excluded from the return values.
     * <p>
     * The returned map uses String keys specifying the encoding names (converted to lowercase) and Float values
     * specifying the qvalue. If no qvalue was specified a <code>null</code> value is used (which should be
     * interpreted as an implicit value "1.0"). If duplicate encodings are specified the highest q-value will be
     * retained. An unspecified q-value is treated as having a higher value than an explicit value of "1.0".
     * <p>
     * An empty <code>Accept-Content</code> header will return an empty map.
     * <p>
     * Iterating over the key-set of the map returned is guaranteed to return the encodings ordered in descending
     * order of qvalue. Ties (i.e. equal qvalues) are broken by sorted so the first to appear in the header value
     * appears first.
     * <p>
     * The map instance returned is unmodifiable.
     *
     * @return <code>null</code> if no <code>Accept-Encoding</code> header was present, or a map of accepted
     *         encodings.
     */
    public Map<String, Float> getAcceptedEncodings() {
        return this.acceptedEncodings;
    }

    public Encoding getContentEncoding() {
        return this.encoding;
    }

    @Override
    public InputStream getContentStream() throws IOException {
        if (this.encoding != null) {
            return this.encoding.getDecoder(super.getContentStream());
        }
        return super.getContentStream();
    }

    @Override
    public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.println(this.method + " " + this.uri + " " + String.valueOf(this.httpVersion));
        pw.print(super.toString());
        return sw.toString();
    }

    @Override
    protected void unpackPreamble(String line) throws HttpResponseException {
        // Method SP Request-URI SP HTTP-Version CRLF
        try {
            int splitIdx = line.indexOf(' ');
            this.method = line.substring(0, splitIdx);

            if (!method.equals(HttpConstants.Methods.GET) && !method.equals(HttpConstants.Methods.POST)) {
                //				System.out.println(Utils.toHexDump(this.data));
                throw new HttpResponseException(HttpConstants.StatusCodes._501_NOT_IMPLEMENTED, "Not Implemented ("
                        + method + ")", this);
            }

            int splitIdx2 = line.indexOf(' ', splitIdx + 1);
            this.rawUri = line.substring(splitIdx + 1, splitIdx2);
            splitIdx = splitIdx2;
            if (this.rawUri.equals("*")) {
                throw new HttpResponseException(HttpConstants.StatusCodes._501_NOT_IMPLEMENTED,
                        "Not Implemented (wildcard URI)", this);
            }
            // TODO: Do we really want URI to validate this too?

            this.uri = Utils.normalizeURIPath(this.rawUri);

            this.httpVersionString = line.substring(splitIdx + 1);

            if (this.httpVersionString.equals("HTTP/1.1")) {
                this.httpVersion = 1.1;
            } else if (this.httpVersionString.equals("HTTP/1.0")) {
                this.httpVersion = 1.0;
            } else {
                throw new HttpResponseException(HttpConstants.StatusCodes._505_HTTP_VERSION_NOT_SUPPORTED,
                        "HTTP Version Not Supported (" + this.httpVersionString + ")", this);
            }
        } catch (RuntimeException e) {
            throw new HttpResponseException(HttpConstants.StatusCodes._400_BAD_REQUEST,
                    "Bad Request (malformed request line: " + e.getMessage() + ")", this, e);
        }

        //		// Method SP Request-URI SP HTTP-Version CRLF
        //		Matcher m = REQUEST_LINE.matcher(line);
        //		if (!m.matches()) {
        //			this.getLog().error(HttpRequestBuffer.class, "Malformed HTTP request header: [" + line + "]");
        //			throw new HttpResponseException(HttpConstants.StatusCodes._400_BAD_REQUEST,
        //					"Bad Request (malformed request line)", this);
        //		}
        //
        //		this.method = m.group(1).toUpperCase();
        //		if (!method.equals("POST")) {
        //			throw new HttpResponseException(HttpConstants.StatusCodes._501_NOT_IMPLEMENTED, "Not Implemented (" + method
        //					+ ")", this);
        //		}
        //
        //		this.uri = m.group(2);
        //		if (this.uri.equals("*")) {
        //			throw new HttpResponseException(HttpConstants.StatusCodes._501_NOT_IMPLEMENTED,
        //					"Not Implemented (wildcard URI)", this);
        //		}
        //		try {
        //			// Give Java a chance to do some additional validation.
        //			new URI(this.uri);
        //		} catch (URISyntaxException e) {
        //			this.getLog().error(HttpRequestBuffer.class, "Malformed URI in HTTP request header: [" + this.uri + "]");
        //			throw new HttpResponseException(HttpConstants.StatusCodes._400_BAD_REQUEST, "Bad Request (malformed URI)",
        //					this, e);
        //		}
        //
        //		String httpVersionString = m.group(3);
        //		try {
        //			this.httpVersion = this.parseHttpVersion(httpVersionString);
        //		} catch (IllegalArgumentException e) {
        //			this.getLog().error(HttpRequestBuffer.class, "Unsupported HTTP version: [" + httpVersionString + "]");
        //			throw new HttpResponseException(HttpConstants.StatusCodes._505_HTTP_VERSION_NOT_SUPPORTED,
        //					"HTTP Version Not Supported (" + httpVersionString + ")", this, e);
        //		}
    }

    @Override
    public double getHttpVersion() {
        return this.httpVersion;
    }

    @Override
    public String getHttpVersionString() {
        return this.httpVersionString;
    }

    @Override
    protected void validateHeaders() throws HttpBufferException {
        super.validateHeaders();

        if (!this.getMethod().equals(HttpConstants.Methods.GET)
                && this.getHeaderValue(HttpConstants.Headers.CONTENT_LENGTH) == null) {
            throw new MissingHeaderException(HttpConstants.Headers.CONTENT_LENGTH);
        }

        if (this.getHttpVersion() > 1.0) {
            // Host is a mandatory header in HTTP 1.1
            if (this.getHeaderValue(HttpConstants.Headers.HOST) == null) {
                throw new MissingHeaderException(HttpConstants.Headers.HOST);
            }
        }

        // Check if a Content-Encoding was specified and if so, look for a handler
        String contentEncoding = this.getHeaderValue(HttpConstants.Headers.CONTENT_ENCODING);
        if (contentEncoding != null && !contentEncoding.equalsIgnoreCase(HttpConstants.ContentEncoding.IDENTITY)) {
            if (this.encodingMap == null) {
                throw new UnsupportedHeaderException(HttpConstants.Headers.CONTENT_ENCODING, contentEncoding);
            }
            this.encoding = this.encodingMap.getEncoding(contentEncoding);
            if (this.encoding == null) {
                throw new UnsupportedHeaderException(HttpConstants.Headers.CONTENT_ENCODING, contentEncoding);
            }
        }

        String acceptEncoding = this.getHeaderValue(HttpConstants.Headers.ACCEPT_ENCODING);
        if (acceptEncoding != null) {
            this.unpackAcceptedEncodings(acceptEncoding);
        }
    }

    private static final Pattern COMMA_DELIM = Pattern.compile("\\s*,\\s*");
    private static final Pattern SEMICOLON_DELIM = Pattern.compile("\\s*;\\s*");
    private static final Pattern EQUALS_DELIM = Pattern.compile("\\s*=\\s*");

    private void unpackAcceptedEncodings(String acceptEncoding) throws InvalidHeaderException {
        acceptEncoding = acceptEncoding.trim();
        this.acceptedEncodings = new LinkedHashMap<>();
        if (!acceptEncoding.equals("")) {
            String[] encodings = COMMA_DELIM.split(acceptEncoding);
            for (String encoding2 : encodings) {
                String[] parts = SEMICOLON_DELIM.split(encoding2);
                if (parts.length > 2) {
                    throw new InvalidHeaderException(HttpConstants.Headers.ACCEPT_ENCODING, acceptEncoding);
                }
                String name = parts[0].toLowerCase();
                Float qvalue = null;
                if (parts.length == 2) {
                    parts = EQUALS_DELIM.split(parts[1]);
                    if (parts.length != 2) {
                        throw new InvalidHeaderException(HttpConstants.Headers.ACCEPT_ENCODING, acceptEncoding);
                    }
                    try {
                        qvalue = new Float(parts[1]);
                        if (qvalue.floatValue() < 0 || qvalue.floatValue() > 1.0) {
                            throw new InvalidHeaderException(HttpConstants.Headers.ACCEPT_ENCODING, acceptEncoding);
                        }
                    } catch (NumberFormatException e) {
                        throw new InvalidHeaderException(HttpConstants.Headers.ACCEPT_ENCODING, acceptEncoding, e);
                    }
                }

                if (this.acceptedEncodings.containsKey(name)) {
                    Float prevQvalue = this.acceptedEncodings.get(name);
                    if (QVALUE_CMP.compare(qvalue, prevQvalue) > 0) {
                        this.acceptedEncodings.put(name, qvalue);
                    }
                } else {
                    this.acceptedEncodings.put(name, qvalue);
                }
            }

            // Finally, sort the keys of the map we've built based on the
            // associated qvalues.
            // List<String> keys = new ArrayList<>(this.acceptedEncodings.keySet());
            Comparator<String> cmp2 = new Comparator<String>() {
                @Override
                public int compare(String name1, String name2) {
                    Float q1 = acceptedEncodings.get(name1);
                    Float q2 = acceptedEncodings.get(name2);
                    // We want them sorted highest to lowest and they can't be equal
                    // or we break the Map interface so tiebreak by sorted name1 ahead
                    // of name2 which means we fall back to order of occurrence
                    // in the Accept-Encoding header.
                    int v = QVALUE_CMP.compare(q2, q1);
                    return (v == 0) ? (1) : v;
                }
            };
            TreeMap<String, Float> tm = new TreeMap<>(cmp2);
            tm.putAll(this.acceptedEncodings);

            // Don't give back tm directly because it contains a
            // reference to acceptEncodings which it will want to use
            // every time it iterates. That gets out of hand quickly.
            this.acceptedEncodings = new LinkedHashMap<>();
            this.acceptedEncodings.putAll(tm);
        }

        // Lock this map down
        this.acceptedEncodings = Collections.unmodifiableMap(this.acceptedEncodings);
    }
}
