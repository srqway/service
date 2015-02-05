package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.seleniumassistant.browser.BrowserBase;
import idv.hsiehpinghan.seleniumassistant.browser.HtmlUnitFirefoxVersionBrowser;
import idv.hsiehpinghan.seleniumassistant.utility.AjaxWaitUtility;
import idv.hsiehpinghan.seleniumassistant.webelement.Button;
import idv.hsiehpinghan.seleniumassistant.webelement.Font;
import idv.hsiehpinghan.seleniumassistant.webelement.Select;
import idv.hsiehpinghan.seleniumassistant.webelement.Select.Option;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.stockservice.utility.StockAjaxWaitUtility;
import idv.hsiehpinghan.stockservice.webelement.XbrlDownloadTable;
import idv.hsiehpinghan.threadutility.utility.ThreadUtility;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Response for download xbrl instance files from stock.
 * 
 * @author thank.hsiehpinghan
 *
 */
@Service
public class FinancialReportDownloader implements InitializingBean {
	private final String NO_DATA_MSG_CSS_SELECTOR = "#table01 > h4 > font";
	private final String NO_DATA_MSG = "查無符合資料！";
	private final int MAX_TRY_AMOUNT = 3;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private File downloadDir;
	private File downloadedLog;
	private List<String> downloadedList;

	@Autowired
	private HtmlUnitFirefoxVersionBrowser browser;
	@Autowired
	private FinancialReportUnzipper unzipper;
	@Autowired
	private StockServiceProperty stockServiceProperty;

	@Override
	public void afterPropertiesSet() throws Exception {
		downloadDir = stockServiceProperty.getFinancialReportDownloadDir();
		generateDownloadedLogFile();
	}

	/**
	 * Download financial report.
	 * 
	 * @return
	 * @throws IOException
	 */
	public File downloadFinancialReport() throws IOException {
		moveToTargetPage();
		downloadedList = FileUtils.readLines(downloadedLog);
		List<Option> mkOpts = getMarketTypeSelect().getOptions();
		for (int iMk = mkOpts.size() - 1; iMk >= 0; --iMk) {
			Option mkOpt = mkOpts.get(iMk);
			if (isTargetMarketType(mkOpt.getText().trim()) == false) {
				continue;
			}
			List<Option> oldIndOpts = getIndustryTypeSelect().getOptions();
			mkOpt.click();
			List<Option> indOpts = getIndustryOptions(oldIndOpts);
			for (int iInd = indOpts.size() - 1; iInd >= 0; --iInd) {
				Option indOpt = indOpts.get(iInd);
				if (isTargetIndustryType(indOpt.getText().trim()) == false) {
					continue;
				}
				indOpt.click();
				List<Option> yearOpts = getYearSelect().getOptions();
				for (int iYear = 0, yearSize = yearOpts.size(); iYear < yearSize; ++iYear) {
					Option yearOpt = yearOpts.get(iYear);
					yearOpt.click();
					List<Option> seasonOpts = getSeasonSelect().getOptions();
					for (int iSeason = 0, seasonSize = seasonOpts.size(); iSeason < seasonSize; ++iSeason) {
						Option seasonOpt = seasonOpts.get(iSeason);
						seasonOpt.click();
						List<Option> reportTypeOpts = getReportTypeSelect()
								.getOptions();
						for (int iRep = 0, repSize = reportTypeOpts.size(); iRep < repSize; ++iRep) {
							Option repOpt = reportTypeOpts.get(iRep);
							String downloadInfo = getDownloadInfo(mkOpt,
									indOpt, yearOpt, seasonOpt, repOpt);
							if (isDownloaded(downloadInfo)) {
								continue;
							}
							// ex : 2013-01-otc-02-C.zip
							String targetFileNamePrefix = getTargetFileNameRegex(
									yearOpt, seasonOpt, mkOpt, repOpt);
							logger.info(downloadInfo + " process start.");
							repeatTryDownload(reportTypeOpts, iRep,
									targetFileNamePrefix);
							logger.info(downloadInfo + " processed success.");
							writeToDownloadedFile(downloadInfo);
						}
					}
				}
			}
		}
		return unzipper.getExtractDir();
	}

	Select getMarketTypeSelect() {
		return browser.getSelect(By.id("MAR_KIND"));
	}

	void moveToTargetPage() {
		final String FINANCIAL_REPORT_PAGE_URL = "http://mops.twse.com.tw/mops/web/t164sb02";
		browser.browse(FINANCIAL_REPORT_PAGE_URL);
	}

	BrowserBase getBrowser() {
		return browser;
	}

	private void generateDownloadedLogFile() throws IOException {
		if (downloadedLog == null) {
			downloadedLog = new File(downloadDir, "downloaded.log");
			if (downloadedLog.exists() == false) {
				FileUtils.touch(downloadedLog);
			}
		}
	}

