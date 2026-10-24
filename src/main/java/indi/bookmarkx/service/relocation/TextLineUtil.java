package indi.bookmarkx.service.relocation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class TextLineUtil {

    private TextLineUtil() {
    }

    static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.singletonList("");
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        return Arrays.asList(normalized.split("\n", -1));
    }

    static String normalizeLine(String line) {
        return line == null ? "" : line.trim().replaceAll("\\s+", " ");
    }

    static String hashLine(String line) {
        return sha256(normalizeLine(line));
    }

    static String hashText(String text) {
        return sha256(text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n'));
    }

    static String hashContext(List<String> lines, int fromInclusive, int toExclusive) {
        if (lines == null || lines.isEmpty()) {
            return sha256("");
        }
        StringBuilder builder = new StringBuilder();
        int from = Math.max(0, fromInclusive);
        int to = Math.min(lines.size(), toExclusive);
        for (int i = from; i < to; i++) {
            builder.append(normalizeLine(lines.get(i))).append('\n');
        }
        return sha256(builder.toString());
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
