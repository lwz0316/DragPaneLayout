package com.lwz.dragpanelayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.Toast;

import com.lwz.dragpanelayout.view.DragPaneLayout;
import com.lwz.dragpanelayout.view.DragPaneLayout.Mode;
import com.lwz.dragpanelayout.view.TransformationDragPaneLayout;

public class MainActivity extends ListActivity {

	TransformationDragPaneLayout mDragPaneLayout;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		mDragPaneLayout = (TransformationDragPaneLayout) findViewById(R.id.drag_pane_layout);
		mDragPaneLayout.setDragPane(android.R.id.list);
		mDragPaneLayout.setSecondaryView(R.id.bottom_view);
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		for( int i=0; i<20; i++ ) {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("text", "item " + i);
			data.add(map);
		}
		SimpleAdapter adapter = new SimpleAdapter(this, data, 
				R.layout.item_swipe, new String[]{"text"}, new int[]{R.id.drag_pane_layout});
		adapter.setViewBinder(new ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Object data, String textRepresentation) {
				final DragPaneLayout paneLayout = ((DragPaneLayout)view);
				paneLayout.setDragRangeOffset(0.4f);
				paneLayout.setMode(Mode.BOTH);
				TextView text;
				(text = (TextView)paneLayout.findViewById(R.id.text)).setText(textRepresentation);
				paneLayout.setDragPane(text);
				paneLayout.findViewById(R.id.botton).setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						paneLayout.closePane();
					}
				});
				return true;
			}
		});
		setListAdapter(adapter);
		
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Toast.makeText(this, getListAdapter().getItem(position).toString(), Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		final int menuId = item.getItemId(); 
		if( R.id.menu_left == menuId ) {
			mDragPaneLayout.setMode(Mode.LEFT);
		} else if( R.id.menu_right == menuId ) {
			mDragPaneLayout.setMode(Mode.RIGHT);
		} else {
			mDragPaneLayout.setMode(Mode.BOTH);
		}
		return true;
	}
	
	public void onToggleOpenPaneBtnCLick(View view) {
		Button btn = (Button) view;
		boolean isOpen = mDragPaneLayout.isOpened();
		btn.setText(isOpen ? "Close" : "Open");
		if( isOpen ) {
			mDragPaneLayout.closePane();
		} else {
			mDragPaneLayout.openPane();
		}
	}
	
	public void onToggleDragOpenable(View view) {
		boolean isDragOpenable = mDragPaneLayout.isDragOpenable();
		Toast.makeText(this, !isDragOpenable ? "可以拖动打开":"禁止打开" , Toast.LENGTH_SHORT).show();
		mDragPaneLayout.setDragOpenable( !isDragOpenable);
	}
	
	public void onOffsetChange(View view) {
		float currentOffset = mDragPaneLayout.getDragRangeOffset();
		if( currentOffset == 0.8f ) {
			mDragPaneLayout.setDragRangeOffset(0.6f);
		} else {
			mDragPaneLayout.setDragRangeOffset(0.8f);
		}
	}
}
