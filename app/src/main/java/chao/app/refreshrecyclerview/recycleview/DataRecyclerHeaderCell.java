package chao.app.refreshrecyclerview.recycleview;

import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import chao.app.protocol.LogHelper;
import chao.app.refreshrecyclerview.R;

/**
 * @author chao.qin
 * @since 2017/3/28.
 */
public class DataRecyclerHeaderCell extends DataRecyclerCell{

    private static final int HEADER_POSITION = 0;

    private static final int REFRESH_IDLE = DataRecyclerAdapter.REFRESH_IDLE;        //下拉状态但还没到达刷新或者没有下拉
    private static final int REFRESH_REFRESHING = DataRecyclerAdapter.REFRESH_REFRESHING;  // 正在刷新
    private static final int REFRESH_FAILED = DataRecyclerAdapter.REFRESH_FAILED;  //刷新失败
    private static final int REFRESH_DONE = DataRecyclerAdapter.REFRESH_DONE;    //刷新完成
    private static final int REFRESH_EMPTY = DataRecyclerAdapter.REFRESH_EMPTY;   //数据为空
    private static final int REFRESH_PULL = DataRecyclerAdapter.REFRESH_PULL;   //数据为空
    private static final int REFRESH_CANCEL = DataRecyclerAdapter.REFRESH_CANCEL;   //数据为空



    private static final int REFRESH_STATE_MASK = DataRecyclerAdapter.REFRESH_STATE_MASK;
    private static final int REFRESH_STATUS_DELAY = 500;
    private static final java.lang.String TAG = DataRecyclerHeaderCell.class.getSimpleName();

    private TextView mText = null;
    private ProgressBar mProgressBar;

    private int mScrollY;

    private DataRecyclerView mDataRecyclerView;
    private int mStatus = REFRESH_IDLE;

    private HeaderHandler mHandler = new HeaderHandler();

    void detach() {
        mHandler.detach();
    }



    private class HeaderHandler extends Handler {

        private static final int WHAT_REFRESH_STARTED = 1;
        private static final int WHAT_REFRESH_PULL = 2;
        private static final int WHAT_REFRESH_FAILED = 3;
        private static final int WHAT_REFRESH_DONE = 4;
        private static final int WHAT_REFRESH_EMPTY = 5;

        private static final int WHAT_CLOSE_HEADER = 10;

        private void sendHeaderMessage(int what) {
            Message message = obtainMessage();
            message.what = what;
            message.sendToTarget();
        }

        private void sendHeaderMessageDelay(int what) {
            Message message = obtainMessage();
            message.what = what;
            sendEmptyMessageDelayed(what,REFRESH_STATUS_DELAY);
        }


