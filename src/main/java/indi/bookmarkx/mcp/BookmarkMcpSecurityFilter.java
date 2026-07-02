package indi.bookmarkx.mcp;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

final class BookmarkMcpSecurityFilter implements Filter {

    private final BookmarkMcpServerService.SettingsSnapshot settings;

    BookmarkMcpSecurityFilter(BookmarkMcpServerService.SettingsSnapshot settings) {
        this.settings = settings;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!BookmarkMcpSecurityUtil.isLoopback(httpRequest.getRemoteAddr())) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Only loopback clients may access Bookmark-X MCP");
            return;
        }

        if (!BookmarkMcpSecurityUtil.isAllowedHost(httpRequest.getHeader("Host"), httpRequest.getServerPort())) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Host header");
            return;
        }

        if (!BookmarkMcpSecurityUtil.isAllowedOrigin(httpRequest.getHeader("Origin"), httpRequest.getServerPort())) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid Origin header");
            return;
        }

        if (!BookmarkMcpSecurityUtil.isAuthorized(settings.password(), httpRequest.getHeader("Authorization"))) {
            httpResponse.setHeader("WWW-Authenticate", "Bearer");
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid MCP password");
            return;
        }

        chain.doFilter(request, response);
    }
}
