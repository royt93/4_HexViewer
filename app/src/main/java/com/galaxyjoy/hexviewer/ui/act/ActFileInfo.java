package com.galaxyjoy.hexviewer.ui.act;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.util.SysHelper;
import com.galaxyjoy.hexviewer.util.io.FileHelper;

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
        MAGIC_NUMBERS.put("47494638", "GIF Image");
        MAGIC_NUMBERS.put("25504446", "PDF Document");
        MAGIC_NUMBERS.put("504B0304", "ZIP / APK / JAR Archive");
        MAGIC_NUMBERS.put("52617221", "RAR Archive");
        MAGIC_NUMBERS.put("377ABCAF271C", "7-Zip Archive");
        MAGIC_NUMBERS.put("7F454C46", "ELF Executable");
        MAGIC_NUMBERS.put("6465780A", "Android Dalvik Executable (DEX)");
        MAGIC_NUMBERS.put("4D5A", "Windows/DOS Executable (EXE/DLL)");
        MAGIC_NUMBERS.put("53514C6974652066", "SQLite 3 Database");
        MAGIC_NUMBERS.put("41564920", "AVI Video"); // Usually RIFF...AVI
        MAGIC_NUMBERS.put("4F676753", "Ogg Audio/Video");
        MAGIC_NUMBERS.put("0000001866747970", "MP4 Video"); // Typical standard MP4
        MAGIC_NUMBERS.put("D0CF11E0A1B11AE1", "Microsoft Office Document (Legacy)");
        MAGIC_NUMBERS.put("1F8B08", "GZIP Archive");
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
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    bytesRead = inputStream.read(header);
                    inputStream.close();
                }

                if (bytesRead > 0) {
                    String hexString = bytesToHex(header, bytesRead);
                    String detectedType = detectFileType(hexString);
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

    private String detectFileType(String hexString) {
        for (Map.Entry<String, String> entry : MAGIC_NUMBERS.entrySet()) {
            if (hexString.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // Specific checks for types that might not start exactly at 0
        if (hexString.startsWith("52494646") && hexString.length() >= 16) {
            // RIFF header, could be AVI, WEBP, WAV
            String type = hexString.substring(16, Math.min(24, hexString.length()));
            if (type.equals("41564920")) return "AVI Video";
            if (type.equals("57454250")) return "WEBP Image";
            if (type.equals("57415645")) return "WAV Audio";
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
        String path = uri.getPath();
        if (path == null)
            return "Unknown File";
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash != -1)
            return path.substring(lastSlash + 1);
        return path;
    }
}
