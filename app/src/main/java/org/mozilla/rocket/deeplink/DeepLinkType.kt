package org.mozilla.rocket.deeplink

import android.content.Context
import androidx.annotation.VisibleForTesting
import org.mozilla.rocket.deeplink.task.StartGameActivityTask
import org.mozilla.rocket.deeplink.task.StartGameItemActivityTask
import org.mozilla.rocket.deeplink.task.StartNewsActivityTask
import org.mozilla.rocket.deeplink.task.StartNewsItemActivityTask
import org.mozilla.rocket.deeplink.task.StartRewardActivityTask
import org.mozilla.rocket.deeplink.task.StartShoppingActivityTask
import org.mozilla.rocket.deeplink.task.StartShoppingItemActivityTask
import org.mozilla.rocket.deeplink.task.StartTravelActivityTask
import org.mozilla.rocket.deeplink.task.StartTravelItemActivityTask
import org.mozilla.rocket.deeplink.task.Task
import org.mozilla.rocket.extension.getParam
import java.net.URI
import java.net.URISyntaxException

enum class DeepLinkType {

    GAME_HOME {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_GAME == uri.path && uri.query.isNullOrEmpty()

        override fun addTasks(uri: URI) {
            addTask(StartGameActivityTask())
        }
    },
    GAME_ITEM {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_GAME_ITEM == uri.path

        override fun addTasks(uri: URI) {
            val url = uri.getParam("url")
            val feed = uri.getParam("feed")
            val source = uri.getParam("source")

            addTask(StartGameItemActivityTask(url, feed, source))
        }
    },

    NEWS_HOME {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_NEWS == uri.path && uri.query.isNullOrEmpty()

        override fun addTasks(uri: URI) {
            addTask(StartNewsActivityTask())
        }
    },
    NEWS_ITEM {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_NEWS_ITEM == uri.path

        override fun addTasks(uri: URI) {
            val url = uri.getParam("url")
            val feed = uri.getParam("feed")
            val source = uri.getParam("source")

            addTask(StartNewsItemActivityTask(url, feed, source))
        }
    },

    SHOPPING_HOME {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_SHOPPING == uri.path && uri.query.isNullOrEmpty()

        override fun addTasks(uri: URI) {
            addTask(StartShoppingActivityTask())
        }
    },
    SHOPPING_ITEM {
        override fun match(uri: URI) =
                isContentLink(uri) && DeepLinkConstants.PATH_SHOPPING_ITEM == uri.path

        override fun addTasks(uri: URI) {
            val url = uri.getParam("url")
            val feed = uri.getParam("feed")
            val source = uri.getParam("source")

            addTask(StartShoppingItemActivityTask(url, feed, source))
        }
    },

    TRAVEL_HOME {
        override fun match(uri: URI) =
            isContentLink(uri) && DeepLinkConstants.PATH_TRAVEL == uri.path && uri.query.isNullOrEmpty()

        override fun addTasks(uri: URI) {
            addTask(StartTravelActivityTask())
        }
    },
    TRAVEL_ITEM {
        override fun match(uri: URI) =
                isContentLink(uri) && DeepLinkConstants.PATH_TRAVEL_ITEM == uri.path

        override fun addTasks(uri: URI) {
            val url = uri.getParam("url")
            val feed = uri.getParam("feed")
            val source = uri.getParam("source")

            addTask(StartTravelItemActivityTask(url, feed, source))
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
            try {
                val uri = URI(url)

                for (type in values()) {
                    if (type === NOT_SUPPORT) {
                        continue
                    }

                    if (type.match(uri)) {
                        type.addTasks(uri)

                        return type
                    }
                }
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }

            return NOT_SUPPORT
        }

        private fun isContentLink(uri: URI) =
            DeepLinkConstants.SCHEMA_ROCKET == uri.scheme && DeepLinkConstants.HOST_CONTENT == uri.host
    }
}