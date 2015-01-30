package idv.hsiehpinghan.stockservice.manager.hbase;

import idv.hsiehpinghan.datatypeutility.utility.StringUtility;
import idv.hsiehpinghan.stockdao.entity.StockClosingCondition;
import idv.hsiehpinghan.stockdao.repository.IStockClosingConditionRepository;
import idv.hsiehpinghan.stockservice.manager.IStockClosingConditionManager;
import idv.hsiehpinghan.stockservice.operator.StockClosingConditionDownloader;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockClosingConditionHbaseManager implements
		IStockClosingConditionManager {
	private final String BIG5 = "big5";
	private final String COMMA_STRING = StringUtility.COMMA_STRING;
	private final String EMPTY_STRING = StringUtility.EMPTY_STRING;
	private final String DOUBLE_UOTATION_STRING = StringUtility.DOUBLE_UOTATION_STRING;
	private Logger logger = Logger.getLogger(this.getClass().getName());

	@Autowired
	private StockClosingConditionDownloader downloader;
	@Autowired
	private IStockClosingConditionRepository condRepo;

	@Override
	public boolean updateStockClosingCondition() {
		File dir = downloadStockClosingCondition();
		if (dir == null) {
			return false;
		}
		try {
			int processFilesAmt = saveStockClosingConditionToHBase(dir);
			logger.info("Saved " + processFilesAmt + " files to "
					+ condRepo.getTargetTableName() + ".");
		} catch (Exception e) {
			logger.error("Save financial report to hbase fail !!!");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean hasData(File file, Date date, List<String> lines)
			throws IOException {
		String targetDateStr = DateFormatUtils.format(date, "yyyy年MM月dd日");
		for (String line : lines) {
			if (targetDateStr.startsWith(line)) {
				if (line.endsWith("查無資料")) {
					return false;
				}
				return true;
			}
		}
		throw new RuntimeException("File(" + file.getAbsolutePath()
				+ ") has wrong date !!!");
	}

	private int getStartRow(List<String> lines) {
		String targetStr = "\"證券代號\",\"證券名稱\",\"成交股數\",\"成交筆數\",\"成交金額\",\"開盤價\",\"最高價\",\"最低價\",\"收盤價\",\"漲跌(+/-)\",\"漲跌價差\",\"最後揭示買價\",\"最後揭示買量\",\"最後揭示賣價\",\"最後揭示賣量\",\"本益比\"";
		for (int i = 0, size = lines.size(); i < size; ++i) {
			if (targetStr.equals(lines.get(i))) {
				return i + 1;
			}
		}
		throw new RuntimeException("Cannot find line(" + targetStr + ") !!!");
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

	int saveStockClosingConditionToHBase(File dir) throws ParseException,
			IOException {
		int count = 0;
		Date now = new Date();
		for (File file : dir.listFiles()) {
			// ex. A11220130107ALL.csv
			Date date = DateUtils.parseDate(file.getName().substring(4, 12),
					"yyyyMMdd");
			List<String> lines = FileUtils.readLines(file, BIG5);
			if (hasData(file, date, lines) == false) {
				continue;
			}
			int startRow = getStartRow(lines);
			for (int i = startRow, size = lines.size(); i < size; ++i) {
				String line = lines.get(i).replace(DOUBLE_UOTATION_STRING,
						EMPTY_STRING);
				String[] strArr = line.split(COMMA_STRING);
				String stockCode = getString(strArr[0]);
				if (stockCode.equals(EMPTY_STRING)) {
					break;
				}
				BigDecimal openingPrice = getBigDecimal(strArr[5]);
				BigDecimal closingPrice = getBigDecimal(strArr[8]);
				BigDecimal change = getBigDecimal(strArr[9], strArr[10]);
				BigDecimal highestPrice = getBigDecimal(strArr[6]);
				BigDecimal lowestPrice = getBigDecimal(strArr[7]);
				BigDecimal finalPurchasePrice = getBigDecimal(strArr[11]);
				BigDecimal finalSellingPrice = getBigDecimal(strArr[13]);
				BigInteger stockAmount = getBigInteger(strArr[2]);
				BigInteger moneyAmount = getBigInteger(strArr[4]);
				BigInteger transactionAmount = getBigInteger(strArr[3]);

				// public static final String OPENING_PRICE = "openingPrice";
				// public static final String CLOSING_PRICE = "closingPrice";
				// public static final String CHANGE = "change";
				// public static final String HIGHEST_PRICE = "highestPrice";
				// public static final String LOWEST_PRICE = "lowestPrice";
				// public static final String FINAL_PURCHASE_PRICE =
				// "finalPurchasePrice";
				// public static final String FINAL_SELLING_PRICE =
				// "finalSellingPrice";
				// public static final String STOCK_AMOUNT = "stockAmount";
				// public static final String MONEY_AMOUNT = "moneyAmount";
				// public static final String TRANSACTION_AMOUNT =
				// "transactionAmount";

				StockClosingCondition entity = new StockClosingCondition();
				entity.new RowKey(stockCode, date, entity);
				
//				entity.getPriceFamily().add(qualifier, date, value);

			}

//			if (instanceRepo.exists(stockCode, reportType, year, season) == false) {
//				instanceRepo.put(stockCode, reportType, year, season, objNode,
//						presentIds);
//				logger.info(file.getName() + " saved to "
//						+ instanceRepo.getTargetTableName() + ".");
//			} else {
//				logger.info(file.getName() + " already saved to "
//						+ instanceRepo.getTargetTableName() + ".");
//			}
//			addToDownloadInfoEntity(downloadInfo, stockCode, reportType, year,
//					season);
			++count;

			return 0;
		}
		return count;
	}

	private File downloadStockClosingCondition() {
		try {
			File dir = downloader.downloadStockClosingCondition();
			logger.info(dir.getAbsolutePath() + " download finish.");
			return dir;
		} catch (Exception e) {
			logger.error("Download stock closing condition fail !!!");
			return null;
		}
	}

}
