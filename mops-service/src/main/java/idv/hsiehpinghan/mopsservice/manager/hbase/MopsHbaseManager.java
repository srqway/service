package idv.hsiehpinghan.mopsservice.manager.hbase;

import idv.hsiehpinghan.mopsservice.manager.IMopsManager;
import idv.hsiehpinghan.mopsservice.operator.FinancialReportDownloader;

import java.io.File;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MopsHbaseManager implements IMopsManager {
	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	FinancialReportDownloader downloader;

	@Override
	public boolean downloadFinancialReport() {
		try {
			File dir = downloader.downloadFinancialReport();
			logger.info(dir.getAbsolutePath() + " download finish.");
			return true;
		} catch (Exception e) {
			logger.error("Download fail !!!");
			return false;
		}
	}

	@Override
	public boolean saveFinancialReportToDatabase() {
		// TODO Auto-generated method stub
		return false;
	}

}
