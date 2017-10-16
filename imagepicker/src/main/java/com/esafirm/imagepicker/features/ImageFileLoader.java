package com.esafirm.imagepicker.features;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import com.esafirm.imagepicker.features.common.ImageLoaderListener;
import com.esafirm.imagepicker.model.Folder;
import com.esafirm.imagepicker.model.Image;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageFileLoader {

    private Context context;
    private ExecutorService executorService;

    public ImageFileLoader(Context context) {
        this.context = context;
    }

    private final String[] projection = new String[]{
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
    };

    public void loadDeviceImages(final boolean isFolderMode, final ImageLoaderListener listener) {
        getExecutorService().execute(new ImageLoadRunnable(isFolderMode, listener));
    }

    public void loadFacebookImages(boolean isFolderMode, ImageLoaderListener listener) {
        this.getExecutorService().execute(new FacebookImageLoadRunnable(isFolderMode, listener));
    }

    public void abortLoadImages() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    private ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = Executors.newSingleThreadExecutor();
        }
        return executorService;
    }

    private class ImageLoadRunnable implements Runnable {

        private boolean isFolderMode;
        private ImageLoaderListener listener;

        public ImageLoadRunnable(boolean isFolderMode, ImageLoaderListener listener) {
            this.isFolderMode = isFolderMode;
            this.listener = listener;
        }

        @Override
        public void run() {
            Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                    null, null, MediaStore.Images.Media.DATE_ADDED);

            if (cursor == null) {
                listener.onFailed(new NullPointerException());
                return;
            }

            List<Image> temp = new ArrayList<>(cursor.getCount());
            Map<String, Folder> folderMap = null;
            if (isFolderMode) {
                folderMap = new HashMap<>();
            }

            if (cursor.moveToLast()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndex(projection[0]));
                    String name = cursor.getString(cursor.getColumnIndex(projection[1]));
                    String path = cursor.getString(cursor.getColumnIndex(projection[2]));
                    String bucket = cursor.getString(cursor.getColumnIndex(projection[3]));

                    File file = makeSafeFile(path);
                    if (file != null && file.exists()) {
                        Image image = new Image(id, name, path);
                        temp.add(image);

                        if (folderMap != null) {
                            Folder folder = folderMap.get(bucket);
                            if (folder == null) {
                                folder = new Folder(bucket);
                                folderMap.put(bucket, folder);
                            }
                            folder.getImages().add(image);
                        }
                    }

                } while (cursor.moveToPrevious());
            }
            cursor.close();

            /* Convert HashMap to ArrayList if not null */
            List<Folder> folders = null;
            if (folderMap != null) {
                folders = new ArrayList<>(folderMap.values());
            }

            listener.onImageLoaded(temp, folders);
        }
    }

    private class FacebookImageLoadRunnable implements Runnable {

        private boolean isFolderMode;
        private ImageLoaderListener listener;

        public FacebookImageLoadRunnable(boolean isFolderMode, ImageLoaderListener listener) {
            this.isFolderMode = isFolderMode;
            this.listener = listener;
        }

        @Override
        public void run() {
            final ArrayList temp = new ArrayList();
            final HashMap folderMap = this.isFolderMode ? new HashMap() : null;
            final ImageLoaderListener listenerF = this.listener;

            FacebookUtils.getFbAlbums(new FacebookUtils.ListCallback() {

                @Override
                public void call(final List list) {
                    final List<String[]> albumList = (List<String[]>) list;
                    final FacebookUtils.FinalCounter counter = new FacebookUtils.FinalCounter(0);
                    for(String[] album : albumList) {
                        final String albumName = album[1]!=null ? album[1] : album[0];
                        FacebookUtils.getFbAlbumPhotos(album[0], new FacebookUtils.ListCallback() {

                            @Override
                            public void call(List list) {
                                for(String[] s : (List<String[]>)list) {
                                    final String linkF = s[0];
                                    final String idF = s[1];

                                    long imageid = Long.parseLong(s[1]);
                                    String name = s[1];
                                    String path = s[0];
                                    String bucket = albumName;
                                    Image image = new Image(imageid, name, path);
                                    temp.add(image);
                                    if(folderMap != null) {
                                        Folder folder = (Folder)folderMap.get(bucket);
                                        if(folder == null) {
                                            folder = new Folder(bucket);
                                            folderMap.put(bucket, folder);
                                        }

                                        folder.getImages().add(image);
                                    }
                                }

                                counter.increment();
                                if(counter.getVal()==albumList.size()) {
                                    ArrayList folders1 = null;
                                    if (folderMap != null) {
                                        folders1 = new ArrayList(folderMap.values());
                                    }

                                    listenerF.onImageLoaded(temp, folders1);
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    private static File makeSafeFile(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        try {
            return new File(path);
        } catch (Exception ignored) {
            return null;
        }
    }

}
