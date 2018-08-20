package com.kt.smartview.ui.items;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kt.smartview.R;
import com.kt.smartview.db.DomaDBHandler;
import com.kt.smartview.support.task.AsyncTaskResult;
import com.kt.smartview.support.task.TokAsyncTask;
import com.kt.smartview.utils.CommonUtil;
import com.kt.smartview.utils.SnackbarUtils;
import com.kt.smartview.utils.YWMTools;
import com.kt.smartview.vo.AlarmVo;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.SelectableAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * @author jkjeon.
 * @project SmartView-Android.
 * @date 2016-12-22.
 */
public class AlarmItem extends AbstractFlexibleItem<AlarmItem.ViewHolder> {

    private AlarmVo mAlarmVo;
    private View emptyView;
    public AlarmItem(AlarmVo alarmVo, View emptyView) {
        this.mAlarmVo = alarmVo;
        this.emptyView = emptyView;
    }

    public AlarmVo getAlarmVo() {
        return mAlarmVo;
    }

    public void setAlarmVo(AlarmVo mAlarmVo) {
        this.mAlarmVo = mAlarmVo;
    }

    /***********************************************************************************************
     * OVERRIDE
     */
    @Override
    public boolean equals(Object inObject) {
        if (inObject instanceof AlarmItem) {
            AlarmItem inItem = (AlarmItem) inObject;
            return this.getAlarmVo().getPushKey().equals(inItem.getAlarmVo().getPushKey());
        }
        return false;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_alarm_history;
    }

    @Override
    public ViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter);
    }

    @Override
    public void bindViewHolder(final FlexibleAdapter adapter, final ViewHolder holder, int position, List payloads) {
        final Context context = holder.itemView.getContext();
        final IFlexible flexibleItem = adapter.getItem(position);
        AlarmItem item = (AlarmItem) flexibleItem;
        final AlarmVo vo = item.getAlarmVo();

//        holder.itemView.setBackgroundResource(YWMTools.isYn2Bool(vo.getIs_click_ok()) ? R.drawable.selector_btn_default : R.drawable.selector_bg_notifications);

        holder.text_date.setText(vo.getOccurTime());
        holder.text_title.setText(vo.getTitle());
        holder.text_message.setText(vo.getAlarmMessage());
        if(adapter.isSelected(position)){
            Log.e("TEST", "selected-->" + position);
        }

    }

    /***********************************************************************************************
     * ViewHolder
     */
    final class ViewHolder extends FlexibleViewHolder {
        TextView text_title;
        TextView text_date;
        TextView text_message;
        private DomaDBHandler dbHandler;
        public ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);
            dbHandler = new DomaDBHandler(view.getContext());
            text_title = (TextView)view.findViewById(R.id.text_title);
            text_date = (TextView)view.findViewById(R.id.text_date);
            text_message = (TextView)view.findViewById(R.id.text_message);
        }

        /*@Override
        public void onClick(View view) {
        *//*    clickItem(view.getContext());*//*
            super.onClick(view);
        }*/

        @Override
        public boolean onLongClick(View view) {
            Log.d("TEST","onLongClick");
            if(mAdapter.getMode() != FlexibleAdapter.MODE_IDLE){
                return super.onLongClick(view);
            }else{
                openPopupMenu(view);
                return true;
            }
        }

        public void openPopupMenu(View view) {
            AlarmItem notiItem = (AlarmItem) mAdapter.getItem(getAdapterPosition());
            if (notiItem != null) {
                final Context context = itemView.getContext();
                final PopupMenu popup = new PopupMenu(context, view, Gravity.CENTER);
                popup.getMenuInflater().inflate(R.menu.menu_alarm_item, popup.getMenu());
                popup.show();
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_share:
                                shareAlarm(context, getAdapterPosition());
                                break;
                            case R.id.action_delete:
                                deleteAlarm(context, getAdapterPosition());
                                break;
                        }
                        return false;
                    }
                });
            }

        }

        private void shareAlarm(Context context, int position){
            AlarmItem item = (AlarmItem)mAdapter.getItem(position);
            if(item != null){
                String text = String.format("%s [%s]\n%s", item.getAlarmVo().getTitle(), item.getAlarmVo().getOccurTime(), item.getAlarmVo().getAlarmMessage());
                CommonUtil.share(context, text);
            }
        }

        private void deleteAlarm(final Context context, final int position) {
            if(mAdapter == null){
                return;
            }
            new TokAsyncTask(){
                @Override
                protected AsyncTaskResult<Object> doInBackground(Object... arg0) {
                    int position = (int)arg0[0];
                    AlarmItem item = (AlarmItem)mAdapter.getItem(position);
                    if(item == null){
                        return new AsyncTaskResult(-1);
                    }
                    AlarmVo vo = item.getAlarmVo();
                    int result = dbHandler.deleteAlarmHistory(vo.getIdx());
                    if(result > 0){
                        return new AsyncTaskResult(position);
                    }else{
                        return new AsyncTaskResult(-1);
                    }
                }

                @Override
                protected void onPostExecute(AsyncTaskResult<Object> result) {
                    super.onPostExecute(result);
                    if(result != null && result.getResult() != null){
                        int position = (int)result.getResult();
                        if(position > -1){
                            mAdapter.removeItem(position);
                            onRemoveItems();
                            SnackbarUtils.show(context, String.format(context.getString(R.string.msg_success_delete_ok), 1));
                        }
                    }
                }
            }.execute(position);
        }

        private void onRemoveItems() {
            if (emptyView != null) {
                emptyView.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        }

        private void clickItem(Context context) {
            if (mAdapter.getMode() == SelectableAdapter.MODE_IDLE) {
                IFlexible flexibleItem = mAdapter.getItem(getAdapterPosition());
                if (flexibleItem instanceof AlarmItem) {
                    AlarmItem item = (AlarmItem) flexibleItem;
                    setClickAlarmOk(context, item.getAlarmVo());
                }
            } else {
                mAdapter.toggleSelection(getAdapterPosition());
                mAdapter.notifyItemChanged(getAdapterPosition());
            }
        }

        private void setClickAlarmOk(final Context context, AlarmVo vo) {
            if (YWMTools.isYn2Bool(vo.getIsClickOk())) {
                return;
            }
            new TokAsyncTask() {
                @Override
                protected AsyncTaskResult<Object> doInBackground(Object... arg0) {
                    AlarmVo vo = (AlarmVo) arg0[0];
                    int result = dbHandler.setUnClickAlarm(vo.getIdx());
                    if (result > 0) {
                        vo.setIsClickOk("Y");
                        vo.setIsReadOk("Y");
                    }
                    return new AsyncTaskResult(vo);
                }

                @Override
                protected void onPostExecute(AsyncTaskResult<Object> result) {
                    super.onPostExecute(result);
                    if (result != null && result.getResult() != null) {
                        AlarmVo vo = (AlarmVo) result.getResult();
                        mAdapter.notifyDataSetChanged();
                        if (YWMTools.isYn2Bool(vo.getIsReadOk())) {
                            //getMainActivity().onNotiBadgeUpdate();
                        }
                    }
                }
            }.execute(vo);
        }

        @Override
        protected boolean shouldAddSelectionInActionMode() {
            return false;
        }
    }

}
