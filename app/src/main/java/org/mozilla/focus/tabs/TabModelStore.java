package org.mozilla.focus.tabs;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.mozilla.focus.R;
import org.mozilla.focus.persistence.TabModel;
import org.mozilla.focus.persistence.TabsDatabase;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.os.AsyncTask.SERIAL_EXECUTOR;

public class TabModelStore {

    private static volatile TabModelStore instance;
    private TabsDatabase tabsDatabase;

    public interface AsyncQueryListener {
        void onQueryComplete(List<TabModel> tabModelList, String currentTabId);
    }

    public interface AsyncSaveListener {
        void onSaveComplete();
    }

    private TabModelStore(@NonNull final Context context) {
        tabsDatabase = TabsDatabase.getInstance(context);
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

    public void getSavedTabs(@NonNull final Context context, final AsyncQueryListener listener) {
        new QueryTabsTask(context, tabsDatabase, listener).executeOnExecutor(SERIAL_EXECUTOR);
    }

    public void saveTabs(@NonNull final Context context, @NonNull final List<TabModel> tabModelList, @NonNull final String currentTabId, final AsyncSaveListener listener) {
        if (tabModelList.size() == 0) {
            if (listener != null) {
                listener.onSaveComplete();
            }
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(context.getResources().getString(R.string.pref_key_current_tab_id), currentTabId)
                .apply();

        new SaveTabsTask(tabsDatabase, listener).executeOnExecutor(SERIAL_EXECUTOR, tabModelList.toArray(new TabModel[0]));
    }

    private static class QueryTabsTask extends AsyncTask<Void, Void, List<TabModel>> {

        private WeakReference<Context> contextRef;
        private TabsDatabase tabsDatabase;
        private WeakReference<AsyncQueryListener> listenerRef;

        public QueryTabsTask(Context context, TabsDatabase tabsDatabase, AsyncQueryListener listener) {
            this.contextRef = new WeakReference(context);
            this.tabsDatabase = tabsDatabase;
            this.listenerRef = new WeakReference<>(listener);
        }

        @Override
        protected List<TabModel> doInBackground(Void... voids) {
            if (tabsDatabase != null) {
                return tabsDatabase.tabDao().getTabs();
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<TabModel> tabModelList) {
            Context context = contextRef.get();
            AsyncQueryListener listener = listenerRef.get();
            if (listener != null && context != null) {
                String currentTabId = PreferenceManager.getDefaultSharedPreferences(context).getString(context.getResources().getString(R.string.pref_key_current_tab_id), "");
                listener.onQueryComplete(tabModelList, currentTabId);
            }
        }
    }

    private static class SaveTabsTask extends AsyncTask<TabModel, Void, Void> {

        private TabsDatabase tabsDatabase;
        private WeakReference<AsyncSaveListener> listenerRef;

        public SaveTabsTask(TabsDatabase tabsDatabase, AsyncSaveListener listener) {
            this.tabsDatabase = tabsDatabase;
            this.listenerRef = new WeakReference<>(listener);
        }

        @Override
        protected Void doInBackground(TabModel... tabModelList) {
            if (tabsDatabase != null) {
                tabsDatabase.tabDao().deleteAllTabs();

                if (tabModelList != null) {
                    tabsDatabase.tabDao().insertTabs(tabModelList);
                }
            }

            return null;
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