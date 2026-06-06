package com.galaxyjoy.hexviewer.ui.act;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import com.galaxyjoy.hexviewer.ui.task.TaskHash;
import com.galaxyjoy.hexviewer.util.SysHelper;
import com.galaxyjoy.hexviewer.util.io.FileHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class ActHashCalculator extends AppCompatActivity {

    private ActivityResultLauncher<String[]> mFilePickerLauncher;
    private AdtHashResult mAdapter;
    private LinearProgressIndicator mProgressIndicator;
    private TextView mTvFileName;
    private TextView mTvFileSize;
    private TextView mTvMatchResult;
    private TextInputEditText mEtCompare;

    /** Current background hash task — cancelled in onDestroy() to prevent leaks. */
    private TaskHash mTaskHash;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_hash_calculator);
        initViews();
        setupFilePicker();
        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTaskHash != null) {
            mTaskHash.cancel();   // TaskRunner.cancel() shuts down executor + posts onCancelled()
            mTaskHash = null;
        }
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        mProgressIndicator = findViewById(R.id.progressIndicator);
        mTvFileName        = findViewById(R.id.tvFileName);
        mTvFileSize        = findViewById(R.id.tvFileSize);
        mEtCompare         = findViewById(R.id.etCompare);
        mTvMatchResult     = findViewById(R.id.tvMatchResult);

        RecyclerView rvHashResults = findViewById(R.id.rvHashResults);
        rvHashResults.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new AdtHashResult(this);
        rvHashResults.setAdapter(mAdapter);
    }

    private void setupFilePicker() {
        mFilePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) handleSelectedFile(uri);
                });
    }

    private void setupListeners() {
        findViewById(R.id.cardFileSelection).setOnClickListener(v -> openFilePicker());

        mEtCompare.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkMatch(s.toString());
            }
        });
    }

    private void openFilePicker() {
        mFilePickerLauncher.launch(new String[]{"*/*"});
    }

    private void handleSelectedFile(Uri uri) {
        String fileName = FileHelper.getFileName(this, uri);
        mTvFileName.setText(fileName != null ? fileName : "Unknown File");

        long size = FileHelper.getFileSize(this, getContentResolver(), uri);
        if (size > 0) {
            mTvFileSize.setText(SysHelper.sizeToHuman(this, size, true, true, false));
            mTvFileSize.setVisibility(View.VISIBLE);
        } else {
            mTvFileSize.setVisibility(View.GONE);
        }

        mProgressIndicator.setVisibility(View.VISIBLE);
        mTvMatchResult.setVisibility(View.GONE);
        mAdapter.setData(new ArrayList<>());

        // Cancel any previous task
        if (mTaskHash != null) {
            mTaskHash.cancel();
        }

        // Use WeakReference to avoid holding a strong reference to the Activity inside the lambda,
        // preventing a memory leak if the task outlives the Activity.
        final WeakReference<ActHashCalculator> weakThis = new WeakReference<>(this);

        mTaskHash = new TaskHash(result -> {
            ActHashCalculator activity = weakThis.get();
            if (activity == null || activity.isDestroyed() || activity.isFinishing()) return;

            activity.mProgressIndicator.setVisibility(View.INVISIBLE);

            if (!result.isSuccess()) {
                if (!result.isCancelled()) {
                    Toast.makeText(activity, R.string.hash_error_open_file, Toast.LENGTH_SHORT).show();
                }
                return;
            }

            activity.mAdapter.setData(result.items);

            // Show comparison input with animation if hidden
            View tilCompare = activity.findViewById(R.id.tilCompare);
            if (tilCompare.getVisibility() != View.VISIBLE) {
                tilCompare.setAlpha(0f);
                tilCompare.setVisibility(View.VISIBLE);
                tilCompare.animate().alpha(1f).setDuration(300).start();
            }

            if (activity.mEtCompare.getText() != null) {
                activity.checkMatch(activity.mEtCompare.getText().toString());
            }
        });

        mTaskHash.execute(new TaskHash.Request(getContentResolver(), uri));
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
}
