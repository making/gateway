package am.ik.k8s;

public class RequestLogBuilder {

	private String address;

	private boolean crawler;

	private String date;

	private long elapsed;

	private String host;

	private String method;

	private String path;

	private String referer;

	private int status;

	private String userAgent;

	public RequestLog createRequestLog() {
		return new RequestLog(date, method, path, status, host, address, elapsed, crawler,
				userAgent, referer);
	}

	public RequestLogBuilder setAddress(String address) {
		this.address = address;
		return this;
	}

	public RequestLogBuilder setCrawler(boolean crawler) {
		this.crawler = crawler;
		return this;
	}

	public RequestLogBuilder setDate(String date) {
		this.date = date;
		return this;
	}

	public RequestLogBuilder setElapsed(long elapsed) {
		this.elapsed = elapsed;
		return this;
	}

	public RequestLogBuilder setHost(String host) {
		this.host = host;
		return this;
	}

	public RequestLogBuilder setMethod(String method) {
		this.method = method;
		return this;
	}

	public RequestLogBuilder setPath(String path) {
		this.path = path;
		return this;
	}

	public RequestLogBuilder setReferer(String referer) {
		this.referer = referer;
		return this;
	}

	public RequestLogBuilder setStatus(int status) {
		this.status = status;
		return this;
	}

	public RequestLogBuilder setUserAgent(String userAgent) {
		this.userAgent = userAgent;
		return this;
	}
}