package idv.hsiehpinghan.mopsservice.manager.hbase;

import idv.hsiehpinghan.mopsdao.entity.FinancialReportInstance;
import idv.hsiehpinghan.mopsdao.entity.FinancialReportInstance.InstanceFamily.InstanceValue;
import idv.hsiehpinghan.mopsdao.enumeration.ReportType;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportInstanceRepository;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportPresentationRepository;
import idv.hsiehpinghan.mopsservice.property.MopsServiceProperty;
import idv.hsiehpinghan.mopsservice.suit.TestngSuitSetting;
import idv.hsiehpinghan.testutility.utility.SystemResourceUtility;
import idv.hsiehpinghan.xbrlassistant.assistant.InstanceAssistant;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;
import idv.hsiehpinghan.xbrlassistant.xbrl.Instance;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.ApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class MopsHbaseManagerTest {
	private final String DATE_PATTERN = "yyyyMMdd";
	private MopsHbaseManager mopsManager;
	private FinancialReportPresentationRepository presentRepo;
	private FinancialReportInstanceRepository instantRepo;
	private MopsServiceProperty mopsServiceProperty;

	@BeforeClass
	public void beforeClass() throws IOException {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		mopsManager = applicationContext.getBean(MopsHbaseManager.class);
		presentRepo = applicationContext
				.getBean(FinancialReportPresentationRepository.class);
		instantRepo = applicationContext
				.getBean(FinancialReportInstanceRepository.class);
		mopsServiceProperty = applicationContext
				.getBean(MopsServiceProperty.class);
	}

	// @Test
	public void updateFinancialReportPresentation()
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException,
			InstantiationException, IOException {
		String tableName = presentRepo.getTargetTableName();
		if (presentRepo.isTableExists(tableName)) {
			presentRepo.dropTable(tableName);
			presentRepo.createTable(presentRepo.getTargetTableClass());
		}
		XbrlTaxonomyVersion[] versions = XbrlTaxonomyVersion.values();
		for (XbrlTaxonomyVersion ver : versions) {
			Assert.assertFalse(presentRepo.exists(ver));
		}
		mopsManager.updateFinancialReportPresentation();
		for (XbrlTaxonomyVersion ver : versions) {
			Assert.assertTrue(presentRepo.exists(ver));
		}
	}

	@Test
	public void processXbrlFiles() throws Exception {
		File instanceFile = SystemResourceUtility
				.getFileResource("xbrl-instance/2013-01-sii-01-C/tifrs-fr0-m1-ci-cr-1101-2013Q1.xml");
		mopsManager.processXbrlFiles(instanceFile);
		String[] strArr = instanceFile.getName().split("-");
		String stockCode = strArr[5];
		ReportType reportType = ReportType.getReportType(strArr[4]);
		int year = Integer.valueOf(strArr[6].substring(0, 4));
		int season = Integer.valueOf(strArr[6].substring(5, 6));
		FinancialReportInstance entity = instantRepo.get(stockCode, reportType,
				year, season);
		// Test version.
		String version = entity.getInfoFamily()
				.getValue(InstanceAssistant.VERSION).getInfoContent();
		Assert.assertEquals(version, "TIFRS_CI_CR_2013_03_31");
		// Test instance.
		String elementId = "tifrs-SCF_DecreaseIncreaseInFinancialAssetsHeldForTrading";
		String periodType = Instance.Attribute.DURATION;
		Date startDate = DateUtils.parseDate("20130101", DATE_PATTERN);
		Date endDate = DateUtils.parseDate("20130331", DATE_PATTERN);
		InstanceValue val = entity.getInstanceFamily().getValue(elementId,
				periodType, startDate, endDate);
		Assert.assertEquals(val.getUnit(), "TWD");
		Assert.assertEquals(val.getValue().toString(), "-120107000");
	}

//	@Test(dependsOnMethods = { "processXbrlFiles" })
	public void saveFinancialReportToHBase() throws Exception {
		File xbrlDirectory = new File(mopsServiceProperty.getExtractDir());
		int processFilesAmt = mopsManager
				.saveFinancialReportToHBase(xbrlDirectory);

		// Assert.assertEquals(7136, processFilesAmt);
	}
}
