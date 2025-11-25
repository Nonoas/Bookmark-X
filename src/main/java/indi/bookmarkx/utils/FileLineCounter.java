package indi.bookmarkx.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

public class FileLineCounter {

    private static final Logger log = Logger.getInstance(FileLineCounter.class);

    /**
     * 获取文件的最大行数
     *
     * @param descriptor 文件描述符
     * @return 文件行数，如果失败返回-1
     */
    public static int getFileMaxLine(@Nullable OpenFileDescriptor descriptor) {
        if (descriptor == null) {
            return -1;
        }
        try {
            VirtualFile file = descriptor.getFile();
            if (file != null) {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document != null) {
                    return document.getLineCount();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get file line count", e);
        }
        return -1;
    }
}
