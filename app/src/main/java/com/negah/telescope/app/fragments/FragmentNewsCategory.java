package com.negah.telescope.app.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.negah.telescope.app.Config;
import com.negah.telescope.app.R;
import com.negah.telescope.app.activities.DetailActivity;
import com.negah.telescope.app.adapters.AdapterFullBannerList;
import com.negah.telescope.app.adapters.BannerCategoriesAdapter;
import com.negah.telescope.app.adapters.CategoryAdapter;
import com.negah.telescope.app.adapters.CategoryCombinedAdapter;
import com.negah.telescope.app.json.JsonConfig;
import com.negah.telescope.app.json.JsonUtils;
import com.negah.telescope.app.models.Banner;
import com.negah.telescope.app.models.Category;
import com.negah.telescope.app.models.ItemCategory;
import com.negah.telescope.app.services.Network;
import com.negah.telescope.app.services.api.APIs;
import com.negah.telescope.app.services.lists.TelescopeBanner;
import com.negah.telescope.app.services.lists.TelescopeCategory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentNewsCategory extends Fragment {

    GridView gridview;
    List<ItemCategory> list;
    AdapterFullBannerList adapter;
    private ItemCategory object;
    ArrayList<String> array_cat_id, array_cat_name, array_cat_image;
    String[] str_cat_id, str_cat_name, str_cat_image;
    int textlength = 0;
    SwipeRefreshLayout swipeRefreshLayout = null;
    ProgressBar progressBar;
    RecyclerView bannerRecyclerView,categoriesRecyclerView;
    CategoryAdapter categoryAdapter;
    //private InterstitialAd interstitial;
    String TAG= FragmentNewsCategory.class.getSimpleName();
    boolean isCategoriesRequestFinished=false,isBannersRequestFinished=false;
    private List<Category> categories;
    private List<Banner> banners;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_news_category, container, false);
        setHasOptionsMenu(true);

        gridview = (GridView) v.findViewById(R.id.gridView1);
        progressBar = (ProgressBar) v.findViewById(R.id.progressBar);
        swipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.orange, R.color.green, R.color.blue, R.color.red);

        list = new ArrayList<ItemCategory>();

        array_cat_id = new ArrayList<String>();
        array_cat_name = new ArrayList<String>();
        array_cat_image = new ArrayList<String>();

        str_cat_id = new String[array_cat_id.size()];
        str_cat_name = new String[array_cat_image.size()];
        str_cat_image = new String[array_cat_name.size()];

        bannerRecyclerView= (RecyclerView) v.findViewById(R.id.categories_bannerList);
        StaggeredGridLayoutManager mStaggeredLayoutManager = new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL);
        categoriesRecyclerView= (RecyclerView) v.findViewById(R.id.categories_list);
        GridLayoutManager gridLayoutManager=new GridLayoutManager(getContext(),2);
        categoriesRecyclerView.setLayoutManager(gridLayoutManager);
        bannerRecyclerView.setLayoutManager(mStaggeredLayoutManager);
        APIs apIs=Network.getRetrofit().create(APIs.class);
        Call<TelescopeCategory> categoriesRequest =apIs.loadCategories();
        categoriesRequest.enqueue(new Callback<TelescopeCategory>() {
            @Override
            public void onResponse(Call<TelescopeCategory> call, Response<TelescopeCategory> response) {
                Log.d(TAG,"categories " + response.body().categories.size());
                /*categoryAdapter=new CategoryAdapter(getContext(),response.body().categories);
                categoryAdapter.setOnItemClickListener(new CategoryAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {

                    }
                });
                categoriesRecyclerView.setAdapter(categoryAdapter);*/
                if(response.isSuccessful()) {
                    isCategoriesRequestFinished = true;
                    categories = response.body().categories;
                    if (isBannersRequestFinished) {
                        CreateCategoryList(categories, banners);
                    }
                }
            }

            @Override
            public void onFailure(Call<TelescopeCategory> call, Throwable t) {
                Log.d(TAG,"categories " + t.getLocalizedMessage());
            }
        });

        if (JsonUtils.isNetworkAvailable(getActivity())) {
          //  new MyTask().execute(Config.SERVER_URL + "/api.php");
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.failed_connect_network), Toast.LENGTH_SHORT).show();
        }

        // Using to refresh webpage when user swipes the screen
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        adapter.clear();
                        new RefreshTask().execute(Config.SERVER_URL + "/api.php");


                    }
                }, 3000);
            }
        });

        gridview.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                boolean enable = false;
                if (gridview != null && gridview.getChildCount() > 0) {
                    boolean firstItemVisible = gridview.getFirstVisiblePosition() == 0;
                    boolean topOfFirstItemVisible = gridview.getChildAt(0).getTop() == 0;
                    enable = firstItemVisible && topOfFirstItemVisible;
                }
                swipeRefreshLayout.setEnabled(enable);
            }
        });

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                object = list.get(position);
                int Catid = object.getCategoryId();
                JsonConfig.CATEGORY_IDD = object.getCategoryId();
                Log.e("cat_id", "" + Catid);
                JsonConfig.CATEGORY_TITLE = object.getCategoryName();

                Intent intcat = new Intent(getActivity(), DetailActivity.class);
                startActivity(intcat);

            }
        });



        Call<TelescopeBanner> call =apIs.loadFullBanners();
        call.enqueue(new Callback<TelescopeBanner>() {
            @Override
            public void onResponse(Call<TelescopeBanner> call, Response<TelescopeBanner> response) {
                Log.d(TAG,"SIZE " + response.body().banners.size());
               /*BannerCategoriesAdapter bannersadapter= new BannerCategoriesAdapter(response.body().banners,getContext());
                bannerRecyclerView.setAdapter(bannersadapter);*/
                if(response.isSuccessful()){
                    isBannersRequestFinished=true;
                    banners=response.body().banners;
                    if(isCategoriesRequestFinished){
                        CreateCategoryList(categories,banners);
                    }
                }




            }

            @Override
            public void onFailure(Call<TelescopeBanner> call, Throwable t) {

            }
        });

        return v;
    }

    private void CreateCategoryList(List<Category> categories, List<Banner> banners){
        final List<Object> items=new ArrayList<>();
        items.addAll(categories);
        items.addAll(banners);
        int columns=getResources().getInteger(R.integer.categories_column_count);
        GridLayoutManager gridLayoutManager=new GridLayoutManager(getContext(),2);
        gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if(items.get(position) instanceof Category)
                    return 1;
                else if(items.get(position) instanceof Banner){
                    return 2;
                }
                return 1;
            }
        });
        categoriesRecyclerView.setLayoutManager(gridLayoutManager);
        CategoryCombinedAdapter adapter=new CategoryCombinedAdapter(getContext(),items);
        categoriesRecyclerView.setAdapter(adapter);
    }

    private class MyTask extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(getResources().getString(R.string.loading));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            return JsonUtils.getJSONString(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (null != progressDialog && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (null == result || result.length() == 0) {
                Toast.makeText(getActivity(), getResources().getString(R.string.failed_connect_network), Toast.LENGTH_SHORT).show();
            } else {

                try {
                    JSONObject mainJson = new JSONObject(result);
                    JSONArray jsonArray = mainJson.getJSONArray(JsonConfig.CATEGORY_ARRAY_NAME);
                    JSONObject objJson = null;
                    for (int i = 0; i < jsonArray.length(); i++) {

                        objJson = jsonArray.getJSONObject(i);
                        ItemCategory objItem = new ItemCategory();
                        objItem.setCategoryName(objJson.getString(JsonConfig.CATEGORY_NAME));
                        objItem.setCategoryId(objJson.getInt(JsonConfig.CATEGORY_CID));
                        objItem.setCategoryImageurl(objJson.getString(JsonConfig.CATEGORY_IMAGE));
                        list.add(objItem);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                for (int j = 0; j < list.size(); j++) {
                    object = list.get(j);

                    array_cat_id.add(String.valueOf(object.getCategoryId()));
                    str_cat_id = array_cat_id.toArray(str_cat_id);

                    array_cat_image.add(object.getCategoryName());
                    str_cat_name = array_cat_image.toArray(str_cat_name);

                    array_cat_name.add(object.getCategoryImageurl());
                    str_cat_image = array_cat_name.toArray(str_cat_image);
                }

                setAdapterToListView();
            }

        }
    }

    private class RefreshTask extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {
            return JsonUtils.getJSONString(params[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressBar.setVisibility(View.GONE);

            if (null == result || result.length() == 0) {
                Toast.makeText(getActivity(), getResources().getString(R.string.failed_connect_network), Toast.LENGTH_SHORT).show();
            } else {

                try {
                    JSONObject mainJson = new JSONObject(result);
                    JSONArray jsonArray = mainJson.getJSONArray(JsonConfig.CATEGORY_ARRAY_NAME);
                    JSONObject objJson = null;
                    for (int i = 0; i < jsonArray.length(); i++) {

                        objJson = jsonArray.getJSONObject(i);
                        ItemCategory objItem = new ItemCategory();
                        objItem.setCategoryName(objJson.getString(JsonConfig.CATEGORY_NAME));
                        objItem.setCategoryId(objJson.getInt(JsonConfig.CATEGORY_CID));
                        objItem.setCategoryImageurl(objJson.getString(JsonConfig.CATEGORY_IMAGE));
                        list.add(objItem);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }


                for (int j = 0; j < list.size(); j++) {
                    object = list.get(j);

                    array_cat_id.add(String.valueOf(object.getCategoryId()));
                    str_cat_id = array_cat_id.toArray(str_cat_id);

                    array_cat_image.add(object.getCategoryName());
                    str_cat_name = array_cat_image.toArray(str_cat_name);

                    array_cat_name.add(object.getCategoryImageurl());
                    str_cat_image = array_cat_name.toArray(str_cat_image);
                }

                setAdapterToListView();
            }

        }
    }


    public void setAdapterToListView() {
        adapter = new AdapterFullBannerList(getActivity(), R.layout.lsv_item_category, list);
        gridview.setAdapter(adapter);
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search, menu);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));

        final MenuItem searchMenuItem = menu.findItem(R.id.search);

        searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    searchMenuItem.collapseActionView();
                    searchView.setQuery("", false);
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String newText) {
                textlength = newText.length();
                list.clear();

                for (int i = 0; i < str_cat_name.length; i++) {
                    if (textlength <= str_cat_name[i].length()) {
                        if (str_cat_name[i].toLowerCase().contains(newText.toLowerCase())) {

                            ItemCategory objItem = new ItemCategory();
                            objItem.setCategoryId(Integer.parseInt(str_cat_id[i]));
                            objItem.setCategoryName(str_cat_name[i]);
                            objItem.setCategoryImageurl(str_cat_image[i]);

                            list.add(objItem);
                        }
                    }
                }

                setAdapterToListView();

                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {

                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:

                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }
}