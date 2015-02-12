package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InfoFamily.InfoQualifier;
import idv.hsiehpinghan.stockdao.entity.Xbrl.InstanceFamily.InstanceValue;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RowKey;
import idv.hsiehpinghan.stockdao.enumeration.PeriodType;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.enumeration.UnitType;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;
import idv.hsiehpinghan.testutility.utility.SystemResourceUtility;
import idv.hsiehpinghan.xbrlassistant.assistant.InstanceAssistant;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class XbrlInstanceConverterTest {
	private XbrlInstanceConverter converter;
	private InstanceAssistant instanceAssistant;
	private List<String> presentIds;
	private String elementId = "ifrs_BasicEarningsLossPerShare";
	private PeriodType periodType = PeriodType.DURATION;
	private Date startDate;
	private Date endDate;
	private UnitType unitType = UnitType.TWD;
	private BigDecimal value = new BigDecimal("0.38");

	@BeforeClass
	public void beforeClass() throws Exception {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		converter = applicationContext.getBean(XbrlInstanceConverter.class);
		instanceAssistant = applicationContext.getBean(InstanceAssistant.class);

		presentIds = new ArrayList<String>(4);
		presentIds.add(Presentation.Id.BalanceSheet);
		presentIds.add(Presentation.Id.StatementOfComprehensiveIncome);
		presentIds.add(Presentation.Id.StatementOfCashFlows);
		presentIds.add(Presentation.Id.StatementOfChangesInEquity);

		startDate = DateUtils.parseDate("20130101", "yyyyMMdd");
		endDate = DateUtils.parseDate("20130331", "yyyyMMdd");
	}

	@Test
	public void convert() throws Exception {
		File file = SystemResourceUtility
				.getFileResource("xbrl-instance/2013-01-sii-01-C/tifrs-fr0-m1-ci-cr-1101-2013Q1.xml");
		String[] strArr = file.getName().split("-");
		String stockCode = strArr[5];
		ReportType reportType = ReportType.getMopsReportType(strArr[4]);
		int year = Integer.valueOf(strArr[6].substring(0, 4));
		int season = Integer.valueOf(strArr[6].substring(5, 6));
		ObjectNode objNode = instanceAssistant
				.getInstanceJson(file, presentIds);
		Xbrl xbrl = converter.convert(stockCode, reportType, year, season,
				objNode);
		// Test rowKey.
		RowKey rowKey = (RowKey) xbrl.getRowKey();
		Assert.assertEquals(stockCode, rowKey.getStockCode());
		// Test InfoFamily
		Set<InfoQualifier> infoQuals = xbrl.getInfoFamily().getQualifiers();
		Assert.assertTrue(infoQuals.size() == 5);
		// Test InstanceFamily
		InstanceValue val = xbrl.getInstanceFamily().getInstanceValue(
				elementId, periodType, startDate, endDate);
		Assert.assertEquals(unitType, val.getUnitType());
		Assert.assertEquals(value, val.getValue());
	}
}
