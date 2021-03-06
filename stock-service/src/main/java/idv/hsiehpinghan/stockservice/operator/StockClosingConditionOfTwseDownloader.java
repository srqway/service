package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.collectionutility.utility.ArrayUtility;
import idv.hsiehpinghan.datetimeutility.utility.DateUtility;
import idv.hsiehpinghan.resourceutility.utility.FileUtility;
import idv.hsiehpinghan.seleniumassistant.browser.BrowserBase;
import idv.hsiehpinghan.seleniumassistant.browser.HtmlUnitBrowser;
import idv.hsiehpinghan.seleniumassistant.pool.HtmlUnitBrowserPool;
import idv.hsiehpinghan.seleniumassistant.utility.AjaxWaitUtility;
import idv.hsiehpinghan.seleniumassistant.webelement.Div;
import idv.hsiehpinghan.seleniumassistant.webelement.Select;
import idv.hsiehpinghan.seleniumassistant.webelement.TextInput;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.threadutility.utility.ThreadUtility;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class StockClosingConditionOfTwseDownloader implements InitializingBean {
	private File downloadDir;
	private File repositoryDir;

	@Autowired
	private StockServiceProperty stockServiceProperty;

	@Override
	public void afterPropertiesSet() throws Exception {
		downloadDir = stockServiceProperty
				.getStockClosingConditionDownloadDirOfTwse();
		repositoryDir = stockServiceProperty
				.getStockClosingConditionRepositoryDirOfTwse();
		// generateDownloadedLogFile();
	}

	@Component
	@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	static class RunnableDownloader implements Runnable {
		private static final String YYYYMMDD = "yyyyMMdd";
		private static final String ALL = "全部(不含權證、牛熊證、可展延牛熊證)";
		private static final Logger logger = Logger
				.getLogger(RunnableDownloader.class.getName());
		private static final int MAX_TRY_AMOUNT = 3;
		private final File DOWNLOAD_DIR;
		private final File REPOSITORY_DIR;
		private final Date BEGIN_DATE;
		private final Date END_DATE;
		private Set<String> downloadedSet;
		private HtmlUnitBrowser browser;
		@Autowired
		private HtmlUnitBrowserPool pool;

		RunnableDownloader(File downloadDir, File repositoryDir,
				Date beginDate, Date endDate) {
			super();
			this.DOWNLOAD_DIR = downloadDir;
			this.REPOSITORY_DIR = repositoryDir;
			this.BEGIN_DATE = beginDate;
			this.END_DATE = endDate;
		}

		@PostConstruct
		public void postConstruct() throws Exception {
			browser = pool.borrowObject();
		}

		@PreDestroy
		public void preDestroy() throws Exception {
			pool.returnObject(browser);
		}

		@Override
		public void run() {
			moveToTargetPage(browser);
			downloadedSet = getDownloadedSetByMonth(BEGIN_DATE, END_DATE,
					DOWNLOAD_DIR, REPOSITORY_DIR);
			Date targetDate = BEGIN_DATE;
			while (targetDate.getTime() < END_DATE.getTime()) {
				String fileName = generateFileName(targetDate);
				if (downloadedSet.contains(fileName) == false) {
					inputDataDate(targetDate);
					selectType(ALL);
					logger.info(fileName + " process start.");
					repeatTryDownload(targetDate);
					logger.info(fileName + " processed success.");
					writeToDownloadedSet(fileName);
				}
				targetDate = DateUtils.addDays(targetDate, 1);
			}

		}

		void selectType(String text) {
			Select typeSel = browser.getSelect(By
					.cssSelector("#main-content > form > select"));
			typeSel.selectByText(text);
		}

		void inputDataDate(Date date) {
			TextInput dataDateInput = browser.getTextInput(By.id("date-field"));
			dataDateInput.clear();
			String dateStr = DateUtility.getRocDateString(date, "yyyy/MM/dd");
			dataDateInput.inputText(dateStr);
		}

		static void moveToTargetPage(BrowserBase browser) {
			final String STOCK_CLOSING_CONDITION_PAGE_URL = "http://www.twse.com.tw/ch/trading/exchange/MI_INDEX/MI_INDEX.php";
			browser.browse(STOCK_CLOSING_CONDITION_PAGE_URL);
			Div div = browser.getDiv(By.id("breadcrumbs"));
			AjaxWaitUtility.waitUntilTextStartWith(div,
					"首頁 > 交易資訊 > 盤後資訊 > 每日收盤行情");
		}

		void repeatTryDownload(Date targetDate) {
			int tryAmount = 0;
			while (true) {
				try {
					downloadCsv(targetDate);
					break;
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

		String getContentDispositionFileName(String contentDisposition) {
			int idxBegin = contentDisposition.indexOf("=") + 1;
			return contentDisposition.substring(idxBegin);
		}

		private void validateContentDispositionFileName(Date targetDate) {
			String contentDispositionFileName = getContentDispositionFileName(browser
					.getContentDisposition());
			// ex. A11220130102MS2.csv
			String expectedFileName = String.format(
					"A112%1$tY%1$tm%1$tdMS2.csv", targetDate);
			if (expectedFileName.equals(contentDispositionFileName) == false) {
				throw new RuntimeException("Content disposition file name("
						+ contentDispositionFileName
						+ ") not equals expected file name(" + expectedFileName
						+ ") !!!");
			}
		}

		private void downloadCsv(Date targetDate) {
			browser.cacheCurrentPage();
			try {
				browser.getButton(By.cssSelector(".dl-csv")).click();
				validateContentDispositionFileName(targetDate);
				File dir = getOrCreateDownloadDirectoryByMonth(DOWNLOAD_DIR,
						targetDate);
				String fileName = generateFileName(targetDate);
				File file = new File(dir, fileName);
				browser.download(file);
				logger.info(file.getAbsolutePath() + " downloaded.");
				browser.restorePage();
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				browser.restorePage();
			}
		}

		private void writeToDownloadedSet(String fileName) {
			downloadedSet.add(fileName);
		}

		private String generateFileName(Date date) {
			return String.format("%s.csv",
					DateFormatUtils.format(date, YYYYMMDD));
		}

		private static Set<String> getDownloadedSetByMonth(Date beginDate,
				Date endDate, File downloadDir, File repositoryDir) {
			Date targetDate = beginDate;
			Set<String> downloadedSet = new HashSet<String>();
			while (targetDate.getTime() < endDate.getTime()) {
				String[] downloadeds = getOrCreateDownloadDirectoryByMonth(
						downloadDir, targetDate).list();
				String[] repositorys = getOrCreateRepositoryDirectoryByMonth(
						repositoryDir, targetDate).list();
				String[] all = ArrayUtils.addAll(downloadeds, repositorys);
				HashSet<String> set = ArrayUtility.asHashSet(all);
				downloadedSet.addAll(set);
				targetDate = DateUtils.addMonths(targetDate, 1);
			}
			return downloadedSet;
		}

		private static File getOrCreateDownloadDirectoryByMonth(
				File downloadDir, Date date) {
			int year = DateUtility.getYear(date);
			int month = DateUtility.getMonth(date);
			return FileUtility.getOrCreateDirectory(downloadDir,
					String.valueOf(year), String.valueOf(month));
		}

		private static File getOrCreateRepositoryDirectoryByMonth(
				File repositoryDir, Date date) {
			int year = DateUtility.getYear(date);
			int month = DateUtility.getMonth(date);
			return FileUtility.getOrCreateDirectory(repositoryDir,
					String.valueOf(year), String.valueOf(month));
		}

	}

}

//
// @Service
// @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// public class StockClosingConditionOfTwseDownloader implements
// InitializingBean {
// private final Charset UTF_8 = CharsetUtility.UTF_8;
// private final String YYYYMMDD = "yyyyMMdd";
// private final String ALL = "全部(不含權證、牛熊證、可展延牛熊證)";
// private final int MAX_TRY_AMOUNT = 3;
// private final Date BEGIN_DATA_DATE = DateUtility.getDate(2015, 3, 25);
// private final int RUNNABLE_AMOUT = 2;
// private Logger logger = Logger.getLogger(this.getClass().getName());
// private File downloadDir;
// private File repositoryDir;
// // private File downloadedLog;
// // private Set<String> downloadedSet;
//
// // @Autowired
// // private HtmlUnitFirefoxVersionBrowser browser;
// // private HtmlUnitWithJavascriptBrowser browser;
// // private FirefoxBrowser browser;
// @Autowired
// private StockServiceProperty stockServiceProperty;
//
// @Override
// public void afterPropertiesSet() throws Exception {
// downloadDir = stockServiceProperty
// .getStockClosingConditionDownloadDirOfTwse();
// repositoryDir = stockServiceProperty
// .getStockClosingConditionRepositoryDirOfTwse();
// // generateDownloadedLogFile();
// }
//
// class RunnableDownloader implements Runnable {
// private final String STOCK_CLOSING_CONDITION_PAGE_URL =
// "http://www.twse.com.tw/ch/trading/exchange/MI_INDEX/MI_INDEX.php";
// private HtmlUnitFirefoxVersionBrowser browser;
// private Date beginDate;
// private Date endDate;
// private Set<String> downloadedSet;
//
//
// RunnableDownloader(Date beginDate, Date endDate) {
// super();
// this.beginDate = beginDate;
// this.endDate = endDate;
// }
//
// @Override
// public void run() {
// aaa
//
// downloadedSet = getDownloadedSetByMonth(beginDate, endDate);
//
// Date targetDate = beginDate;
//
// }
//
// void moveToTargetPage() {
//
// browser.browse(STOCK_CLOSING_CONDITION_PAGE_URL);
// Div div = browser.getDiv(By.id("breadcrumbs"));
// AjaxWaitUtility.waitUntilTextStartWith(div,
// "首頁 > 交易資訊 > 盤後資訊 > 每日收盤行情");
// }
//
//
// }
//
// public File downloadStockClosingCondition() throws IOException {
// // moveToTargetPage();
// // downloadedSet = FileUtility.readLinesAsHashSet(downloadedLog);
// // Date now = Calendar.getInstance().getTime();
//
// // Date targetDate = BEGIN_DATA_DATE;
// // while (targetDate.getTime() < now.getTime()) {
// // // String downloadInfo = getDownloadInfo(targetDate);
// // String targetFileName = generateFileName(targetDate);
// // if (isDownloaded(targetFileName) == false) {
// // inputDataDate(targetDate);
// // selectType(ALL);
// // logger.info(downloadInfo + " process start.");
// // repeatTryDownload(targetDate);
// // logger.info(downloadInfo + " processed success.");
// // writeToDownloadedFileAndSet(downloadInfo);
// // }
// // targetDate = DateUtils.addDays(targetDate, 1);
// // }
// return downloadDir;
// }
//
// // private Set<Runnable> generateRunnables() {
// // Set<Runnable> runnables = new HashSet<Runnable>(RUNNABLE_AMOUT);
// // Date targetDate = BEGIN_DATA_DATE;
// // Date now = Calendar.getInstance().getTime();
// // while (targetDate.getTime() < now.getTime()) {
// // final Date date = targetDate;
// // Runnable runnable = new Runnable() {
// // public void run() {
// // Set<String> downloadedSet = getDownloadedSetByMonth(date);
// // while (en.hasMoreElements()) {
// // type type = (type) en.nextElement();
// //
// // }
// //
// // String targetFileName = generateFileName(date);
// // if (isDownloaded(targetFileName) == false) {
// // inputDataDate(targetDate);
// // selectType(ALL);
// // logger.info(downloadInfo + " process start.");
// // repeatTryDownload(targetDate);
// // logger.info(downloadInfo + " processed success.");
// // writeToDownloadedFileAndSet(downloadInfo);
// // }
// // }
// // };
// // runnables.add(runnable);
// // targetDate = DateUtils.addMonths(targetDate, 1);
// // }
// //
// // return runnables;
// // }
//
// private Set<String> getDownloadedSetByMonth(Date beginDate, Date endDate) {
// Date targetDate = beginDate;
// Set<String> downloadedSet = new HashSet<String>();
// while (targetDate.getTime() < endDate.getTime()) {
// String[] downloadeds = getOrCreateDownloadDirectoryByMonth(
// targetDate).list();
// String[] repositorys = getOrCreateRepositoryDirectoryByMonth(
// targetDate).list();
// String[] all = ArrayUtils.addAll(downloadeds, repositorys);
// HashSet<String> set = ArrayUtility.asHashSet(all);
// downloadedSet.addAll(set);
// targetDate = DateUtils.addMonths(targetDate, 1);
// }
// return downloadedSet;
// }
//
// private File getOrCreateDownloadDirectoryByMonth(Date date) {
// int year = DateUtility.getYear(date);
// int month = DateUtility.getMonth(date);
// return FileUtility.getOrCreateDirectory(downloadDir,
// String.valueOf(year), String.valueOf(month));
// }
//
// private File getOrCreateRepositoryDirectoryByMonth(Date date) {
// int year = DateUtility.getYear(date);
// int month = DateUtility.getMonth(date);
// return FileUtility.getOrCreateDirectory(repositoryDir,
// String.valueOf(year), String.valueOf(month));
// }
//
// // void moveToTargetPage() {
// // final String STOCK_CLOSING_CONDITION_PAGE_URL =
// // "http://www.twse.com.tw/ch/trading/exchange/MI_INDEX/MI_INDEX.php";
// // browser.browse(STOCK_CLOSING_CONDITION_PAGE_URL);
// // Div div = browser.getDiv(By.id("breadcrumbs"));
// // AjaxWaitUtility
// // .waitUntilTextStartWith(div, "首頁 > 交易資訊 > 盤後資訊 > 每日收盤行情");
// // }
//
// // BrowserBase getBrowser() {
// // return browser;
// // }
// //
// // void inputDataDate(Date date) {
// // TextInput dataDateInput = browser.getTextInput(By.id("date-field"));
// // dataDateInput.clear();
// // String dateStr = DateUtility.getRocDateString(date, "yyyy/MM/dd");
// // dataDateInput.inputText(dateStr);
// // }
// //
// // void selectType(String text) {
// // Select typeSel = browser.getSelect(By
// // .cssSelector("#main-content > form > select"));
// // typeSel.selectByText(text);
// // }
// //
// // void repeatTryDownload(Date targetDate) {
// // int tryAmount = 0;
// // while (true) {
// // try {
// // downloadCsv(targetDate);
// // break;
// // } catch (Exception e) {
// // ++tryAmount;
// // logger.info("Download fail " + tryAmount + " times !!!");
// // if (tryAmount >= MAX_TRY_AMOUNT) {
// // logger.error(browser.getWebDriver().getPageSource());
// // throw new RuntimeException(e);
// // }
// // ThreadUtility.sleep(tryAmount * 10);
// // }
// // }
// // }
// //
// // // String getFileName(String str) {
// // // int idxBegin = str.indexOf("=") + 1;
// // // return str.substring(idxBegin);
// // // }
// //
// // private String generateFileName(Date date) {
// // return String.format("%s.csv", DateFormatUtils.format(date, YYYYMMDD));
// // }
// // private void downloadCsv(Date targetDate) {
// // browser.cacheCurrentPage();
// // try {
// // browser.getButton(By.cssSelector(".dl-csv")).click();
// // String fileName = getFileName(browser.getAttachment());
// // File file = new File(downloadDir.getAbsolutePath(), fileName);
// // browser.download(file);
// // logger.info(file.getAbsolutePath() + " downloaded.");
// // browser.restorePage();
// // } catch (Exception e) {
// // throw new RuntimeException(e);
// // } finally {
// // browser.restorePage();
// // }
// // }
//
// // private void generateDownloadedLogFile() throws IOException {
// // if (downloadedLog == null) {
// // downloadedLog = new File(downloadDir, "downloaded.log");
// // if (downloadedLog.exists() == false) {
// // FileUtils.touch(downloadedLog);
// // }
// // }
// // }
//
// // private String getDownloadInfo(Date date) {
// // return DateFormatUtils.format(date, YYYYMMDD);
// // }
//
// // private String getDownloadInfo(Date date) {
// // return DateFormatUtils.format(date, YYYYMMDD);
// // }
//
// // private boolean isDownloaded(String fileName) throws IOException {
// // if (downloadedSet.contains(downloadInfo)) {
// // logger.info(downloadInfo + " downloaded before.");
// // return true;
// // }
// // return false;
// // }
// //
// // private void writeToDownloadedFileAndSet(String downloadInfo)
// // throws IOException {
// // String infoLine = downloadInfo + System.lineSeparator();
// // FileUtils.write(downloadedLog, infoLine, UTF_8, true);
// // downloadedSet.add(downloadInfo);
// // }
// }

