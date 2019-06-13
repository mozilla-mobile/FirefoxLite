package org.mozilla.focus.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * With thanks to <http://stackoverflow.com/a/22679283/22003> for the
 * initial solution.
 * <p>
 * This class encapsulates an approach to checking whether a script
 * is usable on a device. We attempt to draw a character from the
 * script (e.g., ব). If the fonts on the device don't have the correct
 * glyph, Android typically renders whitespace (rather than .notdef).
 * <p>
 * Pass in part of the name of the locale in its local representation,
 * and a whitespace character; this class performs the graphical comparison.
 * <p>
 * See Bug 1023451 Comment 24 for extensive explanation.
 */
public class CharacterValidator {
    private static final int BITMAP_WIDTH = 32;
    private static final int BITMAP_HEIGHT = 48;

    private final Paint paint = new Paint();
    private final byte[] missingCharacter;

    // Note: this constructor fails when running in Robolectric: robolectric only supports bitmaps
    // with 4 bytes per pixel ( https://github.com/robolectric/robolectric/blob/master/robolectric-shadows/shadows-core/src/main/java/org/robolectric/shadows/ShadowBitmap.java#L540 ).
    // We need to either make this code test-aware, or fix robolectric.
    public CharacterValidator(String missing) {
        this.missingCharacter = getPixels(drawBitmap(missing));
    }

    private Bitmap drawBitmap(String text) {
        Bitmap b = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(b);
        c.drawText(text, 0, BITMAP_HEIGHT / 2, this.paint);
        return b;
    }

    private static byte[] getPixels(final Bitmap b) {
        final int byteCount = b.getAllocationByteCount();

        final ByteBuffer buffer = ByteBuffer.allocate(byteCount);
        try {
            b.copyPixelsToBuffer(buffer);
        } catch (RuntimeException e) {
            // Android throws this if there's not enough space in the buffer.
            // This should never occur, but if it does, we don't
            // really care -- we probably don't need the entire image.
            // This is awful. I apologize.
            if ("Buffer not large enough for pixels".equals(e.getMessage())) {
                return buffer.array();
            }
            throw e;
        }

        return buffer.array();
    }

    public boolean characterIsMissingInFont(String ch) {
        byte[] rendered = getPixels(drawBitmap(ch));
        return Arrays.equals(rendered, missingCharacter);
    }
}
