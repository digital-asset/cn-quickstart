package com.digitalasset.quickstart.utility;

import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Party;

import daml_stdlib_da_time_types.da.time.types.RelTime;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import splice_api_token_metadata_v1.splice.api.token.metadatav1.AnyValue;

public class ChoiceContextUtils {

    private ChoiceContextUtils() {
        // utility class
    }

    @SuppressWarnings("unchecked")
    public static Map<String, AnyValue> convertChoiceContextData(Object choiceContextData) {
        if (choiceContextData == null) {
            return Map.of();
        }
        if (!(choiceContextData instanceof Map)) {
            throw new IllegalArgumentException("Unexpected choiceContextData encoding: " + choiceContextData);
        }
        Object values = ((Map<String, Object>) choiceContextData).get("values");
        if (values == null) {
            return Map.of();
        }
        if (!(values instanceof Map)) {
            throw new IllegalArgumentException("Unexpected choiceContextData.values encoding: " + values);
        }
        Map<String, AnyValue> context = new LinkedHashMap<>();
        ((Map<String, Object>) values).forEach((key, raw) -> context.put(key, toAnyValue(raw)));
        return context;
    }

    @SuppressWarnings("unchecked")
    private static AnyValue toAnyValue(Object raw) {
        if (!(raw instanceof Map)) {
            throw new IllegalArgumentException("Unexpected AnyValue encoding: " + raw);
        }
        Map<String, Object> tagged = (Map<String, Object>) raw;
        String tag = (String) tagged.get("tag");
        Object value = tagged.get("value");
        if (tag == null) {
            throw new IllegalArgumentException("AnyValue is missing its tag: " + raw);
        }
        switch (tag) {
            case "AV_Text":
                return new AnyValue.AnyValue_AV_Text((String) value);
            case "AV_Int":
                return new AnyValue.AnyValue_AV_Int(Long.parseLong(value.toString()));
            case "AV_Decimal":
                return new AnyValue.AnyValue_AV_Decimal(new BigDecimal(value.toString()));
            case "AV_Bool":
                return new AnyValue.AnyValue_AV_Bool((Boolean) value);
            case "AV_ContractId":
                return new AnyValue.AnyValue_AV_ContractId(new ContractId<>((String) value));
            case "AV_Party":
                return new AnyValue.AnyValue_AV_Party(new Party((String) value));
            case "AV_Date":
                return new AnyValue.AnyValue_AV_Date(LocalDate.parse(value.toString()));
            case "AV_Time":
                return new AnyValue.AnyValue_AV_Time(Instant.parse(value.toString()));
            case "AV_RelTime":
                return new AnyValue.AnyValue_AV_RelTime(new RelTime(toRelTimeMicros(value)));
            case "AV_List":
                return new AnyValue.AnyValue_AV_List(
                        ((List<Object>) value).stream().map(ChoiceContextUtils::toAnyValue).toList());
            case "AV_Map": {
                Map<String, AnyValue> entries = new LinkedHashMap<>();
                ((Map<String, Object>) value).forEach((k, v) -> entries.put(k, toAnyValue(v)));
                return new AnyValue.AnyValue_AV_Map(entries);
            }
            default:
                throw new IllegalArgumentException("Unsupported AnyValue tag: " + tag);
        }
    }

    @SuppressWarnings("unchecked")
    private static long toRelTimeMicros(Object value) {
        Object micros = value instanceof Map
                ? ((Map<String, Object>) value).get("microseconds")
                : value;
        if (micros == null) {
            throw new IllegalArgumentException("AV_RelTime is missing its microseconds: " + value);
        }
        return Long.parseLong(micros.toString());
    }
}
