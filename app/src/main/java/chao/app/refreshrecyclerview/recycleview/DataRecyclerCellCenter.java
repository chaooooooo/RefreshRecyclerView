package chao.app.refreshrecyclerview.recycleview;

import com.jobs.lib_v1.misc.JavaReflectClass;

/**
 * 单元格管理中心
 * 
 * 负责初始化默认单元格
 * 
 * @author solomon.wen
 * @since 2013-12-18
 */
public final class DataRecyclerCellCenter {
	/**
	 *  获取默认单元格的配置类
	 **/
	private final static JavaReflectClass mDefaultCellSettings = new JavaReflectClass("com.jobs.settings.RecyclerViewDefaultCellClasses");

	/**
	 * 获取指定类型单元格的默认类名
	 * 
	 * @return Class<>
	 */
	private final static Class<?> getDefaultCellClass(String cellType, Class<?> defaultCellClass) {

		Class<?> cellClass = (Class<?>) mDefaultCellSettings.getStaticMethodResult(cellType);
		if (null == cellClass || !(DataRecyclerCell.class.isAssignableFrom(cellClass))) {
			cellClass = defaultCellClass;
		}

		return cellClass;
	}

	/**
	 * 获取出错单元格的配置器
	 * 配置器中会被设上默认的出错单元格
	 *
	 * @return DataRecyclerCellOrganizer
	 */
	public final static DataRecyclerCellOrganizer errorOrganizer(DataRecyclerAdapter adapter) {
		Class<?> cellClass = getDefaultCellClass("errorCellClass", DataRecyclerErrorCell.class);
		return new DataRecyclerCellOrganizer(adapter, cellClass);
	}

	/**
	 * 获取数据为空单元格的配置器
	 * 配置器中会被设上默认的数据为空单元格
	 *
	 * @return DataRecyclerCellOrganizer
	 */
	public final static DataRecyclerCellOrganizer emptyOrganizer(DataRecyclerAdapter adapter) {
		Class<?> cellClass = getDefaultCellClass("emptyCellClass", DataRecyclerEmptyCell.class);
		return new DataRecyclerCellOrganizer(adapter, cellClass);
	}

	/**
	 * 获取加载中单元格的配置器
	 * 配置器中会被设上默认的加载中单元格
	 *
	 * @return DataRecyclerCellOrganizer
	 */
	public final static DataRecyclerCellOrganizer loadingOrganizer(DataRecyclerAdapter adapter) {
		Class<?> cellClass = getDefaultCellClass("loadingCellClass", DataRecyclerLoadingCell.class);
		return new DataRecyclerCellOrganizer(adapter, cellClass);
	}

	/**
	 * 获取下一页单元格的配置器
	 * 配置器中会被设上默认的下一页单元格
	 *
	 * @return DataRecyclerCellOrganizer
	 */
	public final static DataRecyclerCellOrganizer moreOrganizer(DataRecyclerAdapter adapter) {
		Class<?> cellClass = getDefaultCellClass("moreCellClass", DataRecyclerMoreCell.class);
		return new DataRecyclerCellOrganizer(adapter, cellClass);
	}

	/**
	 * 获取数据单元格的配置器
	 * 配置器中会被设上默认的数据单元格
	 * 
	 * @return DataRecyclerCellOrganizer
	 */
	public final static DataRecyclerCellOrganizer dataOrganizer(DataRecyclerAdapter adapter) {
		Class<?> cellClass = getDefaultCellClass("dataCellClass", DataRecyclerDataCell.class);
		return new DataRecyclerCellOrganizer(adapter, cellClass);
	}

	public static DataRecyclerCellOrganizer headerOrganizer(DataRecyclerAdapter adapter) {
		Class<?> cellClass = getDefaultCellClass("headerCellClass", DataRecyclerHeaderCell.class);
		return new DataRecyclerCellOrganizer(adapter, cellClass);
	}
}
