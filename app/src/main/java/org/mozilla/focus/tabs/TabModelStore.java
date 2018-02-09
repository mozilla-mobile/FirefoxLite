package org.mozilla.focus.tabs;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import org.mozilla.focus.persistence.TabModel;
import org.mozilla.focus.persistence.TabsDatabase;

import java.lang.ref.WeakReference;
import java.util.List;

import static android.os.AsyncTask.SERIAL_EXECUTOR;

public class TabModelStore {

    private static volatile TabModelStore instance;
    private TabsDatabase tabsDatabase;

    public interface AsyncQueryListener {
        void onQueryComplete(List<TabModel> tabModelList);
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

    public void getSavedTabs(AsyncQueryListener listener) {
        new QueryTabsTask(tabsDatabase, listener).executeOnExecutor(SERIAL_EXECUTOR);
    }

    public void saveTabs(final List<TabModel> tabModelList, AsyncSaveListener listener) {
        if (tabModelList == null || tabModelList.size() == 0) {
            if (listener != null) {
                listener.onSaveComplete();
            }
            return;
        }

        new SaveTabsTask(tabsDatabase, listener).executeOnExecutor(SERIAL_EXECUTOR, (TabModel[]) tabModelList.toArray());
    }

    private static class QueryTabsTask extends AsyncTask<Void, Void, List<TabModel>> {

        private TabsDatabase tabsDatabase;
        private WeakReference<AsyncQueryListener> listenerRef;

        public QueryTabsTask(TabsDatabase tabsDatabase, AsyncQueryListener listener) {
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
            if (listenerRef.get() != null) {
                listenerRef.get().onQueryComplete(tabModelList);
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
            if (listenerRef.get() != null) {
                listenerRef.get().onSaveComplete();
            }
        }
    }
}