package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.resourceutility.utility.CsvUtility;
import idv.hsiehpinghan.resourceutility.utility.FileUtility;
import idv.hsiehpinghan.stockdao.entity.RatioDifference;
import idv.hsiehpinghan.stockdao.entity.RatioDifference.TTestFamily;
import idv.hsiehpinghan.stockdao.entity.StockInfo.RowKey;
import idv.hsiehpinghan.stockdao.entity.Taxonomy;
import idv.hsiehpinghan.stockdao.entity.Taxonomy.PresentationFamily;
import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.enumeration.PeriodType;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.repository.RatioDifferenceRepository;
import idv.hsiehpinghan.stockdao.repository.StockInfoRepository;
import idv.hsiehpinghan.stockdao.repository.TaxonomyRepository;
import idv.hsiehpinghan.stockdao.repository.XbrlRepository;
import idv.hsiehpinghan.stockservice.manager.IFinancialReportManager;
import idv.hsiehpinghan.stockservice.operator.FinancialReportAnalyzer;
import idv.hsiehpinghan.stockservice.operator.FinancialReportDetailJsonMaker;
import idv.hsiehpinghan.stockservice.operator.FinancialReportDownloader;
import idv.hsiehpinghan.stockservice.operator.XbrlInstanceConverter;
import idv.hsiehpinghan.stockservice.operator.XbrlTransporter;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.xbrlassistant.assistant.InstanceAssistant;
import idv.hsiehpinghan.xbrlassistant.assistant.TaxonomyAssistant;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FinancialReportHbaseManager implements IFinancialReportManager,
		InitializingBean {
	private final String NA = StringUtility.NA_STRING;
	private final String PATTERN = "yyyy-MM-dd";
	private final String[] EXTENSIONS = { "xml" };
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private List<String> presentIds;
	// private List<Dollar> targetDallars;
	private File extractDir;
	private File transportedDir;
	private File processedLog;
	private File transportedLog;
	private File analyzedLog;

	@Autowired
	private FinancialReportDownloader downloader;
	@Autowired
	private XbrlInstanceConverter converter;
	@Autowired
	private XbrlTransporter transporter;
	@Autowired
	private FinancialReportAnalyzer analyzer;

	// @Autowired
	// private ExchangeRateDownloader exchangeRateDownloader;
	@Autowired
	private FinancialReportDetailJsonMaker detailJsonMaker;
	@Autowired
	private StockServiceProperty stockServiceProperty;
	@Autowired
	private TaxonomyAssistant taxonomyAssistant;
	@Autowired
	private InstanceAssistant instanceAssistant;
	@Autowired
	private TaxonomyRepository taxonomyRepo;
	@Autowired
	private XbrlRepository xbrlRepo;
	@Autowired
	private StockInfoRepository infoRepo;
	@Autowired
	private RatioDifferenceRepository diffRepo;

	public FinancialReportHbaseManager() {
		presentIds = new ArrayList<String>(4);
		presentIds.add(Presentation.Id.BalanceSheet);
		presentIds.add(Presentation.Id.StatementOfComprehensiveIncome);
		presentIds.add(Presentation.Id.StatementOfCashFlows);
		presentIds.add(Presentation.Id.StatementOfChangesInEquity);

		// targetDallars = new ArrayList<Dollar>(1);
		// targetDallars.add(Dollar.USD);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		extractDir = stockServiceProperty.getFinancialReportExtractDir();
		generateProcessedLog();
		transportedDir = stockServiceProperty.getTransportDir();
		generateTransportedLog();
		generateAnalyzedLog();
	}

	@Override
	public boolean updateTaxonomyPresentation() {
		XbrlTaxonomyVersion[] versions = XbrlTaxonomyVersion.values();
		XbrlTaxonomyVersion version = null;
		Date ver = Calendar.getInstance().getTime();
		try {
			for (int i = 0, size = versions.length; i < size; ++i) {
				version = versions[i];
				ObjectNode presentNode = taxonomyAssistant.getPresentationJson(
						version, presentIds);
				if (taxonomyRepo.exists(version)) {
					logger.info(version + " exists.");
					continue;
				}
				Taxonomy entity = taxonomyRepo.generateEntity(version);
				generatePresentationFamilyContent(entity, ver, presentNode);
				taxonomyRepo.put(entity);
				logger.info(version + " updated.");
			}
			logger.info("Update financial report presentation finished.");
		} catch (Exception e) {
			logger.error(version + " update fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean updateXbrlInstance() {
		File xbrlDir = downloadFinancialReportInstance();
		if (xbrlDir == null) {
			return false;
		}
		try {
			int processFilesAmt = saveFinancialReportToHBase(xbrlDir);
			logger.info("Saved " + processFilesAmt + " xbrl files to "
					+ xbrlRepo.getTargetTableName() + ".");
		} catch (Exception e) {
			logger.error("Save financial report to hbase fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean updateAnalyzedData() throws IOException {
		TreeSet<String> stockCodes = getStockCodes();
		Set<String> transportedSet = FileUtility
				.readLinesAsHashSet(transportedLog);
		Set<String> analyzedSet = FileUtility.readLinesAsHashSet(analyzedLog);
		try {
			for (String stockCode : stockCodes) {
				for (ReportType reportType : ReportType.values()) {
					File targetDirectory = transport(transportedSet, stockCode,
							reportType);
					if (targetDirectory == null) {
						continue;
					}
					File analyzeFile = analyze(analyzedSet, stockCode,
							reportType, targetDirectory);
					saveRatioDifferenceToHBase(analyzeFile);
					writeToAnalyzedFile(stockCode, reportType);
				}
			}
		} catch (Exception e) {
			logger.error("Update analyzed data fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public TreeSet<String> getStockCodes() {
		TreeSet<RowKey> rowKeys = infoRepo.getRowKeys();
		TreeSet<String> stockCodes = new TreeSet<String>();
		for (RowKey rowKey : rowKeys) {
			stockCodes.add(rowKey.getStockCode());
		}
		return stockCodes;
	}

	@Override
	public TreeSet<Xbrl> getAll(String stockCode, ReportType reportType) {
		return xbrlRepo.fuzzyScan(stockCode, reportType, null, null);
	}

	// @Override
	// public boolean updateExchangeRate() {
	// File exchangeDir = downloadExchangeRate();
	// saveExchangeRateToDatabase(exchangeDir);
	// return true;
	// }

	@Override
	public Map<String, ObjectNode> getFinancialReportDetailJsonMap(
			String stockCode, ReportType reportType, Integer year,
			Integer season, Locale locale) {
		try {
			return detailJsonMaker.getPresentationJsonMap(presentIds,
					stockCode, reportType, year, season, locale);
		} catch (Exception e) {
			logger.error("Get presentation json map fail !!!");
			e.printStackTrace();
			return null;
		}
	}

	File downloadFinancialReportInstance() {
		try {
			File xbrlDir = downloader.downloadFinancialReport();
			logger.info(xbrlDir.getAbsolutePath() + " download finish.");
			return xbrlDir;
		} catch (Exception e) {
			logger.error("Download financial report fail !!!");
			return null;
		}
	}

	int saveFinancialReportToHBase(File xbrlDir) throws Exception {
		Set<String> processedSet = FileUtility.readLinesAsHashSet(processedLog);
		int count = 0;
		// ex. tifrs-fr0-m1-ci-cr-1101-2013Q1.xml
		for (File file : FileUtils.listFiles(xbrlDir, EXTENSIONS, true)) {
			processXbrlFiles(file, processedSet);
			++count;
		}
		return count;
	}

	private String[] getRatioDifferenceTargetTitles(File file) {
		return new String[] { "stockCode", "reportType", "year", "season",
				"elementId", "periodType", "instant", "startDate", "endDate",
				"statistic", "degreeOfFreedom", "confidenceInterval",
				"sampleMean", "hypothesizedMean", "pValue" };
	}

	private String getString(String str) {
		if (NA.equals(str)) {
			return null;
		}
		return str;
	}

	private Date getDate(String str) throws ParseException {
		if (NA.equals(str)) {
			return null;
		}
		return DateUtils.parseDate(str, PATTERN);
	}

	private BigDecimal getBigDecimal(String str) {
		return new BigDecimal(str);
	}

	void saveRatioDifferenceToHBase(File file) throws Exception {
		CSVParser parser = CsvUtility.getParserAtDataStartRow(file,
				getRatioDifferenceTargetTitles(file));
		List<RatioDifference> entities = new ArrayList<RatioDifference>();
		Date ver = new Date();
		for (CSVRecord record : parser) {
			if (record.size() <= 1) {
				break;
			}
			String stockCode = getString(record.get(0));
			ReportType reportType = ReportType
					.valueOf(getString(record.get(1)));
			int year = Integer.valueOf(getString(record.get(2)));
			int season = Integer.valueOf(getString(record.get(3)));
			String elementId = getString(record.get(4));
			PeriodType periodType = PeriodType
					.valueOf(getString(record.get(5)));
			Date instant = getDate(record.get(6));
			Date startDate = getDate(record.get(7));
			Date endDate = getDate(record.get(8));
			BigDecimal statistic = getBigDecimal(record.get(9));
			BigDecimal degreeOfFreedom = getBigDecimal(record.get(10));
			BigDecimal confidenceInterval = getBigDecimal(record.get(11));
			BigDecimal sampleMean = getBigDecimal(record.get(12));
			BigDecimal hypothesizedMean = getBigDecimal(record.get(13));
			BigDecimal pValue = getBigDecimal(record.get(14));
			RatioDifference entity = generateEntity(stockCode, reportType,
					year, season, elementId, ver, statistic, degreeOfFreedom,
					confidenceInterval, sampleMean, hypothesizedMean, pValue);
			entities.add(entity);
		}
		diffRepo.put(entities);
		logger.info(file.getName() + " saved to "
				+ diffRepo.getTargetTableName() + ".");
	}

	private RatioDifference generateEntity(String stockCode,
			ReportType reportType, int year, int season, String elementId,
			Date ver, BigDecimal statistic, BigDecimal degreeOfFreedom,
			BigDecimal confidenceInterval, BigDecimal sampleMean,
			BigDecimal hypothesizedMean, BigDecimal pValue) {
		RatioDifference entity = diffRepo.generateEntity(stockCode, reportType,
				year, season, elementId);
		generateTTestFamilyContent(entity, ver, statistic, degreeOfFreedom,
				confidenceInterval, sampleMean, hypothesizedMean, pValue);
		return entity;
	}

	private void generateTTestFamilyContent(RatioDifference entity, Date ver,
			BigDecimal statistic, BigDecimal degreeOfFreedom,
			BigDecimal confidenceInterval, BigDecimal sampleMean,
			BigDecimal hypothesizedMean, BigDecimal pValue) {
		TTestFamily fam = entity.getTTestFamily();
		fam.setStatistic(ver, statistic);
		fam.setDegreeOfFreedom(ver, degreeOfFreedom);
		fam.setConfidenceInterval(ver, confidenceInterval);
		fam.setSampleMean(ver, sampleMean);
		fam.setHypothesizedMean(ver, hypothesizedMean);
		fam.setPValue(ver, pValue);
	}

	void processXbrlFiles(File file, Set<String> processedSet) throws Exception {
		if (isProcessed(processedSet, file)) {
			return;
		}
		String[] strArr = file.getName().split("-");
		String stockCode = strArr[5];
		ReportType reportType = ReportType.getMopsReportType(strArr[4]);
		int year = Integer.valueOf(strArr[6].substring(0, 4));
		int season = Integer.valueOf(strArr[6].substring(5, 6));
		ObjectNode objNode = instanceAssistant
				.getInstanceJson(file, presentIds);
		Xbrl entity = converter.convert(stockCode, reportType, year, season,
				objNode);
		xbrlRepo.put(entity);
		logger.info(file.getName() + " saved to "
				+ xbrlRepo.getTargetTableName() + ".");
		writeToProcessedFile(file);
	}

	private void writeToProcessedFile(File file) throws IOException {
		String infoLine = generateProcessedInfo(file) + System.lineSeparator();
		FileUtils.write(processedLog, infoLine, Charsets.UTF_8, true);
	}

	private void writeToTransportedFile(String stockCode, ReportType reportType)
			throws IOException {
		String infoLine = generateTransportedInfo(stockCode, reportType)
				+ System.lineSeparator();
		FileUtils.write(transportedLog, infoLine, Charsets.UTF_8, true);
	}

	private void writeToAnalyzedFile(String stockCode, ReportType reportType)
			throws IOException {
		String infoLine = generateAnalyzedInfo(stockCode, reportType)
				+ System.lineSeparator();
		FileUtils.write(analyzedLog, infoLine, Charsets.UTF_8, true);
	}

	private void generateProcessedLog() throws IOException {
		if (processedLog == null) {
			processedLog = FileUtility.getOrCreateFile(extractDir,
					"processed.log");
		}
	}

	private void generateTransportedLog() throws IOException {
		if (transportedLog == null) {
			transportedLog = FileUtility.getOrCreateFile(transportedDir,
					"transported.log");
		}
	}

	private void generateAnalyzedLog() throws IOException {
		if (analyzedLog == null) {
			analyzedLog = FileUtility.getOrCreateFile(transportedDir,
					"analyzed.log");
		}
	}

	private boolean isProcessed(Set<String> processedSet, File file)
			throws IOException {
		String processedInfo = generateProcessedInfo(file);
		if (processedSet.contains(processedInfo)) {
			logger.info(processedInfo + " processed before.");
			return true;
		}
		return false;
	}

	private boolean isTransported(Set<String> transportedSet, String stockCode,
			ReportType reportType) throws IOException {
		String transportedInfo = generateTransportedInfo(stockCode, reportType);
		if (transportedSet.contains(transportedInfo)) {
			logger.info(transportedInfo + " processed before.");
			return true;
		}
		return false;
	}

	private boolean isAnalyzed(Set<String> analyzedSet, String stockCode,
			ReportType reportType) throws IOException {
		String analyzedInfo = generateAnalyzedInfo(stockCode, reportType);
		if (analyzedSet.contains(analyzedInfo)) {
			logger.info(analyzedInfo + " analyzed before.");
			return true;
		}
		return false;
	}

	private String generateProcessedInfo(File file) {
		return file.getName();
	}

	private String generateTransportedInfo(String stockCode,
			ReportType reportType) {
		return String.format("%s_%s", stockCode, reportType);
	}

	private String generateAnalyzedInfo(String stockCode, ReportType reportType) {
		return String.format("%s_%s", stockCode, reportType);
	}

	private void generatePresentationFamilyContent(Taxonomy entity, Date ver,
			ObjectNode presentNode) {
		PresentationFamily fam = entity.getPresentationFamily();
		fam.setBalanceSheet(ver, presentNode.get(Presentation.Id.BalanceSheet)
				.toString());
		fam.setStatementOfCashFlows(ver,
				presentNode.get(Presentation.Id.StatementOfCashFlows)
						.toString());
		fam.setStatementOfChangesInEquity(ver,
				presentNode.get(Presentation.Id.StatementOfChangesInEquity)
						.toString());
		fam.setStatementOfComprehensiveIncome(ver,
				presentNode.get(Presentation.Id.StatementOfComprehensiveIncome)
						.toString());
	}

	private File analyze(Set<String> analyzedSet, String stockCode,
			ReportType reportType, File targetDirectory) throws IOException {
		if (isAnalyzed(analyzedSet, stockCode, reportType)) {
			return null;
		}
		logger.info(String.format("Begin analyze %s %s", stockCode, reportType));
		File resultFile = analyzer.analyzeRatioDifference(targetDirectory);
		logger.info(String
				.format("Finish analyze %s %s", stockCode, reportType));
		return resultFile;
	}

	private File transport(Set<String> transportedSet, String stockCode,
			ReportType reportType) throws IOException {
		File targetDirectory = null;
		if (isTransported(transportedSet, stockCode, reportType)) {
			return null;
		}
		targetDirectory = FileUtility.getOrCreateDirectory(
				stockServiceProperty.getTransportDir(), stockCode,
				reportType.name());
		boolean transRst = transporter.saveHbaseDataToFile(stockCode,
				reportType, targetDirectory);
		writeToTransportedFile(stockCode, reportType);
		if (transRst == false) {
			return null;
		}
		return targetDirectory;
	}
}
