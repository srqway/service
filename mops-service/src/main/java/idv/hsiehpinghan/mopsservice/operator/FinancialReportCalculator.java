package idv.hsiehpinghan.mopsservice.operator;

import idv.hsiehpinghan.xbrlassistant.assistant.XbrlAssistant;
import idv.hsiehpinghan.xbrlassistant.xbrl.Calculation;
import idv.hsiehpinghan.xbrlassistant.xbrl.Presentation;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class FinancialReportCalculator {
	private String[][] ids;
	@Autowired
	private XbrlAssistant xbrlAssistant;

	public FinancialReportCalculator() {
		ids = new String[4][];
		ids[0] = new String[] { Presentation.Id.BalanceSheet,
				Calculation.Id.BalanceSheet };
		// ids[1] = new String[]{Presentation.Id.StatementOfComprehensiveIncome,
		// Calculation.Id.StatementOfComprehensiveIncome};
		// ids[2] = new String[]{Presentation.Id.StatementOfCashFlows,
		// Calculation.Id.StatementOfCashFlows};
		// ids[3] = new String[]{Presentation.Id.StatementOfChangesInEquity,
		// Calculation.Id.StatementOfChangesInEquity};
	}

	public ObjectNode getFinancialReportJson(File instanceFile)
			throws Exception {
		for (int i = 0, size = ids.length; i < size; ++i) {
			ObjectNode presentNode = xbrlAssistant.getPresentationJson(
					instanceFile, ids[i][0]);
			ObjectNode calNode = xbrlAssistant.getCalculationJson(instanceFile,
					ids[i][1]);
			List<String> contextTypes = getContextTypes(presentNode);
			// ObjectNode subFinancialReportJson =
			// generateSubFinancialReportJson(
			// presentNode, calNode, contextTypes);
		}

		return null;
	}

	private ObjectNode generateSubFinancialReportJson(ObjectNode presentNode,
			ObjectNode calNode, String contextType) {
		Map<String, BigDecimal> valueMap = new HashMap<String, BigDecimal>();
		fillUnsummedValueMap(presentNode, contextType, valueMap);
		return null;
	}

	void fillSumValueMap(Map<String, BigDecimal> valueMap, JsonNode node) {
		Iterator<Entry<String, JsonNode>> iter = node.fields();
		while (iter.hasNext()) {
			Entry<String, JsonNode> entry = iter.next();
			JsonNode subNode = entry.getValue();
			if (subNode.isObject() == false) {
				continue;
			}
			fillSumValueMap(valueMap, subNode);
			BigDecimal totalValue = getTotalValue(valueMap, subNode);
			String key = entry.getKey();
			if (totalValue != null && valueMap.get(key).equals(BigDecimal.ZERO)) {
				valueMap.put(key, totalValue);
			}
		}
	}

	private BigDecimal getTotalValue(Map<String, BigDecimal> valueMap,
			JsonNode node) {
		Iterator<Entry<String, JsonNode>> iter = node.fields();
		BigDecimal result = null;
		while (iter.hasNext()) {
			Entry<String, JsonNode> entry = iter.next();
			JsonNode subNode = entry.getValue();
			if (subNode.isObject() == false) {
				continue;
			}
			if (result == null) {
				result = BigDecimal.ZERO;
			}
			String wt = subNode.get(Calculation.Attribute.WEIGHT).asText();
			BigDecimal weight = new BigDecimal(wt);
			BigDecimal value = valueMap.get(entry.getKey());

			if (result == null || weight == null || value == null) {
				System.err.println(entry.getKey());
			}
			result.add(weight.multiply(value));
		}
		return result;
	}

	void fillUnsummedValueMap(JsonNode node, String contextType,
			Map<String, BigDecimal> valueMap) {
		Iterator<Entry<String, JsonNode>> iter = node.fields();
		while (iter.hasNext()) {
			Entry<String, JsonNode> entry = iter.next();
			JsonNode vNode = entry.getValue();
			if (vNode.isObject() == false) {
				continue;
			}
			JsonNode values = vNode.get(Presentation.Attribute.VALUES);
			if (values == null) {
				valueMap.put(entry.getKey(), BigDecimal.ZERO);
				fillUnsummedValueMap(vNode, contextType, valueMap);
			} else {
				JsonNode vNod = values.get(contextType);
				String unit = vNod.get(Presentation.Attribute.UNIT).asText();
				BigDecimal value = new BigDecimal(vNod.get(
						Presentation.Attribute.VALUE).asText());
				if (Presentation.Unit.TWD.equals(unit)) {
					valueMap.put(entry.getKey(), value);
				} else if (Presentation.Unit.SHARES.equals(unit)) {
					valueMap.put(entry.getKey(), value);
				} else {
					throw new RuntimeException("Unit(" + unit
							+ ") Not implements !!!");
				}
			}
		}
	}

	private List<String> getContextTypes(JsonNode presentNode) {
		ObjectNode vNode = (ObjectNode) getValueObjectNode(presentNode);
		Iterator<String> iter = vNode.fieldNames();
		List<String> result = new ArrayList<String>();
		while (iter.hasNext()) {
			result.add(iter.next());
		}
		return result;
	}

	JsonNode getValueObjectNode(JsonNode presentNode) {
		Iterator<Entry<String, JsonNode>> iter = presentNode.fields();
		while (iter.hasNext()) {
			Entry<String, JsonNode> entry = iter.next();
			if (Presentation.Attribute.VALUES.equals(entry.getKey())) {
				return (ObjectNode) entry.getValue();
			}
			JsonNode result = getValueObjectNode(entry.getValue());
			if (result != null) {
				return result;
			}
		}
		return null;
	}
}
