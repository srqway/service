package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.repository.XbrlRepository;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class FinancialReportJsonMakerTest {
	private XbrlRepository xbrlRepo;
	private FinancialReportJsonMaker jsonMaker;
	private String stockCode = "1256";
	private ReportType reportType = ReportType.CONSOLIDATED_STATEMENT;

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		jsonMaker = applicationContext.getBean(FinancialReportJsonMaker.class);
		xbrlRepo = applicationContext.getBean(XbrlRepository.class);
	}

//	@Test(dependsOnGroups = "FinancialReportHbaseManagerTest")
//	@Test
	public void getBalanceSheetJson() throws Exception {
		List<Xbrl> xbrls = xbrlRepo
				.fuzzyScan(stockCode, reportType, null, null);
		ObjectNode objNode = jsonMaker.getBalanceSheetJson(xbrls);
	}
	
	@Test
	public void getStatementOfComprehensiveIncomeJson() throws Exception {
		List<Xbrl> xbrls = xbrlRepo
				.fuzzyScan(stockCode, reportType, null, null);
		ObjectNode objNode = jsonMaker.getStatementOfComprehensiveIncomeJson(xbrls);
	}
}
