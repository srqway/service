package idv.hsiehpinghan.mopsservice.manager.hbase;

import idv.hsiehpinghan.mopsdao.entity.FinancialReportInstance;
import idv.hsiehpinghan.mopsdao.entity.FinancialReportInstance.InstanceFamily.InstanceValue;
import idv.hsiehpinghan.mopsdao.entity.MopsDownloadInfo;
import idv.hsiehpinghan.mopsdao.enumeration.ReportType;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportInstanceRepository;
import idv.hsiehpinghan.mopsdao.repository.FinancialReportPresentationRepository;
import idv.hsiehpinghan.mopsdao.repository.MopsDownloadInfoRepository;
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
	private String allStockCode = MopsDownloadInfo.StockCodeFamily.StockCodeQualifier.ALL;
	private String allReportType = MopsDownloadInfo.ReportTypeFamily.ReportTypeQualifier.ALL;
	private String allYear = MopsDownloadInfo.YearFamily.YearQualifier.ALL;
	private String allSeason = MopsDownloadInfo.SeasonFamily.SeasonQualifier.ALL;
	private MopsHbaseManager mopsManager;
	private FinancialReportPresentationRepository presentRepo;
	private FinancialReportInstanceRepository instantRepo;
	private MopsDownloadInfoRepository mopsDownloadInfoRepo;
	private MopsServiceProperty mopsServiceProperty;

	@BeforeClass
	public void beforeClass() throws Exception {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		mopsManager = applicationContext.getBean(MopsHbaseManager.class);
		presentRepo = applicationContext
				.getBean(FinancialReportPresentationRepository.class);
		instantRepo = applicationContext
				.getBean(FinancialReportInstanceRepository.class);
		mopsServiceProperty = applicationContext
				.getBean(MopsServiceProperty.class);
		mopsDownloadInfoRepo = applicationContext
				.getBean(MopsDownloadInfoRepository.class);

		dropTable();
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

	// @Test
	public void processXbrlFiles() throws Exception {
		MopsDownloadInfo downloadInfo = mopsManager.getDownloadInfoEntity();
		File instanceFile = SystemResourceUtility
				.getFileResource("xbrl-instance/2013-01-sii-01-C/tifrs-fr0-m1-ci-cr-1101-2013Q1.xml");
		mopsManager.processXbrlFiles(instanceFile, downloadInfo);
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
		// Test downloadInfo.
		Assert.assertTrue(downloadInfo.getStockCodeFamily()
				.getLatestValue(allStockCode).getStockCodes().contains("1101"));
		Assert.assertTrue(downloadInfo.getReportTypeFamily()
				.getLatestValue(allReportType).getReportTypes()
				.contains(ReportType.CONSOLIDATED_STATEMENT));
		Assert.assertTrue(downloadInfo.getYearFamily().getLatestValue(allYear)
				.getYears().contains(2013));
		Assert.assertTrue(downloadInfo.getSeasonFamily()
				.getLatestValue(allSeason).getSeasons().contains(1));
	}

	// @Test(dependsOnMethods = { "processXbrlFiles" })
	@Test
	public void saveFinancialReportToHBase() throws Exception {
		MopsDownloadInfo downloadInfo = mopsManager.getDownloadInfoEntity();
		File xbrlDirectory = new File(mopsServiceProperty.getExtractDir());
		int processFilesAmt = mopsManager.saveFinancialReportToHBase(
				xbrlDirectory, downloadInfo);
		Assert.assertEquals(7136, processFilesAmt);
	}

	private void dropTable() throws Exception {
		// HbaseEntityTestUtility.dropTargetTable(presentRepo);
		// HbaseEntityTestUtility.dropTargetTable(instantRepo);
		// HbaseEntityTestUtility.dropTargetTable(mopsDownloadInfoRepo);
	}
}
