package idv.hsiehpinghan.mopsservice.operator;

import idv.hsiehpinghan.xbrlassistant.assistant.XbrlAssistant;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FinancialReportCalculator {
	private List<String> presentIds;
	@Autowired
	private XbrlAssistant xbrlAssistant;
	@Autowired
	private ObjectMapper objectMapper;

	public FinancialReportCalculator() {
		presentIds = new ArrayList<String>(4);
		presentIds.add(Presentation.Id.BalanceSheet);
		presentIds.add(Presentation.Id.StatementOfComprehensiveIncome);
		presentIds.add(Presentation.Id.StatementOfCashFlows);
		presentIds.add(Presentation.Id.StatementOfChangesInEquity);
	}

	/**
	 * Get json format financial report.
	 * 
	 * @param instanceFile
	 * @param date
	 * @return
	 * @throws Exception
	 */
	public ObjectNode getJsonFinancialReport(File instanceFile, Date date)
			throws Exception {
		ObjectNode resultNode = objectMapper.createObjectNode();
		for (String presentId : presentIds) {
			ObjectNode presentNode = xbrlAssistant.getPresentationJson(
					instanceFile, presentId);
			ObjectNode subResultNode = convertToTwDallar(presentNode, date);
			resultNode.set(presentId, subResultNode);
		}
		return resultNode;
	}

	private ObjectNode convertToTwDallar(ObjectNode presentNode, Date date) {
		ObjectNode subResultNode = objectMapper.createObjectNode();
		processSubElement(subResultNode, presentNode, date);
		return subResultNode;
	}

	private void processSubElement(ObjectNode node, JsonNode presentNode,
			Date date) {
		Iterator<Entry<String, JsonNode>> iter = presentNode.fields();
		while (iter.hasNext()) {
			Entry<String, JsonNode> entry = iter.next();
			String key = entry.getKey();
			JsonNode subPresentNode = entry.getValue();
			if (subPresentNode.isObject() == false) {
				node.set(key, subPresentNode);
				continue;
			}
			ObjectNode subNode = objectMapper.createObjectNode();
			node.set(key, subNode);
			if (isValuesObjectNode(entry.getKey())) {
				processValuesObjectNode(subNode, subPresentNode, date);
				continue;
			}
			processSubElement(subNode, subPresentNode, date);
		}
	}

	private boolean isValuesObjectNode(String key) {
		return Presentation.Attribute.VALUES.equals(key);
	}

	private void processValuesObjectNode(ObjectNode node, JsonNode valuesNode,
			Date date) {
		// ex. {
		// "AsOf20130331":{
		// "value":"18994984000",
		// "unit":"TWD",
		// "periodType":"instant",
		// "instant":"20130331"
		// },
		// "AsOf20121231":{
		// "value":"25611406000",
		// "unit":"TWD",
		// "periodType":"instant",
		// "instant":"20121231"
		// }
		Iterator<Entry<String, JsonNode>> fields = valuesNode.fields();
		while (fields.hasNext()) {
			Entry<String, JsonNode> ent = fields.next();
			String contextType = ent.getKey();
			JsonNode contextNode = ent.getValue();
			BigDecimal value = new BigDecimal(contextNode.get(
					Presentation.Attribute.VALUE).asText());
			String unit = contextNode.get(Presentation.Attribute.UNIT).asText();
			if (Presentation.Unit.TWD.equals(unit)) {
				node.put(contextType, value);
			} else if (Presentation.Unit.SHARES.equals(unit)) {
				node.put(contextType, value);
			} else {
				BigDecimal rate = getExchangeRate(date, unit);
				// return rate.multiply(value);
				throw new RuntimeException("Unit(" + unit
						+ ") not implement !!!");
			}
		}
	}

	private BigDecimal getExchangeRate(Date date, String unit) {
		return new BigDecimal("30");
	}
}
