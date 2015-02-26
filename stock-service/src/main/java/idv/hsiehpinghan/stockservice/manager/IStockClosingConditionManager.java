package idv.hsiehpinghan.stockservice.manager;

import idv.hsiehpinghan.stockdao.entity.DailyData;

import java.util.List;

public interface IStockClosingConditionManager {
	boolean updateStockClosingCondition();

	List<DailyData> getAll(String stockCode);
}
