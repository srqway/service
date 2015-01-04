package idv.hsiehpinghan.mopsservice.manager;

import java.io.File;

public interface IMopsManager {
	/**
	 * Download financial reports and return directory.
	 * @return
	 */
	File downloadFinancialReport();
	boolean saveFinancialReportToHdfs(File xbrlDirectory);
	boolean saveFinancialReportToDatabase(File xbrlDirectory);
}
