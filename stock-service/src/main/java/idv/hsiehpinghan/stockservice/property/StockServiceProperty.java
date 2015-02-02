package idv.hsiehpinghan.stockservice.property;

import java.io.File;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StockServiceProperty implements InitializingBean {
	private final String EXCHANGE_RATE = "exchange-rate";
	private final String FINANCIAL_REPORT = "financial-report";
	private final String STOCK_CLOSING_CONDITION = "stock-closing-condition";
	private final String TWSE = "twse";
	private final String GRETAI = "gretai";
	private String downloadDir;
	private String extractDir;

	@Autowired
	private Environment environment;

	@Override
	public void afterPropertiesSet() throws Exception {
		processDownloadDir();
		processExtractDir();
	}

	public File getStockClosingConditionDownloadDirOfTwse() {
		File dir = new File(downloadDir, STOCK_CLOSING_CONDITION);
		return new File(dir, TWSE);
	}

	public File getStockClosingConditionDownloadDirOfGretai() {
		File dir = new File(downloadDir, STOCK_CLOSING_CONDITION);
		return new File(dir, GRETAI);
	}
	
	public File getExchangeRateDownloadDir() {
		return new File(downloadDir, EXCHANGE_RATE);
	}

	public File getFinancialReportDownloadDir() {
		return new File(downloadDir, FINANCIAL_REPORT);
	}

	public File getFinancialReportExtractDir() {
		return new File(extractDir, FINANCIAL_REPORT);
	}

	private void processDownloadDir() {
		String pDownloadDir = "stock-service.download_dir";
		downloadDir = environment.getProperty(pDownloadDir);
		if (downloadDir == null) {
			throw new RuntimeException(pDownloadDir + " not set !!!");
		}
	}

	private void processExtractDir() {
		String pExtractDir = "stock-service.extract_dir";
		extractDir = environment.getProperty(pExtractDir);
		if (extractDir == null) {
			throw new RuntimeException(pExtractDir + " not set !!!");
		}
	}
}
