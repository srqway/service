package idv.hsiehpinghan.mopsservice.operator;

import idv.hsiehpinghan.mopsservice.suit.TestngSuitSetting;
import idv.hsiehpinghan.resourceutility.utility.ResourceUtility;
import idv.hsiehpinghan.xbrlassistant.assistant.XbrlAssistant;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FinancialReportCalculatorTest {
	private FinancialReportCalculator calculator;
	private XbrlAssistant xbrlAssistant;
	private File instanceFile;
	private ObjectMapper objectMapper;
	private ObjectNode presentNode;
	private Map<String, BigDecimal> valueMap;
	
	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		calculator = applicationContext
				.getBean(FinancialReportCalculator.class);
		xbrlAssistant = applicationContext
				.getBean(XbrlAssistant.class);
		objectMapper = applicationContext.getBean(ObjectMapper.class);
		
		String instancePath = "xbrl-instance/2013-01-sii-01-C/tifrs-fr0-m1-ci-cr-1101-2013Q1.xml";
		instanceFile = ResourceUtility.getFileResource(instancePath);
	}

//	@Test
//	public void getValueObjectNode() throws Exception {
//		presentNode = xbrlAssistant.getPresentationJson(
//				instanceFile, Presentation.Id.BalanceSheet);
//		JsonNode valuesNode = calculator.getValueObjectNode(presentNode);
//		JsonNode valuesNodeSample = objectMapper
//				.readTree(ResourceUtility
//						.getFileResource("sample/values.json"));
//		Assert.assertEquals(valuesNode.toString(), valuesNodeSample.toString());
//	}
//	
//	@Test(dependsOnMethods = {"getValueObjectNode"})
//	public void fillUnsummedValueMap() {
//		valueMap = new HashMap<String, BigDecimal>();
//		calculator.fillUnsummedValueMap(presentNode, "AsOf20130331", valueMap);
//		Assert.assertEquals(valueMap.size(), 764);
//	}
//	
//	@Test(dependsOnMethods = {"fillUnsummedValueMap"})
//	public void fillSumValueMap() throws Exception {
//		ObjectNode calNode = xbrlAssistant.getCalculationJson(instanceFile, Calculation.Id.BalanceSheet);
//		calculator.fillSumValueMap(valueMap, calNode);
//		
//		PrintUtility.print(valueMap);
//	}
	
	@Test
	public void getJsonFinancialReport() throws Exception {
		ObjectNode result = calculator.getJsonFinancialReport(instanceFile, new Date());

//		System.err.println(result);
//		System.err.println(presentNode);
	}
	
}
