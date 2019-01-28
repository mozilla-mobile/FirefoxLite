package org.mozilla.rocket.download;

import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.lifecycle.MutableLiveData;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mozilla.focus.download.DownloadInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.mockito.Mockito.mock;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DownloadInfoViewModelTest {

    private static final long MOCK_DOWNLOAD_ID = 1000;
    private static final long MOCK_ROW_ID = 1;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private DownloadInfoRepository repository;

    @Mock
    private MutableLiveData<DownloadInfoPack> downloadInfoObservable;

    private DownloadInfoViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        viewModel = new DownloadInfoViewModel(repository, downloadInfoObservable);
    }

    @Test
    public void testLoadMore() {
        final DownloadInfo download = new DownloadInfo();
        download.setDownloadId(MOCK_DOWNLOAD_ID);

        Mockito.doAnswer(invocation -> {
            List<DownloadInfo> data = new ArrayList<>();
            data.add(download);
            DownloadInfoRepository.OnQueryListCompleteListener callback = invocation.getArgument(2);
            callback.onComplete(data);
            return null;
        })
                .when(repository)
                .loadData(Mockito.anyInt(), Mockito.anyInt(), Mockito.any(DownloadInfoRepository.OnQueryListCompleteListener.class));

        viewModel.loadMore(true);
        Mockito.verify(downloadInfoObservable).setValue(Mockito.any());
        Assert.assertTrue(Objects.equals(viewModel.getDownloadInfoPack().getList().get(0).getDownloadId(), download.getDownloadId()));
        Assert.assertTrue(viewModel.getDownloadInfoPack().getNotifyType() == DownloadInfoPack.Constants.NOTIFY_DATASET_CHANGED);
    }

    @Test
    public void testCancel() {
        final DownloadInfo download = new DownloadInfo();
        download.setRowId(MOCK_ROW_ID);

        DownloadInfoRepository.OnQueryItemCompleteListener callback = mock(DownloadInfoRepository.OnQueryItemCompleteListener.class);
        Mockito.doAnswer(invocation -> {
            callback.onComplete(download);
            return null;
        })
                .when(repository)
                .queryByRowId(Mockito.anyLong(), Mockito.any(DownloadInfoRepository.OnQueryItemCompleteListener.class));

        viewModel.cancel(MOCK_ROW_ID);
        Mockito.verify(callback, times(1)).onComplete(download);
    }

    @Test
    public void testDelete() {
        DownloadInfoRepository.OnQueryItemCompleteListener callback = mock(DownloadInfoRepository.OnQueryItemCompleteListener.class);
        Mockito.doAnswer(invocation -> {
            callback.onComplete(Mockito.any());
            return null;
        })
                .when(repository)
                .queryByRowId(Mockito.anyLong(), Mockito.any(DownloadInfoRepository.OnQueryItemCompleteListener.class));

        viewModel.delete(MOCK_ROW_ID);
        Mockito.verify(callback, times(1)).onComplete(Mockito.any());
    }

    @Test
    public void testRemove() {
        viewModel.remove(MOCK_ROW_ID);
        Mockito.verify(repository).remove(MOCK_ROW_ID);
    }


    @Test
    public void testAdd() {
        final DownloadInfo download = new DownloadInfo();
        download.setRowId(MOCK_ROW_ID);

        viewModel.add(download);
        Assert.assertTrue(Objects.equals(viewModel.getDownloadInfoPack().getList().get(0).getRowId(), download.getRowId()));
        Assert.assertTrue(viewModel.getDownloadInfoPack().getNotifyType() == DownloadInfoPack.Constants.NOTIFY_DATASET_CHANGED);
        Mockito.verify(downloadInfoObservable).setValue(Mockito.any());
    }

    @Test
    public void testHide() {
        final DownloadInfo download = new DownloadInfo();
        download.setRowId(MOCK_ROW_ID);
        viewModel.getDownloadInfoPack().getList().add(download);
        viewModel.hide(download.getRowId());

        Assert.assertTrue(viewModel.getDownloadInfoPack().getNotifyType() == DownloadInfoPack.Constants.NOTIFY_ITEM_REMOVED);
        Mockito.verify(downloadInfoObservable).setValue(Mockito.any());
    }

    @Test
    public void testNotifyDownloadComplete() {
        viewModel.notifyDownloadComplete(MOCK_DOWNLOAD_ID);
        Mockito.verify(repository).queryByDownloadId(MOCK_DOWNLOAD_ID, viewModel.getUpdateListener());
    }

    @Test
    public void testNotifyRowUpdate() {
        viewModel.notifyRowUpdate(MOCK_ROW_ID);
        Mockito.verify(repository).queryByRowId(MOCK_ROW_ID, viewModel.getUpdateListener());
    }

    @Test
    public void testMarkAllItemsAreRead() {
        viewModel.markAllItemsAreRead();
        Mockito.verify(repository).markAllItemsAreRead();
    }

}
