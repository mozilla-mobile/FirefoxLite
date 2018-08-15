package org.mozilla.rocket.tabs

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.robolectric.annotation.Config
import java.util.UUID

@Config(manifest = Config.NONE)
class TabModelTest {

    @Test
    fun testIsValid() {
        TabModel(UUID.randomUUID().toString(),
                "parent_id",
                "title",
                "https://mozilla.org").let { model ->
            assertEquals(true, model.isValid())
        }

        TabModel(UUID.randomUUID().toString(),
                "",
                "",
                "https://mozilla.org").let { model ->
            assertEquals(true, model.isValid())
        }

        TabModel(UUID.randomUUID().toString(),
                null,
                null,
                "https://mozilla.org").let { model ->
            assertEquals(true, model.isValid())
        }

        // no id, invalid
        TabModel("", "", "", "https://mozilla.org").let { model ->
            assertEquals(false, model.isValid())
        }

        TabModel("", "", "", "").let { model ->
            assertEquals(false, model.isValid())
        }

        // no url, invalid
        TabModel(UUID.randomUUID().toString(), "", "", "").let { model ->
            assertEquals(false, model.isValid())
        }

        TabModel(UUID.randomUUID().toString(), "", "", null).let { model ->
            assertEquals(false, model.isValid())
        }
    }
}
