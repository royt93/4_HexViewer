package com.galaxyjoy.hexviewer.ui.act;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.util.SysHelper;
import com.galaxyjoy.hexviewer.util.io.FileHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActFileInfo extends AppCompatActivity {

    private ActivityResultLauncher<String[]> mFilePickerLauncher;
    private LinearProgressIndicator mProgressIndicator;
    private TextView mTvFileName;
    private TextView mTvFileSize;
    private ImageView mIvFileIcon;

    private MaterialCardView mCardResult;
    private TextView mTvDetectedType;
    private TextView mTvMagicBytes;

    private ExecutorService mExecutor;

    private static final Map<String, String> MAGIC_NUMBERS = new HashMap<>();

    static {
        // Build Magic Number Dictionary
        MAGIC_NUMBERS.put("89504E47", "PNG Image");
        MAGIC_NUMBERS.put("FFD8FFE0", "JPEG Image");
        MAGIC_NUMBERS.put("FFD8FFE1", "JPEG Image (EXIF)");
        MAGIC_NUMBERS.put("FFD8FFEE", "JPEG Image (EXIF)");
        MAGIC_NUMBERS.put("FFD8FFDB", "JPEG Image");
        MAGIC_NUMBERS.put("47494638", "GIF Image");
        MAGIC_NUMBERS.put("25504446", "PDF Document");
        MAGIC_NUMBERS.put("504B0304", "ZIP / APK / JAR / Office Archive");
        MAGIC_NUMBERS.put("52617221", "RAR Archive");
        MAGIC_NUMBERS.put("377ABCAF271C", "7-Zip Archive");
        MAGIC_NUMBERS.put("7F454C46", "ELF Executable");
        MAGIC_NUMBERS.put("6465780A", "Android DEX");
        MAGIC_NUMBERS.put("4D5A", "Windows/DOS Executable (EXE/DLL)");
        MAGIC_NUMBERS.put("53514C6974652066", "SQLite 3 Database");
        MAGIC_NUMBERS.put("4F676753", "Ogg Audio/Video");
        MAGIC_NUMBERS.put("D0CF11E0A1B11AE1", "Legacy Office Document");
        MAGIC_NUMBERS.put("1F8B08", "GZIP Archive");
        MAGIC_NUMBERS.put("494433", "MP3 Audio (ID3v2)"); 
        MAGIC_NUMBERS.put("664C6143", "FLAC Audio");
        MAGIC_NUMBERS.put("1A45DFA3", "WebM / MKV Video");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_file_info);

        mExecutor = Executors.newSingleThreadExecutor();

        initViews();
        setupFilePicker();
        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
        }
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        mProgressIndicator = findViewById(R.id.progressIndicator);
        mTvFileName = findViewById(R.id.tvFileName);
        mTvFileSize = findViewById(R.id.tvFileSize);
        mIvFileIcon = findViewById(R.id.ivFileIcon);

        mCardResult = findViewById(R.id.cardResult);
        mTvDetectedType = findViewById(R.id.tvDetectedType);
        mTvMagicBytes = findViewById(R.id.tvMagicBytes);
    }

    private void setupFilePicker() {
        mFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        handleSelectedFile(uri);
                    }
                });
    }

    private void setupListeners() {
        findViewById(R.id.cardFileSelection).setOnClickListener(v -> openFilePicker());
    }

    private void openFilePicker() {
        mFilePickerLauncher.launch(new String[]{"*/*"});
    }

    private void handleSelectedFile(Uri uri) {
        String fileName = getFileName(uri);
        mTvFileName.setText(fileName);

        // Show file size
        long size = FileHelper.getFileSize(this, getContentResolver(), uri);
        if (size > 0) {
            mTvFileSize.setText(SysHelper.sizeToHuman(this, size, true, true, false));
            mTvFileSize.setVisibility(View.VISIBLE);
        } else {
            mTvFileSize.setVisibility(View.GONE);
        }

        mProgressIndicator.setVisibility(View.VISIBLE);
        mCardResult.setVisibility(View.GONE);

        mExecutor.execute(() -> {
            try {
                // Read magic bytes
                byte[] header = new byte[16];
                int bytesRead = 0;
                try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                    if (inputStream != null) {
                        bytesRead = inputStream.read(header);
                    }
                }

                if (bytesRead > 0) {
                    String hexString = bytesToHex(header, bytesRead);
                    String detectedType = detectFileType(hexString, header, bytesRead);
                    String formattedBytes = formatMagicBytes(hexString);

                    runOnUiThread(() -> {
                        if (!isDestroyed() && !isFinishing()) {
                            mCardResult.setVisibility(View.VISIBLE);
                            mTvDetectedType.setText(detectedType);
                            mTvMagicBytes.setText(formattedBytes);
                            mProgressIndicator.setVisibility(View.INVISIBLE);
                        }
                    });
                } else {
                    throw new Exception("File is empty or cannot be read.");
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing()) {
                        Toast.makeText(this, R.string.hash_error_open_file, Toast.LENGTH_SHORT).show();
                        mProgressIndicator.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
    }

    private String detectFileType(String hexString, byte[] header, int bytesRead) {
        for (Map.Entry<String, String> entry : MAGIC_NUMBERS.entrySet()) {
            if (hexString.toUpperCase().startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Specific checks for types that might not start exactly at 0
        if (hexString.toUpperCase().startsWith("52494646") && hexString.length() >= 24) {
            // RIFF header, could be AVI, WEBP, WAV
            String type = hexString.substring(16, 24).toUpperCase();
            if (type.equals("41564920")) return "AVI Video";
            if (type.equals("57454250")) return "WEBP Image";
            if (type.equals("57415645")) return "WAV Audio";
        }

        // MP4 format based on 'ftyp' atom at offset 4
        if (hexString.length() >= 24 && hexString.substring(8, 16).toUpperCase().equals("66747970")) {
            return "MP4 Video";
        }
        
        // XML document fast check
        if (hexString.toUpperCase().startsWith("3C3F786D6C")) {
            return "XML Document";
        }

        // Plain Text Check (ASCII / UTF-8) fallback
        if (bytesRead > 0) {
            boolean isText = true;
            for(int i = 0; i < bytesRead; i++) {
                byte b = header[i];
                // Look for strictly invalid text byte values (control characters from 0 to 31 excluding space, tab, LF, CR)
                // In Java, bytes are signed, so anything >= 0 and < 32 is a basic ASCII control character.
                if (b >= 0 && b < 32 && b != 9 && b != 10 && b != 13) {
                    isText = false;
                    break;
                }
            }
            if (isText) {
                return "Plain Text Document";
            }
        }

        return getString(R.string.file_info_unknown);
    }

    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }

    private String formatMagicBytes(String rawHex) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawHex.length(); i += 2) {
            sb.append(rawHex.substring(i, Math.min(i + 2, rawHex.length()))).append(" ");
        }
        return sb.toString().trim();
    }

    private String getFileName(Uri uri) {
        String fileName = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (fileName == null) {
            String path = uri.getPath();
            if (path == null)
                return "Unknown File";
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1)
                return path.substring(lastSlash + 1);
            return path;
        }
        return fileName;
    }
}
