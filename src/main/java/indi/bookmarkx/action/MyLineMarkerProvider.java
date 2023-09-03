package indi.bookmarkx.action;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.psi.PsiElement;


/**
 * @author Nonoas
 * @date 2023/9/3
 */

public class MyLineMarkerProvider implements LineMarkerProvider {
    @Override
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(PsiElement element) {

        return null; // 如果不满足条件，返回null，表示不显示标记
    }
}
