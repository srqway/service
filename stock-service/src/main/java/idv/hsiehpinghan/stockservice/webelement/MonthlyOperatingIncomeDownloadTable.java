package idv.hsiehpinghan.stockservice.webelement;

import idv.hsiehpinghan.seleniumassistant.webelement.Table;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ListUtils;

public class MonthlyOperatingIncomeDownloadTable extends Table {
	private static final String[] itemNames = { "本月", "去年同期", "增減金額", "增減百分比",
			"本年累計", "去年累計", "增減金額", "增減百分比", "備註" };
	private List<String> targetRowTexts;
	private String currentMonth;
	private String currentMonthOfLastYear;
	private String differentAmount;
	private String differentPercent;
	private String cumulativeAmountOfThisYear;
	private String cumulativeAmountOfLastYear;
	private String cumulativeDifferentAmount;
	private String cumulativeDifferentPercent;
	private String comment;

	public MonthlyOperatingIncomeDownloadTable(Table table) {
		super(table.getWebDriver(), table.getBy());
		checkTitle();
		checkItemName();
		updateFields();
	}

	/**
	 * Get expected row texts.
	 * 
	 * @return
	 */
	public List<String> getTargetRowTexts() {
		if (targetRowTexts == null) {
			targetRowTexts = new ArrayList<String>(2);
			targetRowTexts.add("項目");
			targetRowTexts.add("營業收入淨額");
		}
		return targetRowTexts;
	}

	public String getCurrentMonth() {
		if (currentMonth == null) {
			throw new RuntimeException("CurrentMonth is null !!!");
		}
		return currentMonth;
	}

	public String getCurrentMonthOfLastYear() {
		if (currentMonthOfLastYear == null) {
			throw new RuntimeException("CurrentMonthOfLastYear is null !!!");
		}
		return currentMonthOfLastYear;
	}

	public String getDifferentAmount() {
		if (differentAmount == null) {
			throw new RuntimeException("DifferentAmount is null !!!");
		}
		return differentAmount;
	}

	public String getDifferentPercent() {
		if (differentPercent == null) {
			throw new RuntimeException("DifferentPercent is null !!!");
		}
		return differentPercent;
	}

	public String getCumulativeAmountOfThisYear() {
		if (cumulativeAmountOfThisYear == null) {
			throw new RuntimeException("CumulativeAmountOfThisYear is null !!!");
		}
		return cumulativeAmountOfThisYear;
	}

	public String getCumulativeAmountOfLastYear() {
		if (cumulativeAmountOfLastYear == null) {
			throw new RuntimeException("CumulativeAmountOfLastYear is null !!!");
		}
		return cumulativeAmountOfLastYear;
	}

	public String getCumulativeDifferentAmount() {
		if (cumulativeDifferentAmount == null) {
			throw new RuntimeException("CumulativeDifferentAmount is null !!!");
		}
		return cumulativeDifferentAmount;
	}

	public String getCumulativeDifferentPercent() {
		if (cumulativeDifferentPercent == null) {
			throw new RuntimeException("CumulativeDifferentPercent is null !!!");
		}
		return cumulativeDifferentPercent;
	}

	public String getComment() {
		if (comment == null) {
			throw new RuntimeException("Comment is null !!!");
		}
		return comment;
	}

	public static String[] getItemNames() {
		return itemNames;
	}

	private void checkTitle() {
		List<String> titles = getRowAsStringList(0);
		List<String> targetTitles = getTargetRowTexts();
		if (ListUtils.isEqualList(titles, targetTitles) == false) {
			throw new RuntimeException("TargetTitles(" + targetTitles
					+ ") different from titles(" + titles + ") !!!");
		}
	}

	private void checkItemName() {
		// i = 0 is title.
		for (int i = 1, size = 9; i < size; ++i) {
			String itemName = getTextOfCell(i, 0);
			String targetItemName = itemNames[i - 1];
			if (targetItemName.equals(itemName) == false) {
				throw new RuntimeException("TargetItemName(" + targetItemName
						+ ") different from itemName(" + itemName + ") !!!");
			}
		}
	}

	private void updateFields() {
		currentMonth = getTextOfCell(1, 1).trim();
		currentMonthOfLastYear = getTextOfCell(2, 1).trim();
		differentAmount = getTextOfCell(3, 1).trim();
		differentPercent = getTextOfCell(4, 1).trim();
		cumulativeAmountOfThisYear = getTextOfCell(5, 1).trim();
		cumulativeAmountOfLastYear = getTextOfCell(6, 1).trim();
		cumulativeDifferentAmount = getTextOfCell(7, 1).trim();
		cumulativeDifferentPercent = getTextOfCell(8, 1).trim();
		comment = getTextOfCell(9, 1).trim();
	}
}
