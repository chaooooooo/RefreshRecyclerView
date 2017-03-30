package chao.app.refreshrecyclerview.recycleview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.jobs.lib_v1.data.DataItemResult;

import chao.app.protocol.LogHelper;
import chao.app.protocol.utils.ReflectUtil;
import chao.app.refreshrecyclerview.R;

/**
 * @author chao.qin
 * @since 2017/3/7.
 */

public class DataRecyclerView extends RecyclerView {

    private static final java.lang.String TAG = DataRecyclerView.class.getSimpleName();
    private DataRecyclerAdapter mDataAdapter;

    private LinearLayoutManager mLayoutManager;

    public ItemDecoration mDividerDecoration;

    public DataRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public DataRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DataRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mDataAdapter = new DataRecyclerAdapter(this);
//        mLayoutManager = new DataLinearLayoutManager(context,LinearLayoutManager.VERTICAL,false);
        mLayoutManager = new LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false);
        setLayoutManager(mLayoutManager);
        setAdapter(mDataAdapter);

        mDataAdapter.setSelector(R.drawable.listview_color_selector); //默认点击色

        setDivider(R.drawable.default_recycle_drawable);//默认分割线

        addOnScrollListener(mDataAdapter.new LoadMoreScrollListener());

        ViewConfiguration vc = ViewConfiguration.get(context);
        ReflectUtil.setValue(vc,"mOverscrollDistance",200);
    }


    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        LogHelper.i(TAG,"dispatchNestedFling","vX : "+velocityX,"vy : "+velocityY, "consumed : " + consumed);
        return super.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        LogHelper.i(TAG,"dispatchNestedPreFling ","vX : "+velocityX,"vy : "+velocityY);
        return super.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean fling(int velocityX, int velocityY) {
        LogHelper.i(TAG,"fling ","vX : "+velocityX,"vy : "+velocityY);
        if (mDataAdapter.overHeader()) {
            velocityX = velocityX / 10;
            velocityY = velocityY / 10;
        }
        return super.fling(velocityX, velocityY);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        LogHelper.i(TAG,"fling ","MotionEvent : "+event);
        return super.onTrackballEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return super.onTouchEvent(e);
    }

    /**
     * 设置分隔线
     *
     * 不支持color drawable
     * 建议使用shape drawable
     *
     * @param drawableId 资源id
     */
    public void setDivider(int drawableId) {
        setDivider(getResources().getDrawable(drawableId));
    }

    /**
     * 设置分隔线
     *
     * 不支持color drawable
     * 建议使用shape drawable
     *
     * @param dividerDrawable 资源drawable
     */
    public void setDivider(Drawable dividerDrawable) {

        if (dividerDrawable instanceof ColorDrawable) {
            throw new IllegalArgumentException("ColorDrawable is not supported to be a DataRecyclerView divider.");
        }

        if (mDividerDecoration != null) {
            removeItemDecoration(mDividerDecoration);
        }
        mDividerDecoration = new DividerDecoration(dividerDrawable);

        addItemDecoration(mDividerDecoration);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mDataAdapter.setOnItemClickListener(listener);
    }

    public void setDataRecyclerCell(Class<? extends DataRecyclerCell> clazz) {
        mDataAdapter.setDataRecyclerCell(clazz);
    }

    public void setDataRecyclerCell(Class<? extends DataRecyclerCell> clazz, Object cellClassConstructorParameter) {
        mDataAdapter.setDataRecyclerCell(clazz,cellClassConstructorParameter);
    }

    public final void setDataCellSelector(DataRecyclerCellSelector selector, Object cellClassConstructorParameter) {
        mDataAdapter.setDataCellSelector(selector, cellClassConstructorParameter);
    }

    public final void setDataCellSelector(DataRecyclerCellSelector selector) {
        setDataCellSelector(selector, null);
    }

    public void setOnItemEmptyClickListener(OnItemEmptyClickListener listener) {
        mDataAdapter.setOnItemEmptyClickListener(listener);
    }

    private class DividerDecoration extends ItemDecoration {

        Drawable drawable;//不支持ColorDrawable

        public DividerDecoration(Drawable dividerDrawable) {
            drawable = dividerDrawable;
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, State state) {
            super.onDraw(c, parent, state);
            final int left = parent.getPaddingLeft();
            final int right = parent.getWidth() - parent.getPaddingRight();

            final int childCount = parent.getChildCount();
            for (int i = 0; i < childCount - 1; i++) {
                final View child = parent.getChildAt(i);
                final LayoutParams params = (LayoutParams) child
                        .getLayoutParams();
                final int top = child.getBottom() + params.bottomMargin;
                final int bottom = top + drawable.getIntrinsicHeight();
                drawable.setBounds(left, top, right, bottom);
                drawable.draw(c);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            outRect.set(0, 0, 0, drawable.getIntrinsicHeight());
        }
    }

    public void appendData(DataItemResult appendData) {
        mDataAdapter.appendData(appendData);
    }

    public void setDataLoader(DataLoader dataLoader) {
        mDataAdapter.setDataLoader(dataLoader);
    }


    public void setEmptyCellClass(Class<? extends DataRecyclerCell> emptyCell) {
        mDataAdapter.setEmptyCellClass(emptyCell);
    }

    public void setEmptyCellClass(Class<? extends DataRecyclerCell> emptyCell, Object cellClassConstructorParameter) {
        mDataAdapter.setEmptyCellClass(emptyCell, cellClassConstructorParameter);
    }


    public void setErrorCellClass(Class<? extends DataRecyclerCell> emptyCell) {
        mDataAdapter.setErrorCellClass(emptyCell);
    }

    public void setErrorCellClass(Class<? extends DataRecyclerCell> emptyCell, Object cellClassConstructorParameter) {
        mDataAdapter.setErrorCellClass(emptyCell, cellClassConstructorParameter);
    }

    public void setLoadingCellClass(Class<? extends DataRecyclerCell> emptyCell) {
        mDataAdapter.setLoadingCellClass(emptyCell);
    }

    public void setLoadingCellClass(Class<? extends DataRecyclerCell> emptyCell, Object cellClassConstructorParameter) {
        mDataAdapter.setLoadingCellClass(emptyCell, cellClassConstructorParameter);
    }

    public void setMoreCellClass(Class<? extends DataRecyclerCell> emptyCell) {
        mDataAdapter.setMoreCellClass(emptyCell);
    }

    public void setMoreCellClass(Class<? extends DataRecyclerCell> emptyCell, Object cellClassConstructorParameter) {
        mDataAdapter.setMoreCellClass(emptyCell, cellClassConstructorParameter);
    }

    public DataRecyclerAdapter getDataAdapter(){
        return mDataAdapter;
    }


    public DataItemResult getDataList() {
        return mDataAdapter.getRecyclerData();
    }

}
