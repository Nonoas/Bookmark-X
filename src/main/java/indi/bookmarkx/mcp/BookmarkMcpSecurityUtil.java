package indi.bookmarkx.mcp;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class BookmarkMcpSecurityUtil {

    private BookmarkMcpSecurityUtil() {
    }

    public static boolean isLoopback(String remoteAddress) {
        if (StringUtils.isBlank(remoteAddress)) {
            return false;
        }
        try {
            return InetAddress.getByName(remoteAddress.trim()).isLoopbackAddress();
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isAllowedHost(String hostHeader, int port) {
        if (StringUtils.isBlank(hostHeader)) {
            return false;
        }
        return allowedHosts(port).contains(normalizeHost(hostHeader));
    }

    public static boolean isAllowedOrigin(String originHeader, int port) {
        if (StringUtils.isBlank(originHeader)) {
            return true;
        }
        try {
            URI uri = URI.create(originHeader.trim());
            String host = uri.getHost();
            if (StringUtils.isBlank(host)) {
                return false;
            }
            int originPort = uri.getPort();
            if (originPort != -1 && originPort != port) {
                return false;
            }
            return allowedOriginHosts().contains(host.toLowerCase(Locale.ROOT));
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isAuthorized(String configuredPassword, String authorizationHeader) {
        if (StringUtils.isBlank(configuredPassword)) {
            return true;
        }
        if (StringUtils.isBlank(authorizationHeader)) {
            return false;
        }
        String prefix = "bearer ";
        String normalized = authorizationHeader.trim();
        if (!normalized.toLowerCase(Locale.ROOT).startsWith(prefix)) {
            return false;
        }
        String token = normalized.substring(prefix.length()).trim();
        return configuredPassword.equals(token);
    }

    private static Set<String> allowedHosts(int port) {
        return Set.of(
                "127.0.0.1",
                "127.0.0.1:" + port,
                "localhost",
                "localhost:" + port
        );
    }

    private static Set<String> allowedOriginHosts() {
        return Set.of("127.0.0.1", "localhost");
    }

    private static String normalizeHost(String hostHeader) {
        return hostHeader.trim().toLowerCase(Locale.ROOT);
    }
}
