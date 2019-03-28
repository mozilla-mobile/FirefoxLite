package org.mozilla.rocket.download;

import android.app.DownloadManager;
import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.lifecycle.MutableLiveData;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mozilla.focus.download.DownloadInfo;

import java.util.ArrayList;
import java.util.List;

public class DownloadIndicatorViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private DownloadInfoRepository repository;

    @Mock
    private MutableLiveData<DownloadIndicatorViewModel.Status> downloadInfoObservable;

    private DownloadIndicatorViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        viewModel = new DownloadIndicatorViewModel(repository, downloadInfoObservable);
    }

    @Test
    public void testRunningStatus_showDownloading() {
        final DownloadInfo download1 = new DownloadInfo();
        download1.setStatusInt(DownloadManager.STATUS_RUNNING);

        final DownloadInfo download2 = new DownloadInfo();
        download2.setStatusInt(DownloadManager.STATUS_SUCCESSFUL);
        download2.setRead(false);

        final DownloadInfo download3 = new DownloadInfo();
        download3.setStatusInt(DownloadManager.STATUS_FAILED);

        Mockito.doAnswer(invocation -> {
            List<DownloadInfo> data = new ArrayList<>();
            data.add(download1);
            data.add(download2);
            data.add(download3);
            DownloadInfoRepository.OnQueryListCompleteListener callback = invocation.getArgument(0);
            callback.onComplete(data);
            return null;
        })
                .when(repository)
                .queryIndicatorStatus(Mockito.any(DownloadInfoRepository.OnQueryListCompleteListener.class));

        viewModel.updateIndicator();
        Mockito.verify(downloadInfoObservable).setValue(DownloadIndicatorViewModel.Status.DOWNLOADING);
    }

    @Test
    public void testPendingStatus_showDownloading() {
        final DownloadInfo download1 = new DownloadInfo();
        download1.setStatusInt(DownloadManager.STATUS_PENDING);

        final DownloadInfo download2 = new DownloadInfo();
        download2.setStatusInt(DownloadManager.STATUS_SUCCESSFUL);
        download2.setRead(false);

        final DownloadInfo download3 = new DownloadInfo();
        download3.setStatusInt(DownloadManager.STATUS_FAILED);

        Mockito.doAnswer(invocation -> {
            List<DownloadInfo> data = new ArrayList<>();
            data.add(download1);
            data.add(download2);
            data.add(download3);
            DownloadInfoRepository.OnQueryListCompleteListener callback = invocation.getArgument(0);
            callback.onComplete(data);
            return null;
        })
                .when(repository)
                .queryIndicatorStatus(Mockito.any(DownloadInfoRepository.OnQueryListCompleteListener.class));

        viewModel.updateIndicator();
        Mockito.verify(downloadInfoObservable).setValue(DownloadIndicatorViewModel.Status.DOWNLOADING);
    }

    @Test
    public void testSuccessfulAndUnreadStatus_showUnread() {
        final DownloadInfo download1 = new DownloadInfo();
        download1.setStatusInt(DownloadManager.STATUS_SUCCESSFUL);
        download1.setRead(false);

        final DownloadInfo download2 = new DownloadInfo();
        download2.setStatusInt(DownloadManager.STATUS_FAILED);

        Mockito.doAnswer(invocation -> {
            List<DownloadInfo> data = new ArrayList<>();
            data.add(download1);
            data.add(download2);
            DownloadInfoRepository.OnQueryListCompleteListener callback = invocation.getArgument(0);
            callback.onComplete(data);
            return null;
        })
                .when(repository)
                .queryIndicatorStatus(Mockito.any(DownloadInfoRepository.OnQueryListCompleteListener.class));

        viewModel.updateIndicator();
        Mockito.verify(downloadInfoObservable).setValue(DownloadIndicatorViewModel.Status.UNREAD);
    }

    @Test
    public void testSuccessfulAndReadStatus_showDefault() {
        final DownloadInfo download1 = new DownloadInfo();
        download1.setStatusInt(DownloadManager.STATUS_SUCCESSFUL);
        download1.setRead(true);

        Mockito.doAnswer(invocation -> {
            List<DownloadInfo> data = new ArrayList<>();
            data.add(download1);
            DownloadInfoRepository.OnQueryListCompleteListener callback = invocation.getArgument(0);
            callback.onComplete(data);
            return null;
        })
                .when(repository)
                .queryIndicatorStatus(Mockito.any(DownloadInfoRepository.OnQueryListCompleteListener.class));

        viewModel.updateIndicator();
        Mockito.verify(downloadInfoObservable).setValue(DownloadIndicatorViewModel.Status.DEFAULT);
    }

    @Test
    public void testFailedStatus_showWarning() {
        final DownloadInfo download1 = new DownloadInfo();
        download1.setStatusInt(DownloadManager.STATUS_SUCCESSFUL);
        download1.setRead(true);

        final DownloadInfo download2 = new DownloadInfo();
        download2.setStatusInt(DownloadManager.STATUS_FAILED);

        Mockito.doAnswer(invocation -> {
            List<DownloadInfo> data = new ArrayList<>();
            data.add(download1);
            data.add(download2);
            DownloadInfoRepository.OnQueryListCompleteListener callback = invocation.getArgument(0);
            callback.onComplete(data);
            return null;
        })
                .when(repository)
                .queryIndicatorStatus(Mockito.any(DownloadInfoRepository.OnQueryListCompleteListener.class));

        viewModel.updateIndicator();
        Mockito.verify(downloadInfoObservable).setValue(DownloadIndicatorViewModel.Status.WARNING);
    }

    @Test
    public void testPausedStatus_showWarning() {
        final DownloadInfo download1 = new DownloadInfo();
        download1.setStatusInt(DownloadManager.STATUS_SUCCESSFUL);
        download1.setRead(true);

        final DownloadInfo download2 = new DownloadInfo();
        download2.setStatusInt(DownloadManager.STATUS_PAUSED);

        Mockito.doAnswer(invocation -> {
            List<DownloadInfo> data = new ArrayList<>();
            data.add(download1);
            data.add(download2);
            DownloadInfoRepository.OnQueryListCompleteListener callback = invocation.getArgument(0);
            callback.onComplete(data);
            return null;
        })
                .when(repository)
                .queryIndicatorStatus(Mockito.any(DownloadInfoRepository.OnQueryListCompleteListener.class));

        viewModel.updateIndicator();
        Mockito.verify(downloadInfoObservable).setValue(DownloadIndicatorViewModel.Status.WARNING);
    }
}
