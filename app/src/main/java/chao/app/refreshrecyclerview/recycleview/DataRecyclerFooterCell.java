package chao.app.refreshrecyclerview.recycleview;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import chao.app.protocol.LogHelper;

/**
 * @author chao.qin
 * @since 2017/3/31
 */

public class DataRecyclerFooterCell extends DataRecyclerDataCell {

    private static final int OVER_SCROLLER_DISTANCE = DataRecyclerAdapter.OVER_SCROLLER_DISTANCE;  //px

    private View mContentView;

    private LinearLayout mContainer;

    @Override
    public void bindData() {

    }

    @Override
    public View getCellView() {
        mContentView = super.getCellView();
        if (mContentView == null) {
            return null;
        }
        if (mContainer != null) {
            return mContainer;
        }
        mContainer = new LinearLayout(mAdapter.getContext());
        mContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        mContainer.setBackgroundColor(Color.RED);
        mContainer.setMinimumHeight(OVER_SCROLLER_DISTANCE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        mContentView.setBackgroundColor(Color.BLUE);
        mContainer.addView(mContentView,lp);

        mContainer.setTag(this);
        return mContainer;
    }

    int getContentHeight() {
        return mContentView.getHeight();
    }

    @Override
    void setHeight(int height) {
        int oldHeight = mContainer.getHeight();
        int minHeight = mContentView.getHeight() + OVER_SCROLLER_DISTANCE;
        height = Math.max(height,minHeight);
        if (height == oldHeight) {
            return;
        }
        LogHelper.i("chao.qin","setFooterHeight " + height,"oldHeight " + oldHeight);
        ViewGroup.LayoutParams lp = mContainer.getLayoutParams();
        lp.height = height;
        mContainer.setLayoutParams(lp);
    }


}
