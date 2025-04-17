package com.dalsegnosolutions.ij.swrf.editor

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

fun interface OnCloseCallback : FileEditorManagerListener {
    override fun fileClosed(source: FileEditorManager, file: VirtualFile)
}

abstract class StepFoldingLevel(private val delta: Int) : AnAction() {

    companion object {
        private val executedInFiles = mutableSetOf<VirtualFile>()
    }

    private val AnActionEvent.editor
        get() = CommonDataKeys.EDITOR.getData(dataContext)

    private val AnActionEvent.isFirstRunOnFile
        get() = editor?.virtualFile?.let { executedInFiles.add(it) } == true

    override fun actionPerformed(event: AnActionEvent) {
        val foldRegions = event.editor?.foldingModel?.allFoldRegions ?: return
        val root = object : Region() {
            override fun isExpanded() = true
            override fun cnt(other: Region) = true
            override fun getLevel(limit: Int) = 0
        }
        foldRegions.map(::Region).forEach(root::insert)

        val effectiveLevel = root.maxOf { it.getLevel() }
        val isFirstRunOnFile = event.isFirstRunOnFile.also {
            if (it && executedInFiles.size == 1)
                event.project?.messageBus?.connect()?.run {
                    val closeCallback = OnCloseCallback { source, file ->
                        executedInFiles.remove(file)
                        if (executedInFiles.isEmpty()) disconnect()
                    }
                    subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, closeCallback)
                }
        }
        val desiredLevel = when {
            // this is DecreaseFoldLevel && isFirstRunOnFile -> if (tree.size == 1 && effectiveLevel > 1) 1 else 0
            this is IncrementFoldLevel && isFirstRunOnFile -> 6
            else -> effectiveLevel + delta
        }

        val actionName = when {
            desiredLevel < 1 -> "CollapseAllRegions"
            desiredLevel > 5 -> "ExpandAllRegions"
            else -> "ExpandAllToLevel$desiredLevel"
        }

        event.apply {
            val levelAction = actionManager.getAction(actionName) ?: return
            ActionUtil.invokeAction(levelAction, this, null)
        }
    }
}

class IncrementFoldLevel : StepFoldingLevel(1)

class DecreaseFoldLevel : StepFoldingLevel(-1)

internal open class Region(private val region: FoldRegion? = null) : ArrayList<Region>() {
    open fun isExpanded(): Boolean = region?.isExpanded ?: throw UninitializedPropertyAccessException()
    open fun cnt(other: Region): Boolean = region!!.textRange.contains(other.region!!.textRange)
    open fun getLevel(limit: Int = 6): Int = when {
        !isExpanded() || limit == 0 -> 0
        else -> 1 + (asSequence().map { it.getLevel(limit - 1) }
            .runningReduce(::maxOf)
            .withIndex()
            .firstOrNull { (index, level) -> level >= limit || index == lastIndex }
            ?.value ?: 0)
    }

    fun insert(region: Region) {
        firstOrNull { it.cnt(region) }?.insert(region) ?: add(region)
    }
}
