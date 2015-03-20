package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseColumnQualifier;
import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseValue;
import idv.hsiehpinghan.resourceutility.utility.CsvUtility;
import idv.hsiehpinghan.resourceutility.utility.FileUtility;
import idv.hsiehpinghan.stockdao.entity.RatioDifference;
import idv.hsiehpinghan.stockdao.entity.RatioDifference.TTestFamily;
import idv.hsiehpinghan.stockdao.entity.RatioDifference.TTestFamily.TTestValue;
import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;
import idv.hsiehpinghan.stockdao.repository.RatioDifferenceRepository;
import idv.hsiehpinghan.stockdao.repository.StockInfoRepository;
import idv.hsiehpinghan.stockdao.repository.XbrlRepository;
import idv.hsiehpinghan.stockservice.manager.IAnalysisManager;
import idv.hsiehpinghan.stockservice.operator.RatioDifferenceAnalyzer;
import idv.hsiehpinghan.stockservice.operator.XbrlTransporter;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;
import idv.hsiehpinghan.xbrlassistant.enumeration.XbrlTaxonomyVersion;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalysisHbaseManager implements IAnalysisManager, InitializingBean {
	private final String NA = StringUtility.NA_STRING;
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private File transportedDir;
	private File transportedLog;
	private File analyzedLog;

	@Autowired
	private XbrlTransporter transporter;
	@Autowired
	private RatioDifferenceAnalyzer analyzer;
	@Autowired
	private StockServiceProperty stockServiceProperty;
	@Autowired
	private XbrlRepository xbrlRepo;
	@Autowired
	private StockInfoRepository infoRepo;
	@Autowired
	private RatioDifferenceRepository diffRepo;

	// @Autowired
	// private MailAssistant mailAssist;

	@Override
	public void afterPropertiesSet() throws Exception {
		transportedDir = stockServiceProperty.getTransportDir();
		generateTransportedLog();
		generateAnalyzedLog();
	}

	@Override
	public boolean updateAnalyzedData() throws IOException {
		TreeSet<Xbrl> entities = xbrlRepo.scanWithInfoFamilyOnly();
		Set<String> transportedSet = FileUtility
				.readLinesAsHashSet(transportedLog);
		Set<String> analyzedSet = FileUtility.readLinesAsHashSet(analyzedLog);
		try {
			for (Xbrl entity : entities) {
				Xbrl.RowKey rowKey = (Xbrl.RowKey) entity.getRowKey();
				int year = rowKey.getYear();
				int season = rowKey.getSeason();
				String stockCode = rowKey.getStockCode();
				ReportType reportType = rowKey.getReportType();
				XbrlTaxonomyVersion version = entity.getInfoFamily()
						.getVersion();
				File targetDirectory = transportXbrl(transportedSet, year,
						season, stockCode, reportType, version);
				if (targetDirectory == null) {
					continue;
				}
				File analyzeFile = analyzeRatioDifference(analyzedSet,
						stockCode, reportType, targetDirectory);
				saveRatioDifferenceToHBase(analyzeFile);
				writeToAnalyzedFile(stockCode, reportType);
			}
		} catch (Exception e) {
			logger.error("Update analyzed data fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public RatioDifference getRatioDifference(String stockCode,
			ReportType reportType, int year, int season) {
		try {
			return diffRepo.get(stockCode, reportType, year, season);
		} catch (Exception e) {
			logger.error("Get ratio difference fail !!!");
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public boolean sendAnalysisMail() {

		return true;
	}

	// public TreeSet<RatioDifference> getBeyondThresholdRatioDifferences(
	// BigDecimal pValueThreshold) throws IllegalAccessException,
	// NoSuchMethodException, SecurityException, InstantiationException,
	// IllegalArgumentException, InvocationTargetException, IOException {
	// TreeSet<RatioDifference.RowKey> rowKeys = diffRepo.getRowKeys();
	// TreeSet<RatioDifference> results = new TreeSet<RatioDifference>();
	// for (RatioDifference.RowKey rowKey : rowKeys) {
	// RatioDifference entity = (RatioDifference) diffRepo.get(rowKey);
	// for (Entry<HBaseColumnQualifier, HBaseValue> ent : entity
	// .getTTestFamily().getLatestQualifierAndValueAsSet()) {
	// TTestQualifier qual = (TTestQualifier) ent.getKey();
	// if (TTestFamily.P_VALUE.equals(qual.getColumnName()) == false) {
	// continue;
	// }
	// if (isBeyondThreshold(ent, pValueThreshold) == false) {
	// continue;
	// }
	// results.add(entity);
	// }
	// }
	// return results;
	// }

	private boolean isBeyondThreshold(
			Entry<HBaseColumnQualifier, HBaseValue> ent,
			BigDecimal pValueThreshold) {
		TTestValue val = (TTestValue) ent.getValue();
		BigDecimal pValue = val.getAsBigDecimal();
		if (pValueThreshold.compareTo(pValue) < 0) {
			return true;
		}
		return false;
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
			String chineseName = getString(record.get(5));
			String englishName = getString(record.get(6));
			BigDecimal statistic = getBigDecimal(record.get(7));
			BigDecimal degreeOfFreedom = getBigDecimal(record.get(8));
			BigDecimal confidenceInterval = getBigDecimal(record.get(9));
			BigDecimal sampleMean = getBigDecimal(record.get(10));
			BigDecimal hypothesizedMean = getBigDecimal(record.get(11));
			BigDecimal pValue = getBigDecimal(record.get(12));
			RatioDifference entity = generateEntity(stockCode, reportType,
					year, season, elementId, ver, chineseName, englishName,
					statistic, degreeOfFreedom, confidenceInterval, sampleMean,
					hypothesizedMean, pValue);
			entities.add(entity);
		}
		diffRepo.put(entities);
		logger.info(file.getName() + " saved to "
				+ diffRepo.getTargetTableName() + ".");
	}

	// private TreeSet<String> getStockCodes(StockInfoRepository infoRepo) {
	// TreeSet<StockInfo.RowKey> rowKeys = infoRepo.getRowKeys();
	// TreeSet<String> stockCodes = new TreeSet<String>();
	// for (StockInfo.RowKey rowKey : rowKeys) {
	// stockCodes.add(rowKey.getStockCode());
	// }
	// return stockCodes;
	// }

	private RatioDifference generateEntity(String stockCode,
			ReportType reportType, int year, int season, String elementId,
			Date ver, String chineseName, String englishName,
			BigDecimal statistic, BigDecimal degreeOfFreedom,
			BigDecimal confidenceInterval, BigDecimal sampleMean,
			BigDecimal hypothesizedMean, BigDecimal pValue) {
		RatioDifference entity = diffRepo.generateEntity(stockCode, reportType,
				year, season);
		generateTTestFamilyContent(entity, ver, elementId, chineseName,
				englishName, statistic, degreeOfFreedom, confidenceInterval,
				sampleMean, hypothesizedMean, pValue);
		return entity;
	}

	private void generateTTestFamilyContent(RatioDifference entity, Date ver,
			String elementId, String chineseName, String englishName,
			BigDecimal statistic, BigDecimal degreeOfFreedom,
			BigDecimal confidenceInterval, BigDecimal sampleMean,
			BigDecimal hypothesizedMean, BigDecimal pValue) {
		TTestFamily fam = entity.getTTestFamily();
		fam.setChineseName(elementId, ver, chineseName);
		fam.setEnglishName(elementId, ver, englishName);
		fam.setStatistic(elementId, ver, statistic);
		fam.setDegreeOfFreedom(elementId, ver, degreeOfFreedom);
		fam.setConfidenceInterval(elementId, ver, confidenceInterval);
		fam.setSampleMean(elementId, ver, sampleMean);
		fam.setHypothesizedMean(elementId, ver, hypothesizedMean);
		fam.setPValue(elementId, ver, pValue);
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

	private String generateTransportedInfo(String stockCode,
			ReportType reportType) {
		return String.format("%s_%s", stockCode, reportType);
	}

	private String generateAnalyzedInfo(String stockCode, ReportType reportType) {
		return String.format("%s_%s", stockCode, reportType);
	}

	private File analyzeRatioDifference(Set<String> analyzedSet,
			String stockCode, ReportType reportType, File targetDirectory)
			throws IOException {
		if (isAnalyzed(analyzedSet, stockCode, reportType)) {
			return null;
		}
		logger.info(String.format("Begin analyze %s %s", stockCode, reportType));
		File resultFile = analyzer.analyzeRatioDifference(targetDirectory);
		logger.info(String
				.format("Finish analyze %s %s", stockCode, reportType));
		return resultFile;
	}

	private File transportXbrl(Set<String> transportedSet, int year,
			int season, String stockCode, ReportType reportType,
			XbrlTaxonomyVersion version) throws IOException,
			IllegalAccessException, NoSuchMethodException, SecurityException,
			InstantiationException, IllegalArgumentException,
			InvocationTargetException {
		File targetDirectory = FileUtility.getOrCreateDirectory(
				stockServiceProperty.getTransportDir(), String.valueOf(year),
				String.valueOf(season), stockCode, reportType.name());
		if (isTransported(transportedSet, stockCode, reportType)) {
			return targetDirectory;
		}
		// False means no data in hbase.
		boolean transRst = transporter.saveHbaseDataToFile(stockCode,
				reportType, version, targetDirectory);
		if (transRst == false) {
			return null;
		}
		writeToTransportedFile(stockCode, reportType);
		return targetDirectory;
	}

	private String[] getRatioDifferenceTargetTitles(File file) {
		return new String[] { "stockCode", "reportType", "year", "season",
				"elementId", "chineseName", "englishName", "statistic",
				"degreeOfFreedom", "confidenceInterval", "sampleMean",
				"hypothesizedMean", "pValue" };
	}

	private String getString(String str) {
		if (NA.equals(str)) {
			return null;
		}
		return str;
	}

	private BigDecimal getBigDecimal(String str) {
		return new BigDecimal(str);
	}

}
