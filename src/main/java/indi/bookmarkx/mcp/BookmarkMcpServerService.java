package indi.bookmarkx.mcp;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowId;
import indi.bookmarkx.listener.SettingsListener;
import indi.bookmarkx.persistence.MySettings;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapperSupplier;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.json.schema.jackson3.JacksonJsonSchemaValidatorSupplier;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.transport.ServerTransportSecurityValidator;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.DispatcherType;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;

@Service(Service.Level.APP)
public final class BookmarkMcpServerService implements com.intellij.openapi.Disposable {

    private static final Logger LOG = Logger.getInstance(BookmarkMcpServerService.class);

    private final Object lock = new Object();
    private volatile RuntimeState runtimeState;

    public BookmarkMcpServerService() {
        ApplicationManager.getApplication().getMessageBus()
                .connect(this)
                .subscribe(SettingsListener.TOPIC, (SettingsListener) this::restartAsync);
        restartAsync();
    }

    private void restartAsync() {
        ApplicationManager.getApplication().executeOnPooledThread(this::restartFromSettings);
    }

    private void restartFromSettings() {
        SettingsSnapshot settings = SettingsSnapshot.from(MySettings.getInstance());
        synchronized (lock) {
            if (!settings.enabled()) {
                RuntimeState oldState = runtimeState;
                runtimeState = null;
                closeQuietly(oldState);
                return;
            }

            RuntimeState oldState = runtimeState;
            if (oldState != null && Objects.equals(oldState.settings(), settings)) {
                return;
            }

            try {
                runtimeState = start(settings);
                closeQuietly(oldState);
                LOG.info("Bookmark-X MCP server started at " + settings.endpointUrl());
            } catch (Exception ex) {
                LOG.warn("Failed to start Bookmark-X MCP server", ex);
                notifyError("Bookmark-X MCP server failed to start on " + settings.endpointUrl() + ": " + ex.getMessage());
            }
        }
    }

    private RuntimeState start(SettingsSnapshot settings) throws Exception {
        McpJsonMapper jsonMapper = new JacksonMcpJsonMapperSupplier().get();
        JsonSchemaValidator jsonSchemaValidator = new JacksonJsonSchemaValidatorSupplier().get();
        HttpServletStreamableServerTransportProvider transportProvider =
                HttpServletStreamableServerTransportProvider.builder()
                        .jsonMapper(jsonMapper)
                        .mcpEndpoint(BookmarkMcpConfig.DEFAULT_ENDPOINT)
                        .keepAliveInterval(Duration.ofSeconds(15))
                        .securityValidator(ServerTransportSecurityValidator.NOOP)
                        .build();

        McpSyncServer mcpServer = McpServer.sync(transportProvider)
                .serverInfo("bookmark-x", "3.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .jsonMapper(jsonMapper)
                .jsonSchemaValidator(jsonSchemaValidator)
                .tools(BookmarkMcpTools.toolSpecifications(jsonMapper))
                .build();

        Server jettyServer = new Server();
        ServerConnector connector = new ServerConnector(jettyServer);
        connector.setHost(BookmarkMcpConfig.DEFAULT_HOST);
        connector.setPort(settings.port());
        jettyServer.addConnector(connector);

        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        contextHandler.setContextPath("/");
        contextHandler.addFilter(new FilterHolder(new BookmarkMcpSecurityFilter(settings)), "/*", EnumSet.of(DispatcherType.REQUEST));
        contextHandler.addServlet(new ServletHolder(transportProvider), "/*");
        jettyServer.setHandler(contextHandler);
        jettyServer.start();

        return new RuntimeState(settings, jettyServer, transportProvider, mcpServer);
    }

    private void closeQuietly(RuntimeState state) {
        if (state == null) {
            return;
        }
        try {
            state.close();
        } catch (Exception ex) {
            LOG.warn("Failed to stop Bookmark-X MCP server cleanly", ex);
        }
    }

    private void notifyError(String message) {
        Notification notification = new Notification(
                ToolWindowId.PROJECT_VIEW,
                "Bookmark-X MCP",
                message,
                NotificationType.ERROR
        );
        Notifications.Bus.notify(notification);
    }

    @Override
    public void dispose() {
        synchronized (lock) {
            RuntimeState oldState = runtimeState;
            runtimeState = null;
            closeQuietly(oldState);
        }
    }

    record SettingsSnapshot(boolean enabled, int port, String password) {
        static SettingsSnapshot from(MySettings settings) {
            return new SettingsSnapshot(
                    settings.isMcpEnabled(),
                    settings.getMcpPort(),
                    StringUtil.notNullize(settings.getMcpPassword())
            );
        }

        String endpointUrl() {
            return BookmarkMcpConfig.endpointUrl(port);
        }
    }

    private record RuntimeState(SettingsSnapshot settings,
                                Server jettyServer,
                                HttpServletStreamableServerTransportProvider transportProvider,
                                McpSyncServer mcpServer) {
        void close() throws Exception {
            try {
                mcpServer.closeGracefully();
            } catch (Exception ignored) {
            }
            try {
                transportProvider.closeGracefully().block(Duration.ofSeconds(5));
            } catch (Exception ignored) {
            }
            try {
                transportProvider.destroy();
            } catch (Exception ignored) {
            }
            if (jettyServer.isStarted() || jettyServer.isStarting()) {
                jettyServer.stop();
            }
        }
    }
}
