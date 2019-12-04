package org.mozilla.rocket.deeplink

import android.content.Context
import androidx.annotation.VisibleForTesting
import org.mozilla.rocket.deeplink.task.StartGameActivityTask
import org.mozilla.rocket.deeplink.task.StartNewsActivityTask
import org.mozilla.rocket.deeplink.task.StartRewardActivityTask
import org.mozilla.rocket.deeplink.task.StartShoppingActivityTask
import org.mozilla.rocket.deeplink.task.StartTravelActivityTask
import org.mozilla.rocket.deeplink.task.Task
import java.net.URI

enum class DeepLinkType {

    GAME_HOME {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_GAME == uri.path && uri.query.isNullOrEmpty()

        override fun addTasks(uri: URI) {
            addTask(StartGameActivityTask())
        }
    },

    NEWS_HOME {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_NEWS == uri.path && uri.query.isNullOrEmpty()

        override fun addTasks(uri: URI) {
            addTask(StartNewsActivityTask())
        }
    },

    SHOPPING_HOME {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_SHOPPING == uri.path && uri.query.isNullOrEmpty()

        override fun addTasks(uri: URI) {
            addTask(StartShoppingActivityTask())
        }
    },

    TRAVEL_HOME {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_TRAVEL == uri.path && uri.query.isNullOrEmpty()

        override fun addTasks(uri: URI) {
            addTask(StartTravelActivityTask())
        }
    },

    REWARD_HOME {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_REWARD == uri.path && uri.query.isNullOrEmpty()

        override fun addTasks(uri: URI) {
            addTask(StartRewardActivityTask())
        }
    },

    NOT_SUPPORT {
        override fun match(uri: URI) = false

        override fun addTasks(uri: URI) = Unit
    };

    private val taskList = ArrayList<Task>()

    protected abstract fun match(uri: URI): Boolean

    protected abstract fun addTasks(uri: URI)

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
        taskList.clear()
    }

    companion object {
        fun parse(url: String): DeepLinkType {

            val uri = URI.create(url)

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

        private fun isContentLink(uri: URI) =
            DeepLinkConstants.SCHEMA_ROCKET == uri.scheme && DeepLinkConstants.HOST_CONTENT == uri.host
    }
}