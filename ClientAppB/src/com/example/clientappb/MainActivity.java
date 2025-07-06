package com.example.clientappb;

import android.app.Activity;
import android.content.ContentValues;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final String PROVIDER_AUTHORITY = "com.example.sharedstorage.provider";
    private static final Uri FOLDER_URI = Uri.parse("content://" + PROVIDER_AUTHORITY + "/files");

    private EditText mEditTextFilename;
    private EditText mEditTextContent;
    private TextView mTextViewOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEditTextFilename = findViewById(R.id.edit_text_filename);
        mEditTextContent = findViewById(R.id.edit_text_content);
        mTextViewOutput = findViewById(R.id.text_view_output);

        Button buttonCreate = findViewById(R.id.button_create);
        Button buttonWrite = findViewById(R.id.button_write);
        Button buttonRead = findViewById(R.id.button_read);
        Button buttonDelete = findViewById(R.id.button_delete);
        Button buttonList = findViewById(R.id.button_list);

        buttonCreate.setOnClickListener(v -> createFile());
        buttonWrite.setOnClickListener(v -> writeFile());
        buttonRead.setOnClickListener(v -> readFile());
        buttonDelete.setOnClickListener(v -> deleteFile());
        buttonList.setOnClickListener(v -> listFiles());
    }

    private Uri getFileUri(String filename) {
        if (filename.isEmpty()) {
            Toast.makeText(this, "Filename cannot be empty", Toast.LENGTH_SHORT).show();
            return null;
        }
        return FOLDER_URI.buildUpon().appendPath(filename).build();
    }

    private void createFile() {
        String filename = mEditTextFilename.getText().toString();
        if (filename.isEmpty()) {
            Toast.makeText(this, "Filename cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(OpenableColumns.DISPLAY_NAME, filename);

        try {
            Uri newFileUri = getContentResolver().insert(FOLDER_URI, values);
            if (newFileUri != null) {
                mTextViewOutput.setText("Created file: " + newFileUri.toString());
            } else {
                mTextViewOutput.setText("Failed to create file.");
            }
        } catch (Exception e) {
            mTextViewOutput.setText("Error creating file: " + e.getMessage());
        }
    }

    private void writeFile() {
        String filename = mEditTextFilename.getText().toString();
        String content = mEditTextContent.getText().toString();
        Uri fileUri = getFileUri(filename);
        if (fileUri == null) return;

        try (OutputStream os = getContentResolver().openOutputStream(fileUri, "w")) {
            if (os != null) {
                os.write(content.getBytes(StandardCharsets.UTF_8));
                mTextViewOutput.setText("Wrote to file: " + filename);
            } else {
                mTextViewOutput.setText("Failed to open output stream.");
            }
        } catch (Exception e) {
            mTextViewOutput.setText("Error writing to file: " + e.getMessage());
        }
    }

    private void readFile() {
        String filename = mEditTextFilename.getText().toString();
        Uri fileUri = getFileUri(filename);
        if (fileUri == null) return;

        try (InputStream is = getContentResolver().openInputStream(fileUri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append('\n');
            }
            mEditTextContent.setText(stringBuilder.toString());
            mTextViewOutput.setText("Read from file: " + filename);
        } catch (Exception e) {
            mTextViewOutput.setText("Error reading file: " + e.getMessage());
            mEditTextContent.setText("");
        }
    }

    private void deleteFile() {
        String filename = mEditTextFilename.getText().toString();
        Uri fileUri = getFileUri(filename);
        if (fileUri == null) return;

        try {
            int rowsDeleted = getContentResolver().delete(fileUri, null, null);
            if (rowsDeleted > 0) {
                mTextViewOutput.setText("Deleted file: " + filename);
            } else {
                mTextViewOutput.setText("File not found or could not be deleted.");
            }
        } catch (Exception e) {
            mTextViewOutput.setText("Error deleting file: " + e.getMessage());
        }
    }

    private void listFiles() {
        try (Cursor cursor = getContentResolver().query(FOLDER_URI, null, null, null, null)) {
            if (cursor == null) {
                mTextViewOutput.setText("Query failed, cursor is null.");
                return;
            }

            StringBuilder fileList = new StringBuilder("Shared Files:\n");
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

            while (cursor.moveToNext()) {
                String name = cursor.getString(nameIndex);
                long size = cursor.getLong(sizeIndex);
                fileList.append(name).append(" (").append(size).append(" bytes)\n");
            }

            if (fileList.length() == "Shared Files:\n".length()) {
                mTextViewOutput.setText("No files found.");
            } else {
                mTextViewOutput.setText(fileList.toString());
            }
        } catch (Exception e) {
            mTextViewOutput.setText("Error listing files: " + e.getMessage());
        }
    }
}
