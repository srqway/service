package idv.hsiehpinghan.stockservice.manager.hbase;


public class MonthlyOperatingIncomeHbaseManager {
	
}
// @Service
// public class MonthlyOperatingIncomeHbaseManager implements
// IMonthlyOperatingIncomeHbaseManager, InitializingBean {
// private final String COMMA_STRING = StringUtility.COMMA_STRING;
// private final String[] EXTENSIONS = { "csv" };
// private final String UTF8 = "utf8";
// private Logger logger = Logger.getLogger(this.getClass().getName());
// private File downloadDir;
// private File processedLog;
//
// @Autowired
// private StockServiceProperty stockServiceProperty;
// @Autowired
// private MonthlyOperatingIncomeDownloader downloader;
// @Autowired
// private IMonthlyOperatingIncomeRepository incomeRepo;
//
// @Override
// public void afterPropertiesSet() throws Exception {
// downloadDir = stockServiceProperty
// .getMonthlyOperatingIncomeDownloadDir();
// generateProcessedLog();
// }
//
// @Override
// public synchronized boolean updateMonthlyOperatingIncome() {
// File dir = downloadMonthlyOperatingIncome();
// if (dir == null) {
// return false;
// }
// try {
// int processFilesAmt = saveMonthlyOperatingIncomeToHBase(dir);
// logger.info("Saved " + processFilesAmt + " files to "
// + incomeRepo.getTargetTableName() + ".");
// } catch (Exception e) {
// logger.error("Update monthly operating income fail !!!");
// e.printStackTrace();
// return false;
// }
// return true;
// }
//
// int saveMonthlyOperatingIncomeToHBase(File dir) throws IOException,
// NoSuchFieldException, SecurityException, IllegalArgumentException,
// IllegalAccessException, NoSuchMethodException,
// InvocationTargetException, InstantiationException {
// int count = 0;
// Date now = new Date();
// List<String> processedList = FileUtils.readLines(processedLog);
// // ex. 1101_201301.csv
// for (File file : FileUtils.listFiles(dir, EXTENSIONS, true)) {
// if (isProcessed(processedList, file)) {
// continue;
// }
// List<String> lines = FileUtils.readLines(file, UTF8);
// int startRow = getStartRow(file, lines);
// String[] fnStrArr = file.getName().split("[_.]");
// String stockCode = fnStrArr[0];
// int year = Integer.valueOf(fnStrArr[1].substring(0, 4));
// int month = Integer.valueOf(fnStrArr[1].substring(4));
// int size = lines.size();
// List<MonthlyOperatingIncome> entities = new
// ArrayList<MonthlyOperatingIncome>(
// size - startRow);
// for (int i = startRow; i < size; ++i) {
// String line = lines.get(i);
// String[] strArr = line.split(COMMA_STRING);
// BigInteger currentMonth = new BigInteger(strArr[0]);
// BigInteger currentMonthOfLastYear = new BigInteger(strArr[1]);
// BigInteger differentAmount = new BigInteger(strArr[2]);
// BigDecimal differentPercent = new BigDecimal(strArr[3]);
// BigInteger cumulativeAmountOfThisYear = new BigInteger(
// strArr[4]);
// BigInteger cumulativeAmountOfLastYear = new BigInteger(
// strArr[5]);
// BigInteger cumulativeDifferentAmount = new BigInteger(strArr[6]);
// BigDecimal cumulativeDifferentPercent = new BigDecimal(
// strArr[7]);
// String comment = null;
// if (strArr.length > 8) {
// comment = strArr[8];
// }
// MonthlyOperatingIncome entity = generateEntity(stockCode, now,
// year, month, now, currentMonth, currentMonthOfLastYear,
// differentAmount, differentPercent,
// cumulativeAmountOfThisYear, cumulativeAmountOfLastYear,
// cumulativeDifferentAmount, cumulativeDifferentPercent,
// comment);
// entities.add(entity);
// }
// incomeRepo.put(entities);
// logger.info(file.getName() + " saved to "
// + incomeRepo.getTargetTableName() + ".");
// writeToProcessedFile(file);
// ++count;
// }
// return count;
// }
//
// private void writeToProcessedFile(File file) throws IOException {
// String infoLine = generateProcessedInfo(file) + System.lineSeparator();
// FileUtils.write(processedLog, infoLine, Charsets.UTF_8, true);
// }
//
// private MonthlyOperatingIncome generateEntity(String stockCode, Date now,
// int year, int month, Date date, BigInteger currentMonth,
// BigInteger currentMonthOfLastYear, BigInteger differentAmount,
// BigDecimal differentPercent, BigInteger cumulativeAmountOfThisYear,
// BigInteger cumulativeAmountOfLastYear,
// BigInteger cumulativeDifferentAmount,
// BigDecimal cumulativeDifferentPercent, String comment) {
// MonthlyOperatingIncome entity = new MonthlyOperatingIncome();
// entity.new RowKey(stockCode, entity);
// generateDataFamilyContent(entity, year, month, date, currentMonth,
// currentMonthOfLastYear, differentAmount, differentPercent,
// cumulativeAmountOfThisYear, cumulativeAmountOfLastYear,
// cumulativeDifferentAmount, cumulativeDifferentPercent);
// generateCommentFamilyContent(entity, year, month, date, comment);
// return entity;
// }
//
// private void generateDataFamilyContent(MonthlyOperatingIncome entity,
// int year, int month, Date date, BigInteger currentMonth,
// BigInteger currentMonthOfLastYear, BigInteger differentAmount,
// BigDecimal differentPercent, BigInteger cumulativeAmountOfThisYear,
// BigInteger cumulativeAmountOfLastYear,
// BigInteger cumulativeDifferentAmount,
// BigDecimal cumulativeDifferentPercent) {
// DataFamily dataFamily = entity.getDataFamily();
// dataFamily.add(year, month, date, currentMonth, currentMonthOfLastYear,
// differentAmount, differentPercent, cumulativeAmountOfThisYear,
// cumulativeAmountOfLastYear, cumulativeDifferentAmount,
// cumulativeDifferentPercent);
// }
//
// private void generateCommentFamilyContent(MonthlyOperatingIncome entity,
// int year, int month, Date date, String comment) {
// CommentFamily commentFamily = entity.getCommentFamily();
// commentFamily.add(year, month, date, comment);
// }
//
// private int getStartRow(File file, List<String> lines) {
// String targetStr = getTargetStartRowString();
// for (int i = 0, size = lines.size(); i < size; ++i) {
// if (targetStr.equals(lines.get(i))) {
// return i + 1;
// }
// }
// throw new RuntimeException("File(" + file.getAbsolutePath()
// + ") cannot find line(" + targetStr + ") !!!");
// }
//
// private String getTargetStartRowString() {
// return "本月,去年同期,增減金額,增減百分比,本年累計,去年累計,增減金額,增減百分比,備註";
// }
//
// private File downloadMonthlyOperatingIncome() {
// try {
// File dir = downloader.downloadMonthlyOperatingIncome();
// logger.info(dir.getAbsolutePath() + " download finish.");
// return dir;
// } catch (Exception e) {
// logger.error("Download monthly operating income fail !!!");
// return null;
// }
// }
//
// private void generateProcessedLog() throws IOException {
// if (processedLog == null) {
// processedLog = new File(downloadDir, "processed.log");
// if (processedLog.exists() == false) {
// FileUtils.touch(processedLog);
// }
// }
// }
//
// private boolean isProcessed(List<String> processedList, File file)
// throws IOException {
// String processedInfo = generateProcessedInfo(file);
// if (processedList.contains(processedInfo)) {
// logger.info(processedInfo + " processed before.");
// return true;
// }
// return false;
// }
//
// private String generateProcessedInfo(File file) {
// return file.getName();
// }
// }
