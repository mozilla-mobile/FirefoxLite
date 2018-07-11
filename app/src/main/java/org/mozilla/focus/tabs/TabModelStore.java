package org.mozilla.focus.tabs;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.persistence.TabEntity;
import org.mozilla.focus.persistence.TabsDatabase;
import org.mozilla.focus.utils.FileUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.os.AsyncTask.SERIAL_EXECUTOR;

public class TabModelStore {

    private static final String TAB_WEB_VIEW_STATE_FOLDER_NAME = "tabs_cache";

    private static volatile TabModelStore instance;
    private TabsDatabase tabsDatabase;

    public interface AsyncQueryListener {
        void onQueryComplete(List<TabEntity> tabEntityList, String focusTabId);
    }

    public interface AsyncSaveListener {
        void onSaveComplete();
    }

    private TabModelStore(@NonNull final Context context) {
        tabsDatabase = Inject.getTabsDatabase(context);
    }

    public static TabModelStore getInstance(@NonNull final Context context) {
        if (instance == null) {
            synchronized (TabModelStore.class) {
                if (instance == null) {
                    instance = new TabModelStore(context);
                }
            }
        }
        return instance;
    }

    public void getSavedTabs(@NonNull final Context context, @Nullable final AsyncQueryListener listener) {
        new QueryTabsTask(context, tabsDatabase, listener).executeOnExecutor(SERIAL_EXECUTOR);
    }

    public void saveTabs(@NonNull final Context context,
                         @NonNull final List<TabEntity> tabEntityList,
                         @Nullable final String focusTabId,
                         @Nullable final AsyncSaveListener listener) {

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getResources().getString(R.string.pref_key_focus_tab_id), focusTabId)
                .apply();

        new SaveTabsTask(context, tabsDatabase, listener).executeOnExecutor(SERIAL_EXECUTOR, tabEntityList.toArray(new TabEntity[0]));
    }

    private static class QueryTabsTask extends AsyncTask<Void, Void, List<TabEntity>> {

        private WeakReference<Context> contextRef;
        private TabsDatabase tabsDatabase;
        private WeakReference<AsyncQueryListener> listenerRef;

        public QueryTabsTask(Context context, TabsDatabase tabsDatabase, AsyncQueryListener listener) {
            this.contextRef = new WeakReference<>(context);
            this.tabsDatabase = tabsDatabase;
            this.listenerRef = new WeakReference<>(listener);
        }

        @Override
        protected List<TabEntity> doInBackground(Void... voids) {
            if (tabsDatabase != null) {
                List<TabEntity> tabEntityList = tabsDatabase.tabDao().getTabs();
                Context context = contextRef.get();
                if (context != null && tabEntityList != null) {
                    restoreWebViewState(context, tabEntityList);
                }
                return tabEntityList;
            }

            return null;
        }

        private void restoreWebViewState(@NonNull Context context, @NonNull List<TabEntity> tabEntityList) {
            File cacheDir = new File(context.getCacheDir(), TAB_WEB_VIEW_STATE_FOLDER_NAME);
            for (TabEntity tabEntity : tabEntityList) {
                if (tabEntity != null) {
                    tabEntity.setWebViewState(FileUtils.readBundleFromStorage(cacheDir, tabEntity.getId()));
                }
            }
        }

        @Override
        protected void onPostExecute(List<TabEntity> tabEntityList) {
            Context context = contextRef.get();
            AsyncQueryListener listener = listenerRef.get();
            if (listener != null && context != null) {
                String focusTabId = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(context.getResources().getString(R.string.pref_key_focus_tab_id), "");
                listener.onQueryComplete(tabEntityList, focusTabId);
            }
        }
    }

    private static class SaveTabsTask extends AsyncTask<TabEntity, Void, Void> {

        private WeakReference<Context> contextRef;
        private TabsDatabase tabsDatabase;
        private WeakReference<AsyncSaveListener> listenerRef;

        public SaveTabsTask(Context context, TabsDatabase tabsDatabase, AsyncSaveListener listener) {
            this.contextRef = new WeakReference<>(context);
            this.tabsDatabase = tabsDatabase;
            this.listenerRef = new WeakReference<>(listener);
        }

        @Override
        protected Void doInBackground(TabEntity... tabEntityList) {
            if (tabEntityList != null) {
                Context context = contextRef.get();
                if (context != null) {
                    saveWebViewState(context, tabEntityList);
                }

                if (tabsDatabase != null) {
                    tabsDatabase.tabDao().deleteAllTabsAndInsertTabsInTransaction(tabEntityList);
                }
            }

            return null;
        }

        private void saveWebViewState(@NonNull Context context, @NonNull TabEntity[] tabEntityList) {
            final File cacheDir = new File(context.getCacheDir(), TAB_WEB_VIEW_STATE_FOLDER_NAME);
            final List<File> updateFileList = new ArrayList<>();

            for (TabEntity tabEntity : tabEntityList) {
                if (tabEntity != null && tabEntity.getWebViewState() != null) {
                    FileUtils.writeBundleToStorage(cacheDir, tabEntity.getId(), tabEntity.getWebViewState());
                    updateFileList.add(new File(cacheDir, tabEntity.getId()));
                }
            }

            // Remove the out-of-date WebView state cache file
            File[] cacheFiles = cacheDir.listFiles();
            if (cacheFiles != null) {
                List<File> outOfDateFileList = new ArrayList<>(Arrays.asList(cacheFiles));
                outOfDateFileList.removeAll(updateFileList);
                boolean success = true;
                for (File file : outOfDateFileList) {
                    success &= file.delete();
                }
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            AsyncSaveListener listener = listenerRef.get();
            if (listener != null) {
                listener.onSaveComplete();
            }
        }
    }
}