	private boolean isTargetMarketType(String text) {
		if ("上市".equals(text) == true) {
			return true;
		}
		if ("上櫃".equals(text) == true) {
			return true;
		}
		return false;
	}

	private boolean isTargetIndustryType(String text) {
		if ("".equals(text)) {
			return true;
		}
		return false;
	}

	private List<Option> getIndustryOptions(List<Option> oldIndustryOptions) {
		Select sel = getIndustryTypeSelect();
		AjaxWaitUtility.waitUntilOptionsDifferent(sel, oldIndustryOptions);
		return sel.getOptions();
	}

	private Select getIndustryTypeSelect() {
		return browser.getSelect(By.id("CODE"));
	}

	private Select getYearSelect() {
		return browser.getSelect(By.id("SYEAR"));
	}

	private Select getSeasonSelect() {
		return browser.getSelect(By.id("SSEASON"));
	}

	private Select getReportTypeSelect() {
		return browser.getSelect(By.id("REPORT_ID"));
	}

	private Button getSearchButton() {
		return browser.getButton(By
				.cssSelector("#search_bar1 > div > input[type='button']"));
	}

	private void downLoad(XbrlDownloadTable table) {
		// i = 0 is title.
		for (int i = 1, size = table.getRowSize(); i < size; ++i) {
			if (isDownloaded(table, i)) {
				continue;
			}
			browser.cacheCurrentPage();
			try {
				table.clickDownloadButton(i);
				String fileName = browser.getAttachmentFileName();
				File file = new File(downloadDir.getAbsolutePath(), fileName);
				browser.download(file);
				logger.info(file.getAbsolutePath() + " downloaded.");
				unzipper.repeatTryUnzip(file);
			} finally {
				browser.restorePage();
			}
		}
	}

	private boolean isDownloaded(XbrlDownloadTable table, int rowIdx) {
		String fileName = table.getDownloadFileName(rowIdx);
		// Some industry type has no data.
		if (fileName == null) {
			return true;
		}
		String[] fns = downloadDir.list();
		if (fns == null) {
			return false;
		}
		for (String fn : fns) {
			if (fn.equals(fileName)) {
				return true;
			}
		}
		return false;
	}

	private XbrlDownloadTable waitAjaxTableReload(String targetFileNamePrefix) {
		XbrlDownloadTable tab = new XbrlDownloadTable(browser.getTable(By
				.cssSelector(".hasBorder")));
		boolean result = StockAjaxWaitUtility
				.waitUntilAnyButtonOnclickAttributeLike(tab,
						targetFileNamePrefix);
		if (result == false) {
			throw new RuntimeException("Button onclick attribute not match !!!");
		}
		return tab;

	}

	private XbrlDownloadTable getTargetTable(List<Option> reportTypeOpts,
			int index, String targetFileNamePrefix) {
		reportTypeOpts.get(index).click();
		getSearchButton().click();
		try {
			XbrlDownloadTable tab = waitAjaxTableReload(targetFileNamePrefix);
			AjaxWaitUtility.waitUntilRowTextEqual(tab, 0,
					tab.getTargetRowTexts());
			return tab;
		} catch (TimeoutException e) {
			Font font = browser.getFont(By
					.cssSelector(NO_DATA_MSG_CSS_SELECTOR));
			if (NO_DATA_MSG.equals(font.getText())) {
				return null;
			}
			throw e;
		}
	}

	private String getTargetFileNameRegex(Option yearOpt, Option seasonOpt,
			Option mkOpt, Option repOpt) {
		return yearOpt.getValue() + "-" + seasonOpt.getValue() + "-"
				+ mkOpt.getValue() + "-.*-" + repOpt.getValue() + ".zip";
	}

	private void repeatTryDownload(List<Option> reportTypeOpts, int index,
			String targetFileNamePrefix) {
		int tryAmount = 0;
		while (true) {
			try {
				XbrlDownloadTable tab = getTargetTable(reportTypeOpts, index,
						targetFileNamePrefix);
				// Null means "查無符合資料！"
				if (tab == null) {
					return;
				}
				downLoad(tab);
				break;
			} catch (Exception e) {
				++tryAmount;
				logger.warn("Download fail " + tryAmount + " times !!!");
				logger.warn(browser.getWebDriver().getPageSource());
				if (tryAmount >= MAX_TRY_AMOUNT) {
					throw new RuntimeException(e);
				}
				ThreadUtility.sleep(tryAmount * 60);
			}
		}
	}

	private String getDownloadInfo(Option mkOpt, Option indOpt, Option yearOpt,
			Option seasonOpt, Option repOpt) {
		return mkOpt.getValue() + "/" + indOpt.getValue() + "/"
				+ yearOpt.getValue() + "/" + seasonOpt.getValue() + "/"
				+ repOpt.getValue();
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
}