        private void detach() {
            removeMessages(WHAT_REFRESH_STARTED);
            removeMessages(WHAT_REFRESH_PULL);
            removeMessages(WHAT_REFRESH_FAILED);
            removeMessages(WHAT_REFRESH_EMPTY);
            removeMessages(WHAT_REFRESH_DONE);
            removeMessages(WHAT_CLOSE_HEADER);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_REFRESH_STARTED:
                    refreshStarted();
                    break;
                case WHAT_REFRESH_PULL:
                    refreshPulling();
                    break;
                case WHAT_REFRESH_FAILED:
                    refreshFailed();
                    break;
                case WHAT_REFRESH_EMPTY:
                    refreshDataEmpty();
                    break;
                case WHAT_REFRESH_DONE:
                    refreshDone();
                    break;
                case WHAT_CLOSE_HEADER:
                    closeHeader(true);
                    break;
            }

        }
    }
    public void refreshStarted() {
        mText.setText(R.string.recycler_view_refreshing_text);
        mProgressBar.setVisibility(View.VISIBLE);
    }

    public void refreshPulling() {
        mText.setText(R.string.recycler_view_pre_refresh_text);
        mProgressBar.setVisibility(View.INVISIBLE);
    }


    private void refreshDone() {
        mText.setText(R.string.recycler_view_refreshed_text);
        mProgressBar.setVisibility(View.INVISIBLE);
        mHandler.sendHeaderMessageDelay(HeaderHandler.WHAT_CLOSE_HEADER);
    }

    private void refreshFailed() {
        mText.setText(R.string.recycler_view_refresh_failed_text);
        mProgressBar.setVisibility(View.INVISIBLE);
        mHandler.sendHeaderMessageDelay(HeaderHandler.WHAT_CLOSE_HEADER);

    }

    private void refreshDataEmpty() {
        mText.setText(R.string.recycler_view_refresh_empty_text);
        mProgressBar.setVisibility(View.INVISIBLE);
        mHandler.sendHeaderMessageDelay(HeaderHandler.WHAT_CLOSE_HEADER);
    }


    public int getHeaderHeight() {
        return getCellView().getHeight();
    }

    public void setRecyclerView(DataRecyclerView recyclerView) {
        mDataRecyclerView = recyclerView;
    }

    public boolean overHeader() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mDataRecyclerView.getLayoutManager();
        int firstPosition = layoutManager.findFirstVisibleItemPosition();
        if (firstPosition == HEADER_POSITION) {
            return true;
        }
        return false;
    }

    public boolean overHeaderRefresh() {
        if (overHeader() && mScrollY <= getHeaderHeight() / 4) {
            return true;
        }
        return false;
    }

    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        mScrollY += dy;
    }

    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        if (!overHeader()) {
            return;
        }
        if (newState == RecyclerView.SCROLL_STATE_IDLE ) {
            if (isStatus(REFRESH_PULL)) {
                closeHeader(true);
            } else if (isStatus(REFRESH_REFRESHING)) {
                gotoTop();
            }
        }
    }

    private boolean isStatus(int status) {
        return (mStatus & status) != 0;
    }

    public void refreshStatusChanged(int newStatus) {
        mStatus = newStatus;
        switch (newStatus) {
            case REFRESH_PULL:
                mHandler.sendHeaderMessage(HeaderHandler.WHAT_REFRESH_PULL);
                break;
            case REFRESH_REFRESHING:
                mHandler.sendHeaderMessage(HeaderHandler.WHAT_REFRESH_STARTED);
                break;
            case REFRESH_DONE:
                mHandler.sendHeaderMessage(HeaderHandler.WHAT_REFRESH_DONE);
                break;
            case REFRESH_FAILED:
                mHandler.sendHeaderMessage(HeaderHandler.WHAT_REFRESH_FAILED);
                break;
            case REFRESH_EMPTY:
                mHandler.sendHeaderMessage(HeaderHandler.WHAT_REFRESH_EMPTY);
                break;
            case REFRESH_IDLE:
                break;
            case REFRESH_CANCEL:
                break;
            default:
                throw new IllegalStateException("unknown refresh status. " + DataRecyclerAdapter.statusText(newStatus));
        }
    }

    public void gotoTop() {
        if (!overHeader()) {
            return;
        }
        mDataRecyclerView.smoothScrollToPosition(0);
    }

    private void closeHeader(boolean animation) {
        LogHelper.i(TAG,"closeHeader");
        int offsetY = getHeaderHeight() - mScrollY;
//        DataLinearLayoutManager dlm = (DataLinearLayoutManager) mDataRecyclerView.getLayoutManager();
//        dlm.offsetChildren(-getHeaderHeight());
        if (animation) {
            mDataRecyclerView.smoothScrollBy(0, offsetY + 1); // +1 使refresh_status进入idle状态
        } else {
            mDataRecyclerView.scrollBy(0,offsetY + 1);
        }
    }

    @Override
    public final int getCellViewLayoutID() {
        return R.layout.recycler_view_refresh_header;
    }

    @Override
    public final void bindView() {
        mText = findViewById(R.id.progress_message);
        mProgressBar = findViewById(R.id.progress_bar);
    }

    @Override
    public final void bindData() {
        refreshPulling();
    }
}
