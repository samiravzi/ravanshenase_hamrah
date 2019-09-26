package com.hope.commonsense.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hope.commonsense.Adapters.CategoryAdapter;
import com.hope.commonsense.DataStore;
import com.hope.commonsense.Entities.Category;
import com.hope.commonsense.JsonParser;
import com.hope.commonsense.R;
import com.hope.commonsense.Utils;
import com.hope.commonsense.Web.WebStatistics;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class CategoryActivity extends MyActivity implements NavigationView.OnNavigationItemSelectedListener {

    MyActivity activity;
    ArrayList<Category> categories;
    CategoryAdapter adapter;

    RecyclerView mRecyclerView;
    DrawerLayout mDrawer;
    NavigationView mNavigation;
    Button mNavigationBtn;
    LinearLayout mLogout;
    View mLoading;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        activity = this;
        categories = new ArrayList<>();

        onCreateView();
        getCategories();
    }


    private void onCreateView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavigation = (NavigationView) findViewById(R.id.nav_view);
        mNavigationBtn = (Button) findViewById(R.id.btn_navigation);
        mLogout = (LinearLayout) findViewById(R.id.logout);
        Button searchBtn = (Button) findViewById(R.id.btn_search);
        mLoading = findViewById(R.id.loading);

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(activity, SearchActivity.class));
            }
        });

        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doLogout();
            }
        });

        setupRecycler();
        setupNavigationDrawer();
    }

    public void refreshList() {
        adapter.notifyDataSetChanged();
    }

    private void setupRecycler() {
        LinearLayoutManager myLayoutManager = new LinearLayoutManager(activity);
        mRecyclerView.setLayoutManager(myLayoutManager);
        adapter = new CategoryAdapter(activity, categories);
        mRecyclerView.setAdapter(adapter);
    }

    private void setupNavigationDrawer() {
        Utils.applyFont(activity, mNavigation);
        mNavigation.setNavigationItemSelectedListener(this);
        mNavigationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDrawer.isDrawerOpen(mNavigation)) {
                    mDrawer.closeDrawer(mNavigation);
                } else
                    mDrawer.openDrawer(mNavigation);
            }
        });

        Menu nav_Menu = mNavigation.getMenu();
        nav_Menu.findItem(R.id.videos).setVisible(false);
        if (getString(R.string.showvideolist).equals("0")) {
            nav_Menu.findItem(R.id.videolist).setVisible(false);
            nav_Menu.findItem(R.id.applist).setVisible(false);
        }
        setupNavigationHeader();
    }

    private void setupNavigationHeader() {
        final View header = mNavigation.getHeaderView(0);
        ImageView profileImage = (ImageView) header.findViewById(R.id.profile_image);
        TextView nicknameTxv = (TextView) header.findViewById(R.id.nav_nickname);
        TextView statusTxv = (TextView) header.findViewById(R.id.nav_status);

        if (DataStore.isUserRegistered) {
            String img = DataStore.currentUser.getImage();
            if (img.equals("")) {
                Picasso.with(activity).load(R.drawable.noprofile).into(profileImage);
            } else {
                Picasso.with(activity).load(new File(img)).into(profileImage);
            }

            nicknameTxv.setText(DataStore.currentUser.getNickname());
            statusTxv.setText(DataStore.currentUser.getStatus());
            mLogout.setVisibility(View.VISIBLE);
        } else {
            Picasso.with(activity).load(R.drawable.noprofile).into(profileImage);
            int color = getResources().getColor(R.color.text_layout_description);
            String string = getString(R.string.login_txt, color);
            statusTxv.setText(Html.fromHtml(string));
            nicknameTxv.setText("");
            mLogout.setVisibility(View.GONE);
        }

        statusTxv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!DataStore.isUserRegistered) {
                    startActivity(new Intent(activity, LoginActivity.class));
                }
            }
        });
    }

// -------------------------------------------------------------------------------------------------

    private void getCategories() {
        mRecyclerView.setVisibility(View.GONE);
        mLoading.setVisibility(View.VISIBLE);
        categories.clear();
        requestCode = 1;
        parameters.put(WebStatistics.KEY_URL, WebStatistics.URL_GET_CATEGORIES_WITH_TAGS);
        super.sendRequest();
    }

    @Override
    public void onActivityRequestResult(boolean result, int requestCode, String data) {
        super.onActivityRequestResult(result, requestCode, data);
        mRecyclerView.setVisibility(View.VISIBLE);
        mLoading.setVisibility(View.GONE);
        if (result) {
            try {
                JSONArray body = new JSONObject(data).getJSONArray("Body");
                categories.addAll(JsonParser.jsonToCategoryTags(body));
                refreshList();
            } catch (JSONException e) {
                Log.e("sami", e.getMessage() + "");
                e.printStackTrace();
            }
        }
    }


    // -------------------------------------------------------------------------------------------------
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.articles:
                startActivity(new Intent(activity, ArticleListActivity.class));
                break;
            case R.id.questions:
                startActivity(new Intent(activity, QuestionListActivity.class));
                break;
            case R.id.videos:
                startActivity(new Intent(activity, VideoListActivity.class));
                break;
            case R.id.categories:
                startActivity(new Intent(activity, CategoryActivity.class));
                break;
            case R.id.applist:
                break;
            case R.id.videolist:
                break;
            case R.id.profile: {
                if (DataStore.isUserRegistered) {
                    startActivity(new Intent(activity, ProfileActivity.class).putExtra("user", DataStore.currentUser));
                } else {
                    startActivity(new Intent(activity, LoginActivity.class));
                }
            }
            break;
        }
        mDrawer.closeDrawer(GravityCompat.END);
        return true;
    }

// -------------------------------------------------------------------------------------------------

    @Override
    protected void onResume() {
        super.onResume();
        setupNavigationHeader();
    }

    private void doLogout() {
        DataStore.isUserRegistered = false;
        SharedPreferences preferences = getSharedPreferences(DataStore.PREFERENCES_NAME, MODE_PRIVATE);
        preferences.edit().putBoolean(DataStore.PREFERENCES_IS_USER_LOGGED_IN, false).commit();
        setupNavigationHeader();
    }

}