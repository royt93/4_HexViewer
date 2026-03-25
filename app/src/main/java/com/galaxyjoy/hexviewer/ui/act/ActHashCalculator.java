package com.galaxyjoy.hexviewer.ui.act;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.ui.adt.AdtHashResult;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ActHashCalculator extends AppCompatActivity {

    private ActivityResultLauncher<String[]> mFilePickerLauncher;
    private AdtHashResult mAdapter;
    private LinearProgressIndicator mProgressIndicator;
    private TextView mTvFileName;
    private TextView mTvFileSize;
    private ImageView mIvFileIcon;
    private TextView mTvMatchResult;
    private TextInputEditText mEtCompare;

    // Executor for background tasks
    private ExecutorService mExecutor;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_hash_calculator);

        mExecutor = Executors.newSingleThreadExecutor();

        initViews();
        setupFilePicker();
        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mExecutor != null) {
            mExecutor.shutdownNow(); // Cancel running tasks
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
        mEtCompare = findViewById(R.id.etCompare);
        mTvMatchResult = findViewById(R.id.tvMatchResult);

        RecyclerView rvHashResults = findViewById(R.id.rvHashResults);
        rvHashResults.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new AdtHashResult(this);
        rvHashResults.setAdapter(mAdapter);
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

        mEtCompare.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkMatch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void openFilePicker() {
        mFilePickerLauncher.launch(new String[] { "*/*" });
    }

    private void handleSelectedFile(Uri uri) {
        String fileName = getFileName(uri);
        mTvFileName.setText(fileName);

        // Show file size
        long size = com.galaxyjoy.hexviewer.util.io.FileHelper.getFileSize(this, getContentResolver(), uri);
        if (size > 0) {
            mTvFileSize.setText(com.galaxyjoy.hexviewer.util.SysHelper.sizeToHuman(this, size, true, true, false));
            mTvFileSize.setVisibility(View.VISIBLE);
        } else {
            mTvFileSize.setVisibility(View.GONE);
        }

        mProgressIndicator.setVisibility(View.VISIBLE);
        mTvMatchResult.setVisibility(View.GONE);
        mAdapter.setData(new ArrayList<>()); // Clear previous results

        mExecutor.execute(() -> {
            try {
                // OPTIMIZATION: Calculate ALL hashes in ONE pass
                List<AdtHashResult.HashItem> results = calculateAllHashes(uri);

                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing()) {
                        mAdapter.setData(results);
                        mProgressIndicator.setVisibility(View.INVISIBLE);

                        // Show comparison input with animation if hidden
                        View tilCompare = findViewById(R.id.tilCompare);
                        if (tilCompare.getVisibility() != View.VISIBLE) {
                            tilCompare.setAlpha(0f);
                            tilCompare.setVisibility(View.VISIBLE);
                            tilCompare.animate().alpha(1f).setDuration(300).start();
                        }

                        if (mEtCompare.getText() != null) {
                            checkMatch(mEtCompare.getText().toString());
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (!isDestroyed() && !isFinishing()) {
                        Toast.makeText(ActHashCalculator.this, R.string.hash_error_open_file, Toast.LENGTH_SHORT).show();
                        mProgressIndicator.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
    }

    private List<AdtHashResult.HashItem> calculateAllHashes(Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null)
            throw new IllegalArgumentException("Stream is null");

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        MessageDigest sha512 = MessageDigest.getInstance("SHA-512");

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            md5.update(buffer, 0, bytesRead);
            sha1.update(buffer, 0, bytesRead);
            sha256.update(buffer, 0, bytesRead);
            sha512.update(buffer, 0, bytesRead);
        }
        inputStream.close();

        List<AdtHashResult.HashItem> list = new ArrayList<>();
        list.add(new AdtHashResult.HashItem("MD5", bytesToHex(md5.digest())));
        list.add(new AdtHashResult.HashItem("SHA-1", bytesToHex(sha1.digest())));
        list.add(new AdtHashResult.HashItem("SHA-256", bytesToHex(sha256.digest())));
        list.add(new AdtHashResult.HashItem("SHA-512", bytesToHex(sha512.digest())));
        return list;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void checkMatch(String compareString) {
        boolean matched = mAdapter.updateMatchStatus(compareString);
        if (compareString.isEmpty()) {
            mTvMatchResult.setVisibility(View.GONE);
        } else {
            mTvMatchResult.setVisibility(View.VISIBLE);
            if (matched) {
                mTvMatchResult.setText(R.string.hash_result_match);
                mTvMatchResult.setTextColor(getColor(R.color.colorResultSuccess));
            } else {
                mTvMatchResult.setText(R.string.hash_result_mismatch);
                mTvMatchResult.setTextColor(getColor(R.color.colorResultError));
            }
        }
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
