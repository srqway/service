package idv.hsiehpinghan.stockservice.manager;

import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface IFinancialReportManager {
	boolean updateTaxonomyPresentation();

	boolean updateXbrlInstance();

	List<String> getStockCodes();

	// boolean updateExchangeRate();

	// boolean calculateFinancialReport();
	//
	// StockDownloadInfo getFinancialReportDownloadInfo();
	//
	Map<String, ObjectNode> getFinancialReportDetailJsonMap(String stockCode,
			ReportType reportType, Integer year, Integer season);

	List<Xbrl> getAll(String stockCode, ReportType reportType);
}
