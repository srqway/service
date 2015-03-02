package idv.hsiehpinghan.stockservice.manager;

import idv.hsiehpinghan.stockdao.entity.MonthlyData;

import java.util.List;

public interface IMonthlyOperatingIncomeHbaseManager {
	boolean updateMonthlyOperatingIncome();
	
	List<MonthlyData> getAll(String stockCode);
}
