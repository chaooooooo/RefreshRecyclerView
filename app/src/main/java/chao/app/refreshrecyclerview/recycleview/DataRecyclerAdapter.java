package chao.app.refreshrecyclerview.recycleview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.jobs.lib_v1.app.AppUtil;
import com.jobs.lib_v1.data.DataItemDetail;
import com.jobs.lib_v1.data.DataItemResult;
import com.jobs.lib_v1.task.SilentTask;

import chao.app.protocol.LogHelper;
import chao.app.refreshrecyclerview.BuildConfig;


/**
 * @author chao.qin
 * @since 2017/3/7.
 */

public class DataRecyclerAdapter extends RecyclerView.Adapter {

    private static final String TAG = "DataRecyclerAdapter";

    private static final int IDLE = 1;              //  0000001
    private static final int LOADING = 1 << 1;     //  0000010
    private static final int EMPTY = 1 << 2;       //  0000100
    private static final int ERROR = 1<< 3;        //  0001000
    private static final int MORE = 1<< 4;         //  0010000
    private static final int LOAD_STATE_MASK = 0x1f;

    static final int REFRESH_IDLE = 1 << 16;        //没有下拉
    static final int REFRESH_REFRESHING = 1 << 17;  // 正在刷新
    static final int REFRESH_FAILED = 1 << 18;  //刷新失败
    static final int REFRESH_DONE = 1 << 19;    //刷新完成
    static final int REFRESH_EMPTY = 1 << 20;   //数据为空
    static final int REFRESH_INIT = 1 << 21;    //初始化状态
    static final int REFRESH_PULL = 1 << 22;    //下拉，还没到达刷新

    static final int REFRESH_STATE_MASK = 0x7f0000;

    private static final boolean DEBUG = BuildConfig.DEBUG;


    private Context mContext;

    private DataItemResult mRecyclerData = new DataItemResult();

    private DataRecyclerView mRecyclerView;

    private DataLoader mDataLoader;

    private SilentTask mDataLoaderTask;

    private int mPageSize = 20;
    private int mCurrentPage = 0;

    private int mSelectorDrawableId; // 列表点击色drawable

    private DataRecyclerHeaderCell mHeaderCell;  //不支持多个header

    /**
     * 数据单元格样式
     */
    private final DataRecyclerCellOrganizer mErrorOrganizer = DataRecyclerCellCenter.errorOrganizer(this);
    private final DataRecyclerCellOrganizer mLoadingOrganizer = DataRecyclerCellCenter.loadingOrganizer(this);
    private final DataRecyclerCellOrganizer mEmptyOrganizer = DataRecyclerCellCenter.emptyOrganizer(this);
    private final DataRecyclerCellOrganizer mMoreOrganizer = DataRecyclerCellCenter.moreOrganizer(this);
    private final DataRecyclerCellOrganizer mDataOrganizer = DataRecyclerCellCenter.dataOrganizer(this);
    private final DataRecyclerCellOrganizer mHeaderOrganizer = DataRecyclerCellCenter.headerOrganizer(this);//不支持多个header

    private OnItemClickListener mItemClickListener;

    private OnItemEmptyClickListener mEmptyClickListener;
    private int mPosition;
    private SilentTask mDataRefreshTask;


    DataRecyclerAdapter(DataRecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mContext = recyclerView.getContext();
        notifyDataSetChanged();
    }

    Context getContext() {
        return mContext;
    }

    void setPageSize(int pageSize) {
        mPageSize = pageSize;
    }

    public void setSelector(int drawableId) {
        mSelectorDrawableId = drawableId;
    }

    @Override
    public int getItemViewType(int position) {
        mPosition = position;

        DataRecyclerCellOrganizer organizer = getItemOrganizer(position);
        int startViewType = 0;

        if (!organizer.equals(mLoadingOrganizer)) {
            startViewType += mLoadingOrganizer.getCellTypeCount();

            if (!organizer.equals(mErrorOrganizer)) {
                startViewType += mErrorOrganizer.getCellTypeCount();

                if (!organizer.equals(mMoreOrganizer)) {
                    startViewType += mMoreOrganizer.getCellTypeCount();

                    if (!organizer.equals(mEmptyOrganizer)) {
                        startViewType += mEmptyOrganizer.getCellTypeCount();

                        if (!organizer.equals(mHeaderOrganizer)){
                            startViewType += mHeaderOrganizer.getCellTypeCount();
                        }
                    }
                }
            }
        }

        return startViewType + organizer.getCellType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DataRecyclerCellOrganizer organizer = getItemOrganizer(mPosition);
        return new DataViewHolder(organizer.getCellView(mPosition));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        View view = holder.itemView;
        view.setOnClickListener(new ItemClickListener(position));
        DataRecyclerCell cell = (DataRecyclerCell) view.getTag();
        View cellView = cell.getCellView();
        Drawable drawable = cellView.getBackground();
        if (drawable == null) {
            cellView.setBackgroundResource(mSelectorDrawableId);
        }

        cell.updateCellData(position);
        cell.bindView();
        cell.bindData();

        if (mHeaderCell == null && cell instanceof DataRecyclerHeaderCell) {
            mHeaderCell = (DataRecyclerHeaderCell) cell;
            mHeaderCell.setRecyclerView(mRecyclerView);
            mHeaderCell.refreshStatusChanged(REFRESH_REFRESHING);
        }
    }

