package com.commit451.gitlab.viewHolder;

import android.graphics.Color;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.commit451.gitlab.App;
import com.commit451.gitlab.R;
import com.commit451.gitlab.model.api.Project;
import com.commit451.gitlab.transformation.CircleTransformation;
import com.github.ivbaranov.mli.MaterialLetterIcon;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Projects, yay!
 */
public class ProjectViewHolder extends RecyclerView.ViewHolder {

    public static ProjectViewHolder inflate(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @BindView(R.id.project_image)
    ImageView image;
    @BindView(R.id.project_letter)
    MaterialLetterIcon iconLetter;
    @BindView(R.id.project_title)
    TextView textTitle;
    @BindView(R.id.project_description)
    TextView textDescription;
    @BindView(R.id.project_visibility)
    ImageView iconVisibility;

    public ProjectViewHolder(View view) {
        super(view);
        ButterKnife.bind(this, view);
    }

    public void bind(Project project, int color) {
        if (project.getAvatarUrl() != null && !project.getAvatarUrl().equals(Uri.EMPTY)) {
            iconLetter.setVisibility(View.GONE);

            image.setVisibility(View.VISIBLE);
            App.get().getPicasso()
                    .load(project.getAvatarUrl())
                    .transform(new CircleTransformation())
                    .into(image);
        } else {
            image.setVisibility(View.GONE);

            iconLetter.setVisibility(View.VISIBLE);
            iconLetter.setLetter(project.getName().substring(0, 1));
            iconLetter.setLetterColor(Color.WHITE);
            iconLetter.setShapeColor(color);
        }

        textTitle.setText(project.getNameWithNamespace());
        if (!TextUtils.isEmpty(project.getDescription())) {
            textDescription.setVisibility(View.VISIBLE);
            textDescription.setText(project.getDescription());
        } else {
            textDescription.setVisibility(View.GONE);
            textDescription.setText("");
        }

        if (project.isPublic()) {
            iconVisibility.setImageResource(R.drawable.ic_public_24dp);
        } else {
            iconVisibility.setImageResource(R.drawable.ic_private_24dp);
        }
    }
}
