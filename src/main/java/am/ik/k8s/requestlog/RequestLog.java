package am.ik.k8s.requestlog;

import brave.Span;
import brave.propagation.TraceContext;

import java.util.Objects;

public class RequestLog {
    private String date;
    private String method;
    private String path;
    private int status;
    private String host;
    private String address;
    private long elapsed;
    private boolean crawler;
    private String userAgent;
    private String referer;

    public RequestLog(String date, String method, String path, int status, String host,
                      String address, long elapsed, boolean crawler, String userAgent,
                      String referer) {
        this.date = date;
        this.method = method;
        this.path = path;
        this.status = status;
        this.host = host;
        this.address = address;
        this.elapsed = elapsed;
        this.crawler = crawler;
        this.userAgent = userAgent;
        this.referer = referer;
    }

    public RequestLog() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getElapsed() {
        return elapsed;
    }

    public void setElapsed(long elapsed) {
        this.elapsed = elapsed;
    }

    public boolean isCrawler() {
        return crawler;
    }

    public void setCrawler(boolean crawler) {
        this.crawler = crawler;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    @Override
    public String toString() {
        return "date:" + this.date + "\thost:" + this.host
                + "\tmethod:" + this.method + "\tpath:" + this.path
                + "\tstatus:" + this.status + "\taddress:"
                + this.address + "\telapsed:" + this.elapsed + "ms\tcrawler:"
                + this.crawler + "\tuser-agent:" + this.userAgent + "\treferer:"
                + this.referer;
    }

    /**
     * @see https://docs.cloudfoundry.org/concepts/architecture/router.html#about-access-logs
     */
    public String goRouterCompliant(String xForwardedFor, String xForwardedProto, Span span) {
        final TraceContext context = span.context();
        return String.format("%s - [%s] \"%s %s %s\" %d %d %d \"%s\" \"%s\" \"%s\" \"-\" x_forwarded_for:\"%s\" x_forwarded_proto:\"%s\" vcap_request_id:\"-\" response_time:%s gorouter_time:0.0 app_id:\"-\" app_index:\"-\" x_b3_traceid:\"%s\" x_b3_spanid:\"%s\" x_b3_parentspanid:\"%s\" b3:\"-\"",
                this.host, this.date, this.method, this.path, "HTTP/1.1" /* TODO */, this.status, 0 /* TODO */, 0 /* TODO */,
                Objects.toString(this.referer, "-"),
                this.userAgent,
                this.address,
                Objects.toString(xForwardedFor, "-"),
                Objects.toString(xForwardedProto, "-"),
                this.elapsed / 1000.0, context.traceIdString(),
                context.spanIdString(),
                Objects.toString(context.parentIdString(), "-"));
    }
}
