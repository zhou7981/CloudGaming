package com.example.yungamegl;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class GameList extends ListActivity {
	private List<Integer> picArray;
	private List<String> nameArray;
	private ImageView img;
	private int selected;

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		// 显示布局
		setContentView(R.layout.gamelist);

		TextView txt = (TextView) findViewById(R.id.text);
		txt.setText("Please select a game:");

		selected = 5;
		picArray = fillPic();
		nameArray = fillName();
		img = (ImageView) findViewById(R.id.pic);
		img.setImageResource(R.drawable.shift);
		img.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				 Intent i = new Intent(GameList.this, MainActivity.class);
				 i.putExtra("gameName", nameArray.get(selected));
				 //i.putExtra("uAge", (short)18);
				 startActivity (i);
			}
		});

		List<String> items = fillArray();
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.listrow, items);

		this.setListAdapter(adapter);
	}

	private List<Integer> fillPic() {
		List<Integer> items = new ArrayList<Integer>();
		items.add(R.drawable.angrybirds);
		items.add(R.drawable.battlefield);
		items.add(R.drawable.cod);
		items.add(R.drawable.gudao);
		items.add(R.drawable.shenghua);
		items.add(R.drawable.shift);
		items.add(R.drawable.war);
		return items;
	}

	private List<String> fillName() {
		List<String> items = new ArrayList<String>();
		items.add("angrybirds");
		items.add("BF2");
		items.add("CD6");
		items.add("Crysis");
		items.add("evil");
		items.add("need_for_speed_wanted");
		items.add("war3");
		return items;
	}

	private List<String> fillArray() {
		List<String> items = new ArrayList<String>();
		items.add("愤怒的小鸟");
		items.add("战地");
		items.add("使命召唤");
		items.add("孤岛危机");
		items.add("生化危机");
		items.add("极品飞车");
		items.add("魔兽世界");
		return items;
	}

	protected void onListItemClick(ListView l, View v, int position, long id) {
		String s = "Click: " + position
				+ l.getItemAtPosition(position).toString();
		Log.e("Text", s);
		// if (l.getSelectedItem() == null) {
		// Log.e("TextView", "null");
		// return;
		// }
		selected = position;
		img.setImageResource(picArray.get(position));
		// Intent i = new Intent(GameList.this, MainActivity.class);
		// i.putExtra("gameName", l.getItemAtPosition(position).toString());
		// //i.putExtra("uAge", (short)18);
		// startActivity (i);
	}
}