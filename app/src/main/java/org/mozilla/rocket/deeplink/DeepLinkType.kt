package org.mozilla.rocket.deeplink

import android.content.Context
import android.net.Uri
import androidx.annotation.VisibleForTesting
import org.mozilla.rocket.deeplink.task.StartGameActivityTask
import org.mozilla.rocket.deeplink.task.Task

enum class DeepLinkType {

    GAME_HOME {
        override fun match(uri: Uri) =
            isContentLink(uri) && DeepLinkConstants.PATH_GAME == uri.path && uri.query.isNullOrEmpty()

        override fun addTasks(uri: Uri) {
            addTask(StartGameActivityTask())
        }
    },

    NOT_SUPPORT {
        override fun match(uri: Uri) = false

        override fun addTasks(uri: Uri) = Unit
    };

    private val taskList = ArrayList<Task>()

    protected abstract fun match(uri: Uri): Boolean

    protected abstract fun addTasks(uri: Uri)

    protected fun addTask(task: Task) {
        taskList.add(task)
    }

    @VisibleForTesting
    fun getTaskList(): List<Task> {
        return taskList
    }

    fun execute(context: Context) {
        for (task in taskList) {
            task.execute(context)
        }
    }

    companion object {
        fun parse(url: String): DeepLinkType {

            val uri = Uri.parse(url)

            for (type in values()) {
                if (type === NOT_SUPPORT) {
                    continue
                }

                if (type.match(uri)) {
                    type.addTasks(uri)

                    return type
                }
            }

            return NOT_SUPPORT
        }

        private fun isContentLink(uri: Uri) =
            DeepLinkConstants.SCHEMA_ROCKET == uri.scheme && DeepLinkConstants.HOST_CONTENT == uri.host
    }
}