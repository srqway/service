package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.resourceutility.utility.FileUtility;
import idv.hsiehpinghan.stockdao.entity.MonthlyData;
import idv.hsiehpinghan.stockdao.entity.MonthlyData.OperatingIncomeFamily;
import idv.hsiehpinghan.stockdao.repository.MonthlyDataRepository;
import idv.hsiehpinghan.stockservice.manager.IMonthlyOperatingIncomeHbaseManager;
import idv.hsiehpinghan.stockservice.operator.MonthlyOperatingIncomeDownloader;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MonthlyOperatingIncomeHbaseManager implements
		IMonthlyOperatingIncomeHbaseManager, InitializingBean {
	private final String COMMA_STRING = StringUtility.COMMA_STRING;
	private final String[] EXTENSIONS = { "csv" };
	private final String UTF8 = "utf8";
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private File downloadDir;
	private File processedLog;

	@Autowired
	private StockServiceProperty stockServiceProperty;
	@Autowired
	private MonthlyOperatingIncomeDownloader downloader;
	@Autowired
	private MonthlyDataRepository monthlyRepo;

	@Override
	public void afterPropertiesSet() throws Exception {
		downloadDir = stockServiceProperty
				.getMonthlyOperatingIncomeDownloadDir();
		generateProcessedLog();
	}

	@Override
	public synchronized boolean updateMonthlyOperatingIncome() {
		File dir = downloadMonthlyOperatingIncome();
		if (dir == null) {
			return false;
		}
		try {
			int processFilesAmt = saveMonthlyOperatingIncomeToHBase(dir);
			logger.info("Saved " + processFilesAmt + " files to "
					+ monthlyRepo.getTargetTableName() + ".");
		} catch (Exception e) {
			logger.error("Update monthly operating income fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	int saveMonthlyOperatingIncomeToHBase(File dir) throws IOException,
			NoSuchFieldException, SecurityException, IllegalArgumentException,
			IllegalAccessException, NoSuchMethodException,
			InvocationTargetException, InstantiationException {
		int count = 0;
		Date ver = new Date();
		Set<String> processedSet = FileUtility.readLinesAsHashSet(processedLog);
		// ex. 1101_201301.csv
		for (File file : FileUtils.listFiles(dir, EXTENSIONS, true)) {
			if (isProcessed(processedSet, file)) {
				continue;
			}
			List<String> lines = FileUtils.readLines(file, UTF8);
			int startRow = getStartRow(file, lines);
			String[] fnStrArr = file.getName().split("[_.]");
			String stockCode = fnStrArr[0];
			int year = Integer.valueOf(fnStrArr[1].substring(0, 4));
			int month = Integer.valueOf(fnStrArr[1].substring(4));
			int size = lines.size();
			List<MonthlyData> entities = new ArrayList<MonthlyData>(size
					- startRow);
			for (int i = startRow; i < size; ++i) {
				String line = lines.get(i);
				String[] strArr = line.split(COMMA_STRING);
				BigInteger currentMonth = new BigInteger(strArr[0]);
				BigInteger currentMonthOfLastYear = new BigInteger(strArr[1]);
				BigInteger differentAmount = new BigInteger(strArr[2]);
				BigDecimal differentPercent = new BigDecimal(strArr[3]);
				BigInteger cumulativeAmountOfThisYear = new BigInteger(
						strArr[4]);
				BigInteger cumulativeAmountOfLastYear = new BigInteger(
						strArr[5]);
				BigInteger cumulativeDifferentAmount = new BigInteger(strArr[6]);
				BigDecimal cumulativeDifferentPercent = new BigDecimal(
						strArr[7]);
				String comment = null;
				if (strArr.length > 8) {
					comment = strArr[8];
				}
				MonthlyData entity = generateEntity(stockCode, year, month,
						ver, currentMonth, currentMonthOfLastYear,
						differentAmount, differentPercent,
						cumulativeAmountOfThisYear, cumulativeAmountOfLastYear,
						cumulativeDifferentAmount, cumulativeDifferentPercent,
						comment);
				entities.add(entity);
			}
			monthlyRepo.put(entities);
			logger.info(file.getName() + " saved to "
					+ monthlyRepo.getTargetTableName() + ".");
			writeToProcessedFile(file);
			++count;
		}
		return count;
	}

	private void writeToProcessedFile(File file) throws IOException {
		String infoLine = generateProcessedInfo(file) + System.lineSeparator();
		FileUtils.write(processedLog, infoLine, Charsets.UTF_8, true);
	}

	private MonthlyData generateEntity(String stockCode, int year, int month,
			Date ver, BigInteger currentMonth,
			BigInteger currentMonthOfLastYear, BigInteger differentAmount,
			BigDecimal differentPercent, BigInteger cumulativeAmountOfThisYear,
			BigInteger cumulativeAmountOfLastYear,
			BigInteger cumulativeDifferentAmount,
			BigDecimal cumulativeDifferentPercent, String comment) {
		MonthlyData entity = new MonthlyData();
		entity.new RowKey(stockCode, year, month, entity);
		generateOperatingIncomeFamilyContent(entity, ver, currentMonth,
				currentMonthOfLastYear, differentAmount, differentPercent,
				cumulativeAmountOfThisYear, cumulativeAmountOfLastYear,
				cumulativeDifferentAmount, cumulativeDifferentPercent, comment);
		return entity;
	}

	private void generateOperatingIncomeFamilyContent(MonthlyData entity,
			Date ver, BigInteger currentMonth,
			BigInteger currentMonthOfLastYear, BigInteger differentAmount,
			BigDecimal differentPercent, BigInteger cumulativeAmountOfThisYear,
			BigInteger cumulativeAmountOfLastYear,
			BigInteger cumulativeDifferentAmount,
			BigDecimal cumulativeDifferentPercent, String comment) {
		OperatingIncomeFamily fam = entity.getOperatingIncomeFamily();
		fam.setComment(ver, comment);
		fam.setCumulativeAmountOfLastYear(ver, cumulativeAmountOfLastYear);
		fam.setCumulativeAmountOfThisYear(ver, cumulativeAmountOfThisYear);
		fam.setCumulativeDifferentAmount(ver, cumulativeDifferentAmount);
		fam.setCumulativeDifferentPercent(ver, cumulativeDifferentPercent);
		fam.setCurrentMonth(ver, currentMonth);
		fam.setCurrentMonthOfLastYear(ver, currentMonthOfLastYear);
		fam.setDifferentAmount(ver, differentAmount);
		fam.setDifferentPercent(ver, differentPercent);
	}

	private int getStartRow(File file, List<String> lines) {
		String targetStr = getTargetStartRowString();
		for (int i = 0, size = lines.size(); i < size; ++i) {
			if (targetStr.equals(lines.get(i))) {
				return i + 1;
			}
		}
		throw new RuntimeException("File(" + file.getAbsolutePath()
				+ ") cannot find line(" + targetStr + ") !!!");
	}

	private String getTargetStartRowString() {
		return "本月,去年同期,增減金額,增減百分比,本年累計,去年累計,增減金額,增減百分比,備註";
	}

	private File downloadMonthlyOperatingIncome() {
		try {
			File dir = downloader.downloadMonthlyOperatingIncome();
			logger.info(dir.getAbsolutePath() + " download finish.");
			return dir;
		} catch (Exception e) {
			logger.error("Download monthly operating income fail !!!");
			return null;
		}
	}

	private void generateProcessedLog() throws IOException {
		if (processedLog == null) {
			processedLog = new File(downloadDir, "processed.log");
			if (processedLog.exists() == false) {
				FileUtils.touch(processedLog);
			}
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

	private String generateProcessedInfo(File file) {
		return file.getName();
	}
}
