package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.hbaseassistant.assistant.HbaseAssistant;
import idv.hsiehpinghan.hbaseassistant.utility.HbaseEntityTestUtility;
import idv.hsiehpinghan.stockdao.entity.FinancialReportInstance;
import idv.hsiehpinghan.stockdao.entity.FinancialReportInstance.InstanceFamily.InstanceValue;
import idv.hsiehpinghan.stockdao.entity.StockDownloadInfo;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.repository.hbase.FinancialReportDataRepository;
import idv.hsiehpinghan.stockdao.repository.hbase.FinancialReportInstanceRepository;
import idv.hsiehpinghan.stockdao.repository.hbase.FinancialReportPresentationRepository;
import idv.hsiehpinghan.stockdao.repository.hbase.StockDownloadInfoRepository;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.stockservice.suit.TestngSuitSetting;
import idv.hsiehpinghan.testutility.utility.SystemResourceUtility;
import idv.hsiehpinghan.xbrlassistant.assistant.InstanceAssistant;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;
import idv.hsiehpinghan.xbrlassistant.xbrl.Instance;

import java.io.File;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.ApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class FinancialReportHbaseManagerTest {
	private final String DATE_PATTERN = "yyyyMMdd";
	private String allStockCode = StockDownloadInfo.StockCodeFamily.StockCodeQualifier.ALL;
	private String allReportType = StockDownloadInfo.ReportTypeFamily.ReportTypeQualifier.ALL;
	private String allYear = StockDownloadInfo.YearFamily.YearQualifier.ALL;
	private String allSeason = StockDownloadInfo.SeasonFamily.SeasonQualifier.ALL;
	private FinancialReportHbaseManager reportManager;
	private FinancialReportPresentationRepository presentRepo;
	private FinancialReportInstanceRepository instanceRepo;
	private StockDownloadInfoRepository infoRepo;
	private FinancialReportDataRepository dataRepo;
	private StockServiceProperty stockServiceProperty;
	private HbaseAssistant hbaseAssistant;

	@BeforeClass
	public void beforeClass() throws Exception {
		ApplicationContext applicationContext = TestngSuitSetting
				.getApplicationContext();
		reportManager = applicationContext
				.getBean(FinancialReportHbaseManager.class);
		presentRepo = applicationContext
				.getBean(FinancialReportPresentationRepository.class);
		instanceRepo = applicationContext
				.getBean(FinancialReportInstanceRepository.class);
		stockServiceProperty = applicationContext
				.getBean(StockServiceProperty.class);
		infoRepo = applicationContext
				.getBean(StockDownloadInfoRepository.class);
		dataRepo = applicationContext
				.getBean(FinancialReportDataRepository.class);
		hbaseAssistant = applicationContext.getBean(HbaseAssistant.class);

//		dropTable();
	}

	@Test
	public void updateFinancialReportPresentation() throws Exception {
		String tableName = presentRepo.getTargetTableName();
		if (presentRepo.isTableExists(tableName)) {
			presentRepo.dropTable(tableName);
			presentRepo.createTable(presentRepo.getTargetTableClass());
		}
		XbrlTaxonomyVersion[] versions = XbrlTaxonomyVersion.values();
		for (XbrlTaxonomyVersion ver : versions) {
			Assert.assertFalse(presentRepo.exists(ver));
		}
		reportManager.updateFinancialReportPresentation();
		for (XbrlTaxonomyVersion ver : versions) {
			Assert.assertTrue(presentRepo.exists(ver));
		}
	}

	@Test
	public void processXbrlFiles() throws Exception {
		StockDownloadInfo downloadInfo = reportManager.getDownloadInfoEntity();
		File instanceFile = SystemResourceUtility
				.getFileResource("xbrl-instance/2013-01-sii-01-C/tifrs-fr0-m1-ci-cr-1101-2013Q1.xml");
		reportManager.processXbrlFiles(instanceFile, downloadInfo);
		String[] strArr = instanceFile.getName().split("-");
		String stockCode = strArr[5];
		ReportType reportType = ReportType.getReportType(strArr[4]);
		int year = Integer.valueOf(strArr[6].substring(0, 4));
		int season = Integer.valueOf(strArr[6].substring(5, 6));
		FinancialReportInstance entity = instanceRepo.get(stockCode,
				reportType, year, season);
		// Test version.
		String version = entity.getInfoFamily()
				.getLatestValue(InstanceAssistant.VERSION).getInfoContent();
		Assert.assertEquals(version, "TIFRS_CI_CR_2013_03_31");
		// Test instance.
		String elementId = "tifrs-SCF_DecreaseIncreaseInFinancialAssetsHeldForTrading";
		String periodType = Instance.Attribute.DURATION;
		Date startDate = DateUtils.parseDate("20130101", DATE_PATTERN);
		Date endDate = DateUtils.parseDate("20130331", DATE_PATTERN);
		InstanceValue val = entity.getInstanceFamily().getLatestValue(
				elementId, periodType, startDate, endDate);
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

		HbaseEntityTestUtility.dropAndCreateTargetTable(instanceRepo);
	}

	@Test(dependsOnMethods = { "processXbrlFiles" })
	public void saveFinancialReportToHBase() throws Exception {
		StockDownloadInfo downloadInfo = reportManager.getDownloadInfoEntity();
		File xbrlDirectory = new File(stockServiceProperty.getExtractDir(), "xbrl");
		int processFilesAmt = reportManager.saveFinancialReportToHBase(
				xbrlDirectory, downloadInfo);
		int fileAmt = getSubFilesAmt(xbrlDirectory);
		Assert.assertEquals(processFilesAmt, fileAmt);
	}

	@Test(dependsOnMethods = { "saveFinancialReportToHBase" })
	public void updateFinancialReportInstance() throws Exception {
		reportManager.updateFinancialReportInstance();
		StockDownloadInfo infoEntity = infoRepo.get(instanceRepo
				.getTargetTableName());
		Assert.assertTrue(infoEntity.getStockCodeFamily()
				.getQualifierVersionValueSet().size() > 0);
		Assert.assertTrue(infoEntity.getReportTypeFamily()
				.getQualifierVersionValueSet().size() > 0);
		Assert.assertTrue(infoEntity.getYearFamily()
				.getQualifierVersionValueSet().size() > 0);
		Assert.assertTrue(infoEntity.getSeasonFamily()
				.getQualifierVersionValueSet().size() > 0);
	}

	@Test(dependsOnMethods = { "updateFinancialReportInstance" })
	public void calculateFinancialReport() throws Exception {
		reportManager.calculateFinancialReport();
		int actual = hbaseAssistant
				.getRowAmount(dataRepo.getTargetTableClass());
		int expected = hbaseAssistant.getRowAmount(instanceRepo
				.getTargetTableClass());
		Assert.assertEquals(expected, actual);
	}

	private void dropTable() throws Exception {
		HbaseEntityTestUtility.dropAndCreateTargetTable(presentRepo);
		HbaseEntityTestUtility.dropAndCreateTargetTable(instanceRepo);
		HbaseEntityTestUtility.dropAndCreateTargetTable(infoRepo);
		HbaseEntityTestUtility.dropAndCreateTargetTable(dataRepo);
	}

	private int getSubFilesAmt(File file) {
		int count = 0;
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				count += getSubFilesAmt(f);
			}
		} else {
			++count;
		}
		return count;
	}
}
