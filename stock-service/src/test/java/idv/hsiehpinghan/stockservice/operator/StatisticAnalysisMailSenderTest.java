package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;

import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class StatisticAnalysisMailSenderTest {
	private StatisticAnalysisMailSender mailSender;
	private String stockCode = "1256";
	private ReportType reportType = ReportType.CONSOLIDATED_STATEMENT;

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		mailSender = applicationContext
				.getBean(StatisticAnalysisMailSender.class);
	}

	@Test
	public void sendMainRatioAnalysis() throws Exception {
		mailSender.sendMainRatioAnalysis(stockCode, reportType);
	}
}
