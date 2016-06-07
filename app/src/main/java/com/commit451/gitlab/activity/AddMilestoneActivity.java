package com.commit451.gitlab.activity;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.commit451.easel.Easel;
import com.commit451.gitlab.LabCoatApp;
import com.commit451.gitlab.R;
import com.commit451.easycallback.EasyCallback;
import com.commit451.gitlab.api.GitLabClient;
import com.commit451.gitlab.event.MilestoneChangedEvent;
import com.commit451.gitlab.event.MilestoneCreatedEvent;
import com.commit451.gitlab.model.api.Milestone;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.parceler.Parcels;

import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Callback;
import timber.log.Timber;

public class AddMilestoneActivity extends MorphActivity {

    private static final String KEY_PROJECT_ID = "project_id";
    private static final String KEY_MILESTONE = "milestone";

    public static Intent newIntent(Context context, long projectId) {
        return newIntent(context, projectId, null);
    }

    public static Intent newIntent(Context context, long projectId, Milestone milestone) {
        Intent intent = new Intent(context, AddMilestoneActivity.class);
        intent.putExtra(KEY_PROJECT_ID, projectId);
        if (milestone != null) {
            intent.putExtra(KEY_MILESTONE, Parcels.wrap(milestone));
        }
        return intent;
    }

    @BindView(R.id.root)
    FrameLayout mRoot;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.title_text_input_layout)
    TextInputLayout mTitleTextInputLayout;
    @BindView(R.id.title)
    EditText mTitle;
    @BindView(R.id.description)
    EditText mDescription;
    @BindView(R.id.due_date)
    Button mDueDate;
    @BindView(R.id.progress)
    View mProgress;

    @OnClick(R.id.due_date)
    void onDueDateClicked() {
        Calendar now = Calendar.getInstance();
        if (mCurrentDate != null) {
            now.setTime(mCurrentDate);
        }
        DatePickerDialog dpd = DatePickerDialog.newInstance(
                mOnDateSetListener,
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dpd.setAccentColor(Easel.getThemeAttrColor(this, R.attr.colorAccent));
        dpd.show(getFragmentManager(), "date_picker");
    }

    long mProjectId;
    Milestone mMilestone;
    Date mCurrentDate;

    private final DatePickerDialog.OnDateSetListener mOnDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, monthOfYear);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            mCurrentDate = calendar.getTime();
            bind(mCurrentDate);
        }
    };

    private final View.OnClickListener mOnBackPressed = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onBackPressed();
        }
    };

    private Callback<Milestone> mMilestoneCallback = new EasyCallback<Milestone>() {

        @Override
        public void success(@NonNull Milestone response) {
            mProgress.setVisibility(View.GONE);
            if (mMilestone == null) {
                LabCoatApp.bus().post(new MilestoneCreatedEvent(response));
            } else {
                LabCoatApp.bus().post(new MilestoneChangedEvent(response));
            }
            finish();
        }

        @Override
        public void failure(Throwable t) {
            Timber.e(t, null);
            mProgress.setVisibility(View.GONE);
            showError();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_milestone);
        ButterKnife.bind(this);
        morph(mRoot);
        mProjectId = getIntent().getLongExtra(KEY_PROJECT_ID, -1);
        mMilestone = Parcels.unwrap(getIntent().getParcelableExtra(KEY_MILESTONE));
        if (mMilestone != null) {
            bind(mMilestone);
            mToolbar.inflateMenu(R.menu.menu_edit_milestone);
        } else {
            mToolbar.inflateMenu(R.menu.menu_add_milestone);
        }
        mToolbar.setNavigationIcon(R.drawable.ic_back_24dp);
        mToolbar.setNavigationOnClickListener(mOnBackPressed);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_create:
                    case R.id.action_edit:
                        createMilestone();
                        return true;
                }
                return false;
            }
        });
    }

    private void createMilestone() {
        if (TextUtils.isEmpty(mTitle.getText())) {
            mTitleTextInputLayout.setError(getString(R.string.required_field));
            return;
        }

        mProgress.setVisibility(View.VISIBLE);
        String dueDate = null;
        if (mCurrentDate != null) {
            dueDate = Milestone.DUE_DATE_FORMAT.format(mCurrentDate);
        }

        if (mMilestone == null) {
            GitLabClient.instance().createMilestone(mProjectId,
                    mTitle.getText().toString(),
                    mDescription.getText().toString(),
                    dueDate).enqueue(mMilestoneCallback);
        } else {
            GitLabClient.instance().editMilestone(mProjectId,
                    mMilestone.getId(),
                    mTitle.getText().toString(),
                    mDescription.getText().toString(),
                    dueDate).enqueue(mMilestoneCallback);
        }

    }

    private void showError() {
        Snackbar.make(mRoot, getString(R.string.failed_to_create_milestone), Snackbar.LENGTH_SHORT)
                .show();
    }

    private void bind(Date date) {
        mDueDate.setText(Milestone.DUE_DATE_FORMAT.format(date));
    }

    private void bind(Milestone milestone) {
        mTitle.setText(milestone.getTitle());
        if (milestone.getDescription() != null) {
            mDescription.setText(milestone.getDescription());
        }
        if (milestone.getDueDate() != null) {
            mCurrentDate = milestone.getDueDate();
            bind(mCurrentDate);
        }
    }
}
