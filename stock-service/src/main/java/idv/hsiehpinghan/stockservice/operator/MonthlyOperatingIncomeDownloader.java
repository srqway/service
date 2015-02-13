package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.collectionutility.utility.ArrayUtility;
import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.datetimeutility.utility.DateUtility;
import idv.hsiehpinghan.seleniumassistant.browser.BrowserBase;
import idv.hsiehpinghan.seleniumassistant.browser.HtmlUnitFirefoxVersionBrowser;
import idv.hsiehpinghan.seleniumassistant.utility.AjaxWaitUtility;
import idv.hsiehpinghan.seleniumassistant.webelement.Div;
import idv.hsiehpinghan.seleniumassistant.webelement.Font;
import idv.hsiehpinghan.seleniumassistant.webelement.Select;
import idv.hsiehpinghan.seleniumassistant.webelement.Td;
import idv.hsiehpinghan.seleniumassistant.webelement.TextInput;
import idv.hsiehpinghan.stockdao.entity.StockInfo.RowKey;
import idv.hsiehpinghan.stockdao.repository.StockInfoRepository;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.stockservice.webelement.MonthlyOperatingIncomeDownloadTable;
import idv.hsiehpinghan.threadutility.utility.ThreadUtility;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonthlyOperatingIncomeDownloader implements InitializingBean {
	private final String COMMA_STRING = StringUtility.COMMA_STRING;
	private final String EMPTY_STRING = StringUtility.EMPTY_STRING;
	private String titleString;
	private final String YYYYMM = "yyyyMM";
	private final String HISTORY = "歷史資料";
	private final int MAX_TRY_AMOUNT = 10;
	private final Date BEGIN_DATA_DATE = generateBeginDataDate();
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private File downloadDir;
	private File downloadedLog;
	private List<String> downloadedList;
	private StringBuilder sb = new StringBuilder();

	@Autowired
	private StockServiceProperty stockServiceProperty;
	@Autowired
	private StockInfoRepository infoRepo;
	@Autowired
	private HtmlUnitFirefoxVersionBrowser browser;

	// private FirefoxBrowser browser = new FirefoxBrowser();

	@Override
	public void afterPropertiesSet() throws Exception {
		downloadDir = stockServiceProperty
				.getMonthlyOperatingIncomeDownloadDir();
		generateDownloadedLogFile();
	}

	public File downloadMonthlyOperatingIncome() throws IOException,
			IllegalAccessException, NoSuchMethodException, SecurityException,
			InstantiationException, IllegalArgumentException,
			InvocationTargetException {
		moveToTargetPage();
		downloadedList = FileUtils.readLines(downloadedLog);
		Date now = Calendar.getInstance().getTime();
		selectSearchType(HISTORY);
		for (RowKey rowKey : infoRepo.getRowKeys()) {
			String stockCode = rowKey.getStockCode();
			inputStockCode(stockCode);
			Date targetDate = BEGIN_DATA_DATE;
			while (targetDate.getTime() < now.getTime()) {
				String downloadInfo = getDownloadInfo(stockCode, targetDate);
				if (isDownloaded(downloadInfo) == false) {
					inputYear(targetDate);
					selectMonth(targetDate);
					logger.info(downloadInfo + " process start.");
					boolean hasData = repeatTryDownload(stockCode, targetDate);
					if (hasData == true) {
						logger.info(downloadInfo + " processed success.");
						writeToDownloadedFile(downloadInfo);
					} else {
						logger.info(downloadInfo + " has no data.");
					}

				}
				targetDate = DateUtils.addMonths(targetDate, 1);
			}
		}
		return downloadDir;
	}

	void moveToTargetPage() {
		final String MONTHLY_OPERATION_INCOME_PAGE_URL = "http://mops.twse.com.tw/mops/web/t05st10_ifrs";
		browser.browse(MONTHLY_OPERATION_INCOME_PAGE_URL);
		Div div = browser.getDiv(By.cssSelector("#caption"));
		AjaxWaitUtility.waitUntilTextStartWith(div, "   採用IFRSs後之月營業收入資訊");
	}

	BrowserBase getBrowser() {
		return browser;
	}

	void selectSearchType(String text) {
		Select sel = browser.getSelect(By.cssSelector("#isnew"));
		sel.selectByText(text);
		AjaxWaitUtility.waitUntilDisplayed(getYearDiv());
	}

	void inputStockCode(String stockCode) {
		TextInput input = browser.getTextInput(By.cssSelector("#co_id"));
		input.clear();
		input.inputText(stockCode);
	}

	void inputYear(Date date) {
		TextInput input = browser.getTextInput(By.cssSelector("#year"));
		input.clear();
		int year = DateUtility.getRocYear(date);
		input.inputText(String.valueOf(year));
	}

	void selectMonth(Date date) {
		Select sel = browser.getSelect(By.cssSelector("#month"));
		int month = DateUtility.getMonth(date);
		sel.selectByText(String.valueOf(month));
	}

	boolean repeatTryDownload(String stockCode, Date targetDate) {
		int tryAmount = 0;
		while (true) {
			try {
				return download(stockCode, targetDate);
			} catch (Exception e) {
				++tryAmount;
				logger.info("Download fail " + tryAmount + " times !!!");
				if (tryAmount >= MAX_TRY_AMOUNT) {
					logger.error(browser.getWebDriver().getPageSource());
					throw new RuntimeException(e);
				}
				ThreadUtility.sleep(tryAmount * 10);
			}
		}
	}

	String getFileName(String stockCode, Date date) {
		return getDownloadInfo(stockCode, date) + ".csv";
	}

	private String getTargetText(Date date) {
		String rocYear = DateUtility.getRocDateString(date, "yyyy");
		String month = DateUtility.getRocDateString(date, "MM");
		return "民國" + rocYear + "年" + month + "月";
	}

	private boolean download(String stockCode, Date targetDate)
			throws IOException {
		browser.getButton(
				By.cssSelector("td.bar01b:nth-child(4) > table:nth-child(1) > tbody:nth-child(1) > tr:nth-child(1) > td:nth-child(2) > div:nth-child(1) > div:nth-child(1) > input:nth-child(1)"))
				.click();
		try {
			Td td = browser
					.getTd(By
							.cssSelector("#table01 > table:nth-child(4) > tbody > tr > td:nth-child(2)"));
			AjaxWaitUtility.waitUntilTextEqual(td, getTargetText(targetDate));
			MonthlyOperatingIncomeDownloadTable tab = new MonthlyOperatingIncomeDownloadTable(
					browser.getTable(By
							.cssSelector("#table01 > table.hasBorder")));
			File file = writeToCsvFile(stockCode, targetDate, tab);
			logger.info(file.getAbsolutePath() + " downloaded.");
			return true;
		} catch (TimeoutException e) {
			Font font = browser.getFont(By
					.cssSelector("#table01 > center > h3:nth-child(1)"));
			if ("資料庫中查無需求資料。".equals(font.getText())) {
				return false;
			}
			throw e;
		}
	}

	private File writeToCsvFile(String stockCode, Date date,
			MonthlyOperatingIncomeDownloadTable table) throws IOException {
		File csvFile = new File(downloadDir.getAbsolutePath(), getFileName(
				stockCode, date));
		FileUtils.write(csvFile, generateCsvFileTitle(), Charsets.UTF_8, false);
		FileUtils.write(csvFile, generateCsvFileData(table), Charsets.UTF_8,
				true);
		return csvFile;
	}

	private String generateCsvFileTitle() {
		if (titleString == null) {
			titleString = ArrayUtility.toString(
					MonthlyOperatingIncomeDownloadTable.getItemNames(),
					COMMA_STRING);
		}
		return titleString + System.lineSeparator();
	}

	private String generateCsvFileData(MonthlyOperatingIncomeDownloadTable table) {
		String currentMonth = table.getCurrentMonth().replace(COMMA_STRING,
				EMPTY_STRING);
		String currentMonthOfLastYear = table.getCurrentMonthOfLastYear()
				.replace(COMMA_STRING, EMPTY_STRING);
		String differentAmount = table.getDifferentAmount().replace(
				COMMA_STRING, EMPTY_STRING);
		String differentPercent = table.getDifferentPercent().replace(
				COMMA_STRING, EMPTY_STRING);
		String cumulativeAmountOfThisYear = table
				.getCumulativeAmountOfThisYear().replace(COMMA_STRING,
						EMPTY_STRING);
		String cumulativeAmountOfLastYear = table
				.getCumulativeAmountOfLastYear().replace(COMMA_STRING,
						EMPTY_STRING);
		String cumulativeDifferentAmount = table.getCumulativeDifferentAmount()
				.replace(COMMA_STRING, EMPTY_STRING);
		String cumulativeDifferentPercent = table
				.getCumulativeDifferentPercent().replace(COMMA_STRING,
						EMPTY_STRING);
		String comment = table.getComment();
		sb.setLength(0);
		sb.append(currentMonth);
		sb.append(COMMA_STRING);
		sb.append(currentMonthOfLastYear);
		sb.append(COMMA_STRING);
		sb.append(differentAmount);
		sb.append(COMMA_STRING);
		sb.append(differentPercent);
		sb.append(COMMA_STRING);
		sb.append(cumulativeAmountOfThisYear);
		sb.append(COMMA_STRING);
		sb.append(cumulativeAmountOfLastYear);
		sb.append(COMMA_STRING);
		sb.append(cumulativeDifferentAmount);
		sb.append(COMMA_STRING);
		sb.append(cumulativeDifferentPercent);
		sb.append(COMMA_STRING);
		sb.append(comment);
		return sb.toString();
	}

	private Date generateBeginDataDate() {
		return DateUtility.getDate(2013, 1, 1);
	}

	private void generateDownloadedLogFile() throws IOException {
		if (downloadedLog == null) {
			downloadedLog = new File(downloadDir, "downloaded.log");
			if (downloadedLog.exists() == false) {
				FileUtils.touch(downloadedLog);
			}
		}
	}

	private String getDownloadInfo(String stockCode, Date date) {
		return stockCode + "_" + DateFormatUtils.format(date, YYYYMM);
	}

	private boolean isDownloaded(String downloadInfo) throws IOException {
		if (downloadedList.contains(downloadInfo)) {
			logger.info(downloadInfo + " downloaded before.");
			return true;
		}
		return false;
	}

	private void writeToDownloadedFile(String downloadInfo) throws IOException {
		String infoLine = downloadInfo + System.lineSeparator();
		FileUtils.write(downloadedLog, infoLine, Charsets.UTF_8, true);
	}

	private Div getYearDiv() {
		return browser.getDiv(By.cssSelector("#year"));
	}
}
