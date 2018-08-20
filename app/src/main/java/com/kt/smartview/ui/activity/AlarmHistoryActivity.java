package com.kt.smartview.ui.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.kt.smartview.R;
import com.kt.smartview.common.Constants;
import com.kt.smartview.db.DomaDBHandler;
import com.kt.smartview.support.helper.MyActionModeHelper;
import com.kt.smartview.support.helper.RecyclerViewPositionHelper;
import com.kt.smartview.support.task.AsyncTaskResult;
import com.kt.smartview.support.task.TokAsyncTask;
import com.kt.smartview.ui.items.AlarmItem;
import com.kt.smartview.ui.items.ProgressItem;
import com.kt.smartview.utils.CommonUtil;
import com.kt.smartview.utils.SnackbarUtils;
import com.kt.smartview.utils.YWMTools;
import com.kt.smartview.vo.AlarmVo;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.common.DividerItemDecoration;
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class AlarmHistoryActivity extends BaseCompatActivity implements SwipeRefreshLayout.OnRefreshListener, FlexibleAdapter.EndlessScrollListener, ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener, FlexibleAdapter.OnUpdateListener {

    // UI
    private MyActionModeHelper mActionModeHelper;
    private FlexibleAdapter mAdapter;
    private TextView mEmptyView;
    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // DATA
    private DomaDBHandler domaDBHandler;
    private int pageNumber = 1, pageSize = 100;
    private TokAsyncTask dataLoader;

    private RecyclerViewPositionHelper mRecyclerViewHelper;
    private ArrayList<AlarmVo> readOkPushList = null;
    private TokAsyncTask alarmCheckTask;
    private NewAlarmReceiver newAlarmReceiver;
    private TokAsyncTask addDataLoader;
    private ProgressDialog progressDialog;
    /***********************************************************************************************
     * LIFE CYCLE
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null){
            readOkPushList = savedInstanceState.getParcelableArrayList(AlarmVo.EXT_ALARMVO);
        }else{
            readOkPushList = new ArrayList<AlarmVo>();
        }
        domaDBHandler = new DomaDBHandler(this);
        setContentView(R.layout.activity_alarm_history);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mEmptyView = (TextView)findViewById(R.id.empty);
        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initRecyclerView();
        initSwipeToRefresh();
        initActionModeHelper(SelectableAdapter.MODE_IDLE);
        onRefresh();
        regReceiver();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(AlarmVo.EXT_ALARMVO, readOkPushList);
        super.onSaveInstanceState(outState);
    }

    /***********************************************************************************************
     * OVERRIDE
     */

    @Override
    public void onRefresh() {
        pageNumber = 1;
        loadData(Constants.LIST_REFRESH);
        mActionModeHelper.destroyActionModeIfCan();
        mRefreshHandler.sendEmptyMessageDelayed(0, 1000L);
    }

    @Override
    public void onLoadMore() {
        loadData(Constants.LIST_LOADMORE);
    }


    @Override
    public void onUpdateEmptyView(int size) {
        if (size > 0) {
            mEmptyView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.action_delete:
                startActionMode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_alarm_history, menu);
        MenuItem action_delete = menu.findItem(R.id.action_delete);
        action_delete.setIcon(new IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_delete).sizeDp(24).colorRes(android.R.color.white));
        return true;
    }

    @Override
    public void onBackPressed() {
        if(mActionModeHelper != null && mActionModeHelper.destroyActionModeIfCan()){
            return;
        }
        super.onBackPressed();
    }

    /***********************************************************************************************
     * METHOD
     */

    private void completeRefresh() {
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.scrollToPosition(0);
            }
        });
        mSwipeRefreshLayout.setRefreshing(false);
        mSwipeRefreshLayout.setEnabled(true);
    }

    /**
     * initialize RecyclerView & Adapter
     */
    @SuppressWarnings({"ConstantConditions", "NullableProblems", "unchecked"})
    private void initRecyclerView() {
        mAdapter = new FlexibleAdapter<AbstractFlexibleItem>(new ArrayList<AbstractFlexibleItem>(), this);
        mAdapter.setAnimationOnScrolling(true);
        mAdapter.setAnimationOnReverseScrolling(true);
        mRecyclerView.setLayoutManager(new SmoothScrollLinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setHasFixedSize(true); //Size of RV will not change
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), R.drawable.bg_divider_gray));
        mRecyclerView.addOnScrollListener(scrollListener);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder) {
                //NOTE: This allows to receive Payload objects on notifyItemChanged called by the Adapter!!!
                return true;
            }
        });
        mAdapter.setDisplayHeadersAtStartUp(true);//Show Headers at startUp!
        mAdapter.setUnlinkAllItemsOnRemoveHeaders(true);
        mAdapter.enableStickyHeaders();
        mAdapter.setEndlessScrollListener(this, new ProgressItem());
        mAdapter.setEndlessScrollThreshold(3);//Default=1
        mRecyclerViewHelper = RecyclerViewPositionHelper.createHelper(mRecyclerView);
    }

    /**
     * initialize SwipeRefreshLayout
     */
    private void initSwipeToRefresh() {
        mSwipeRefreshLayout.setDistanceToTriggerSync(390);
        mSwipeRefreshLayout.setEnabled(true);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.color_refresh_1, R.color.color_refresh_2,
                R.color.color_refresh_3, R.color.color_refresh_4);
        mSwipeRefreshLayout.setOnRefreshListener(this);
    }

    private void cancelDataLoader(){
        if(dataLoader != null){
            dataLoader.stop();
        }
    }

    private void loadData(final int mode){
        cancelDataLoader();
        dataLoader = new TokAsyncTask(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected AsyncTaskResult<Object> doInBackground(Object... arg0) {
                List<AlarmVo> list = null;
                try{
                    int totalCount = domaDBHandler.getAlarmTotalCount();
                    if(totalCount > 0){
                        if(mode == Constants.LIST_LOADMORE){
                            if(mAdapter.getItemCount() < totalCount){
                                list = domaDBHandler.getAlarmHistoryListAll(pageNumber++, pageSize);
                            }
                        }else if(mode == Constants.LIST_REFRESH){
                            list = domaDBHandler.getAlarmHistoryListAll(pageNumber++, pageSize);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    return new AsyncTaskResult<Object>(e);
                }

                return new AsyncTaskResult<Object>(list);
            }

            @Override
            protected void onPostExecute(AsyncTaskResult<Object> result) {
                super.onPostExecute(result);
                List<AbstractFlexibleItem> items = null;
                if(result != null && result.existError() == false && result.getResult() != null){
                    List<AlarmVo> list = (List<AlarmVo>)result.getResult();
                    if(list != null && list.size() > 0){
                        items = new ArrayList<AbstractFlexibleItem>();
                        for(AlarmVo vo : list){
                            AlarmItem item = new AlarmItem(vo, mEmptyView);
                            items.add(item);
                        }
                    }
                }
                loadComplete(mode, items);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                loadComplete(mode, null);
            }
        };
        dataLoader.execute();
    }

    private void loadComplete(int mode, List<AbstractFlexibleItem> items){
        if(items == null){
            if(mode == Constants.LIST_REFRESH){
                mAdapter.updateDataSet(null);
            }else{
                mAdapter.onLoadMoreComplete(null);
            }
        }else{
            if(mode == Constants.LIST_REFRESH){
                mAdapter.updateDataSet(null);
                mAdapter.addItems(0, items);
            }else{
                mAdapter.onLoadMoreComplete(items);
            }
        }
        mSwipeRefreshLayout.setRefreshing(false);
        mEmptyView.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public void startActionMode(){
        ActionMode actionMode = mActionModeHelper.startActionMode(this);
        mAdapter.setMode(SelectableAdapter.MODE_MULTI);
        if(actionMode != null){
            actionMode.setTitle(getString(R.string.action_item_selected, String.valueOf(mAdapter.getSelectedItemCount())));
        }
    }

    public boolean stopActionMode(){
        if(mActionModeHelper == null) return false;
        return mActionModeHelper.destroyActionModeIfCan();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.colorAccentDark));
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_all:
                if(isSelectionAll()){
                    clearSelectionAll();
                }else{
                    mAdapter.selectAll();
                }
                mActionModeHelper.updateContextTitle(mAdapter.getSelectedItemCount());
                return true;

            case R.id.action_delete:
                selectedItemDelete();
                return true;
            default:
                return false;
        }
    }

    private boolean isSelectionAll(){
        if(mAdapter != null){
            for(int i = 0; i < mAdapter.getItemCount(); i++){
                if(mAdapter.isSelected(i) == false){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void clearSelectionAll(){
        if(mAdapter != null){
            for(int i = 0; i < mAdapter.getItemCount(); i++){
                if(mAdapter.isSelected(i)){
                    mAdapter.removeSelection(i);
                    mAdapter.notifyItemChanged(i);
                }
            }
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryDark));
        }
    }

    @Override
    public boolean onItemClick(int position) {
        if (mActionModeHelper != null && position != RecyclerView.NO_POSITION) {
            return mActionModeHelper.onClick(position);
        } else {
            return false;
        }
    }
    @Override
    public void onItemLongClick(int position) {
        /*if (mActionModeHelper != null && position != RecyclerView.NO_POSITION) {
            mActionModeHelper.onLongClick((AppCompatActivity) getActivity(), position);
        }*/
    }


    private void deleteItems(final List<String> deleteList){
        if(deleteList != null && deleteList.size() > 0){
            TokAsyncTask task = new TokAsyncTask(){
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    showProgressDialog();
                }

                @Override
                protected AsyncTaskResult<Object> doInBackground(Object... arg0) {
                    try{
                        List<String> deleteList = (List<String>)arg0[0];
                        String[] strArray = new String[deleteList.size()];
                        deleteList.toArray(strArray);
                        int deleteCount = 0, totalCount = 0;
                        deleteCount = domaDBHandler.deleteAlarmHistory(strArray);
                        if(deleteCount > 0){
                            totalCount = domaDBHandler.getUnReadAlarmCount();
                        }
                        totalCount = domaDBHandler.getUnReadAlarmCount();
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("deleteCount", deleteCount);
                        map.put("totalCount", totalCount);
                        return new AsyncTaskResult(map);
                    }catch (Exception e){
                        e.printStackTrace();
                        return new AsyncTaskResult(null);
                    }
                }

                @Override
                protected void onPostExecute(AsyncTaskResult<Object> result) {
                    super.onPostExecute(result);
                    dismissProgressDialog();
                    if(result != null && result.getResult() != null){
                        Map<String, Object> map = (Map<String, Object>)result.getResult();
                        int deleteCount = (int)map.get("deleteCount");
                        if(deleteCount > 0){
                            List<Integer> selectedItem = mAdapter.getSelectedPositions();
                            mAdapter.removeItems(selectedItem);
                            onRemoveItems();
                            SnackbarUtils.show(getActivity(), String.format(getString(R.string.msg_success_delete_ok), deleteCount));
                            int totalCount = (int)map.get("totalCount");
                            CommonUtil.applyUnReadAlarmCountToBadge(getContext(), totalCount);
                        }else{
                            SnackbarUtils.show(getActivity(), R.string.msg_fail_delete_alarm);
                        }
                        if(mAdapter.getItemCount() == 0){
                            onRefresh();
                        }
                    }else{
                        SnackbarUtils.show(getActivity(), R.string.msg_fail_delete_alarm);
                    }
                }

                @Override
                protected void onCancelled() {
                    super.onCancelled();
                    dismissProgressDialog();
                }
            };
            task.execute(deleteList);
        }
    }

    private void selectedItemDelete() {
        List<Integer> selectedItem = mAdapter.getSelectedPositions();
        List<String> deleteList = new ArrayList<>();
        for (int i = 0; i < selectedItem.size(); i++) {
            int position = selectedItem.get(i);
            IFlexible flexibleItem = mAdapter.getItem(position);
            AlarmItem alarmItem = (AlarmItem) flexibleItem;
            deleteList.add(String.valueOf(alarmItem.getAlarmVo().getIdx()));
        }
        if(deleteList != null && deleteList.size() > 0){
            deleteItems(deleteList);
        }else{
            SnackbarUtils.show(getActivity(), R.string.msg_no_selected_alert);
        }
    }

    private void initActionModeHelper(int mode) {
        mActionModeHelper = new MyActionModeHelper(mAdapter, R.menu.menu_action_mode_delete, this) {
            @Override
            public void updateContextTitle(int count) {
                if (mActionMode != null) {
                    mActionMode.setTitle(getString(R.string.action_item_selected, String.valueOf(count)));
                }
            }
        };
        mActionModeHelper.withDefaultMode(mode);
    }

    private final Handler mRefreshHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 0: //Stop
                    mSwipeRefreshLayout.setRefreshing(false);
                    mSwipeRefreshLayout.setEnabled(true);
                    return true;
                case 1: //1 Start
                    mSwipeRefreshLayout.setRefreshing(true);
                    mSwipeRefreshLayout.setEnabled(false);
                    return true;
                default:
                    return false;
            }
        }
    });

    private void onRemoveItems() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    public int getItemCount(){
        if(mAdapter != null){
            return mAdapter.getItemCount();
        }
        return 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(newAlarmReceiver);
        cancelAddDataLoader();
        cancelDataLoader();
        if(alarmCheckTask != null){
            alarmCheckTask.stop();
        }
    }

    RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if(newState == RecyclerView.SCROLL_STATE_IDLE || newState == RecyclerView.SCROLL_STATE_SETTLING){
                setReadOkForItem();
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            setReadOkForItem();
            List<AlarmVo> list = new ArrayList<AlarmVo>();
            for(AlarmVo vo : readOkPushList){
                list.add(vo);
            }
            setClickAlarmOk(list);
            readOkPushList = new ArrayList<AlarmVo>();
        }
    };

    private void setReadOkForItem(){
        if(mRecyclerView == null) return;
        int visibleItemCount = mRecyclerView.getChildCount();
        int totalItemCount = mRecyclerViewHelper.getItemCount();
        int firstVisibleItem = mRecyclerViewHelper.findFirstVisibleItemPosition();
        int lastVisibleItem = mRecyclerViewHelper.findLastVisibleItemPosition();
        for(int i = firstVisibleItem; i <= lastVisibleItem; i++){
            if(mAdapter.getItem(i) instanceof  AlarmItem){
                AlarmItem item = (AlarmItem)mAdapter.getItem(i);
                if(YWMTools.isYn2Bool(item.getAlarmVo().getIsReadOk()) == false){
                    item.getAlarmVo().setIsReadOk("Y");
                    if(readOkPushList == null){
                        readOkPushList = new ArrayList<AlarmVo>();
                    }
                    readOkPushList.add(item.getAlarmVo());
                }
            }
        }
    }

    private void setClickAlarmOk(List<AlarmVo> items){
        if(items == null || items.size() == 0){
            return;
        }
        List<String> codes = new ArrayList<String>();
        for(int i = 0; i < items.size(); i++){
            if(mAdapter.getItem(i) instanceof  AlarmItem){
                AlarmVo item = items.get(i);
                codes.add(String.valueOf(item.getIdx()));
            }
        }
        if(codes.size() > 0){
            String[] strArray = new String[codes.size()];
            final String[] strArray2 = codes.toArray(strArray);
            new TokAsyncTask(){
                @Override
                protected AsyncTaskResult<Object> doInBackground(Object... arg0) {
                    try{
                        String indexes = String.valueOf(arg0[0]);
                        int result = domaDBHandler.setReadAlarmOK(strArray2);
                        List<AlarmVo> list = domaDBHandler.getAlarmHistory(strArray2);
                        return new AsyncTaskResult(list);
                    }catch (Exception e){
                        return new AsyncTaskResult(null);
                    }
                }

                @Override
                protected void onPostExecute(AsyncTaskResult<Object> result) {
                    super.onPostExecute(result);
                    if(result != null && result.getResult() != null && result.getResult() instanceof List){
                        List<AlarmVo> list = (List<AlarmVo>)result.getResult();
                        for(int i = 0; i < list.size(); i++){
                            AlarmVo vo = list.get(i);
                            if(vo != null){
                                for(int k = 0; k < mAdapter.getItemCount(); k++){
                                    if(mAdapter.getItem(k) instanceof  AlarmItem){
                                        AlarmItem item = (AlarmItem)mAdapter.getItem(k);
                                        if(item.getAlarmVo().getIdx() == vo.getIdx()){
                                            item.setAlarmVo(vo);
                                            mAdapter.notifyItemChanged(k);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }.execute(strArray);
        }
    }

    private void dismissProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog(){
        progressDialog = ProgressDialog.show(getContext(), null, getString(R.string.delete_progress));
    }

    @Override
    public void startActivityAnimation() {
        overridePendingTransition(R.anim.start_enter, R.anim.start_exit);
    }

    @Override
    public void finishActivityAnimation() {
        overridePendingTransition(R.anim.end_enter, R.anim.end_exit);
    }

    public class NewAlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent != null && Constants.ACTION_NEW_ALARM_ARRIVED.equals(intent.getAction())){
                AlarmVo alarmVo = intent.getParcelableExtra(AlarmVo.EXT_ALARMVO);
                if(alarmVo != null){
                    Log.e("TEST", "New alarm message : " + alarmVo.getIdx());
                    onDataAdded(alarmVo);
                }
            }
        }
    }

    private void regReceiver(){
        if(newAlarmReceiver == null){
            newAlarmReceiver = new NewAlarmReceiver();
        }
        registerReceiver(newAlarmReceiver, new IntentFilter(Constants.ACTION_NEW_ALARM_ARRIVED));
    }

    private void cancelAddDataLoader(){
        if(addDataLoader != null){
            addDataLoader.stop();
        }
    }

    public void onDataAdded(AlarmVo vo){
        if(mRecyclerView == null || mAdapter == null){
            return;
        }
        cancelAddDataLoader();
        addDataLoader = new TokAsyncTask(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected AsyncTaskResult<Object> doInBackground(Object... arg0) {
                List<AlarmVo> list = null;
                try{
                    long lastIdxNo = 0L;
                    if(mAdapter.getItemCount() > 0){
                        AlarmItem item =  (AlarmItem)mAdapter.getItem(0);
                        lastIdxNo = item.getAlarmVo().getIdx();
                    }
                    Log.d("TEST", "lastIdxNo-->" + lastIdxNo);
                    list = domaDBHandler.getRecentAlarmHistoryList(lastIdxNo);
                    Log.d("TEST", "list-->" + list.size());
                }catch (Exception e){
                    e.printStackTrace();
                    return new AsyncTaskResult<Object>(e);
                }

                return new AsyncTaskResult<Object>(list);
            }

            @Override
            protected void onPostExecute(AsyncTaskResult<Object> result) {
                super.onPostExecute(result);

                if(result != null && result.existError() == false && result.getResult() != null){
                    final List<AlarmVo> list = (List<AlarmVo>)result.getResult();
                    if(list != null && list.size() > 0){
                        mRecyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                List<String> codes = null;
                                for(AlarmVo vo : list){
                                    int position = getNotiItemPosition(mAdapter, vo);
                                    if(position < 0){
                                        AlarmItem item = new AlarmItem(vo, mEmptyView);
                                        int newItemPosition = getItemPostionByCreateDate(vo);
                                        mAdapter.addItem(newItemPosition, item);
                                        mAdapter.notifyItemChanged(newItemPosition);
                                        if(codes == null){
                                            codes = new ArrayList<String>();
                                        }
                                        codes.add(String.valueOf(vo.getIdx()));
                                    }
                                }
                                if(codes != null){
//                                    mRecyclerView.scrollToPosition(0);
                                    if(codes!= null && codes.size() > 0) {
                                        String[] strArray = new String[codes.size()];
                                        final String[] strArray2 = codes.toArray(strArray);
                                        domaDBHandler.setReadAlarmOK(strArray2);
                                    }
                                }
                            }
                        });
                    }
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
            }
        };
        addDataLoader.execute();
    }
    private int getNotiItemPosition(FlexibleAdapter adapter, AlarmVo vo){
        if(adapter == null || vo == null){
            return -1;
        }
        for(int i = 0; i < adapter.getItemCount(); i++){
            if(adapter.getItem(i) instanceof AlarmVo){
                AlarmItem item = (AlarmItem)adapter.getItem(i);
                if(item != null && vo.getPushKey().equals(item.getAlarmVo().getPushKey())){
                    return i;
                }
            }
        }
        return -1;
    }

    private int getItemPostionByCreateDate(AlarmVo vo){
        if(mAdapter != null && mAdapter.getItemCount() > 0){
            long createDate = strDate2TimeMills(vo.getOccurTime());
            for(int i = 0; i< mAdapter.getItemCount(); i++){
                AlarmItem item = (AlarmItem)mAdapter.getItem(i);
                if(item.getAlarmVo() != null){
                    long date = strDate2TimeMills(item.getAlarmVo().getOccurTime());
                    if(createDate > date){
                        return i;
                    }
                }
            }
        }
        return 0;
    }

    public static long strDate2TimeMills(String strDate){
        Date date = null;
        String dateFormat = "yyyy-MM-dd HH:mm:ss";
        try {
            SimpleDateFormat converter = new SimpleDateFormat(dateFormat);
            date = converter.parse(strDate);
            return date.getTime();
        } catch (java.text.ParseException pe) {
            pe.printStackTrace();
            return 0L;
        }
    }

}
