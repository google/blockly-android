package com.google.blockly.android.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.blockly.android.demo.flyout.NoCategoriesToolboxActivity;
import com.google.blockly.android.demo.flyout.AlwaysOpenToolboxActivity;

/**
 * Entry activity for demos showing different flyout configurations.
 */

public class FlyoutDemos extends Activity{
    final static int[] DEMO_NAMES = {
            R.string.no_categories_toolbox_activity_name,
            R.string.always_open_toolbox_activity_name
    };

    final static Class[] DEMO_TARGETS = {
            NoCategoriesToolboxActivity.class,
            AlwaysOpenToolboxActivity.class
    };

    private LayoutInflater mHelium;
    private Resources mRes;
    private RecyclerView mRecyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flyout_demo_list);
        mRecyclerView = (RecyclerView) findViewById(R.id.demo_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(new Adapter());
        mHelium = getLayoutInflater();
        mRes = getResources();
    }

    private class Adapter extends RecyclerView.Adapter {

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mHelium.inflate(R.layout.list_text_item, parent, false));
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            ((TextView) holder.itemView).setText(mRes.getText(DEMO_NAMES[position]));
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(FlyoutDemos.this, DEMO_TARGETS[holder.getAdapterPosition()]);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return DEMO_NAMES.length;
        }
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
