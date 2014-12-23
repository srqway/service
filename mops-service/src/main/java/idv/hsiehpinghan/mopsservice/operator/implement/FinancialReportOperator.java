package idv.hsiehpinghan.mopsservice.operator.implement;

import idv.hsiehpinghan.mopsservice.operator.IFinancialReportOperator;
import idv.hsiehpinghan.seleniumutility.webelement.Select;

import java.io.File;

import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FinancialReportOperator implements IFinancialReportOperator {
	
	@Autowired
	private HtmlUnitDriver webDriver;
	
	@Override
	public File downloadFinancialReport() {
		moveToTargetPage();
		
		return null;
	}
	
	Select getMarketTypeSelector() {
		return new Select(webDriver, By.id("MAR_KIND"));
	}

	HtmlUnitDriver moveToTargetPage() {
		final String FINANCIAL_REPORT_PAGE_URL = "http://mops.twse.com.tw/mops/web/t164sb02";
		webDriver.get(FINANCIAL_REPORT_PAGE_URL);
		return webDriver;
	}
}
