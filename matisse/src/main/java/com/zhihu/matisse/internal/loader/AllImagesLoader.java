/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.loader;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.loader.content.CursorLoader;

import com.zhihu.matisse.internal.entity.Album;

public class AllImagesLoader extends CursorLoader {
    public static final String COLUMN_COUNT = "count";
    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");
    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            "duration"};
    private static final String SELECTION_ALL =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";
    private static final String[] SELECTION_ALL_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };
    private static final String ORDER_BY = MediaStore.Files.FileColumns.DATE_ADDED + " DESC";

    public AllImagesLoader(Context context) {
        super(context, QUERY_URI, PROJECTION, SELECTION_ALL, SELECTION_ALL_ARGS, ORDER_BY);
    }

    @Override
    public Cursor loadInBackground() {
        Cursor albums = super.loadInBackground();
        MatrixCursor allAlbum = new MatrixCursor(PROJECTION);
        int totalCount = 0;
        while (albums.moveToNext()) {
            totalCount += albums.getInt(albums.getColumnIndex(COLUMN_COUNT));
        }
        String allAlbumCoverPath;
        if (albums.moveToFirst()) {
            allAlbumCoverPath = albums.getString(albums.getColumnIndex(MediaStore.MediaColumns.DATA));
        } else {
            allAlbumCoverPath = "";
        }
        allAlbum.addRow(new String[]{Album.ALBUM_ID_ALL, Album.ALBUM_ID_ALL, Album.ALBUM_NAME_ALL, allAlbumCoverPath,
                String.valueOf(totalCount)});

        return new MergeCursor(new Cursor[]{allAlbum, albums});
    }

    @Override
    public void onContentChanged() {
        // FIXME a dirty way to fix loading multiple times
    }
}