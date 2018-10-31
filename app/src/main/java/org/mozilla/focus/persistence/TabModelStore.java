package org.mozilla.focus.persistence;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.rocket.tabs.SessionManager;
import org.mozilla.rocket.tabs.TabViewEngineSession;
import org.mozilla.rocket.tabs.ext.SessionKt;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mozilla.components.browser.session.Session;

import static android.os.AsyncTask.SERIAL_EXECUTOR;

public class TabModelStore {

    private static final String TAB_WEB_VIEW_STATE_FOLDER_NAME = "tabs_cache";

    private static volatile TabModelStore instance;
    private TabsDatabase tabsDatabase;

    public interface AsyncQueryListener {
        void onQueryComplete(List<SessionManager.SessionWithState> states, String focusTabId);
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
                         @NonNull final List<SessionManager.SessionWithState> states,
                         @Nullable final String focusTabId,
                         @Nullable final AsyncSaveListener listener) {

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getResources().getString(R.string.pref_key_focus_tab_id), focusTabId)
                .apply();

        new SaveTabsTask(context, tabsDatabase, listener).executeOnExecutor(SERIAL_EXECUTOR, states.toArray(new SessionManager.SessionWithState[0]));
    }

    private static class QueryTabsTask extends AsyncTask<Void, Void, List<SessionManager.SessionWithState>> {

        private WeakReference<Context> contextRef;
        private TabsDatabase tabsDatabase;
        private WeakReference<AsyncQueryListener> listenerRef;

        public QueryTabsTask(Context context, TabsDatabase tabsDatabase, AsyncQueryListener listener) {
            this.contextRef = new WeakReference<>(context);
            this.tabsDatabase = tabsDatabase;
            this.listenerRef = new WeakReference<>(listener);
        }

        @Override
        protected List<SessionManager.SessionWithState> doInBackground(Void... voids) {
            final Context context = contextRef.get();
            if (context != null && tabsDatabase != null) {
                List<TabEntity> tabEntityList = tabsDatabase.tabDao().getTabs();

                List<Session> sessions = new ArrayList<>();
                for (final TabEntity entity : tabEntityList) {
                    Session session = SessionKt.createOnRestoring(
                            entity.getUrl(),
                            false,
                            mozilla.components.browser.session.Session.Source.NONE,
                            entity.getId());
                    SessionKt.setParentId(session, entity.getParentId());
                    session.setTitle(entity.getTitle());
                    sessions.add(session);
                }

                return restoreWebViewState(context, sessions);
            }

            return null;
        }

        private List<SessionManager.SessionWithState> restoreWebViewState(@NonNull Context context, @NonNull List<Session> sessionList) {
            final List<SessionManager.SessionWithState> states = new ArrayList<>();
            File cacheDir = new File(context.getCacheDir(), TAB_WEB_VIEW_STATE_FOLDER_NAME);
            for (Session session : sessionList) {
                TabViewEngineSession es = new TabViewEngineSession();
                es.setWebViewState(FileUtils.readBundleFromStorage(cacheDir, session.getId()));
                states.add(new SessionManager.SessionWithState(session, es));
            }
            return states;
        }

        @Override
        protected void onPostExecute(List<SessionManager.SessionWithState> list) {
            Context context = contextRef.get();
            AsyncQueryListener listener = listenerRef.get();
            if (listener != null && context != null) {
                String focusTabId = PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(context.getResources().getString(R.string.pref_key_focus_tab_id), "");
                listener.onQueryComplete(list, focusTabId);
            }
        }
    }

    private static class SaveTabsTask extends AsyncTask<SessionManager.SessionWithState, Void, Void> {

        private WeakReference<Context> contextRef;
        private TabsDatabase tabsDatabase;
        private WeakReference<AsyncSaveListener> listenerRef;

        public SaveTabsTask(Context context, TabsDatabase tabsDatabase, AsyncSaveListener listener) {
            this.contextRef = new WeakReference<>(context);
            this.tabsDatabase = tabsDatabase;
            this.listenerRef = new WeakReference<>(listener);
        }

        @Override
        protected Void doInBackground(SessionManager.SessionWithState... states) {
            if (states != null) {
                Context context = contextRef.get();
                if (context != null) {
                    saveWebViewState(context, states);
                }

                if (tabsDatabase != null) {
                    TabEntity[] entities = new TabEntity[states.length];
                    for (int i = 0; i < entities.length; i++) {
                        entities[i] = new TabEntity(states[i].getSession().getId(),
                                SessionKt.getParentId(states[i].getSession()));

                        entities[i].setTitle(states[i].getSession().getTitle());
                        entities[i].setUrl(states[i].getSession().getUrl());
                    }
                    tabsDatabase.tabDao().deleteAllTabsAndInsertTabsInTransaction(entities);
                }
            }

            return null;
        }

        private void saveWebViewState(@NonNull Context context, @NonNull SessionManager.SessionWithState[] states) {
            final File cacheDir = new File(context.getCacheDir(), TAB_WEB_VIEW_STATE_FOLDER_NAME);
            final List<File> updateFileList = new ArrayList<>();

            for (SessionManager.SessionWithState state : states) {
                if (state != null
                        && state.getEngineSession() != null
                        && state.getEngineSession().getWebViewState() != null) {
                    FileUtils.writeBundleToStorage(cacheDir,
                            state.getSession().getId(),
                            state.getEngineSession().getWebViewState());
                    updateFileList.add(new File(cacheDir, state.getSession().getId()));
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
