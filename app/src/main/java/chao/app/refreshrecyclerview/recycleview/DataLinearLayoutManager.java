package chao.app.refreshrecyclerview.recycleview;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;

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
//        mHelper = OrientationHelper.createOrientationHelper(this,orientation);
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int consumed = super.scrollVerticallyBy(dy, recycler, state);
        if (consumed > 0) {
//            mHelper.offsetChildren(unconsumed);
        }
//        LogHelper.i("chao.qin","scrollVerticallyBy --> ","consumed : " + consumed,"unconsumed : " + (dy - consumed));
        return consumed;
    }


    @Override
    public int computeVerticalScrollExtent(RecyclerView.State state) {
        return super.computeVerticalScrollExtent(state) + 480;
    }

    @Override
    public int computeVerticalScrollOffset(RecyclerView.State state) {
        return super.computeVerticalScrollOffset(state) + 480;
    }

    @Override
    public int computeVerticalScrollRange(RecyclerView.State state) {
        return super.computeVerticalScrollRange(state) + 480;
    }
}
