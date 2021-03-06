package de.geek_hub.freezermanager;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SortDialogFragment.SortDialogListener {
    private ItemList frozenItems;
    private RecyclerView itemList;
    private RecyclerView.Adapter itemListAdapter;

    private static final int ITEM_CREATE_REQUEST = 10;
    private static final int ITEM_EDIT_REQUEST = 11;
    private static final int ITEM_DETAIL_REQUEST = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        this.frozenItems = new ItemList(this);
        this.itemList = findViewById(R.id.item_list);

        SharedPreferences prefs = getSharedPreferences("de.geek-hub.freezermanager.data", Context.MODE_PRIVATE);
        this.frozenItems.sortList(prefs.getString("sort", "name"));

        showItems();

        ItemClickSupport.addTo(itemList).setOnItemClickListener((recyclerView, position, v) -> {
            Intent itemDetail = new Intent(getApplicationContext(), ItemDetailActivity.class);
            itemDetail.putExtra("item", frozenItems.getItem(position));
            itemDetail.putExtra("id", position);
            startActivityForResult(itemDetail, ITEM_DETAIL_REQUEST);
        });

        // set the task description a bit darker, so the title font in the recents menu changes to white
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription =
                    new ActivityManager.TaskDescription(this.getResources().getString(R.string.app_name),
                        BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher),
                        ContextCompat.getColor(this, R.color.colorPrimary600));
            this.setTaskDescription(taskDescription);
        }

        String action = getIntent().getStringExtra("action");
        if (action != null && action.equals("itemDetail")) {
            int id = getIntent().getIntExtra("id", -1);
            Intent itemDetail = new Intent(this, ItemDetailActivity.class);
            itemDetail.putExtra("item", frozenItems.getItem(id));
            itemDetail.putExtra("id", id);
            startActivityForResult(itemDetail, ITEM_DETAIL_REQUEST);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        paintIconWhite(menu.findItem(R.id.action_sort));
        //paintIconWhite(menu.findItem(R.id.action_filter));

        return true;
    }

    private void paintIconWhite(MenuItem menuItem) {
        if (menuItem != null) {
            Drawable normalDrawable = menuItem.getIcon();
            Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(this, android.R.color.white));

            menuItem.setIcon(wrapDrawable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Intent settings = new Intent(this, SettingsActivity.class);
                settings.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT,
                        SettingsActivity.NotificationPreferenceFragment.class.getName() );
                settings.putExtra(SettingsActivity.EXTRA_NO_HEADERS, true );
                startActivity(settings);
                return true;
            case R.id.action_sort:
                SortDialogFragment sortDialog = new SortDialogFragment();
                sortDialog.show(getSupportFragmentManager(), "sort");
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        SharedPreferences prefs = getSharedPreferences("de.geek-hub.freezermanager.data", Context.MODE_PRIVATE);
        switch (requestCode) {
            case ITEM_CREATE_REQUEST:
                Item newItem = data.getParcelableExtra("newItem");

                this.frozenItems.addItem(newItem);

                this.frozenItems.sortList(prefs.getString("sort", "name"));
                this.notifyItemListChanged();
                break;
            case ITEM_EDIT_REQUEST:
                int id = data.getIntExtra("id", -1);

                if (data.getStringExtra("action").equals("edit")) {
                    Item editedItem = data.getParcelableExtra("item");

                    this.frozenItems.deleteItem(id);
                    id = this.frozenItems.addItem(editedItem);

                    this.frozenItems.sortList(prefs.getString("sort", "name"));
                    this.notifyItemListChanged();
                }

                // TODO: fix display of wrong item, if list is sorted differently
                Intent itemDetail = new Intent(getApplicationContext(), ItemDetailActivity.class);
                itemDetail.putExtra("item", frozenItems.getItem(id));
                itemDetail.putExtra("id", id);
                itemDetail.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(itemDetail, ITEM_DETAIL_REQUEST);
                break;
            case ITEM_DETAIL_REQUEST:
                switch (data.getStringExtra("action")) {
                    case "defrost":
                        final Item deletedItem = this.frozenItems.deleteItem(data.getIntExtra("id", -1));

                        this.frozenItems.sortList(prefs.getString("sort", "name"));
                        this.notifyItemListChanged();

                        Snackbar.make(findViewById(R.id.main_activity_inner_coordinator_layout),
                                    deletedItem.getName() + getResources().getString(R.string.main_snackbar_defrost),
                                    Snackbar.LENGTH_LONG)
                                .setAction(R.string.main_snackbar_defrost_undo, view -> {
                                    frozenItems.addItem(deletedItem);
                                    this.notifyItemListChanged();
                                }).setActionTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
                                .show();
                        break;
                    case "edit":
                        Intent itemEdit = new Intent(getApplicationContext(), ItemEditActivity.class);
                        itemEdit.putExtra("action", "edit");
                        itemEdit.putExtra("item", frozenItems.getItem(data.getIntExtra("id", -1)));
                        itemEdit.putExtra("id", data.getIntExtra("id", -1));
                        startActivityForResult(itemEdit, ITEM_EDIT_REQUEST);
                        break;
                }
                break;
        }
    }

    private void showItems() {
        this.itemList.setHasFixedSize(true);

        LinearLayoutManager itemListLayoutManager = new LinearLayoutManager(this);
        this.itemList.setLayoutManager(itemListLayoutManager);

        this.itemListAdapter = new ItemListAdapter(this.frozenItems, this);

        /* //This is the code to provide a sectioned list
        List<SimpleSectionedRecyclerViewAdapter.Section> sections = new ArrayList<SimpleSectionedRecyclerViewAdapter.Section>();

        //Sections
        sections.add(new SimpleSectionedRecyclerViewAdapter.Section(0,"Section 1"));
        sections.add(new SimpleSectionedRecyclerViewAdapter.Section(5,"Section 2"));

        //Add your adapter to the sectionAdapter
        SimpleSectionedRecyclerViewAdapter.Section[] dummy = new SimpleSectionedRecyclerViewAdapter.Section[sections.size()];
        SimpleSectionedRecyclerViewAdapter mSectionedAdapter = new SimpleSectionedRecyclerViewAdapter(this,R.layout.section,R.id.section_text, this.itemListAdapter);
        mSectionedAdapter.setSections(sections.toArray(dummy));

        itemList.setAdapter(mSectionedAdapter);*/
        this.itemList.setAdapter(this.itemListAdapter);

        DividerItemDecoration itemDecoration = new DividerItemDecoration(itemList.getContext(), itemListLayoutManager.getOrientation());
        this.itemList.addItemDecoration(itemDecoration);

        this.notifyItemListChanged();
    }

    private void notifyItemListChanged() {
        TextView noItems = findViewById(R.id.main_no_items);
        if (this.frozenItems.length() == 0) {
            this.itemList.setVisibility(View.GONE);
            noItems.setVisibility(View.VISIBLE);
        } else {
            this.itemList.setVisibility(View.VISIBLE);
            noItems.setVisibility(View.GONE);
        }

        this.itemListAdapter.notifyDataSetChanged();
    }

    public void createItem(View view) {
        Intent intent = new Intent(this, ItemEditActivity.class);
        intent.putExtra("action", "create");
        startActivityForResult(intent, ITEM_CREATE_REQUEST);
    }

    @Override
    public void onSortSelect(int position) {
        String sort = "name";
        switch (position) {
            case 0:
                sort = "name";
                break;
            case 1:
                sort = "size";
                break;
            case 2:
                sort = "freezeDate";
                break;
            case 3:
                sort = "expDate";
                break;
        }
        this.frozenItems.sortList(sort);

        SharedPreferences prefs = getSharedPreferences("de.geek-hub.freezermanager.data", Context.MODE_PRIVATE);
        prefs.edit().putString("sort", sort).apply();

        this.notifyItemListChanged();
    }
}
