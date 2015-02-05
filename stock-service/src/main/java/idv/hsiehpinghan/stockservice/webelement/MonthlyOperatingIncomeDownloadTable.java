package idv.hsiehpinghan.stockservice.webelement;

import idv.hsiehpinghan.seleniumassistant.webelement.Table;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ListUtils;

public class MonthlyOperatingIncomeDownloadTable extends Table {
	// private final int DOWNLOAD_BUTTON_COLUMN_INDEX = 2;
	// private final String ONCLICK = "onclick";
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

	private String getTargetItemName(int index) {
		switch (index) {
		case 1:
			return "本月";
		case 2:
			return "去年同期";
		case 3:
			return "增減金額";
		case 4:
			return "增減百分比";
		case 5:
			return "本年累計";
		case 6:
			return "去年累計";
		case 7:
			return "增減金額";
		case 8:
			return "增減百分比";
		case 9:
			return "備註";
		default:
			return "";
		}
	}
	
	private void checkTitle() {
		List<String> titles = getRowAsStringList(0);
		List<String> targetTitles = getTargetRowTexts();
		if(ListUtils.isEqualList(titles, targetTitles) == false) {
			throw new RuntimeException("TargetTitles(" + targetTitles + ") different from titles(" + titles + ") !!!");
		}
	}
	private void checkItemName() {
		// i = 0 is title.
		for (int i = 1, size = 9; i < size; ++i) {
			String itemName = getTextOfCell(i, 0);
			String targetItemName = getTargetItemName(i);
			if(targetItemName.equals(itemName) == false) {
				throw new RuntimeException("TargetItemName(" + targetItemName + ") different from itemName(" + itemName + ") !!!");
			}
		}
	}
	
	private void updateFields() {
		// i = 0 is title.
		for (int i = 1, size = getRowSize(); i < size; ++i) {
			String name = getTextOfCell(i, 0);
			String value = getTextOfCell(i, 1).trim();
			if ("本月".equals(name)) {
				currentMonth = value;
			} else if ("去年同期".equals(name)) {
				currentMonthOfLastYear = value;
			} else if ("增減金額".equals(name)) {
				differentAmount = value;
			} else if ("增減百分比".equals(name)) {
				differentPercent = value;
			} else if ("本年累計".equals(name)) {
				cumulativeAmountOfThisYear = value;
			} else if ("去年累計".equals(name)) {
				cumulativeAmountOfLastYear = value;
			} else if ("增減金額".equals(name)) {
				cumulativeDifferentAmount = value;
			} else if ("增減百分比".equals(name)) {
				cumulativeDifferentPercent = value;
			} else if ("備註".equals(name)) {
				comment = value;
			}
		}
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

}
