package com.example.sharedstorage;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileContentProvider extends ContentProvider {

    private static final String TAG = "FileContentProvider";
    public static final String AUTHORITY = "com.example.sharedstorage.provider";

    // The URI scheme and authority
    private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/files");

    // URI matcher setup
    private static final int FILES = 1; // Matches the directory of files
    private static final int FILE_ID = 2; // Matches a single file
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "files", FILES);
        uriMatcher.addURI(AUTHORITY, "files/*", FILE_ID);
    }

    private File mSharedDir;

    @Override
    public boolean onCreate() {
        // Get the directory in our app's private storage where we'll store the files.
        // Using getContext().getFilesDir() ensures this is secure and private to our app.
        mSharedDir = new File(getContext().getFilesDir(), "shared_files");
        if (!mSharedDir.exists()) {
            if (!mSharedDir.mkdirs()) {
                Log.e(TAG, "Failed to create shared directory");
                return false;
            }
        }
        Log.d(TAG, "FileContentProvider created. Shared directory: " + mSharedDir.getAbsolutePath());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (uriMatcher.match(uri)) {
            case FILES:
                // A request to list all files in the directory.
                return listFiles(projection);
            case FILE_ID:
                // A request for a specific file's metadata (name and size).
                return getFileMetadata(uri, projection);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    private Cursor listFiles(String[] projection) {
        final String[] columns = resolveProjection(projection);
        MatrixCursor cursor = new MatrixCursor(columns);

        File[] files = mSharedDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    MatrixCursor.RowBuilder row = cursor.newRow();
                    row.add(OpenableColumns.DISPLAY_NAME, file.getName());
                    row.add(OpenableColumns.SIZE, file.length());
                }
            }
        }
        return cursor;
    }

    private Cursor getFileMetadata(Uri uri, String[] projection) {
        final String[] columns = resolveProjection(projection);
        MatrixCursor cursor = new MatrixCursor(columns);
        File file = getFileForUri(uri);

        if (file.exists() && file.isFile()) {
            MatrixCursor.RowBuilder row = cursor.newRow();
            row.add(OpenableColumns.DISPLAY_NAME, file.getName());
            row.add(OpenableColumns.SIZE, file.length());
        }
        return cursor;
    }


    @Override
    public String getType(Uri uri) {
        // A simple MIME type implementation. A real-world app might use MimeTypeMap.
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (uriMatcher.match(uri) != FILES) {
            throw new IllegalArgumentException("Invalid URI for insert: " + uri);
        }

        String fileName = values.getAsString(OpenableColumns.DISPLAY_NAME);
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name must be provided");
        }

        File file = new File(mSharedDir, fileName);

        // To prevent path traversal attacks, ensure the final path is within our shared dir.
        try {
            if (!file.getCanonicalPath().startsWith(mSharedDir.getCanonicalPath())) {
                 throw new SecurityException("Attempted path traversal");
            }
        } catch (IOException e) {
            throw new SecurityException("Could not verify path", e);
        }


        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    Log.e(TAG, "Failed to create new file: " + fileName);
                    return null;
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException while creating file", e);
                return null;
            }
        }

        // Notify observers that the data has changed.
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.withAppendedPath(CONTENT_URI, fileName);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if (uriMatcher.match(uri) != FILE_ID) {
            throw new IllegalArgumentException("Invalid URI for delete: " + uri);
        }

        File file = getFileForUri(uri);
        if (!file.exists()) {
            return 0; // File not found
        }

        if (file.delete()) {
            // Notify observers.
            getContext().getContentResolver().notifyChange(uri, null);
            return 1; // 1 row affected
        }
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // Updates are not typically used for file-based providers in this way.
        // Clients can get a write handle via openFile() and modify the contents directly.
        throw new UnsupportedOperationException("Update not supported");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (uriMatcher.match(uri) != FILE_ID) {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        File file = getFileForUri(uri);

        // Again, check for path traversal for safety.
        try {
            if (!file.getCanonicalPath().startsWith(mSharedDir.getCanonicalPath())) {
                 throw new SecurityException("Attempted path traversal");
            }
        } catch (IOException e) {
            throw new SecurityException("Could not verify path", e);
        }


        if (!file.exists()) {
            throw new FileNotFoundException(uri.getPath());
        }

        int accessMode = ParcelFileDescriptor.parseMode(mode);
        return ParcelFileDescriptor.open(file, accessMode);
    }

    /**
     * Helper to get the java.io.File for a given URI.
     */
    private File getFileForUri(Uri uri) {
        String fileName = uri.getLastPathSegment();
        return new File(mSharedDir, fileName);
    }

    /**
     * Helper to handle the projection. If the client provides a null projection,
     * we return a default set of columns for file metadata.
     */
    private String[] resolveProjection(String[] projection) {
        if (projection == null) {
            return new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        }
        return projection;
    }
}
