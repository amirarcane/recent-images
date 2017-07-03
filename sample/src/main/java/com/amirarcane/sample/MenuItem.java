package com.amirarcane.sample;

import android.view.View;

/**
 * Created by Nt on 9/28/2015.
 */
public class MenuItem {
	private final String name;
	private final int resource;
	private View.OnClickListener onClickListener;

	public MenuItem(String name, int resource, View.OnClickListener onClickListener) {
		this.name = name;
		this.resource = resource;
		this.onClickListener = onClickListener;
	}

	public MenuItem(String name, int resource) {
		this.name = name;
		this.resource = resource;
	}

	public String getName() {
		return name;
	}

	public int getResource() {
		return resource;
	}

	public View.OnClickListener getOnClickListener() {
		return onClickListener;
	}
}
