package idv.hsiehpinghan.botservice.entity;

import idv.hsiehpinghan.botservice.enumeration.Dollar;
import idv.hsiehpinghan.collectionutility.utility.ArrayUtility;
import idv.hsiehpinghan.datatypeutility.utility.LongUtility;
import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseRowKey;
import idv.hsiehpinghan.hbaseassistant.abstractclass.HBaseTable;

import java.util.Date;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * Exchange rate table.
 * 
 * @author thank.hsiehpinghan
 *
 */
public class ExchangeRate extends HBaseTable {
	private RowKey rowKey;

	public ExchangeRate() {
		super();
	}

	@Override
	public HBaseRowKey getRowKey() {
		return rowKey;
	}

	@Override
	public void setRowKey(HBaseRowKey rowKey) {
		this.rowKey = (RowKey) rowKey;
	}

	public class RowKey extends HBaseRowKey {
		private final int DOLLAR_BEGIN = 0;
		private final int DOLLAR_END = DOLLAR_BEGIN + 3;
		private final int DATE_BEGIN = DOLLAR_END;
		private final int DATE_END = DATE_BEGIN + LongUtility.LONG_BYTE_AMOUNT;
		private Dollar dollar;
		private Date date;

		public RowKey(HBaseTable table) {
			super(table);
		}

		public RowKey(Dollar dollar, Date date, HBaseTable table) {
			super(table);
			this.dollar = dollar;
			this.date = date;
		}

		@Override
		public byte[] toBytes() {
			byte[] dollarArr = Bytes.toBytes(dollar.name());
			byte[] dateArr = Bytes.toBytes(date.getTime());
			byte[] all = ArrayUtility.addAll(dollarArr, dateArr);
			return all;
		}

		@Override
		public void fromBytes(byte[] bytes) {
			String dollarStr = new String(ArrayUtils.subarray(bytes,
					DOLLAR_BEGIN, DOLLAR_END));
			this.dollar = Dollar.valueOf(dollarStr);
			long time = Bytes.toLong(ArrayUtils.subarray(bytes, DATE_BEGIN,
					DATE_END));
			this.date = new Date(time);
		}

		public Dollar getDollar() {
			return dollar;
		}

		public void setDollar(Dollar dollar) {
			this.dollar = dollar;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		@Override
		public String toString() {
			return "Key [dollar=" + dollar + ", date=" + date + "]";
		}

	}

}
