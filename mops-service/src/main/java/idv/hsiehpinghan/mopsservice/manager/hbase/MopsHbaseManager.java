package idv.hsiehpinghan.mopsservice.manager.hbase;

import idv.hsiehpinghan.hdfsassistant.utility.HdfsAssistant;
import idv.hsiehpinghan.mopsservice.manager.IMopsManager;
import idv.hsiehpinghan.mopsservice.operator.FinancialReportDownloader;
import idv.hsiehpinghan.mopsservice.property.MopsServiceProperty;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MopsHbaseManager implements IMopsManager, InitializingBean {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String hdfsXbrlDir;

	@Autowired
	private FinancialReportDownloader downloader;
	@Autowired
	private HdfsAssistant hdfsAssistant;
	@Autowired
	private MopsServiceProperty mopsServiceProperty;

	@Override
	public void afterPropertiesSet() throws Exception {
		hdfsXbrlDir = mopsServiceProperty.getHdfsXbrlDir();
	}

	@Override
	public File downloadFinancialReport() {
		try {
			File xbrlDir = downloader.downloadFinancialReport();
			logger.info(xbrlDir.getAbsolutePath() + " download finish.");
			String hdfsDir = saveFinancialReportToHdfs(xbrlDir);
			logger.info(hdfsDir + " save finish.");
			return xbrlDir;
		} catch (Exception e) {
			logger.error("Download fail !!!");
			return null;
		}
	}

	String saveFinancialReportToHdfs(File xbrlDir) throws IOException {
		return hdfsAssistant.copyFromLocal(xbrlDir, hdfsXbrlDir);
	}

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
