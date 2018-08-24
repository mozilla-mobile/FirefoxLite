package org.mozilla.rocket.tabs

import junit.framework.Assert.assertEquals
import org.junit.Test
import org.robolectric.annotation.Config
import java.util.UUID

@Config(manifest = Config.NONE)
class SessionTest {

    @Test
    fun testIsValid() {
        Session(UUID.randomUUID().toString(),
                "parent_id",
                "title",
                "https://mozilla.org").let { model ->
            assertEquals(true, model.isValid())
        }

        Session(UUID.randomUUID().toString(),
                "",
                "",
                "https://mozilla.org").let { model ->
            assertEquals(true, model.isValid())
        }

        Session(UUID.randomUUID().toString(),
                null,
                null,
                "https://mozilla.org").let { model ->
            assertEquals(true, model.isValid())
        }

        // no id, invalid
        Session("", "", "", "https://mozilla.org").let { model ->
            assertEquals(false, model.isValid())
        }

        Session("", "", "", "").let { model ->
            assertEquals(false, model.isValid())
        }

        // no url, invalid
        Session(UUID.randomUUID().toString(), "", "", "").let { model ->
            assertEquals(false, model.isValid())
        }

    }
}
