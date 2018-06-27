package org.mozilla.focus.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import org.mozilla.focus.persistence.BookmarkModel;
import org.mozilla.focus.repository.BookmarkRepository;

import java.util.List;

public class BookmarkViewModel extends ViewModel {

    private final LiveData<List<BookmarkModel>> observableBookmarks;

    private BookmarkRepository bookmarkRepository;

    public BookmarkViewModel(@NonNull BookmarkRepository repository) {
        bookmarkRepository = repository;
        observableBookmarks = repository.loadBookmarks();
    }

    public LiveData<List<BookmarkModel>> getBookmarks() {
        return observableBookmarks;
    }

    public LiveData<BookmarkModel> getBookmarkById(String id) {
        return bookmarkRepository.getBookmarkById(id);
    }

    public LiveData<List<BookmarkModel>> getBookmarksByUrl(String url) {
        return bookmarkRepository.getBookmarksByUrl(url);
    }

    public String addBookmark(String title, String url) {
        return bookmarkRepository.addBookmark(title, url);
    }

    public void updateBookmark(BookmarkModel bookmark) {
        bookmarkRepository.updateBookmark(bookmark);
    }

    public void deleteBookmark(BookmarkModel bookmark) {
        bookmarkRepository.deleteBookmark(bookmark);
    }

    public void deleteBookmarksByUrl(String url) {
        bookmarkRepository.deleteBookmarksByUrl(url);
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {

        private final BookmarkRepository repository;

        public Factory(BookmarkRepository repository) {
            this.repository = repository;
        }

        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            //noinspection unchecked
            return (T) new BookmarkViewModel(repository);
        }
    }
}
