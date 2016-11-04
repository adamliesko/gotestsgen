package com.adamliesko.gotests.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class TestGenerator extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        String fileName;

        final Project project = e.getProject();

        assert project != null;
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

        assert editor != null;
        SelectionModel selection = editor.getSelectionModel();

        // fetch file name
        final Document document = editor.getDocument();
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);

        assert virtualFile != null;
        fileName = virtualFile.getPath();

        // fetch selected function name
        String funcName = selection.getSelectedText();
        String[] commands = {"/bin/bash", "-c", "gotests -w -only=\"^" + funcName + "$\" " + fileName};

        Runtime rt = Runtime.getRuntime();
        Process proc = null;
        try {
            proc = rt.exec(commands);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        assert proc != null;
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        String stdOutString = "", stdErrString = "";

        try {
            stdOutString = getStdContent(stdInput);
            stdErrString = getStdContent(stdError);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // get the output component
        StatusBar statusBar = WindowManager.getInstance()
                .getStatusBar(CommonDataKeys.PROJECT.getData(e.getDataContext()));

        // print output
        if (("").equals(stdErrString)) {
            popup("<strong>Gotestgen:</strong> " + stdOutString, MessageType.INFO, 5000, statusBar);

        } else {
            popup("<strong>Gotestgen:</strong> " + stdErrString, MessageType.ERROR, 5000, statusBar);
        }
    }

    private String getStdContent(BufferedReader input) throws IOException {
        String s;
        String output = "";
        while ((s = input.readLine()) != null) {
            output = output.concat(s);
        }
        return output;
    }

    private void popup(String string, MessageType msgType, int timeout, StatusBar disposable) {
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(string, msgType, null)
                .setFadeoutTime(timeout)
                .createBalloon()
                .show(RelativePoint.getCenterOf(disposable.getComponent()),
                        Balloon.Position.atRight);
    }
}
