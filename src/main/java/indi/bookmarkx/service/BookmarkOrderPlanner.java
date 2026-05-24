package indi.bookmarkx.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class BookmarkOrderPlanner {

    private BookmarkOrderPlanner() {
    }

    static List<String> reorder(List<String> currentOrder, List<String> requestedOrder) {
        List<String> current = sanitize(currentOrder);
        List<String> requested = sanitize(requestedOrder);
        validateNoDuplicates(current, "current order");
        validateNoDuplicates(requested, "requested order");
        if (current.size() != requested.size()) {
            throw new IllegalArgumentException("Requested order size does not match current bookmark count");
        }
        if (!new HashSet<>(current).equals(new HashSet<>(requested))) {
            throw new IllegalArgumentException("Requested order must contain the same bookmark UUIDs as the target group");
        }
        return requested;
    }

    static List<String> move(List<String> currentOrder, String uuid, int targetIndex) {
        List<String> current = sanitize(currentOrder);
        validateNoDuplicates(current, "current order");
        if (!current.contains(uuid)) {
            throw new IllegalArgumentException("Bookmark UUID does not belong to the target group: " + uuid);
        }
        if (targetIndex < 0 || targetIndex >= current.size()) {
            throw new IllegalArgumentException("Target index is out of range: " + targetIndex);
        }
        List<String> reordered = new ArrayList<>(current);
        reordered.remove(uuid);
        reordered.add(targetIndex, uuid);
        return reordered;
    }

    private static List<String> sanitize(List<String> uuids) {
        if (uuids == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String uuid : uuids) {
            if (uuid == null || uuid.isBlank()) {
                continue;
            }
            result.add(uuid);
        }
        return result;
    }

    private static void validateNoDuplicates(List<String> uuids, String label) {
        Set<String> unique = new HashSet<>(uuids);
        if (unique.size() != uuids.size()) {
            throw new IllegalArgumentException("Duplicate bookmark UUIDs are not allowed in " + label);
        }
    }
}
