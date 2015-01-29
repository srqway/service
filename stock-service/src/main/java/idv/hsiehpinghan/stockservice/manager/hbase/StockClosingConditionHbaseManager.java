package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.stockservice.manager.IStockClosingConditionManager;
import idv.hsiehpinghan.stockservice.operator.StockClosingConditionDownloader;

import java.io.File;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockClosingConditionHbaseManager implements IStockClosingConditionManager {
	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private StockClosingConditionDownloader downloader;

	@Override
	public boolean updateStockClosingCondition() {
		File dir = downloadStockClosingCondition();
		if (dir == null) {
			return false;
		}
		// try {
		// int processFilesAmt = saveStockClosingCondition(dir);
		// logger.info("Saved " + processFilesAmt + " files to "
		// + instanceRepo.getTargetTableName() + ".");
		// } catch (Exception e) {
		// logger.error("Save financial report to hbase fail !!!");
		// e.printStackTrace();
		// return false;
		// }
		return true;
	}

	private File downloadStockClosingCondition() {
		try {
			File dir = downloader.downloadStockClosingCondition();
			logger.info(dir.getAbsolutePath() + " download finish.");
			return dir;
		} catch (Exception e) {
			logger.error("Download stock closing condition fail !!!");
			return null;
		}
	}

}
