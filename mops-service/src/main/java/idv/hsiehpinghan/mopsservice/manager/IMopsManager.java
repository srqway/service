package idv.hsiehpinghan.mopsservice.manager;

import idv.hsiehpinghan.mopsdao.entity.MopsDownloadInfo;

public interface IMopsManager {
	boolean updateFinancialReportPresentation();

	boolean updateFinancialReportInstance();

	boolean calculateFinancialReport();

	MopsDownloadInfo getFinancialReportDownloadInfo();
}
