package idv.hsiehpinghan.twseservice.operator;

import idv.hsiehpinghan.datetimeutility.utility.DateUtility;
import idv.hsiehpinghan.seleniumassistant.browser.BrowserBase;
import idv.hsiehpinghan.seleniumassistant.browser.HtmlUnitFirefoxVersionBrowser;
import idv.hsiehpinghan.seleniumassistant.utility.AjaxWaitUtility;
import idv.hsiehpinghan.seleniumassistant.webelement.Div;
import idv.hsiehpinghan.seleniumassistant.webelement.Select;
import idv.hsiehpinghan.seleniumassistant.webelement.TextInput;
import idv.hsiehpinghan.twseservice.property.TwseServiceProperty;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockClosingConditionDownloader implements InitializingBean {
	private final Date BEGIN_DATA_DATE = generateBeginDataDate();
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private File downloadDir;
	private File controlFile;

	@Autowired
	private HtmlUnitFirefoxVersionBrowser browser;
	// private HtmlUnitWithJavascriptBrowser browser;
	// private FirefoxBrowser browser;
	@Autowired
	private TwseServiceProperty twseServiceProperty;

	@Override
	public void afterPropertiesSet() throws Exception {
		downloadDir = new File(twseServiceProperty.getDownloadDir(),
				"closing-condition");
		generateControlFile();
	}

	public File downloadStockClosingCondition() {
		moveToTargetPage();
		Date now = Calendar.getInstance().getTime();
		Date targetDate = BEGIN_DATA_DATE;
		while (targetDate.getTime() < now.getTime()) {
			inputDataDate(targetDate);
			selectType("全部");
			downloadCsv();
			// query();

			targetDate = DateUtils.addDays(targetDate, 1);

			return null;
		}

		return null;
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

	// void query() {
	// browser.getButton(By
	// .cssSelector("input.board")).click();
	// }

	void selectType(String text) {
		Select typeSel = browser.getSelect(By
				.cssSelector("#main-content > form > select"));

		System.err.println(typeSel.getText());

		typeSel.selectByText(text);
	}

	void downloadCsv() {
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

	String getFileName(String str) {
		int idxBegin = str.indexOf("=") + 1;
		return str.substring(idxBegin);
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

}
