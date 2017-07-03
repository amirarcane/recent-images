package com.amirarcane.recentimagesapp;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.ButterKnife;

/**
 * Created by Nt on 9/28/2015.
 */
public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
	ArrayList<MenuItem> menuItems;

	public MenuAdapter(ArrayList<MenuItem> menuItems) {
		this.menuItems = menuItems;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.menu_item, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		int i = getItemViewType(position);
		if (i == 0) {
			return;
		}
		MenuItem item = menuItems.get(position);
		if (holder.menuText != null) {
			holder.menuText.setText(item.getName());
		}
		if (holder.menuIcon != null) {
			holder.menuIcon.setImageResource(item.getResource());
		}
	}

	@Override
	public int getItemCount() {
		return menuItems.size();
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		TextView menuText;
		ImageView menuIcon;

		public ViewHolder(View itemView) {
			super(itemView);
			ButterKnife.bind(this, itemView);
			menuText = (TextView) itemView.findViewById(R.id.menuText);
			menuIcon = (ImageView) itemView.findViewById(R.id.menuIcon);
		}
	}

	@Override
	public int getItemViewType(int position) {
		MenuItem menuItem = menuItems.get(position);
		if (menuItem.getName().equals("")) {
			return 0;
		} else
			return 1;
	}
}
