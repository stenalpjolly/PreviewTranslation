package com.synamedia.stenal.ctap.previewtranslation.listeners;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Optional;

public class PreviewTranslation extends AnAction {

    @Override
    public void update(@NotNull final AnActionEvent e) {
        // Get required data keys
        final Project project = e.getProject();
        final Editor editor = e.getData(CommonDataKeys.EDITOR);

        // Set visibility only in case of existing project and editor and if a selection exists
        e.getPresentation().setEnabledAndVisible(project != null
                && editor != null
                && editor.getSelectionModel().hasSelection());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        final Editor editor = anActionEvent.getRequiredData(CommonDataKeys.EDITOR);
        final Project project = anActionEvent.getRequiredData(CommonDataKeys.PROJECT);
        final Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        final String selectedText = primaryCaret.getSelectedText();

        Collection<VirtualFile> csvFiles = FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME,
                FileTypeManager.getInstance().getFileTypeByFileName("languageMap.csv"),
                GlobalSearchScope.projectScope(project));

        StringBuilder stringBuilder = new StringBuilder();

        csvFiles.stream()
                .parallel()
                .forEach(virtualFile -> {
                    try {
                        InputStreamReader inputStreamReader = new InputStreamReader(virtualFile.getInputStream(), virtualFile.getCharset());
                        CSVParser csvRecords = new CSVParser(inputStreamReader, CSVFormat.DEFAULT);

                        Optional<CSVRecord> first = csvRecords.getRecords()
                                .parallelStream()
                                .filter(strings -> strings.get(0).equals(selectedText))
                                .findFirst();
                        first.ifPresent(csvRecord -> stringBuilder.append("Plugin: ")
                                .append(virtualFile.getParent().getName())
                                .append(", ")
                                .append(virtualFile.getName())
                                .append("\n")
                                .append("<b>English</b>: ")
                                .append(csvRecord.get(1))
                                .append("\n"));

                    } catch (IOException e) {
                        //Ignore
                    }
                });

        if (stringBuilder.length() > 0) {
            Messages.showMessageDialog(project, stringBuilder.toString(), "Translation", Messages.getInformationIcon());
        }

    }
}
