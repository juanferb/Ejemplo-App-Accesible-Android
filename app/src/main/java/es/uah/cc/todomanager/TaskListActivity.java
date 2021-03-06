package es.uah.cc.todomanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import es.uah.cc.todomanager.R;
import es.uah.cc.todomanager.domain.TaskList;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An activity representing a list of Tasks. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link TaskDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class TaskListActivity extends AppCompatActivity {

    /**
     * The key to exchange task objects between activities.
     */
    public static  final String ARG_TASK = "cc.uah.es.todomanager.task";
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    /**
     * A filtered task list.
     */
    private List<TaskList.Task> filteredTasks;
    /**
     * A OnSharedPreferenceChangeListener.
     * It must be a field of the activity in order to avoid the GC collects it.
     */
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    /**
     * Filters the task list attending to several preferences.
     * @param source    The original list.
     * @return The filtered list.
     */
    protected  List<TaskList.Task> filterTasks(List<TaskList.Task> source) {
        List<TaskList.Task> l = new ArrayList<TaskList.Task>();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hideCompleted = preferences.getBoolean(SettingsActivity.GeneralPreferenceFragment.HIDE_COMPLETED, false);
boolean hideCancelled = preferences.getBoolean(SettingsActivity.GeneralPreferenceFragment.hide_canceled, false);
        Iterator it = source.iterator();
        while (it.hasNext()) {
            TaskList.Task t = (TaskList.Task) it.next();
if (hideCancelled & t.getStatus().getStatusDescription().equals(TaskList.CanceledTask.STATUS)) continue;
            if (hideCompleted & t.getStatus().getStatusDescription().equals(TaskList.CompletedTask.STATUS)) continue;
            l.add(t);
        }
        return l;
    }

    /**
     * Filter the master task list and refresh the recycler view using the resulting list.
     * @param list    The recycler view list.
     */
    protected void refreshTasks(RecyclerView list) {
        filteredTasks = filterTasks(TaskList.getInstance().getTasks());
        setupRecyclerView(list, filteredTasks);
    }

    /**
     * Refresh the main recycler view.
     */
    protected void refreshTasks() {
refreshTasks((RecyclerView) findViewById(R.id.task_list));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        //Initializes the task list.
        TaskList.fillSampleData(TaskList.getInstance());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newTask(view);
            }
        });

        // Listens to changes on preferences.
        preferenceChangeListener = new OnFilterChangedListener();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        // Initializes the recycler view.
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.task_list);
        assert recyclerView != null;
        refreshTasks(recyclerView);

        if (findViewById(R.id.task_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private void setupRecyclerView(@NonNull RecyclerView recyclerView, List<TaskList.Task> tasks) {
        recyclerView.setAdapter(new SimpleItemRecyclerViewAdapter(tasks));
    }

    /**
     * An adapter to setup the recycler view elements.
     */
    public class SimpleItemRecyclerViewAdapter
            extends RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder> {

        // the task list.
        private final List<TaskList.Task> mValues;

        public SimpleItemRecyclerViewAdapter(List<TaskList.Task> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.task_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.mItem = mValues.get(position);
            // We change the color of the task's title depending on its priority if it is pending.
            if (holder.mItem.getStatus() instanceof TaskList.PendingTask) {
                switch (holder.mItem.getPriority()) {
                    case TaskList.Task.HIGH_PRIORITY:
                        holder.mNameView.setTextColor(getResources().getColor(R.color.high_priority));
                        break;
                    case TaskList.Task.LOW_PRIORITY:
                        holder.mNameView.setTextColor(getResources().getColor(R.color.low_priority));
                        break;
                    default: holder.mNameView.setTextColor(getResources().getColor(R.color.medium_priority));
                }
            } else {
                // Else we change the color of the task's title depending on its status.
                if (holder.mItem.getStatus() instanceof TaskList.CompletedTask) holder.mNameView.setTextColor(getResources().getColor(R.color.completed));
                else if (holder.mItem.getStatus() instanceof TaskList.CanceledTask) holder.mNameView.setTextColor(getResources().getColor(R.color.canceled));
            }

            holder.mNameView.setText(holder.mItem.getName());
            if (holder.mItem.isComplex() & holder.mItem.getStatus() instanceof TaskList.PendingTask)
                holder.mNameView.append("\n" + String.format(getResources().getString(R.string.percentage_completed), holder.mItem.getCompleted()));
            // Deadline is only shown if it is setted.
            if (holder.mItem.getDeadline() != null)
            holder.mDeadlineView.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(holder.mItem.getDeadline()));
            // If the task is pending buttons to complete or cancel it are visible.
            if (holder.mItem.getStatus() instanceof TaskList.CompletedTask | holder.mItem.getStatus() instanceof  TaskList.CanceledTask) {
                holder.mCompleteButton.setVisibility(View.INVISIBLE);
                holder.mCancelButton.setVisibility(View.INVISIBLE);
            } else {
                holder.mCompleteButton.setVisibility(View.VISIBLE);
                holder.mCancelButton.setVisibility(View.VISIBLE);
            }
// If the user presses the title of the task its details will be shown.
            holder.mNameView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    viewTask(holder.mItem, position, v);
                }
            });

            holder.mCompleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    completeTask(holder.mItem, position);
                }
            });
            holder.mCancelButton.setOnClickListener((new View.OnClickListener() {
                @Override
                public void onClick(View v) {
cancelTask(holder.mItem, position);
                }
            }));
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        /**
         * A holder for the recycler view.
         */
        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final TextView mNameView;
            public final TextView mDeadlineView;
            public final ImageButton mCancelButton;
            public final ImageButton mCompleteButton;
            public TaskList.Task mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;
                mNameView = (TextView) view.findViewById(R.id.name);
                mDeadlineView = (TextView) view.findViewById(R.id.deadline);
                mCancelButton = (ImageButton) view.findViewById(R.id.cancel_button);
                mCompleteButton = (ImageButton) view.findViewById(R.id.complete_button);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mNameView.getText() + "'";
            }
        }
    }

    /**
     * Marks  a task as completed.
     * @param task        The task to complete.
     * @param position the position on the list view.
     */
    protected void completeTask(TaskList.Task task, int position) {
        CompleteTaskDialog dialog = new CompleteTaskDialog(task, position, new OnListCompleteTaskListener());
        Bundle args = new Bundle();
        args.putParcelable(ARG_TASK, task);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "CompleteTask");
    }

    /**
     * Marks a task as cancelled.
     * @param task        The task to be cancelled.
     * @param position    te position on the list view.
     */
    protected void cancelTask (TaskList.Task task, int position) {
        CancelTaskDialog dialog = new CancelTaskDialog(task, position, new OnListCancelTaskListener());
        Bundle args = new Bundle();
        args.putParcelable(ARG_TASK, task);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "CancelDialog");
    }

    /**
     * Shows details of a task.
     * @param task the task to be shown.
     * @param position    The position of the task on the list view.
     * @param v           The view pressed.
     */
    protected void viewTask(TaskList.Task task, int position, View v) {
        if (mTwoPane) {
            TaskDetailFragment fragment = TaskDetailFragment.newInstance(new OnListTaskChangedListener(), new OnListEditButtonListener(), task, position);
            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(TaskDetailFragment.TAG)
                    .replace(R.id.task_detail_container, fragment)
                    .commit();
        } else {
            Context context = v.getContext();
            Intent intent = new Intent(context, TaskDetailActivity.class);
            intent.putExtra(ARG_TASK, task);
            intent.putExtra(TaskDetailFragment.ARG_ITEM_POS, position);

            startActivityForResult(intent, TaskDetailActivity.ACTIVITY_CODE);
        }
    }

    /**
     * Shows the new task form.
     * @param v    The view pressed.
     */
    protected void newTask(View v) {
        if (mTwoPane) {
            EditTask1Fragment fragment = EditTask1Fragment.newInstance(new OnNewTaskListener(), new TaskList.Task());
            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(EditTask1Fragment.TAG)
                    .replace(R.id.task_detail_container, fragment)
                    .commit();
        } else {
            Context context = v.getContext();
            Intent intent = new Intent(context, NewTaskActivity.class);
                    startActivityForResult(intent, NewTaskActivity.ACTIVITY_CODE);
        }
    }

    /**
     * Notifies to recycler view a task was changed.
     * @param position    The position on the list view.
     */
    protected void notifyTaskChanged(int position) {
        RecyclerView list = (RecyclerView) findViewById(R.id.task_list);
        list.getAdapter().notifyItemChanged(position);
    }

    /**
     * Notifies that the tassk list was changed.
     */
    protected void notifyTaskListChanged() {
        RecyclerView list = (RecyclerView) findViewById(R.id.task_list);
        list.getAdapter().notifyDataSetChanged();
    }

    /**
     * Notifies that a new item was inserted on the list.
     */
    protected void notifyItemInserted() {
        RecyclerView list = (RecyclerView) findViewById(R.id.task_list);
        list.getAdapter().notifyItemInserted(TaskList.getInstance().getTasks().size());
    }

    /**
     * Notifies that an item was removed from the list.
     * @param position    the position on the list view.
     */
    protected void notifyItemRemoved(int position) {
        RecyclerView list = (RecyclerView) findViewById(R.id.task_list);
        list.getAdapter().notifyItemRemoved(position);
    }

    /**
     * Do some operations after adding a task.
     */
    protected void addTask() {
        refreshTasks();
        Toast toast = Toast.makeText(getApplicationContext(), R.string.task_added, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * A listener for task cancellation.
     */
    public class  OnListCancelTaskListener implements CancelTaskDialog.CancelDialogListener {
        @Override
        public void onCancel(TaskList.Task task, int position) {
            TaskList.getInstance().setTask(task);
            /* if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingsActivity.GeneralPreferenceFragment.hide_canceled, false)) {
                notifyItemRemoved(position);
                filteredTasks.remove(filteredTasks.indexOf(task));
            } else notifyTaskChanged(position); */
            refreshTasks();
            Toast toast = Toast.makeText(getApplicationContext(), R.string.task_canceled, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * A listenerfor task completion.
     */
    public class OnListCompleteTaskListener implements CompleteTaskDialog.CompleteDialogListener {
        @Override
        public void onComplete(TaskList.Task task, int position) {
            TaskList.getInstance().setTask(task);
            /* if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(SettingsActivity.GeneralPreferenceFragment.HIDE_COMPLETED, false)) {
                filteredTasks.remove(filteredTasks.indexOf(task));
                notifyItemRemoved(position);
            } else notifyTaskChanged(position); */
            refreshTasks();
            Toast toast = Toast.makeText(getApplicationContext(), R.string.task_completed, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * A listener for task changes.
     */
    public class OnListTaskChangedListener implements  OnTaskChangedListener {
        @Override
        public void onTaskChanged(TaskList.Task task, int position) {
            updateTask(task, position);
        }
    }

    /**
     * Update when a task changes.
     * @param task
     * @param position
     */
    protected void updateTask(TaskList.Task task, int position) {
        TaskList.getInstance().setTask(task);
        refreshTasks();
        /* SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean hideCompleted = preferences.getBoolean(SettingsActivity.GeneralPreferenceFragment.HIDE_COMPLETED, false);
        boolean hideCancelled = preferences.getBoolean(SettingsActivity.GeneralPreferenceFragment.hide_canceled, false);
        if (hideCancelled & task.getStatus().getStatusDescription().equals(TaskList.CanceledTask.STATUS)) {
            filteredTasks.remove(filteredTasks.indexOf(task));
            notifyItemRemoved(position);
        } else if (hideCompleted & task.getStatus().getStatusDescription().equals(TaskList.CompletedTask.STATUS)) {
            filteredTasks.remove(filteredTasks.indexOf(task));
            notifyItemRemoved(position);
        } else {
            filteredTasks.set(filteredTasks.indexOf(task), task);
            notifyTaskChanged(position);
        }
        */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TaskDetailActivity.ACTIVITY_CODE:
                if (resultCode == TaskDetailActivity.CHANGED) {
                    TaskList.Task t = data.getParcelableExtra(ARG_TASK);
                    int position = data.getIntExtra(TaskDetailFragment.ARG_ITEM_POS, -1);
updateTask(t, position);
                }
                break;
            case NewTaskActivity.ACTIVITY_CODE:
                if (resultCode == EditTask1Fragment.TASK_CREATION_COMPLETED) {
                    TaskList.Task t = data.getParcelableExtra(TaskListActivity.ARG_TASK);
                    TaskList.getInstance().addTask(t);
                    addTask();
                }
                    break;
            case EditTaskActivity.ACTIVITY_CODE: if (requestCode == TaskDetailActivity.CHANGED) {
                TaskList.Task t = data.getParcelableExtra(ARG_TASK);
                TaskList.getInstance().setTask(t);
                filteredTasks.set(filteredTasks.indexOf(t), t);
                notifyTaskChanged(data.getIntExtra(TaskDetailFragment.ARG_ITEM_POS, -1));
            }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.settings_option: Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.help_option: Intent helpIntent = new Intent(this, HelpActivity.class);
                startActivity(helpIntent);
                return true;
            case R.id.contact_option: Intent contactIntent = new Intent(this, ContactActivity.class);
                startActivity(contactIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A listener for new task procedure.
     */
    protected  class  OnNewTaskListener implements OnEditTaskListener {

        @Override
        public void onNextStep(TaskList.Task task) {
Fragment fragment = EditTask2Fragment.newInstance(new OnNewTaskListener(), task);
            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(EditTask2Fragment.TAG)
                    .replace(R.id.task_detail_container, fragment)
                    .commit();
        }

        @Override
        public void onPreviousStep(TaskList.Task task) {
getSupportFragmentManager().popBackStack();
        }

        @Override
        public void onCancel(TaskList.Task task) {
            getSupportFragmentManager().popBackStack(EditTask1Fragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        @Override
        public void onFinish(TaskList.Task task) {
TaskList.getInstance().addTask(task);
            addTask();
            getSupportFragmentManager().popBackStack(EditTask1Fragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            viewTask(task, TaskList.getInstance().getTasks().size(), null);
        }

    }

    /**
     * A listener for task edition procedure.
     */
    protected class  OnUpdateTaskListener implements OnEditTaskListener {

        @Override
        public void onNextStep(TaskList.Task task) {
            Fragment fragment = EditTask2Fragment.newInstance(new OnUpdateTaskListener(), task);
            getSupportFragmentManager().beginTransaction()
                    .addToBackStack(EditTask2Fragment.TAG)
                    .replace(R.id.task_detail_container, fragment)
                    .commit();
        }

        @Override
        public void onPreviousStep(TaskList.Task task) {
            getSupportFragmentManager().popBackStack(EditTask2Fragment.TAG, 0);
        }

        @Override
        public void onCancel(TaskList.Task task) {
            getSupportFragmentManager().popBackStack(EditTask1Fragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        @Override
        public void onFinish(TaskList.Task task) {
            int position = filteredTasks.indexOf(task);
new OnListTaskChangedListener().onTaskChanged(task, position);
            getSupportFragmentManager().popBackStack(EditTask1Fragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getSupportFragmentManager().popBackStack();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.task_detail_container, TaskDetailFragment.newInstance(new OnListTaskChangedListener(), new OnListEditButtonListener(), task, position))
                    .addToBackStack(TaskDetailFragment.TAG)
                    .commit();
        }
    }

    /**
     * A listener for start edition event.
     */
    protected class OnListEditButtonListener implements  OnEditButtonListener {
    @Override
    public void init(TaskList.Task task) {
        EditTask1Fragment fragment = EditTask1Fragment.newInstance(new OnUpdateTaskListener(), task);
        getSupportFragmentManager().beginTransaction()
                .addToBackStack(EditTask1Fragment.TAG)
                .replace(R.id.task_detail_container, fragment)
.commit();
    }
}

    /**
     * A listener for changes of preferences.
     */
    protected class OnFilterChangedListener implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(SettingsActivity.GeneralPreferenceFragment.hide_canceled) | key.equals(SettingsActivity.GeneralPreferenceFragment.HIDE_COMPLETED))
refreshTasks();
    }
}
}

