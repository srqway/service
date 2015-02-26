package idv.hsiehpinghan.stockservice.manager;

import idv.hsiehpinghan.stockdao.entity.DailyData;

public interface IStockClosingConditionManager {
	boolean updateStockClosingCondition();

	DailyData getLatestDailyData(String stockCode);
}
