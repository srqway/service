package idv.hsiehpinghan.mopsservice.manager;

import idv.hsiehpinghan.mopsdao.entity.FinancialReportInstance;
import idv.hsiehpinghan.mopsdao.entity.MopsDownloadInfo;
import idv.hsiehpinghan.mopsdao.enumeration.ReportType;

import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface IMopsManager {
	boolean updateFinancialReportPresentation();

	boolean updateFinancialReportInstance();

	boolean calculateFinancialReport();

	MopsDownloadInfo getFinancialReportDownloadInfo();

//	FinancialReportInstance getFinancialReportInstance(String stockCode,
//			ReportType reportType, Integer year, Integer season);

	Map<String, ObjectNode> getFinancialReportDetailJsonMap(String stockCode,
			ReportType reportType, Integer year, Integer season);
}
