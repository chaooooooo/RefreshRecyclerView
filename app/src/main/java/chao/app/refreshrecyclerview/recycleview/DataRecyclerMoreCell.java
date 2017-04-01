package chao.app.refreshrecyclerview.recycleview;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;

import com.jobs.lib_v1.settings.LocalStrings;

/**
 * 默认的下一页单元格
 * 如果需要写子类，请直接继承 DataListCell
 * 当前类不希望被其他子类继承
 * 
 * @author solomon.wen
 * @since 2013-12-18
 */
public final class DataRecyclerMoreCell extends DataRecyclerFooterCell {
	@Override
	public final View createCellView() {
		View tmpView = super.createCellView();
		mTextView.setGravity(Gravity.CENTER);
		mTextView.setTextColor(ColorStateList.valueOf(Color.parseColor("#000000")));
		return tmpView;
	}

	@Override
	public final void bindData() {
		mTextView.setText(LocalStrings.common_text_show_next_page);

//		if (mAdapter.getListView().getEnableAutoHeight()) {
//			mTextView.setMaxWidth(mAdapter.getRecyclerView().getWidth());
//		}
	}
}
