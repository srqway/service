package idv.hsiehpinghan.stockservice.manager;

public interface IFinancialReportManager {
	boolean updateTaxonomyPresentation();

	boolean updateXbrlInstance();

	// boolean updateExchangeRate();

	boolean calculateFinancialReport();
	//
	// StockDownloadInfo getFinancialReportDownloadInfo();
	//
	// Map<String, ObjectNode> getFinancialReportDetailJsonMap(String stockCode,
	// ReportType reportType, Integer year, Integer season);
}
