package idv.hsiehpinghan.stockservice.manager;

import idv.hsiehpinghan.stockdao.entity.MonthlyOperatingIncome;

import java.util.List;

public interface IMonthlyOperatingIncomeHbaseManager {
	boolean updateMonthlyOperatingIncome();

	List<MonthlyOperatingIncome> getAll(String stockCode,
			boolean isFunctionalCurrency);
}
