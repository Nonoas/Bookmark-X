package indi.bookmarkx.ui;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import indi.bookmarkx.BookmarksManager;
import indi.bookmarkx.action.BookmarkEditAction;
import indi.bookmarkx.action.BookmarkRemoveAction;
import indi.bookmarkx.common.MyIcons;
import indi.bookmarkx.global.FileMarksCache;
import indi.bookmarkx.model.BookmarkNodeModel;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Nonoas
 * @date 2024/12/14
 * @since 2.2.0
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
        FileMarksCache fileMarksCache = manager.getFileMarksCache();

        Set<Integer> lineNumbers = new HashSet<>();
        for (PsiElement element : elements) {
            PsiFile file = element.getContainingFile();
            String path = file.getVirtualFile().getPath();

            Document document = file.getViewProvider().getDocument();
            int number = document.getLineNumber(element.getTextOffset());
            if (!lineNumbers.add(number)) {
                continue;
            }
            Optional<BookmarkNodeModel> nodeModel = fileMarksCache.findModel(path, number);

            if (nodeModel.isEmpty()) {
                continue;
            }

            LineMarkerInfo<PsiElement> markerInfo = new BkLineMarkerInfo(
                    nodeModel.get(),
                    element,
                    (ev, el) -> {
                    }
            );
            result.add(markerInfo);
        }
    }


    /**
     * 书签的标签信息类，存储行标记中的书签信息，指定标签文本，图标样式
     */
    static class BkLineMarkerInfo extends LineMarkerInfo<PsiElement> {

        private final BookmarkNodeModel model;

        public BkLineMarkerInfo(BookmarkNodeModel model,
                                @NotNull PsiElement element,
                                GutterIconNavigationHandler<PsiElement> navHandler) {
            super(element, element.getTextRange(), MyIcons.BOOKMARK, null, navHandler, GutterIconRenderer.Alignment.LEFT, model::getName);
            this.model = model;
        }

        @Override
        public GutterIconRenderer createGutterRenderer() {
            return new LineMarkerGutterIconRenderer<>(this) {

                @Override
                public @NotNull ActionGroup getPopupMenuActions() {
                    DefaultActionGroup actionGroup = new DefaultActionGroup();
                    actionGroup.add(new BookmarkEditAction(model));
                    actionGroup.add(new BookmarkRemoveAction(model));
                    return actionGroup;
                }
            };
        }

        @Override
        public String getLineMarkerTooltip() {
            return model.getDesc();
        }
    }
}
