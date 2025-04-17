package com.dalsegnosolutions.ij.swrf.editor

import com.intellij.codeInsight.generation.CommentByBlockCommentHandler
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.openapi.command.WriteCommandAction.writeCommandAction
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.util.startOffset

class MMBSelectHandler : EditorMouseListener {
    override fun mouseReleased(event: EditorMouseEvent) {
        if (event.mouseEvent.mouseButton != MouseButton.Middle) return
        val (editor, virtualFile) = with(event.editor) { Pair(this, virtualFile) }
        if (virtualFile == null) return
        val project = editor.project ?: return

        val onlyCaret = editor.caretModel.allCarets.singleOrNull() ?: return
        val commentHandler = CommentByBlockCommentHandler()
        val psiFile = virtualFile.findPsiFile(project) ?: return
        onlyCaret.selectionRange.run {
            if (isEmpty) return
            val elementAtStart = psiFile.findElementAt(startOffset)
            val elementAtEnd = psiFile.findElementAt(endOffset)
            val intersectsAny = setOf(
                elementAtStart?.let { it.startOffset < startOffset },
                elementAtEnd?.let { it.startOffset < endOffset })
                .contains(true)
            if (intersectsAny) return
        }
        writeCommandAction(project)
            .run<Exception> { commentHandler.invoke(project, editor, onlyCaret, psiFile) }
    }
}
