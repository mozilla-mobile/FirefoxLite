package org.mozilla.rocket.download;


import android.app.DownloadManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mozilla.focus.widget.DownloadListAdapter;

import static org.junit.Assert.assertEquals;

public class DownloadListAdapterTest {

    @Mock
    private DownloadListAdapter adapter;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void changeDownloadStatus_parseStatusStringCorrectly() {
        String textPause = "pause";
        Mockito.doReturn(textPause).when(adapter).statusConvertStr(DownloadManager.STATUS_PAUSED);
        assertEquals(adapter.statusConvertStr(DownloadManager.STATUS_PAUSED), textPause);

        String textPending = "Pending";
        Mockito.doReturn(textPending).when(adapter).statusConvertStr(DownloadManager.STATUS_PENDING);
        assertEquals(adapter.statusConvertStr(DownloadManager.STATUS_PENDING), textPending);

        String textDownloading = "downloading";
        Mockito.doReturn(textDownloading).when(adapter).statusConvertStr(DownloadManager.STATUS_RUNNING);
        assertEquals(adapter.statusConvertStr(DownloadManager.STATUS_RUNNING), textDownloading);

        String textSuccessful = "Successful";
        Mockito.doReturn(textSuccessful).when(adapter).statusConvertStr(DownloadManager.STATUS_SUCCESSFUL);
        assertEquals(adapter.statusConvertStr(DownloadManager.STATUS_SUCCESSFUL), textSuccessful);

        String textFailed = "failed";
        Mockito.doReturn(textFailed).when(adapter).statusConvertStr(DownloadManager.STATUS_FAILED);
        assertEquals(adapter.statusConvertStr(DownloadManager.STATUS_FAILED), textFailed);
    }
}
