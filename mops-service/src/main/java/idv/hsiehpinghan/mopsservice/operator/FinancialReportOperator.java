package idv.hsiehpinghan.mopsservice.operator;

import idv.hsiehpinghan.seleniumassistant.browser.BrowserBase;
import idv.hsiehpinghan.seleniumassistant.browser.HtmlUnitBrowser;
import idv.hsiehpinghan.seleniumassistant.utility.AjaxWaitUtility;
import idv.hsiehpinghan.seleniumassistant.webelement.Button;
import idv.hsiehpinghan.seleniumassistant.webelement.Select;
import idv.hsiehpinghan.seleniumassistant.webelement.Select.Option;
import idv.hsiehpinghan.seleniumassistant.webelement.Table;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinancialReportOperator {
	static {
		List<String> txts = new ArrayList<String>(3);
		txts.add("代號");
		txts.add("產業別");
		txts.add("下載");
		targetRowTexts = txts;
	}
	private static final List<String> targetRowTexts;
	 @Autowired
	 private HtmlUnitBrowser browser;
//	@Autowired
//	private FireFoxBrowser browser;

	/**
	 * Download financial report.
	 * 
	 * @return
	 */
	public File downloadFinancialReport() {
		moveToTargetPage();
		List<Option> mkOpts = getMarketTypeSelect().getOptions();
		for (int iMk = mkOpts.size() - 1; iMk >= 0; --iMk) {
			Option mkOpt = mkOpts.get(iMk);
			if (isTargetMarketType(mkOpt.getText().trim()) == false) {
				continue;
			}
			List<Option> oldIndOpts = getIndustryTypeSelect().getOptions();
			mkOpt.click();
			List<Option> indOpts = getIndustryOptions(oldIndOpts);
			for(int iInd = indOpts.size() - 1; iInd >= 0; --iInd) {
				Option indOpt = indOpts.get(iInd);
				if(isTargetIndustryType(indOpt.getText().trim()) == false) {
					continue;
				}
				indOpt.click();
				List<Option> yearOpts = getYearSelect().getOptions();
				for(int iYear = 0, yearSize = yearOpts.size(); iYear < yearSize; ++iYear) {
					yearOpts.get(iYear).click();
					List<Option> seasonOpts = getSeasonSelect().getOptions();
					for(int iSeason = 0, seasonSize = seasonOpts.size(); iSeason < seasonSize; ++seasonSize) {
						seasonOpts.get(iSeason).click();
						List<Option> reportTypeOpts = getReportTypeSelect().getOptions();
						for(int iRep = 0, repSize = reportTypeOpts.size(); iRep < repSize; ++iRep) {
							reportTypeOpts.get(iRep).click();
							getSearchButton().click();
							Table tab = browser.getTable(By.cssSelector(".hasBorder"));
							AjaxWaitUtility.waitUntilRowTextEqual(tab, 0, targetRowTexts);
							downLoad(tab);
							System.err.println("done !!!");
							
							return null;
						}
					}
				}
			}
		}
		return null;
	}

	private void downLoad(Table table) {
		// i = 0 is title.
		for(int i = 1, size = table.getRowSize(); i < size; ++i) {
			table.clickButtonCell(i, 2);
			
			System.out.println("begin");
			System.out.println(browser.getWebDriver().getPageSource());
			System.out.println("end");
			break;
		}
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

	private boolean isTargetMarketType(String text) {
		if("上市".equals(text) == true) {
			return true;
		}
		if("上櫃".equals(text) == true) {
			return true;
		}
		return false;
	}
	
	private boolean isTargetIndustryType(String text) {
		if("".equals(text) == true) {
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
		return browser.getButton(By.cssSelector("#search_bar1 > div > input[type='button']"));
	}
}
