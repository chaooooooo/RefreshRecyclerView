package chao.app.refreshrecyclerview.recycleview;

import android.view.View;

/**
 * 实现RecyclerView单选效果的监听回调
 * Created by k.huang on 2017/3/17.
 */

public interface OnSingleSelectionListener {
    /**
     * 当两次点击在同一个Item的时候
     * @param cellView
     * @param pos
     */
    void onSameItemClick(View cellView, int pos);

    /**
     * 当第二次点击在其他Item的时候
     * @param cellView
     * @param pos
     */
    void onDifItemClick(View cellView, int pos);

}
