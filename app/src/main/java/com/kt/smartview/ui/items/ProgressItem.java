package com.kt.smartview.ui.items;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.kt.smartview.R;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class ProgressItem extends AbstractFlexibleItem<ProgressItem.ProgressViewHolder> {
	ProgressViewHolder viewHolder;
	@Override
	public boolean equals(Object o) {
		return this == o;//The default implementation
	}

	@Override
	public int getLayoutRes() {
		return R.layout.item_progress;
	}

	@Override
	public ProgressViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
		viewHolder = new ProgressViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter);
		return viewHolder;
	}

	@Override
	public void bindViewHolder(FlexibleAdapter adapter, ProgressViewHolder holder, int position, List payloads) {
		//nothing to bind
	}

	public static class ProgressViewHolder extends FlexibleViewHolder {

		public ProgressBar progressBar;

		public ProgressViewHolder(View view, FlexibleAdapter adapter) {
			super(view, adapter);
			progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
		}
	}

	public FlexibleViewHolder getViewHolder(){
		return viewHolder;
	}
}