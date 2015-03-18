package idv.hsiehpinghan.stockservice.manager;

import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface IFinancialReportManager {
	boolean updateTaxonomyPresentation();

	boolean updateXbrlInstance();

	boolean updateAnalyzedData() throws IOException;

	TreeSet<String> getStockCodes();

	// boolean updateExchangeRate();

	// boolean calculateFinancialReport();
	//
	// StockDownloadInfo getFinancialReportDownloadInfo();
	//
	Map<String, ObjectNode> getFinancialReportDetailJsonMap(String stockCode,
			ReportType reportType, Integer year, Integer season, Locale locale);

	TreeSet<Xbrl> getAll(String stockCode, ReportType reportType);

}
