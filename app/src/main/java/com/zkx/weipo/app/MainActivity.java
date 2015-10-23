package com.zkx.weipo.app;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.afollestad.materialdialogs.MaterialDialog;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.zkx.weipo.app.Adapter.MyRecyclerViewAdapter;
import com.zkx.weipo.app.Util.AccessTokenKeeper;
import com.zkx.weipo.app.api.Constants;
import com.zkx.weipo.app.app.WeiboApplication;
import com.zkx.weipo.app.openapi.OnRcvScrollListener;
import com.zkx.weipo.app.openapi.StatusesAPI;
import com.zkx.weipo.app.openapi.UsersAPI;
import com.zkx.weipo.app.openapi.models.ErrorInfo;
import com.zkx.weipo.app.openapi.models.StatusList;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static Boolean isRefreshing=false;
    private LinearLayoutManager mLayoutManage;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;
    private NavigationView mNavigationView;
    private RecyclerView mRecyclerView;
    private StatusList testDatas;
    private StatusList mStatusLists;
    private SwipeRefreshLayout mRefreshLayout;
    /** 当前 Token 信息 */
    private Oauth2AccessToken mAccessToken;
    /** 用户信息接口 */
    private UsersAPI mUsersAPI;
    /** 用于获取微博信息流等操作的API */
    private StatusesAPI mStatusesAPI;
    private MyRecyclerViewAdapter mAdapter;
    private long maxId=0;
    private long sinceId=0;
    private void initData(){
        testDatas=new StatusList();
    }

    private void initViews(){

        mRefreshLayout =(SwipeRefreshLayout)findViewById(R.id.swipeRefreshLayout);
        mRefreshLayout.setOnRefreshListener(this);
        mRefreshLayout.setColorSchemeResources(R.color.red, R.color.orange, R.color.yellow, R.color.green);

        mToolbar=(Toolbar)findViewById(R.id.id_Toolbar);
        setSupportActionBar(mToolbar);
        mDrawerLayout=(DrawerLayout)findViewById(R.id.id_DrawerLayout);
        ActionBarDrawerToggle mActionBarDrawerToggle=new ActionBarDrawerToggle(this,mDrawerLayout,mToolbar,R.string.open,R.string.close);
        mActionBarDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mActionBarDrawerToggle);

        mNavigationView=(NavigationView)findViewById(R.id.id_NavigationView);
        mNavigationView.inflateHeaderView(R.layout.navi_header);
        mNavigationView.inflateMenu(R.menu.menu_nav);
        onNavigationViewMenuItemSelected(mNavigationView);

        mRecyclerView=(RecyclerView)findViewById(R.id.id_RecyclerView);
        mLayoutManage =new LinearLayoutManager(MainActivity.this);
        mRecyclerView.setLayoutManager(mLayoutManage);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        // 获取当前已保存过的 Token
        mAccessToken = AccessTokenKeeper.readAccessToken(this);
        // 获取用户信息接口
        mUsersAPI = new UsersAPI(this, Constants.APP_KEY, mAccessToken);
        mStatusesAPI = new StatusesAPI(this, Constants.APP_KEY, mAccessToken);
        //getUserInfo();
        getStatus();
        initData();

        WeiboApplication.getInstance();
        WeiboApplication.addActivity(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit) {

            WeiboApplication.getInstance();
            WeiboApplication.appExit(this);
        }
        return super.onOptionsItemSelected(item);
    }

    private void onNavigationViewMenuItemSelected(NavigationView navigationView){
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                switch (menuItem.getItemId()){
                    default:
                        mDrawerLayout.closeDrawers();
                        break;
                }
                return true;
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK){
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode,event);
    }

   /* private void getUserInfo(){
        if (mAccessToken != null && mAccessToken.isSessionValid()) {
            long uid = Long.parseLong(mAccessToken.getUid());
            mUsersAPI.show(uid, mListener);
        }
    }*/

    private void getStatus(){
        mStatusesAPI.friendsTimeline(0L, maxId, 20, 1, false, 0, false, mListener);
    }

    private void initAdapter(){

        mAdapter=new MyRecyclerViewAdapter(testDatas);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.setOnScrollListener(new OnRcvScrollListener(){
            @Override
            public void onBottom() {
                super.onBottom();
                if (!isRefreshing){
                    loadMore();
                    isRefreshing=true;
                }else {
                    isRefreshing=false;
                }
            }
        });

        mAdapter.setOnItemClickLitener(new MyRecyclerViewAdapter.OnItemClickLitener() {
            @Override
            public void onItemClick(View view, final int position) {
                switch (view.getId()) {

                    case R.id.id_CardView:
                        Toast.makeText(MainActivity.this, position + " 卡片被点击",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.content_img:
                        Toast.makeText(MainActivity.this, position + " 图片被点击",
                                Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                switch (view.getId()) {
                    case R.id.id_CardView:
                        Toast.makeText(MainActivity.this, position + " 卡片被长点击",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.btn_repeat:
                        new MaterialDialog.Builder(MainActivity.this)
                                .title(R.string.repeat_title)
                                .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                                        Toast.makeText(MainActivity.this, "转发",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .show();
                        break;
                    case R.id.btn_comment:
                        new MaterialDialog.Builder(MainActivity.this)
                                .title(R.string.com_title)
                                .input(R.string.com_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                    @Override
                                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                                        Toast.makeText(MainActivity.this, "评论",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .show();
                        break;
                    case R.id.user_headimg:
                        Toast.makeText(MainActivity.this, position + " 用户头像被点击",
                                Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    private RequestListener mListener=new RequestListener(){
        @Override
        public void onComplete(String s) {
            if (!TextUtils.isEmpty(s)){
                if (s.startsWith("{\"statuses\"")){
                    mStatusLists=StatusList.parse(s);
                    if (mStatusLists != null && mStatusLists.total_number > 0) {
                        testDatas=mStatusLists;
                        //Long.parseLong(status.get(status.size() -1).getMid())-1;
                        maxId=Long.parseLong(mStatusLists.statusList.get(mStatusLists.statusList.size() - 1).mid)-1;
                        if (mAdapter==null){
                            initAdapter();
                        }else {
                            mAdapter.refresh(testDatas.statusList);
                        }
                    }
                }
            }
        }
        @Override
        public void onWeiboException(WeiboException e) {
            ErrorInfo info = ErrorInfo.parse(e.getMessage());
            Toast.makeText(MainActivity.this, info.toString(), Toast.LENGTH_LONG).show();
        }
    };
    /**
     * 加载更多微博
     */
    public void loadMore()
    {
        getStatus();
    }
    /**
     * 下拉刷新
     */
    @Override
    public void onRefresh() {
        mStatusesAPI.friendsTimeline(0L, 0L, 20, 1, false, 0, false, new RequestListener() {
            @Override
            public void onComplete(String s) {
                if (!TextUtils.isEmpty(s)){
                    if (s.startsWith("{\"statuses\"")){
                        mStatusLists=StatusList.parse(s);
                        if (mStatusLists != null && mStatusLists.total_number > 0) {
                            testDatas=mStatusLists;
                            initAdapter();
                            mRefreshLayout.setRefreshing(false);
                        }
                    }
                }
            }

            @Override
            public void onWeiboException(WeiboException e) {
                ErrorInfo info = ErrorInfo.parse(e.getMessage());
                Toast.makeText(MainActivity.this, info.toString(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
