package indi.bookmarkx.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import indi.bookmarkx.service.BookmarkDomainService;
import indi.bookmarkx.service.dto.BookmarkDraft;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.lang.reflect.Type;
import java.util.*;

final class BookmarkMcpTools {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final Gson PRETTY_GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    private static final Type BOOKMARK_DRAFT_LIST_TYPE = new TypeToken<List<BookmarkDraft>>() {
    }.getType();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    private BookmarkMcpTools() {
    }

    static McpServerFeatures.SyncToolSpecification[] toolSpecifications(McpJsonMapper jsonMapper) {
        return new McpServerFeatures.SyncToolSpecification[]{
                tool(jsonMapper, "bookmark_list_projects",
                        "List open IntelliJ projects currently served by Bookmark-X.",
                        """
                                {
                                  "type": "object",
                                  "properties": {}
                                }
                                """,
                        BookmarkMcpTools::listProjects),
                tool(jsonMapper, "bookmark_list",
                        "List bookmarks. projectPath is optional when a single IntelliJ project is open. filePath may be absolute or project-relative. groupPath is an optional nested group path. Returned lines are 1-based.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "projectPath": { "type": "string" },
                                    "filePath": { "type": "string" },
                                    "groupPath": {
                                      "type": "array",
                                      "items": { "type": "string" }
                                    }
                                  }
                                }
                                """,
                        BookmarkMcpTools::listBookmarks),
                tool(jsonMapper, "bookmark_get_tree",
                        "Return the complete Bookmark-X tree for the target IntelliJ project.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "projectPath": { "type": "string" }
                                  }
                                }
                                """,
                        BookmarkMcpTools::getTree),
                tool(jsonMapper, "bookmark_create",
                        "Create a single bookmark. line is 1-based. If groupPath does not exist it will be created automatically.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "projectPath": { "type": "string" },
                                    "filePath": { "type": "string" },
                                    "line": { "type": "integer" },
                                    "name": { "type": "string" },
                                    "desc": { "type": "string" },
                                    "groupPath": {
                                      "type": "array",
                                      "items": { "type": "string" }
                                    }
                                  },
                                  "required": ["filePath", "line"]
                                }
                                """,
                        BookmarkMcpTools::createBookmark),
                tool(jsonMapper, "bookmark_batch_create",
                        "Create multiple bookmarks. Each bookmark line is 1-based. If autoApply is false, Bookmark-X returns a preview without mutating the project.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "projectPath": { "type": "string" },
                                    "groupPath": {
                                      "type": "array",
                                      "items": { "type": "string" }
                                    },
                                    "autoApply": { "type": "boolean" },
                                    "bookmarks": {
                                      "type": "array",
                                      "items": {
                                        "type": "object",
                                        "properties": {
                                          "filePath": { "type": "string" },
                                          "line": { "type": "integer" },
                                          "name": { "type": "string" },
                                          "desc": { "type": "string" },
                                          "groupPath": {
                                            "type": "array",
                                            "items": { "type": "string" }
                                          }
                                        },
                                        "required": ["filePath", "line"]
                                      }
                                    }
                                  },
                                  "required": ["bookmarks"]
                                }
                                """,
                        BookmarkMcpTools::batchCreateBookmarks),
                tool(jsonMapper, "bookmark_update",
                        "Update a bookmark by UUID. line is 1-based. groupPath moves the bookmark to another group if provided.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "projectPath": { "type": "string" },
                                    "uuid": { "type": "string" },
                                    "line": { "type": "integer" },
                                    "name": { "type": "string" },
                                    "desc": { "type": "string" },
                                    "groupPath": {
                                      "type": "array",
                                      "items": { "type": "string" }
                                    }
                                  },
                                  "required": ["uuid"]
                                }
                                """,
                        BookmarkMcpTools::updateBookmark),
                tool(jsonMapper, "bookmark_delete",
                        "Delete a single bookmark by UUID.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "projectPath": { "type": "string" },
                                    "uuid": { "type": "string" }
                                  },
                                  "required": ["uuid"]
                                }
                                """,
                        BookmarkMcpTools::deleteBookmark),
                tool(jsonMapper, "bookmark_batch_delete",
                        "Delete multiple bookmarks by UUID.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "projectPath": { "type": "string" },
                                    "uuids": {
                                      "type": "array",
                                      "items": { "type": "string" }
                                    }
                                  },
                                  "required": ["uuids"]
                                }
                                """,
                        BookmarkMcpTools::batchDeleteBookmarks),
                tool(jsonMapper, "bookmark_reorder_in_group",
                        "Reorder bookmarks inside a flat group by passing the complete UUID order. groupPath identifies the group. Returned indexInGroup is 0-based.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "projectPath": { "type": "string" },
                                    "groupPath": {
                                      "type": "array",
                                      "items": { "type": "string" }
                                    },
                                    "uuids": {
                                      "type": "array",
                                      "items": { "type": "string" }
                                    }
                                  },
                                  "required": ["groupPath", "uuids"]
                                }
                                """,
                        BookmarkMcpTools::reorderInGroup),
                tool(jsonMapper, "bookmark_move_in_group",
                        "Move a bookmark to a targetIndex inside a flat group. targetIndex is 0-based.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "projectPath": { "type": "string" },
                                    "groupPath": {
                                      "type": "array",
                                      "items": { "type": "string" }
                                    },
                                    "uuid": { "type": "string" },
                                    "targetIndex": { "type": "integer" }
                                  },
                                  "required": ["groupPath", "uuid", "targetIndex"]
                                }
                                """,
                        BookmarkMcpTools::moveInGroup),
                tool(jsonMapper, "bookmark_upsert_reading_tour",
                        "Create or update a reading tour. Bookmarks are created or updated in the order provided and then reordered to match that order. If autoApply is false, Bookmark-X returns a preview only.",
                        """
                                {
                                  "type": "object",
                                  "properties": {
                                    "projectPath": { "type": "string" },
                                    "groupPath": {
                                      "type": "array",
                                      "items": { "type": "string" }
                                    },
                                    "autoApply": { "type": "boolean" },
                                    "bookmarks": {
                                      "type": "array",
                                      "items": {
                                        "type": "object",
                                        "properties": {
                                          "uuid": { "type": "string" },
                                          "filePath": { "type": "string" },
                                          "line": { "type": "integer" },
                                          "name": { "type": "string" },
                                          "desc": { "type": "string" },
                                          "groupPath": {
                                            "type": "array",
                                            "items": { "type": "string" }
                                          }
                                        },
                                        "required": ["filePath", "line"]
                                      }
                                    }
                                  },
                                  "required": ["bookmarks"]
                                }
                                """,
                        BookmarkMcpTools::upsertReadingTour)
        };
    }

    private static McpServerFeatures.SyncToolSpecification tool(McpJsonMapper jsonMapper,
                                                                String name,
                                                                String description,
                                                                String inputSchema,
                                                                ToolAction action) {
        return new McpServerFeatures.SyncToolSpecification(
                McpSchema.Tool.builder()
                        .name(name)
                        .description(description)
                        .inputSchema(jsonMapper, inputSchema)
                        .build(),
                (exchange, request) -> executeTool(jsonMapper, request, action)
        );
    }

    private static McpSchema.CallToolResult executeTool(McpJsonMapper jsonMapper,
                                                        McpSchema.CallToolRequest request,
                                                        ToolAction action) {
        try {
            JsonElement arguments = GSON.toJsonTree(request.arguments());
            JsonObject payload = arguments != null && arguments.isJsonObject()
                    ? arguments.getAsJsonObject()
                    : new JsonObject();
            Object result = action.execute(payload);
            String json = GSON.toJson(result);
            return McpSchema.CallToolResult.builder()
                    .structuredContent(jsonMapper, json)
                    .addTextContent(PRETTY_GSON.toJson(result))
                    .build();
        } catch (Exception ex) {
            Map<String, String> error = Map.of(
                    "error",
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()
            );
            String json = GSON.toJson(error);
            return McpSchema.CallToolResult.builder()
                    .isError(true)
                    .structuredContent(jsonMapper, json)
                    .addTextContent(PRETTY_GSON.toJson(error))
                    .build();
        }
    }

    private static Object listProjects(JsonObject payload) {
        List<Map<String, String>> projects = new ArrayList<>();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.getBasePath() == null) {
                continue;
            }
            Map<String, String> info = new LinkedHashMap<>();
            info.put("name", project.getName());
            info.put("path", normalizeProjectPath(project.getBasePath()));
            projects.add(info);
        }
        return projects;
    }

    private static Object listBookmarks(JsonObject payload) {
        BookmarkDomainService service = resolveService(payload);
        String filePath = getString(payload, "filePath");
        return service.listBookmarks(filePath, getStringList(payload, "groupPath"));
    }

    private static Object getTree(JsonObject payload) {
        return resolveService(payload).getBookmarkTree();
    }

    private static Object createBookmark(JsonObject payload) {
        return resolveService(payload).createBookmark(GSON.fromJson(payload, BookmarkDraft.class));
    }

    private static Object batchCreateBookmarks(JsonObject payload) {
        BookmarkDomainService service = resolveService(payload);
        List<BookmarkDraft> drafts = getBookmarkDrafts(payload, "bookmarks");
        List<String> defaultGroupPath = getStringList(payload, "groupPath");
        boolean autoApply = getBoolean(payload, "autoApply", true);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applied", autoApply);
        result.put("bookmarks", autoApply
                ? service.batchCreateBookmarks(drafts, defaultGroupPath)
                : service.previewBookmarks(drafts, defaultGroupPath));
        return result;
    }

    private static Object updateBookmark(JsonObject payload) {
        return resolveService(payload).updateBookmark(GSON.fromJson(payload, BookmarkDraft.class));
    }

    private static Object deleteBookmark(JsonObject payload) {
        return resolveService(payload).deleteBookmark(getRequiredString(payload, "uuid"));
    }

    private static Object batchDeleteBookmarks(JsonObject payload) {
        return resolveService(payload).deleteBookmarks(getRequiredStringList(payload, "uuids"));
    }

    private static Object reorderInGroup(JsonObject payload) {
        return resolveService(payload).reorderInGroup(
                getStringList(payload, "groupPath"),
                getRequiredStringList(payload, "uuids")
        );
    }

    private static Object moveInGroup(JsonObject payload) {
        return resolveService(payload).moveInGroup(
                getStringList(payload, "groupPath"),
                getRequiredString(payload, "uuid"),
                getRequiredInt(payload, "targetIndex")
        );
    }

    private static Object upsertReadingTour(JsonObject payload) {
        BookmarkDomainService service = resolveService(payload);
        List<BookmarkDraft> drafts = getBookmarkDrafts(payload, "bookmarks");
        List<String> defaultGroupPath = getStringList(payload, "groupPath");
        boolean autoApply = getBoolean(payload, "autoApply", true);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("applied", autoApply);
        result.put("bookmarks", autoApply
                ? service.upsertReadingTour(defaultGroupPath, drafts)
                : service.previewBookmarks(drafts, defaultGroupPath));
        return result;
    }

    private static BookmarkDomainService resolveService(JsonObject payload) {
        return BookmarkDomainService.getInstance(resolveProject(payload));
    }

    private static Project resolveProject(JsonObject payload) {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length == 0) {
            throw new IllegalArgumentException("No open IntelliJ projects are available");
        }

        String projectPath = getString(payload, "projectPath");
        if (projectPath == null || projectPath.isBlank()) {
            if (openProjects.length == 1) {
                return openProjects[0];
            }
            throw new IllegalArgumentException("Multiple open IntelliJ projects found; projectPath is required");
        }

        String normalized = normalizeProjectPath(projectPath);
        for (Project project : openProjects) {
            if (project.getBasePath() == null) {
                continue;
            }
            if (Objects.equals(normalizeProjectPath(project.getBasePath()), normalized)) {
                return project;
            }
        }
        throw new IllegalArgumentException("No open IntelliJ project matches path: " + normalized);
    }

    private static String normalizeProjectPath(String path) {
        return java.nio.file.Path.of(path).toAbsolutePath().normalize().toString().replace('\\', '/');
    }

    private static String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null;
    }

    private static String getRequiredString(JsonObject object, String key) {
        String value = getString(object, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return value;
    }

    private static int getRequiredInt(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static boolean getBoolean(JsonObject object, String key, boolean defaultValue) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsBoolean() : defaultValue;
    }

    private static List<String> getStringList(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return List.of();
        }
        return GSON.fromJson(object.get(key), STRING_LIST_TYPE);
    }

    private static List<String> getRequiredStringList(JsonObject object, String key) {
        List<String> values = getStringList(object, key);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Missing required list field: " + key);
        }
        return values;
    }

    private static List<BookmarkDraft> getBookmarkDrafts(JsonObject object, String key) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return List.of();
        }
        return GSON.fromJson(object.get(key), BOOKMARK_DRAFT_LIST_TYPE);
    }

    @FunctionalInterface
    private interface ToolAction {
        Object execute(JsonObject payload);
    }
}
