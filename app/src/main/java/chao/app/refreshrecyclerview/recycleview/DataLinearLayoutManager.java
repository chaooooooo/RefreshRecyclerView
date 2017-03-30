package chao.app.refreshrecyclerview.recycleview;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;

import chao.app.protocol.LogHelper;

/**
 * @author chao.qin
 * @since 2017/3/30
 */

public class DataLinearLayoutManager extends LinearLayoutManager {

    private OrientationHelper mHelper;

    public DataLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
        init(orientation);
    }

    private void init(int orientation) {
        mHelper = OrientationHelper.createOrientationHelper(this,orientation);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int unconsumed = super.scrollVerticallyBy(dy, recycler, state);
        if (unconsumed > 0) {
            mHelper.offsetChildren(unconsumed);
        }
        LogHelper.i("chao.qin","scrollVerticallyBy --> ","unConsumed : " + unconsumed);
        return 0;
    }

    public void offsetChildren(int offset) {
        mHelper.offsetChildren(offset);
    }
}
