package indi.bookmarkx.ui;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nonoas
 * @date 2024/12/14
 * @since
 */
public class BookmarkLineMarkerProvider implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (elements.isEmpty()) {
            return;
        }

        Project project = elements.get(0).getProject();
        BookmarksManager manager = BookmarksManager.getInstance(project);
        Map<String, Set<BookmarkNodeModel>> linesCache = manager.getFileMarksCache();

        Set<Integer> lineNumbers = new HashSet<>();
        for (PsiElement element : elements) {
            PsiFile file = element.getContainingFile();
            String path = file.getVirtualFile().getPath();

            Document document = file.getViewProvider().getDocument();
            int number = document.getLineNumber(element.getTextOffset());
            if (!lineNumbers.add(number)) {
                continue;
            }
            Optional<BookmarkNodeModel> nodeModel = linesCache.getOrDefault(path, new HashSet<>()).stream()
                    .filter(e -> Objects.equals(number, e.getLine()))
                    .findAny();

            if (nodeModel.isEmpty()) {
                continue;
            }

            LineMarkerInfo<PsiElement> markerInfo = new LineMarkerInfo<>(
                    element,
                    element.getTextRange(),
                    IconLoader.getIcon("/icons/bookmark.svg", getClass()),
                    Pass.LINE_MARKERS,
                    psiElement -> nodeModel.get().getUuid(),
                    (mouseEvent, psiElement) -> {
                        // 点击事件
                        System.out.println(nodeModel.get().getUuid());
                    },
                    GutterIconRenderer.Alignment.LEFT
            );
            result.add(markerInfo);
        }
    }
}
