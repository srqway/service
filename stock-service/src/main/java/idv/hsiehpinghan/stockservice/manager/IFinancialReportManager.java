package idv.hsiehpinghan.stockservice.manager;

import idv.hsiehpinghan.stockdao.entity.StockDownloadInfo;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface IFinancialReportManager {
	boolean updateFinancialReportPresentation();

	boolean updateFinancialReportInstance();

	boolean updateExchangeRate();

	boolean calculateFinancialReport();

	StockDownloadInfo getFinancialReportDownloadInfo();

	Map<String, ObjectNode> getFinancialReportDetailJsonMap(String stockCode,
			ReportType reportType, Integer year, Integer season);
}
