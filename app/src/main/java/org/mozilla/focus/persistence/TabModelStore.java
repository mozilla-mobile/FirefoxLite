package org.mozilla.focus.persistence;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.rocket.tabs.Session;
import org.mozilla.rocket.tabs.TabViewEngineObserver;
import org.mozilla.rocket.tabs.TabViewEngineSession;

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
        void onQueryComplete(List<Session> sessionList, String focusTabId);
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
                         @NonNull final List<Session> sessionList,
                         @Nullable final String focusTabId,
                         @Nullable final AsyncSaveListener listener) {

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getResources().getString(R.string.pref_key_focus_tab_id), focusTabId)
                .apply();

        new SaveTabsTask(context, tabsDatabase, listener).executeOnExecutor(SERIAL_EXECUTOR, sessionList.toArray(new Session[0]));
    }

    private static class QueryTabsTask extends AsyncTask<Void, Void, List<Session>> {

        private WeakReference<Context> contextRef;
        private TabsDatabase tabsDatabase;
        private WeakReference<AsyncQueryListener> listenerRef;

        public QueryTabsTask(Context context, TabsDatabase tabsDatabase, AsyncQueryListener listener) {
            this.contextRef = new WeakReference<>(context);
            this.tabsDatabase = tabsDatabase;
            this.listenerRef = new WeakReference<>(listener);
        }

        @Override
        protected List<Session> doInBackground(Void... voids) {
            if (tabsDatabase != null) {
                List<TabEntity> tabEntityList = tabsDatabase.tabDao().getTabs();
                Context context = contextRef.get();

                List<Session> sessions = new ArrayList<>();
                for (final TabEntity entity : tabEntityList) {
                    Session session = new Session(entity.getId(),
                            entity.getParentId(),
                            entity.getUrl());
                    session.setTitle(entity.getTitle());

                    session.setEngineSession(new TabViewEngineSession());
                    session.setEngineObserver(new TabViewEngineObserver(session));
                    session.getEngineSession().register(session.getEngineObserver());
                    sessions.add(session);
                }

                if (context != null && sessions.size() > 0) {
                    restoreWebViewState(context, sessions);
                }

                return sessions;
            }

            return null;
        }

        private void restoreWebViewState(@NonNull Context context, @NonNull List<Session> sessionList) {
            File cacheDir = new File(context.getCacheDir(), TAB_WEB_VIEW_STATE_FOLDER_NAME);
            for (Session session : sessionList) {
                if (session != null && session.getEngineSession() != null) {
                    session.getEngineSession().setWebViewState(FileUtils.readBundleFromStorage(cacheDir, session.getId()));
                }
            }
        }

        @Override
        protected void onPostExecute(List<Session> sessionList) {
            Context context = contextRef.get();
            AsyncQueryListener listener = listenerRef.get();
            if (listener != null && context != null) {
                String focusTabId = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(context.getResources().getString(R.string.pref_key_focus_tab_id), "");
                listener.onQueryComplete(sessionList, focusTabId);
            }
        }
    }

    private static class SaveTabsTask extends AsyncTask<Session, Void, Void> {

        private WeakReference<Context> contextRef;
        private TabsDatabase tabsDatabase;
        private WeakReference<AsyncSaveListener> listenerRef;

        public SaveTabsTask(Context context, TabsDatabase tabsDatabase, AsyncSaveListener listener) {
            this.contextRef = new WeakReference<>(context);
            this.tabsDatabase = tabsDatabase;
            this.listenerRef = new WeakReference<>(listener);
        }

        @Override
        protected Void doInBackground(Session... sessionList) {
            if (sessionList != null) {
                Context context = contextRef.get();
                if (context != null) {
                    saveWebViewState(context, sessionList);
                }

                if (tabsDatabase != null) {
                    TabEntity[] entities = new TabEntity[sessionList.length];
                    for (int i = 0; i < entities.length; i++) {
                        entities[i] = new TabEntity(sessionList[i].getId(),
                                sessionList[i].getParentId());

                        entities[i].setTitle(sessionList[i].getTitle());
                        entities[i].setUrl(sessionList[i].getUrl());
                    }
                    tabsDatabase.tabDao().deleteAllTabsAndInsertTabsInTransaction(entities);
                }
            }

            return null;
        }

        private void saveWebViewState(@NonNull Context context, @NonNull Session[] sessionList) {
            final File cacheDir = new File(context.getCacheDir(), TAB_WEB_VIEW_STATE_FOLDER_NAME);
            final List<File> updateFileList = new ArrayList<>();

            for (Session session : sessionList) {
                if (session != null
                        && session.getEngineSession() != null
                        && session.getEngineSession().getWebViewState() != null) {
                    FileUtils.writeBundleToStorage(cacheDir,
                            session.getId(),
                            session.getEngineSession().getWebViewState());
                    updateFileList.add(new File(cacheDir, session.getId()));
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
