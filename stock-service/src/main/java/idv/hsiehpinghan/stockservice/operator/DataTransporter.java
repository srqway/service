package idv.hsiehpinghan.stockservice.operator;

import idv.hsiehpinghan.hbaseassistant.utility.ByteConvertUtility;
import idv.hsiehpinghan.stockdao.entity.Xbrl;
import idv.hsiehpinghan.stockdao.entity.Xbrl.RowKey;
import idv.hsiehpinghan.stockdao.enumeration.ReportType;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataTransporter {
	// private Logger logger = Logger.getLogger(this.getClass().getName());
//	@Autowired
//	private PigAssistant pigAssist;
//
//	public void getXbrlFromHbase(File targetDirectory) throws IOException {
//		String dataName = "data";
//		String beginStockCode = "1258";
//		Xbrl entity = new Xbrl();
//		String famName = entity.getRatioDifferenceFamily()
//				.getColumnFamilyName();
//		RowKey beginRowKey = entity.new RowKey(beginStockCode,
//				ReportType.CONSOLIDATED_STATEMENT, 0, 0, entity);
//
//		// System.err.println(rowKey);
//		String endStockCode = ByteConvertUtility
//				.incrementString(beginStockCode);
//
//		RowKey endRowKey = entity.new RowKey(endStockCode,
//				ReportType.CONSOLIDATED_STATEMENT, 0, 0, entity);
//
//		// System.err.println(endRowKey);
//
//		String query = dataName + " = load 'hbase://" + entity.getTableName()
//				+ "' using org.apache.pig.backend.hadoop.hbase.HBaseStorage('"
//				+ famName + ":*', '-loadKey true -ignoreWhitespace false -gte "
//				+ beginRowKey.getHexString() + " -lt "
//				+ endRowKey.getHexString() + "') as (id, " + famName
//				+ ":map[]);";
//
//		System.err.println(query);
//
//		pigAssist.runQuery(query);
//		pigAssist.store(targetDirectory, dataName);
//	}
}
