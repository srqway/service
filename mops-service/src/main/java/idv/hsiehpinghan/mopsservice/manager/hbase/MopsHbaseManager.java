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
	private FinancialReportDownloader downloader;

	@Override
	public File downloadFinancialReport() {
		try {
			File xbrlDir = downloader.downloadFinancialReport();
			logger.info(xbrlDir.getAbsolutePath() + " download finish.");
			return xbrlDir;
		} catch (Exception e) {
			logger.error("Download fail !!!");
			return null;
		}
	}

	@Override
	public boolean saveFinancialReportToDatabase(File xbrlDirectory) {
		processSubFiles(xbrlDirectory);
		return false;
	}

	private void processSubFiles(File file) {
		if (file.isDirectory()) {
			File[] fs = file.listFiles();
			for (File f : fs) {
				processSubFiles(f);
			}
		} else {
			// xbrlAssistant.get
			System.err.println(file.getAbsolutePath());
		}

	}
}
