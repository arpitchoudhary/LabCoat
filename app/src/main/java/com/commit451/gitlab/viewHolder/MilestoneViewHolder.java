package com.commit451.gitlab.viewHolder;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.commit451.gitlab.R;
import com.commit451.gitlab.model.api.Milestone;
import com.commit451.gitlab.util.DateUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Milestone
 */
public class MilestoneViewHolder extends RecyclerView.ViewHolder {

    public static MilestoneViewHolder inflate(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_milestone, parent, false);
        return new MilestoneViewHolder(view);
    }

    @BindView(R.id.title)
    TextView textTitle;
    @BindView(R.id.due_date)
    TextView textDueDate;

    public MilestoneViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);
    }

    public void bind(Milestone milestone) {
        textTitle.setText(milestone.getTitle());
        if (milestone.getDueDate() != null) {
            textDueDate.setVisibility(View.VISIBLE);
            CharSequence due = DateUtil.getRelativeTimeSpanString(itemView.getContext(), milestone.getDueDate());
            textDueDate.setText(String.format(itemView.getResources().getString(R.string.due_date_formatted), due));
        } else {
            textDueDate.setVisibility(View.GONE);
            textDueDate.setText("");
        }
    }
}