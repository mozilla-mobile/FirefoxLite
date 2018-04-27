package org.mozilla.focus.utils;

import com.squareup.leakcanary.AnalysisResult;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.HeapDump;

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
