package com.galaxyjoy.hexviewer.ui.act;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.galaxyjoy.hexviewer.MyApplication;
import com.galaxyjoy.hexviewer.R;
import com.galaxyjoy.hexviewer.models.FileData;
import com.galaxyjoy.hexviewer.models.UriData;
import com.galaxyjoy.hexviewer.ui.adt.AdtRecentlyOpenRecycler;

import java.util.ArrayList;
import java.util.List;

public class ActRecentlyOpen extends AppCompatActivity implements AdtRecentlyOpenRecycler.OnEventListener {
    private MyApplication mApp = null;
    public static final String RESULT_START_OFFSET = "startOffset";
    public static final String RESULT_END_OFFSET = "endOffset";
    public static final String RESULT_OLD_TO_STRING = "oldToString";

    /**
     * Starts an activity.
     *
     * @param c                      Android context.
     * @param activityResultLauncher Activity Result Launcher.
     */
    public static void startActivity(final Context c,
                                     final ActivityResultLauncher<Intent> activityResultLauncher) {
        Intent intent = new Intent(c, ActRecentlyOpen.class);
        activityResultLauncher.launch(intent);
    }

    /**
     * Set the base context for this ContextWrapper.
     * All calls will then be delegated to the base context.
     * Throws IllegalStateException if a base context has already been set.
     *
     * @param base The new base context for this wrapper.
     */
//    @Override
//    protected void attachBaseContext(Context base) {
//        super.attachBaseContext(((MyApplication) base.getApplicationContext()).onAttach(base));
//    }

    @Override
    protected void attachBaseContext(Context base) {
        Configuration override = new Configuration(base.getResources().getConfiguration());
        override.fontScale = 1.0f;
        applyOverrideConfiguration(override);
//        super.attachBaseContext(base);
        super.attachBaseContext(((MyApplication) base.getApplicationContext()).onAttach(base));
    }

    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState Bundle
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.act_recently_open);
        mApp = (MyApplication) getApplicationContext();
        setupViews();
    }

    private void setupViews() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mApp.getRecentlyOpened().reload();
        // Lookup the recyclerview in activity layout
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        List<UriData> list = new ArrayList<>();
        final List<FileData> li = mApp.getRecentlyOpened().list();
        int index = 0;
        int max = li.size();
        int m = String.valueOf(max).length();
        for (int i = max - 1; i >= 0; i--) {
            list.add(new UriData(this, ++index, m, li.get(i)));
        }
        AdtRecentlyOpenRecycler adapter = new AdtRecentlyOpenRecycler(this, list, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(adapter.getSwipeToDeleteCallback());
        itemTouchHelper.attachToRecyclerView(recyclerView);

        setTitle(getString(R.string.action_recently_open_title));
    }

    /**
     * Called by the system when the device configuration changes while your activity is running.
     *
     * @param newConfig The new device configuration. This value cannot be null.
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mApp.setConfiguration(newConfig);
    }

    /**
     * Called when the options item is clicked (home).
     *
     * @param item The selected menu.
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when a click is captured.
     *
     * @param ud The associated item.
     */
    @Override
    public void onClick(@NonNull UriData ud) {
        Intent i = new Intent();
        i.setData(ud.getFd().getUri());
        i.putExtra(RESULT_START_OFFSET, ud.getFd().getStartOffset());
        i.putExtra(RESULT_END_OFFSET, ud.getFd().getEndOffset());
        if (ud.isSizeChanged())
            i.putExtra(RESULT_OLD_TO_STRING, ud.getFd().toString());
        setResult(RESULT_OK, i);
        finish();
    }

    /**
     * Called when a click is captured.
     *
     * @param ud The associated item.
     */
    @Override
    public void onDelete(@NonNull UriData ud) {
        mApp.getRecentlyOpened().remove(ud.getFd());
        if (mApp.getRecentlyOpened().list().isEmpty()) {
            Intent i = new Intent();
            i.setData(null);
            setResult(RESULT_OK, i);
            finish();
        }
    }

}
