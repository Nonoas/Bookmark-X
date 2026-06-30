package indi.bookmarkx.mcp;

public final class BookmarkMcpConfig {

    public static final int DEFAULT_PORT = 43182;
    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final String DEFAULT_ENDPOINT = "/mcp";

    private BookmarkMcpConfig() {
    }

    public static String endpointUrl(int port) {
        return "http://" + DEFAULT_HOST + ":" + port + DEFAULT_ENDPOINT;
    }
}
