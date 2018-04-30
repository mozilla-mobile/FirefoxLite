package org.mozilla.focus.utils;

import android.util.Log;

import com.squareup.leakcanary.AnalysisResult;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.HeapDump;

/**
 * Created by mozilla on 2018/4/27.
 */

public class AndroidTestAnalysisResultService  extends DisplayLeakService {

    public static boolean hasLeak;
    public static boolean isCompleted;

    @Override
    protected void afterDefaultHandling(HeapDump heapDump, AnalysisResult result, String leakInfo) {
        Log.d("test", "afterDefaultHandling() called with: heapDump = [" + heapDump + "], result = [" + result + "], leakInfo = [" + leakInfo + "]");
        isCompleted = true;
        if (result.leakFound && !result.excludedLeak) {
            hasLeak = true;
        }
    }

}
