package idv.hsiehpinghan.mopsservice.manager.hbase;

import idv.hsiehpinghan.hdfsassistant.utility.HdfsAssistant;
import idv.hsiehpinghan.mopsservice.manager.IMopsManager;
import idv.hsiehpinghan.mopsservice.operator.FinancialReportDownloader;

import java.io.File;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MopsHbaseManager implements IMopsManager {
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private String hdfsDir = "hdfs://localhost/user/centos/mops/xbrl";
	
	@Autowired
	private FinancialReportDownloader downloader;
	@Autowired
	private HdfsAssistant hdfsAssistant;
	
	@Override
	public File downloadFinancialReport() {
		try {
			File xbrlDir = downloader.downloadFinancialReport();
			logger.info(xbrlDir.getAbsolutePath() + " download finish.");
			hdfsAssistant.writeHdfsDirectory(hdfsDir, xbrlDir);
			logger.info(hdfsDir + " save finish.");
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

	@Override
	public boolean saveFinancialReportToHdfs(File xbrlDirectory) {
		return false;
	}
}
