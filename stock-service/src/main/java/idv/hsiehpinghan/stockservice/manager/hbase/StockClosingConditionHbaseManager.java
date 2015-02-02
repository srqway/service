package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.stockdao.entity.StockClosingCondition;
import idv.hsiehpinghan.stockdao.entity.StockClosingCondition.PriceFamily;
import idv.hsiehpinghan.stockdao.entity.StockClosingCondition.PriceFamily.PriceQualifier;
import idv.hsiehpinghan.stockdao.entity.StockClosingCondition.VolumeFamily;
import idv.hsiehpinghan.stockdao.entity.StockClosingCondition.VolumeFamily.VolumeQualifier;
import idv.hsiehpinghan.stockdao.repository.IStockClosingConditionRepository;
import idv.hsiehpinghan.stockservice.manager.IStockClosingConditionManager;
import idv.hsiehpinghan.stockservice.operator.StockClosingConditionOfTwseDownloader;
import idv.hsiehpinghan.stockservice.property.StockServiceProperty;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockClosingConditionHbaseManager implements
		IStockClosingConditionManager, InitializingBean {
	private final String[] EXTENSIONS = { "csv" };
	private final String BIG5 = "big5";
	private final String COMMA_STRING = StringUtility.COMMA_STRING;
	private final String EMPTY_STRING = StringUtility.EMPTY_STRING;
	private final String DOUBLE_UOTATION_STRING = StringUtility.DOUBLE_UOTATION_STRING;
	private final String YYYYMMDD = "yyyyMMdd";
	private Logger logger = Logger.getLogger(this.getClass().getName());
	private File downloadDirOfTwse;
	private File processedLogOfTwse;
	private List<String> processedListOfTwse;
	private File downloadDirOfGretai;
	private File processedLogOfGretai;
	private List<String> processedListOfGretai;

	@Autowired
	private StockServiceProperty stockServiceProperty;
	@Autowired
	private StockClosingConditionOfTwseDownloader downloaderOfTwse;
	@Autowired
	private IStockClosingConditionRepository condRepo;

	@Override
	public void afterPropertiesSet() throws Exception {
		downloadDirOfTwse = stockServiceProperty
				.getStockClosingConditionDownloadDirOfTwse();
		generateProcessedLogFile();
	}

	@Override
	public synchronized boolean updateStockClosingCondition() {
		boolean result = true;
		try {
			updateStockClosingConditionOfTwse();
		} catch (Exception e) {
			logger.error("Update Stock Closing Condition of Twse fail !!!");
			e.printStackTrace();
			result = false;
		}
		return result;
	}

	public boolean updateStockClosingConditionOfTwse()
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException,
			InstantiationException, ParseException, IOException {
		File dir = downloadStockClosingConditionOfTwse();
		if (dir == null) {
			return false;
		}
		int processFilesAmt = saveStockClosingConditionOfTwseToHBase(dir);
		logger.info("Saved " + processFilesAmt + " files to "
				+ condRepo.getTargetTableName() + ".");
		return true;
	}

	int saveStockClosingConditionOfTwseToHBase(File dir) throws ParseException,
			IOException, NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException,
			InstantiationException {
		int count = 0;
		Date now = new Date();
		processedListOfTwse = FileUtils.readLines(processedLogOfTwse);
		for (File file : FileUtils.listFiles(dir, EXTENSIONS, true)) {
			// ex. A11220130104ALLBUT0999.csv
			Date date = DateUtils.parseDate(file.getName().substring(4, 12),
					YYYYMMDD);
			if (isProcessedOfTwse(file)) {
				continue;
			}
			List<String> lines = FileUtils.readLines(file, BIG5);
			if (hasDataOfTwse(file, date, lines) == false) {
				continue;
			}
			int startRow = getStartRowOfTwse(file, lines);
			int size = lines.size();
			List<StockClosingCondition> entities = new ArrayList<StockClosingCondition>(
					size - startRow);
			for (int i = startRow; i < size; ++i) {
				String line = lines.get(i).replace(DOUBLE_UOTATION_STRING,
						EMPTY_STRING);
				String[] strArr = line.split(COMMA_STRING);
				String stockCode = getString(strArr[0]);
				if (stockCode.equals(EMPTY_STRING)) {
					break;
				}
				if (condRepo.exists(stockCode, date)) {
					continue;
				}
				StockClosingCondition entity = generateEntity(stockCode, date,
						strArr, now);
				entities.add(entity);
			}
			condRepo.put(entities);
			writeToProcessedFileOfTwse(file);
			logger.info(file.getName() + " saved to "
					+ condRepo.getTargetTableName() + ".");
			++count;
		}
		return count;
	}

	private boolean hasDataOfTwse(File file, Date date, List<String> lines)
			throws IOException {
		String targetDateStr = DateFormatUtils.format(date, "yyyy年MM月dd日");
		for (String line : lines) {
			String trimmedLine = line.trim();
			if (trimmedLine.startsWith(targetDateStr)) {
				if (trimmedLine.endsWith("查無資料")) {
					return false;
				}
				return true;
			}
		}
		throw new RuntimeException("File(" + file.getAbsolutePath()
				+ ") has wrong date !!!");
	}

	private int getStartRowOfTwse(File file, List<String> lines) {
		String targetStr = "\"證券代號\",\"證券名稱\",\"成交股數\",\"成交筆數\",\"成交金額\",\"開盤價\",\"最高價\",\"最低價\",\"收盤價\",\"漲跌(+/-)\",\"漲跌價差\",\"最後揭示買價\",\"最後揭示買量\",\"最後揭示賣價\",\"最後揭示賣量\",\"本益比\"";
		for (int i = 0, size = lines.size(); i < size; ++i) {
			if (targetStr.equals(lines.get(i))) {
				return i + 1;
			}
		}
		throw new RuntimeException("File(" + file.getAbsolutePath()
				+ ") cannot find line(" + targetStr + ") !!!");
	}

	private BigDecimal getBigDecimal(String sign, String val) {
		if ("X".equals(sign)) {
			return getBigDecimal(val);
		} else {
			return getBigDecimal(sign + val);
		}
	}

	private BigDecimal getBigDecimal(String str) {
		String trimmedStr = str.trim();
		if ("--".equals(trimmedStr)) {
			return null;
		}
		return new BigDecimal(trimmedStr);
	}

	private BigInteger getBigInteger(String str) {
		String trimmedStr = str.trim();
		if ("--".equals(trimmedStr)) {
			return null;
		}
		return new BigInteger(trimmedStr);
	}

	private String getString(String str) {
		return str.trim();
	}

	private boolean isProcessedOfTwse(File file) throws IOException {
		String processedInfo = generateProcessedInfo(file);
		if (processedListOfTwse.contains(processedInfo)) {
			logger.info(processedInfo + " processed before.");
			return true;
		}
		return false;
	}

	private String generateProcessedInfo(File file) {
		return file.getName();
	}

	private void writeToProcessedFileOfTwse(File file) throws IOException {
		String infoLine = generateProcessedInfo(file) + System.lineSeparator();
		FileUtils.write(processedLogOfTwse, infoLine, Charsets.UTF_8, true);
	}

	private StockClosingCondition generateEntity(String stockCode, Date date,
			String[] strArr, Date now) {
		StockClosingCondition entity = new StockClosingCondition();
		entity.new RowKey(stockCode, date, entity);
		generatePriceFamilyContent(entity, strArr, now);
		generateVolumeFamilyContent(entity, strArr, now);
		return entity;
	}

	private void generateVolumeFamilyContent(StockClosingCondition entity,
			String[] strArr, Date now) {
		BigInteger stockAmount = getBigInteger(strArr[2]);
		BigInteger moneyAmount = getBigInteger(strArr[4]);
		BigInteger transactionAmount = getBigInteger(strArr[3]);
		VolumeFamily volumeFamily = entity.getVolumeFamily();
		volumeFamily.add(VolumeQualifier.STOCK_AMOUNT, now, stockAmount);
		volumeFamily.add(VolumeQualifier.MONEY_AMOUNT, now, moneyAmount);
		volumeFamily.add(VolumeQualifier.TRANSACTION_AMOUNT, now,
				transactionAmount);
	}

	private void generatePriceFamilyContent(StockClosingCondition entity,
			String[] strArr, Date now) {
		BigDecimal openingPrice = getBigDecimal(strArr[5]);
		BigDecimal closingPrice = getBigDecimal(strArr[8]);
		BigDecimal change = getBigDecimal(strArr[9], strArr[10]);
		BigDecimal highestPrice = getBigDecimal(strArr[6]);
		BigDecimal lowestPrice = getBigDecimal(strArr[7]);
		BigDecimal finalPurchasePrice = getBigDecimal(strArr[11]);
		BigDecimal finalSellingPrice = getBigDecimal(strArr[13]);
		PriceFamily priceFamily = entity.getPriceFamily();
		priceFamily.add(PriceQualifier.OPENING_PRICE, now, openingPrice);
		priceFamily.add(PriceQualifier.CLOSING_PRICE, now, closingPrice);
		priceFamily.add(PriceQualifier.CHANGE, now, change);
		priceFamily.add(PriceQualifier.HIGHEST_PRICE, now, highestPrice);
		priceFamily.add(PriceQualifier.LOWEST_PRICE, now, lowestPrice);
		priceFamily.add(PriceQualifier.FINAL_PURCHASE_PRICE, now,
				finalPurchasePrice);
		priceFamily.add(PriceQualifier.FINAL_SELLING_PRICE, now,
				finalSellingPrice);

	}

	private File downloadStockClosingConditionOfTwse() {
		try {
			File dir = downloaderOfTwse.downloadStockClosingCondition();
			logger.info(dir.getAbsolutePath() + " download finish.");
			return dir;
		} catch (Exception e) {
			logger.error("Download stock closing condition fail !!!");
			return null;
		}
	}

	private void generateProcessedLogFile() throws IOException {
		if (processedLogOfTwse == null) {
			processedLogOfTwse = new File(downloadDirOfTwse, "processed.log");
			if (processedLogOfTwse.exists() == false) {
				FileUtils.touch(processedLogOfTwse);
			}
		}
	}
}
