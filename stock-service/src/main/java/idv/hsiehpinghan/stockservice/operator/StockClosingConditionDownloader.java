package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.datetimeutility.utility.DateUtility;
import idv.hsiehpinghan.seleniumassistant.browser.BrowserBase;
import idv.hsiehpinghan.seleniumassistant.browser.HtmlUnitFirefoxVersionBrowser;
import idv.hsiehpinghan.seleniumassistant.utility.AjaxWaitUtility;
import idv.hsiehpinghan.seleniumassistant.webelement.Div;
import idv.hsiehpinghan.seleniumassistant.webelement.Select;
import idv.hsiehpinghan.seleniumassistant.webelement.TextInput;
import idv.hsiehpinghan.threadutility.utility.ThreadUtility;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockClosingConditionDownloader implements InitializingBean {
	private final String YYYYMMDD = "yyyyMMdd";
	private final String ALL = "全部";
	private final int MAX_TRY_AMOUNT = 3;
	private final Date BEGIN_DATA_DATE = generateBeginDataDate();
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private File downloadDir;
	private File controlFile;

	@Autowired
	private HtmlUnitFirefoxVersionBrowser browser;
	// private HtmlUnitWithJavascriptBrowser browser;
	// private FirefoxBrowser browser;
	@Autowired
	private StockServiceProperty stockServiceProperty;

	@Override
	public void afterPropertiesSet() throws Exception {
		downloadDir = new File(stockServiceProperty.getDownloadDir(),
				"closing-condition");
		generateControlFile();
	}

	public File downloadStockClosingCondition() throws IOException {
		moveToTargetPage();
		Date now = Calendar.getInstance().getTime();
		Date targetDate = BEGIN_DATA_DATE;
		while (targetDate.getTime() < now.getTime()) {
			String downloadInfo = getDownloadInfo(targetDate);
			if (isDownloaded(downloadInfo) == false) {
				inputDataDate(targetDate);
				selectType(ALL);
				repeatTryDownload(targetDate);
				writeToControlFile(downloadInfo);
			}
			targetDate = DateUtils.addDays(targetDate, 1);
		}
		return downloadDir;
	}

	void moveToTargetPage() {
		final String STOCK_CLOSING_CONDITION_PAGE_URL = "http://www.twse.com.tw/ch/trading/exchange/MI_INDEX/MI_INDEX.php";
		browser.browse(STOCK_CLOSING_CONDITION_PAGE_URL);
		Div div = browser.getDiv(By.id("breadcrumbs"));
		AjaxWaitUtility.waitUntilDivTextStartWith(div,
				"首頁 > 交易資訊 > 盤後資訊 > 每日收盤行情");
	}

	BrowserBase getBrowser() {
		return browser;
	}

	void inputDataDate(Date date) {
		TextInput dataDateInput = browser.getTextInput(By.id("date-field"));
		dataDateInput.clear();
		String dateStr = DateUtility.getRocDateString(date, "yyyy/MM/dd");
		dataDateInput.inputText(dateStr);
	}

	void selectType(String text) {
		Select typeSel = browser.getSelect(By
				.cssSelector("#main-content > form > select"));
		typeSel.selectByText(text);
	}

	void repeatTryDownload(Date targetDate) {
		int tryAmount = 0;
		while (true) {
			try {
				downloadCsv(targetDate);
				break;
			} catch (Exception e) {
				++tryAmount;
				logger.warn("Download fail " + tryAmount + " times !!!");
				logger.warn(browser.getWebDriver().getPageSource());
				if (tryAmount >= MAX_TRY_AMOUNT) {
					throw new RuntimeException(e);
				}
				ThreadUtility.sleep(tryAmount * 10);
			}
		}
	}

	String getFileName(String str) {
		int idxBegin = str.indexOf("=") + 1;
		return str.substring(idxBegin);
	}

	private void downloadCsv(Date targetDate) {
		browser.cacheCurrentPage();
		try {
			browser.getButton(By.cssSelector(".dl-csv")).click();
			String fileName = getFileName(browser.getAttachment());
			File f = browser.download(downloadDir.getAbsolutePath() + "/"
					+ fileName);
			logger.info(f.getAbsolutePath() + " downloaded.");
			browser.restorePage();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			browser.restorePage();
		}
	}

	private Date generateBeginDataDate() {
		return DateUtility.getDate(2013, 1, 1);
	}

	private void generateControlFile() throws IOException {
		if (controlFile == null) {
			controlFile = new File(downloadDir, "control_file");
			if (controlFile.exists() == false) {
				FileUtils.touch(controlFile);
			}
		}
	}

	private String getDownloadInfo(Date date) {
		return DateFormatUtils.format(date, YYYYMMDD);
	}

	private boolean isDownloaded(String downloadInfo) throws IOException {
		List<String> downloadedList = FileUtils.readLines(controlFile);
		if (downloadedList.contains(downloadInfo)) {
			logger.info(downloadInfo + " downloaded before.");
			return true;
		}
		return false;
	}

	private void writeToControlFile(String downloadInfo) throws IOException {
		String infoLine = downloadInfo + System.lineSeparator();
		FileUtils.write(controlFile, infoLine, Charsets.UTF_8, true);
	}
}
