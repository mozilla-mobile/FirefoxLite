package org.mozilla.focus.utils;

import android.app.PendingIntent;
import android.os.SystemClock;

import com.squareup.leakcanary.AbstractAnalysisResultService;
import com.squareup.leakcanary.AnalysisResult;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.HeapDump;
import com.squareup.leakcanary.internal.DisplayLeakActivity;

import static android.text.format.Formatter.formatShortFileSize;
import static com.squareup.leakcanary.internal.LeakCanaryInternals.classSimpleName;
import static com.squareup.leakcanary.internal.LeakCanaryInternals.showNotification;

/**
 * Created by mozilla on 2018/4/27.
 */

public class AndroidTestAnalysisResultService extends DisplayLeakService {

    private boolean hasLeak;

    @Override
    protected void afterDefaultHandling(HeapDump heapDump, AnalysisResult result, String leakInfo) {
        if (result.leakFound && !result.excludedLeak) {
            hasLeak = true;
        }
    }

}
