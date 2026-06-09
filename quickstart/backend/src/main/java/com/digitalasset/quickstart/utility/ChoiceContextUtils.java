package com.digitalasset.quickstart.utility;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Party;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.AnyValue;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.ChoiceContext;

public class ChoiceContextUtils {

     private ChoiceContextUtils() {}

    @SuppressWarnings("unchecked")
    public static ChoiceContext toChoiceContext(Object choiceContextData) {
        if (choiceContextData == null) {
            return new ChoiceContext(Map.of());
        }
        if (!(choiceContextData instanceof Map)) {
            throw new IllegalArgumentException("Unexpected choiceContextData encoding: " + choiceContextData);
        }
        Object values = ((Map<String, Object>) choiceContextData).get("values");
        if (values == null) {
            return new ChoiceContext(Map.of());
        }
        if (!(values instanceof Map)) {
            throw new IllegalArgumentException("Unexpected choiceContextData.values encoding: " + values);
        }
        Map<String, AnyValue> context = new LinkedHashMap<>();
        ((Map<String, Object>) values).forEach((key, raw) -> context.put(key, toAnyValue(raw)));
        return new ChoiceContext(context);
    }

    @SuppressWarnings("unchecked")
    private static AnyValue toAnyValue(Object raw) {
        if (!(raw instanceof Map)) {
            throw new IllegalArgumentException("Unexpected AnyValue encoding: " + raw);
        }
        return AnyValue.fromDynamicValue.apply(((Map<String, Object>) raw).get("value"));
    }

    private static AnyValue toAnyValueContractId(String contractId) {
        return new AnyValue.AnyValue_AV_ContractId(new ContractId<>(contractId));
    }

}