    void setDataLoader(DataLoader dataLoader) {
        if (!hasStatus(LOADING)) {
            mRecyclerData.clear();
            mDataLoader = dataLoader;
            mCurrentPage = 0;
            startRefreshData();
        }
    }

    private boolean isTaskRunning(SilentTask task) {
        return task != null && task.getStatus() == AsyncTask.Status.RUNNING;
    }

    void refreshData() {
        if (isTaskRunning(mDataRefreshTask) || hasStatus(REFRESH_REFRESHING)) {
            return;
        }
        startRefreshData();
    }

    private void startRefreshData() {

        if (isTaskRunning(mDataRefreshTask) || hasStatus(REFRESH_REFRESHING)) {
            return;
        }
        mDataRefreshTask = new SilentTask() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                toRefreshStatus(REFRESH_REFRESHING);
                mDataLoader.onPreFetch();
            }

            @Override
            protected DataItemResult doInBackground(String... params) {
                mCurrentPage = 1;
                return mDataLoader.fetchData(DataRecyclerAdapter.this, mCurrentPage, mPageSize);
            }

            @Override
            protected void onTaskFinished(DataItemResult result) {
                mRecyclerData.clear();
                appendRefreshData(result);
                mDataLoader.onFetchDone(result);
            }
        };
        mDataRefreshTask.executeOnPool();
    }

    private synchronized void startLoadingData() {
        if (isTaskRunning(mDataLoaderTask) || hasStatus(LOADING)) {
            return;
        }
        mDataLoaderTask = new SilentTask() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                toLoadStatus(LOADING);
                mDataLoader.onPreFetch();
            }

            @Override
            protected DataItemResult doInBackground(String... params) {
                mCurrentPage++;
                return mDataLoader.fetchData(DataRecyclerAdapter.this, mCurrentPage, mPageSize);
            }

            @Override
            protected void onTaskFinished(DataItemResult result) {
                appendData(result);
                mDataLoader.onFetchDone(result);
            }
        };
        mDataLoaderTask.executeOnPool();
    }

    void setEmptyCellClass(Class<? extends DataRecyclerCell> emptyCell) {
        mEmptyOrganizer.setCellClass(emptyCell, null);
    }

    void setEmptyCellClass(Class<? extends DataRecyclerCell> emptyCell, Object cellClassConstructorParameter) {
        mEmptyOrganizer.setCellClass(emptyCell, cellClassConstructorParameter);
    }

    void setErrorCellClass(Class<? extends DataRecyclerCell> emptyCell) {
        mErrorOrganizer.setCellClass(emptyCell, null);
    }

    void setErrorCellClass(Class<? extends DataRecyclerCell> emptyCell, Object cellClassConstructorParameter) {
        mErrorOrganizer.setCellClass(emptyCell, cellClassConstructorParameter);
    }

    void setLoadingCellClass(Class<? extends DataRecyclerCell> emptyCell) {
        mErrorOrganizer.setCellClass(emptyCell, null);
    }

    void setLoadingCellClass(Class<? extends DataRecyclerCell> emptyCell, Object cellClassConstructorParameter) {
        mErrorOrganizer.setCellClass(emptyCell, cellClassConstructorParameter);
    }

    void setMoreCellClass(Class<? extends DataRecyclerCell> emptyCell) {
        mErrorOrganizer.setCellClass(emptyCell, null);
    }

    void setMoreCellClass(Class<? extends DataRecyclerCell> emptyCell, Object cellClassConstructorParameter) {
        mErrorOrganizer.setCellClass(emptyCell, cellClassConstructorParameter);
    }



    private class DataViewHolder extends RecyclerView.ViewHolder {

        private DataViewHolder(View cellView) {
            super(cellView);
        }
    }

    void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    void setOnItemEmptyClickListener(OnItemEmptyClickListener listener) {
        mEmptyClickListener = listener;
    }

    private class ItemClickListener implements View.OnClickListener {
        private int position = 0;

        ItemClickListener(int _position) {
            position = _position;
        }

        @Override
        public void onClick(View v) {
            if (position == 0) {//header没有点击事件
                return;
            }
            //点击的不是Item,可能是出错，为空或者下一页
            if (getItemPosition(position) >= mRecyclerData.getDataCount()) {
                if (hasStatus(EMPTY) && mEmptyClickListener != null) {
                    if (mEmptyClickListener.onItemEmptyClickListener(v))
                        return;
                }
                startLoadingData();
                return;
            }

            if (mItemClickListener != null) {
                mItemClickListener.onItemClickListener(getRecyclerView(), v, position);
            }
        }
    }

    private int getItemPosition(int position) {
        return position - mHeaderOrganizer.getCellTypeCount();
    }

    private DataRecyclerCellOrganizer getItemOrganizer(int position) {
        if (position < mHeaderOrganizer.getCellTypeCount()) {
            return mHeaderOrganizer;
        }
        if (position < getDataCount() + mHeaderOrganizer.getCellTypeCount()) {
            return mDataOrganizer;
        }

        switch (mStatus) {
            case LOADING:
                return mLoadingOrganizer;
            case ERROR:
                return mErrorOrganizer;
            case EMPTY:
                return mEmptyOrganizer;
            case MORE:
            default:
                return mMoreOrganizer;
        }
    }

    @Override
    public int getItemCount() {
        int count = mRecyclerData.getDataCount() + mHeaderOrganizer.getCellTypeCount();

        if (!hasStatus(IDLE)) {
            count++;
        }
        return count;
    }

    final void setDataRecyclerCell(Class<?> clazz) {
        mDataOrganizer.setCellClass(clazz, null);
    }

    final void setDataRecyclerCell(Class<?> clazz, Object cellClassConstructorParameter) {
        mDataOrganizer.setCellClass(clazz, cellClassConstructorParameter);
    }

    /**
     * 指定 数据 单元格选择器和单元格参数
     *
     * @param selector                      数据单元格选择器实例
     * @param cellClassConstructorParameter 单元格默认构造方法带的参数 (默认为null)
     */
    final void setDataCellSelector(DataRecyclerCellSelector selector, Object cellClassConstructorParameter) {
        mDataOrganizer.setCellSelector(selector, cellClassConstructorParameter);
    }

    final void setDataCellSelector(DataRecyclerCellSelector selector) {
        setDataCellSelector(selector, null);
    }

    private synchronized void appendRefreshData(DataItemResult appendData) {

        if (appendData.hasError) {
            mRecyclerData.message = appendData.message;
            mRecyclerData.hasError = true;
            toRefreshStatus(REFRESH_FAILED);
            return;
        }

        if (appendData.isValidListData()) {
            //appendItems方法会置换mRecyclerData的maxCount
            mRecyclerData.appendItems(appendData);
        }

        if (mRecyclerData.getDataCount() == 0) {
            toRefreshStatus(REFRESH_EMPTY);
            return;
        }

        toRefreshStatus(REFRESH_DONE);
    }

    synchronized void appendData(DataItemResult appendData) {
        if (appendData == null) {
            return;
        }

        if (appendData.hasError) {
            mRecyclerData.message = appendData.message;
            mRecyclerData.hasError = true;
            toLoadStatus(ERROR);
            return;
        }

        if (appendData.isValidListData()) {
            //appendItems方法会置换mRecyclerData的maxCount
            mRecyclerData.appendItems(appendData);
        }

        if (mRecyclerData.getDataCount() == 0) {
            toLoadStatus(EMPTY);
            return;
        }

        toLoadStatus(hasMore()?MORE:IDLE);
    }

    DataItemDetail getItem(int position) {
        return mRecyclerData.getItem(getItemPosition(position));
    }

    RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    DataItemResult getRecyclerData() {
        return mRecyclerData;
    }

    private int getDataCount() {
        return mRecyclerData.getDataCount();
    }

    private volatile int mStatus = IDLE | REFRESH_IDLE;

    private final Object mLoadStatusLock = new Object();

    private void toLoadStatus(int status) {
        synchronized (mLoadStatusLock) {

            int oldLoadStatus = mStatus & LOAD_STATE_MASK;
            int newLoadStatus = status & LOAD_STATE_MASK;
            if ((oldLoadStatus ^ newLoadStatus) == 0) {
                return;
            }

            mStatus = mStatus & ~LOAD_STATE_MASK | newLoadStatus;

            if (DEBUG) {
                LogHelper.d(TAG, "to status : " + statusText(status));
            }

        }
        notifyDataSetChanged();
    }

    private void toRefreshStatus(int status) {
        synchronized (mLoadStatusLock) {
            int oldRefreshStatus = mStatus & REFRESH_STATE_MASK;
            int newRefreshStatus = status & REFRESH_STATE_MASK;
            if ((oldRefreshStatus ^ newRefreshStatus) == 0) {
                return;
            }

            mStatus = mStatus & ~REFRESH_STATE_MASK | newRefreshStatus;

            if (mHeaderCell != null ) {
                mHeaderCell.refreshStatusChanged(mStatus & REFRESH_STATE_MASK);
            }

            if (DEBUG) {
                LogHelper.d(TAG, "to refresh status : " + statusText(mStatus));
            }

        }
        notifyDataSetChanged();
    }

    private void toStatus(int status) {
        synchronized (mLoadStatusLock) {
            if (mStatus == status) return;

            mStatus = status;

            LogHelper.e(TAG,"to status : " + statusText(status) + "(0x" + Integer.toHexString(status) + ")");

        }
        notifyDataSetChanged();
    }

    private void addStatus(int status) {
        synchronized (mLoadStatusLock) {
            if ((mStatus & status) != 0) {
                return;
            }

            mStatus |= status;

            AppUtil.print("add status : " + statusText(status) + "(0x" + Integer.toHexString(status) + ")");

        }
        notifyDataSetChanged();
    }

    private void removeStatus(int status) {
        synchronized (mLoadStatusLock) {
            if ((mStatus & status) != 0) {
                return;
            }

            mStatus &= ~status;

            AppUtil.print("remove status : " + statusText(status) + "(0x" + Integer.toHexString(status) + ")");

        }
        notifyDataSetChanged();
    }

    static String statusText(int status) {
        int loadStatus = status & LOAD_STATE_MASK;
        int refreshStatus = status & REFRESH_STATE_MASK;
        String statusText;
        switch (loadStatus) {
            case IDLE:
                statusText = "IDLE";
                break;
            case LOADING:
                statusText = "LOADING";
                break;
            case ERROR:
                statusText = "ERROR";
                break;
            case EMPTY:
                statusText = "EMPTY";
                break;
            case MORE:
                statusText = "MORE";
                break;
            default:
                statusText = "default";
        }
        statusText += "(0x" + Integer.toHexString(loadStatus) + ")";
        switch (refreshStatus) {
            case REFRESH_IDLE:
                statusText += " | REFRESH_IDLE";
                break;
            case REFRESH_REFRESHING:
                statusText += " | REFRESH_REFRESHING";
                break;
            case REFRESH_FAILED:
                statusText += " | REFRESH_FAILED";
                break;
            case REFRESH_DONE:
                statusText += " | REFRESH_DONE";
                break;
            case REFRESH_PULL:
                statusText = "REFRESH_PULL";
                break;
            default:
                statusText += " | default";
                break;
        }
        statusText += "(0x" + Integer.toHexString(refreshStatus >>> 16) + ")";
        return statusText;
    }

    /**
     *
     * @param status 加载状态，可以使用|语句表示一个状态集合
     *
     * @return mLoadStatus是status中任意状态之一返回true
     */
    private boolean hasStatus(int status) {
        if (DEBUG) {
            String statusText = statusText(status);
//            LogHelper.d(TAG,"statusText : " + statusText);
        }
        return (mStatus & status) != 0;
    }

    class LoadMoreScrollListener extends RecyclerView.OnScrollListener {
        private LinearLayoutManager mLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            int visibleItemCount = recyclerView.getChildCount();
            int lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();

            int totalCount = mRecyclerData.getDataCount();

            boolean atPreLoadingPosition = totalCount - lastVisibleItemPosition < visibleItemCount; //滚动到达预加载的位置


            if (hasStatus(IDLE | MORE) && atPreLoadingPosition && hasMore()) {
                startLoadingData();
            }
            mHeaderCell.onScrolled(recyclerView, dx, dy);

            if (!mHeaderCell.overHeader()) {
                toRefreshStatus(REFRESH_IDLE);
            } else if (!hasStatus(REFRESH_REFRESHING)) {
                toRefreshStatus(REFRESH_PULL);
            }

            if (mHeaderCell.overHeaderRefresh() && !hasStatus(LOADING | REFRESH_REFRESHING) && mRecyclerData.getDataCount() > 0) {
                refreshData();
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            mHeaderCell.onScrollStateChanged(recyclerView, newState);
        }
    }

    private boolean hasMore() {
        return mRecyclerData.maxCount > mRecyclerData.getDataCount();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mHeaderCell.detach();
    }
}