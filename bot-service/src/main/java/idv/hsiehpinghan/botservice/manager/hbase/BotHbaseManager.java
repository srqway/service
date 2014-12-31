package idv.hsiehpinghan.botservice.manager.hbase;

import idv.hsiehpinghan.botservice.manager.IBotManager;

import java.io.File;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class BotHbaseManager implements IBotManager {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	

	public File downloadExchangeRate() {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean saveExchangeRateToDatabase(File dataDirectory) {
		// TODO Auto-generated method stub
		return false;
	}
	
	//
	// @Autowired
	// private FinancialReportDownloader downloader;
	//
	// @Override
	// public File downloadFinancialReport() {
	// try {
	// File xbrlDir = downloader.downloadFinancialReport();
	// logger.info(xbrlDir.getAbsolutePath() + " download finish.");
	// return xbrlDir;
	// } catch (Exception e) {
	// logger.error("Download fail !!!");
	// return null;
	// }
	// }
	//
	// @Override
	// public boolean saveFinancialReportToDatabase(File xbrlDirectory) {
	// processSubFiles(xbrlDirectory);
	// return false;
	// }
	//
	// private void processSubFiles(File file) {
	// if (file.isDirectory()) {
	// File[] fs = file.listFiles();
	// for (File f : fs) {
	// processSubFiles(f);
	// }
	// } else {
	// // xbrlAssistant.get
	// System.err.println(file.getAbsolutePath());
	// }
	//
	// }

}
