package chao.app.refreshrecyclerview.recycleview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

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

    static final int OVER_SCROLLER_DISTANCE = 30;  //px

    private static final int HEADER_POSITION = 0;

    static final int IDLE = 1;              //  0000001
    static final int LOADING = 1 << 1;     //  0000010
    static final int EMPTY = 1 << 2;       //  0000100
    static final int ERROR = 1<< 3;        //  0001000
    static final int MORE = 1<< 4;         //  0010000
    private static final int LOAD_STATE_MASK = 0xffff;

    static final int REFRESH_IDLE = 1 << 16;        //没有下拉
    static final int REFRESH_REFRESHING = 1 << 17;  // 正在刷新
    static final int REFRESH_FAILED = 1 << 18;  //刷新失败
    static final int REFRESH_DONE = 1 << 19;    //刷新完成
    static final int REFRESH_EMPTY = 1 << 20;   //数据为空
    static final int REFRESH_INIT = 1 << 21;    //初始化状态
    static final int REFRESH_PULL = 1 << 22;    //下拉，还没到达刷新
    static final int REFRESH_CANCEL = 1 << 23;    //下拉，还没到达刷新


    private static final int REFRESH_STATE_MASK = 0xffff0000;

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final int NO_PAGE = -1;
    private static final int START_PAGE = 1;


    private Context mContext;

    private DataItemResult mRecyclerData = new DataItemResult();

    private DataRecyclerView mRecyclerView;

    private DataLoader mDataLoader;

    private SilentTask mDataLoaderTask;

    private int mPageSize = 20;
    private int mCurrentPage = NO_PAGE;

    private int mSelectorDrawableId; // 列表点击色drawable

    private DataRecyclerHeaderCell mHeaderCell;  //不支持多个header
    private DataRecyclerFooterCell mFooterCell;

    private int mRecyclerViewVisibleItemCount = 0;


    /**
     * 数据单元格样式
     */
    private final DataRecyclerCellOrganizer mDataOrganizer = DataRecyclerCellCenter.dataOrganizer(this);
    private final DataRecyclerCellOrganizer mHeaderOrganizer = DataRecyclerCellCenter.headerOrganizer(this);//不支持多个header
    private final DataRecyclerCellOrganizer mFooterOrganizer = DataRecyclerCellCenter.footerOrganizer(this);

    private OnItemClickListener mItemClickListener;

    private OnItemEmptyClickListener mEmptyClickListener;
    private int mPosition;
    private SilentTask mDataRefreshTask;
    private boolean mPullRefreshEnabled = true;
    private boolean mAutoLoadEnabled = true;


    DataRecyclerAdapter(DataRecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mContext = recyclerView.getContext();
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

        if (!organizer.equals(mFooterOrganizer)) {
            startViewType += mFooterOrganizer.getCellTypeCount();

            if (!organizer.equals(mHeaderOrganizer)) {
                startViewType += mHeaderOrganizer.getCellTypeCount();
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
        final View view = holder.itemView;
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
            mHeaderCell.refreshStatusChanged(REFRESH_REFRESHING);//刚进入加载页面，进入刷新状态
            if (!isRefreshEnabled()) {
                mHeaderCell.setHeight(0);
            }
        }
        if (isFooterPosition(position) && cell instanceof DataRecyclerFooterCell) {
            mFooterCell = (DataRecyclerFooterCell) cell;
        }
        view.getViewTreeObserver().addOnGlobalLayoutListener(new LayoutObserver(view));
    }

    public boolean canRequestLayout() {
        return true;
    }

    public void appendData(DataItemResult appendData) {
        mRecyclerData.appendItems(appendData);
        notifyDataSetChanged();
    }

    void disablePullRefresh() {
        mPullRefreshEnabled = false;
    }

    void disableAutoLoad() {
        mAutoLoadEnabled = false;
    }

    private class LayoutObserver implements ViewTreeObserver.OnGlobalLayoutListener {

        private View observeView;

        private LayoutObserver(View view) {
            observeView = view;
        }

        @Override
        public void onGlobalLayout() {

            resizeFooterView();

            removeListener();
        }


        private void removeListener() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                observeView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } else {
                observeView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
        }
    }

    private void resizeFooterView() {
        if (!isRefreshEnabled()) {
            return;
        }
        int visibleCount = mRecyclerView.getChildCount();
        if (visibleCount > mRecyclerViewVisibleItemCount) { //可见的列表项增加,重新计算footer高度
            int visibleItemHeight = 0;
            for (int i = 0;i < visibleCount; i++) {
                View view = mRecyclerView.getChildAt(i);
                if (view == mFooterCell.getCellView() || view == mHeaderCell.getCellView()) {
                    continue;
                }
                visibleItemHeight += view.getHeight();
            }
            mFooterCell.setHeight(mRecyclerView.getHeight() - visibleItemHeight);
            mRecyclerViewVisibleItemCount = visibleCount;
        }

    }

    private boolean isFooterPosition(int position) {
        return position > getDataCount();
    }

    private boolean isItemPosition(int position) {
        return position > HEADER_POSITION && position <= getDataCount();
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

    private void stopRefreshData() {
        if (mDataRefreshTask != null) {
            mDataRefreshTask.cancel(true);
        }
    }

    private void startRefreshData() {

        if (!isRefreshEnabled()) {
            return;
        }

        if (isTaskRunning(mDataRefreshTask) || hasStatus(REFRESH_REFRESHING)) {
            return;
        }
        mDataRefreshTask = new SilentTask() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                stopLoadingData();
                toRefreshStatus(REFRESH_REFRESHING);
                mDataLoader.onPreFetch();
            }

            @Override
            protected DataItemResult doInBackground(String... params) {
                return mDataLoader.fetchData(DataRecyclerAdapter.this, START_PAGE, mPageSize);
            }

            @Override
            protected void onTaskFinished(DataItemResult result) {
                appendRefreshData(result);
                mDataLoader.onFetchDone(result);
            }

            @Override
            protected void onCancelled(DataItemResult dataItemDetails) {
                super.onCancelled(dataItemDetails);
                toRefreshStatus(REFRESH_CANCEL);
                toRefreshStatus(REFRESH_PULL);
            }
        };
        mDataRefreshTask.executeOnPool();
    }

    private void stopLoadingData() {
        if (mDataLoaderTask != null) {
            mDataLoaderTask.cancel(true);
        }
    }

    private void startLoadingData() {

        if (!isLoadEnabled()) {
            return;
        }

        if (isTaskRunning(mDataLoaderTask) || hasStatus(LOADING|REFRESH_REFRESHING)) {
            return;
        }
        mDataLoaderTask = new SilentTask() {
            public int loadPage;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                toLoadStatus(LOADING);
                mDataLoader.onPreFetch();
            }

            @Override
            protected DataItemResult doInBackground(String... params) {
                loadPage = mCurrentPage + 1;
                if (mCurrentPage == NO_PAGE) {
                    loadPage = START_PAGE;
                }
                return mDataLoader.fetchData(DataRecyclerAdapter.this, loadPage, mPageSize);
            }

            @Override
            protected void onTaskFinished(DataItemResult result) {
                appendLoadData(result,loadPage);
                mDataLoader.onFetchDone(result);
            }

            @Override
            protected void onCancelled(DataItemResult dataItemDetails) {
                super.onCancelled(dataItemDetails);
                toLoadStatus(IDLE);
            }
        };
        mDataLoaderTask.executeOnPool();
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
        if (isLoadEnabled() && position >= getDataCount() + mHeaderOrganizer.getCellTypeCount()) {
            return mFooterOrganizer;
        }

        return mDataOrganizer;
    }

    @Override
    public int getItemCount() {
        int count = getDataCount();

        count += mHeaderOrganizer.getCellTypeCount();

        if (isLoadEnabled()) {
            count += mFooterOrganizer.getCellTypeCount();
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

        mRecyclerData.clear();
        mRecyclerViewVisibleItemCount = 0;
        mCurrentPage = NO_PAGE;

        if (appendData.isValidListData()) {
            mCurrentPage = START_PAGE;
            //appendItems方法会置换mRecyclerData的maxCount
            mRecyclerData.appendItems(appendData);
        }
        notifyDataSetChanged();

        if (mRecyclerData.getDataCount() == 0) {
            toRefreshStatus(REFRESH_EMPTY);
            return;
        }

        toRefreshStatus(REFRESH_DONE);
    }

    private synchronized void appendLoadData(DataItemResult appendData, int loadPage) {
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
            mCurrentPage = loadPage;
            appendData(appendData);
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

    private final Object mStatusLock = new Object();

    private void toLoadStatus(int status) {
        synchronized (mStatusLock) {

            int oldLoadStatus = mStatus & LOAD_STATE_MASK;
            int newLoadStatus = status & LOAD_STATE_MASK;
            if ((oldLoadStatus ^ newLoadStatus) == 0) {
                return;
            }

            mStatus = mStatus & ~LOAD_STATE_MASK | newLoadStatus;

            if (mFooterCell != null) {
                mFooterCell.loadStatusChanged(newLoadStatus);
            }

            if (DEBUG) {
                LogHelper.d(TAG, "to load status : " + statusText(mStatus));
            }
        }
    }

    void toRefreshStatus(int status) {
        synchronized (mStatusLock) {
            int oldRefreshStatus = mStatus & REFRESH_STATE_MASK;
            int newRefreshStatus = status & REFRESH_STATE_MASK;
            if ((oldRefreshStatus ^ newRefreshStatus) == 0) {
                return;
            }

            mStatus = mStatus & ~REFRESH_STATE_MASK | newRefreshStatus;

            if (mHeaderCell != null ) {//setDataLoader时，mHeader可能还没有初始化，这种情况延迟到onBindViewHolder中mHeaderCell初始化时调用刷新
                mHeaderCell.refreshStatusChanged(mStatus & REFRESH_STATE_MASK);
            }

            if (DEBUG) {
                LogHelper.d(TAG, "to refresh status : " + statusText(mStatus));
            }

        }
    }

    private void toStatus(int status) {
        synchronized (mStatusLock) {
            if (mStatus == status) return;

            mStatus = status;

            LogHelper.i(TAG,"to status : " + statusText(status) + "(0x" + Integer.toHexString(status) + ")");

        }
        notifyDataSetChanged();
    }

    private void addStatus(int status) {
        synchronized (mStatusLock) {
            if ((mStatus & status) != 0) {
                return;
            }

            mStatus |= status;

            AppUtil.print("add status : " + statusText(status) + "(0x" + Integer.toHexString(status) + ")");

        }
        notifyDataSetChanged();
    }

    private void removeStatus(int status) {
        synchronized (mStatusLock) {
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
                statusText += " | REFRESH_PULL";
                break;
            case REFRESH_CANCEL:
                statusText += " | REFRESH_CANCEL";
                break;
            case REFRESH_EMPTY:
                statusText += " | REFRESH_EMPTY";
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

            mHeaderCell.onScrolled(recyclerView, dx, dy);

            if (!mHeaderCell.overHeader() && !hasStatus(REFRESH_REFRESHING)) {
                toRefreshStatus(REFRESH_IDLE);

//                if (hasStatus(IDLE | MORE | ERROR | EMPTY) && atPreLoadingPosition && hasMore()) {
//                    startLoadingData();
//                }

                if (hasStatus(IDLE | MORE) && atPreLoadingPosition && hasMore()) {
                    startLoadingData();
                }
            }
            if (mHeaderCell.overHeader() && hasStatus(REFRESH_IDLE)) {
                toRefreshStatus(REFRESH_PULL);
            }
            if (mHeaderCell.overHeaderRefresh() && hasStatus(REFRESH_REFRESHING)) {
                mHeaderCell.moveToTop();
            }
            if (mHeaderCell.overFling() && mRecyclerView.getScrollState() == RecyclerView.SCROLL_STATE_SETTLING) {
                mRecyclerView.stopScroll();
            }
            if (mHeaderCell.overHeaderRefresh() && mRecyclerView.getScrollState() != RecyclerView.SCROLL_STATE_SETTLING && !hasStatus(REFRESH_REFRESHING | REFRESH_DONE) && dy < 0) {
                startRefreshData();
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (!isRefreshEnabled()) {
                return;
            }
            mHeaderCell.onScrollStateChanged(recyclerView, newState);
        }
    }

    private boolean isRefreshEnabled() {
        return mDataLoader != null && mPullRefreshEnabled;
    }

    private boolean isLoadEnabled() {
        return mDataLoader != null && mAutoLoadEnabled;
    }

    private boolean hasMore() {
        return mRecyclerData.maxCount > mRecyclerData.getDataCount();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        mHeaderCell.detach();
        if (mFooterCell != null) {
            mFooterCell.detach();
        }
    }
}
