/**
 * taskwarrior for android – a task list manager
 *
 * Copyright (c) 2012 Sujeevan Vijayakumaran
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in
 * allcopies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * http://www.opensource.org/licenses/mit-license.php
 *
 */

package org.svij.taskwarriorapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import org.svij.taskwarriorapp.data.Task;
import org.svij.taskwarriorapp.db.TaskDataSource;

import com.actionbarsherlock.app.SherlockListFragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MenuListFragment extends SherlockListFragment {

	TaskDataSource datasource;
	private long selectedItemId = -1;
	private String column;
	ArrayListFragment listFragment;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListView();

		ListView listview = getListView();
		listview.setDividerHeight(0);

		listFragment = (ArrayListFragment) getSherlockActivity()
				.getSupportFragmentManager().findFragmentById(
						R.id.content_frame);

		final SlidingPaneLayout paneLayout = (SlidingPaneLayout) getActivity()
				.findViewById(R.id.drawer_layout);

		listview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				listFragment.onTaskButtonClick(view);
				String menu_text = ((TextView) view).getText().toString();
				listFragment.setColumn(menu_text);
				listFragment.setListView();
				paneLayout.closePane();
				setActionBarTitle();
				getListView().setItemChecked(position, true);
			}
		});
	}

	public void setListView() {
		datasource = new TaskDataSource(getActivity());
		datasource.createDataIfNotExist();

		ArrayList<String> menuCommands = new ArrayList<String>();

		menuCommands.add(getResources().getString(R.string.task_next));
		menuCommands.add(getResources().getString(R.string.task_long));
		menuCommands.add(getResources().getString(R.string.task_all));
		menuCommands.add(getResources().getString(R.string.task_wait));

		menuCommands.addAll(datasource.getProjects());
		if (menuCommands.remove(null)) {
			menuCommands.add(getString(R.string.no_project));
		}
		menuCommands.removeAll(Collections.singleton(null));

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
				android.R.layout.simple_list_item_1, menuCommands);
		setListAdapter(adapter);
	}

	public void setActionBarTitle() {
		int counter = listFragment.getListView().getCount();

		if (counter == 1) {
			getSherlockActivity().getSupportActionBar().setTitle(
					listFragment.getColumn() + " (1 Task)");
		} else if (counter > 1) {
			getSherlockActivity().getSupportActionBar().setTitle(
					listFragment.getColumn()
							+ " ("
							+ listFragment.getListView().getCount()
							+ getResources().getString(
									R.string.title_task_counter) + ")");
		}
	}

	public void onTaskButtonClick(View view) {
		switch (view.getId()) {
		case R.id.btnTaskDelete:
			deleteTask(getTaskWithId(selectedItemId));
			break;
		case R.id.btnTaskModify:
			showAddTaskActivity(getTaskWithId(selectedItemId));
			break;
		case R.id.btnTaskAddReminder:
			Task task = datasource.getTask(getTaskWithId(selectedItemId));
			Intent intent = new Intent(Intent.ACTION_EDIT);
			intent.setType("vnd.android.cursor.item/event");
			if (task.getDue() != null) {
				intent.putExtra("beginTime", task.getDue().getTime());
			}
			intent.putExtra("title", task.getDescription());
			startActivity(intent);
			break;
		case R.id.btnTaskDone:
			doneTask(getTaskWithId(selectedItemId));
			break;
		default:
			break;
		}
	}

	protected void onSavedInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		getChildFragmentManager().putFragment(outState,
				ArrayListFragment.class.getName(), listFragment);
	}

	private void showAddTaskActivity(UUID uuid) {
		Intent intent = new Intent(getActivity(), TaskAddActivity.class);
		intent.putExtra("taskID", uuid.toString());
		startActivity(intent);
	}

	private void deleteTask(UUID uuid) {
		datasource.deleteTask(uuid);
		setListView();
		Toast.makeText(
				getActivity(),
				getString(R.string.task_action_delete) + " '"
						+ datasource.getTask(uuid).getDescription() + "'",
				Toast.LENGTH_SHORT).show();
	}

	private void doneTask(UUID uuid) {
		datasource.doneTask(uuid);
		setListView();
		Toast.makeText(
				getActivity(),
				getString(R.string.task_action_done) + " '"
						+ datasource.getTask(uuid).getDescription() + "'",
				Toast.LENGTH_SHORT).show();
	}

	private UUID getTaskWithId(long selectedItemId) {
		return ((Task) getListAdapter().getItem((int) selectedItemId - 1))
				.getUuid();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	class TaskSorter implements Comparator<Task> {
		private String sortType;

		public TaskSorter(String sortType) {
			this.sortType = sortType;
		}

		@Override
		public int compare(Task task1, Task task2) {
			if (sortType.equals("urgency")) {
				return Float.compare(task2.getUrgency(), task1.getUrgency());
			} else {
				if (task1.getDue() == null && task2.getDue() == null) {
					return 0;
				} else if (task1.getDue() == null) {
					return 1;
				} else if (task2.getDue() == null) {
					return -1;
				}
				return task1.getDue().compareTo(task2.getDue());
			}
		}
	}
}