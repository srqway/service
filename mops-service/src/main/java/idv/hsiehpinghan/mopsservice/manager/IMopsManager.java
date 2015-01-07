package idv.hsiehpinghan.mopsservice.manager;

import java.io.File;

public interface IMopsManager {
	/**
	 * Download financial reports and return directory.
	 * @return
	 */
	File downloadFinancialReport();
//	boolean saveFinancialReportToDatabase(File xbrlDirectory);
}
