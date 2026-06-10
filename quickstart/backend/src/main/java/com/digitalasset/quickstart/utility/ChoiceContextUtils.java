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
        return switch (AnyValueTag.fromString(tag)) {
            case AV_Text -> new AnyValue.AnyValue_AV_Text((String) value);
            case AV_Int -> new AnyValue.AnyValue_AV_Int((Long)value);
            case AV_Decimal -> new AnyValue.AnyValue_AV_Decimal(new BigDecimal((Double)value));
            case AV_Bool -> new AnyValue.AnyValue_AV_Bool((Boolean) value);
            case AV_ContractId -> new AnyValue.AnyValue_AV_ContractId(new ContractId<>((String) value));
            case AV_Party -> new AnyValue.AnyValue_AV_Party(new Party((String) value));
            case AV_Date -> new AnyValue.AnyValue_AV_Date(LocalDate.parse((String) value));
            case AV_Time -> new AnyValue.AnyValue_AV_Time(Instant.parse((String) value));
            case AV_RelTime -> new AnyValue.AnyValue_AV_RelTime(new RelTime(toRelTimeMicros(value)));
            case AV_List -> new AnyValue.AnyValue_AV_List(
                    ((List<Object>) value).stream().map(ChoiceContextUtils::toAnyValue).toList());
            case AV_Map -> {
                Map<String, AnyValue> entries = new LinkedHashMap<>();
                ((Map<String, Object>) value).forEach((k, v) -> entries.put(k, toAnyValue(v)));
                yield new AnyValue.AnyValue_AV_Map(entries);
            }
        };
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

    private enum AnyValueTag {
        AV_Text, AV_Int, AV_Decimal, AV_Bool, AV_ContractId,
        AV_Party, AV_Date, AV_Time, AV_RelTime, AV_List, AV_Map;

        static AnyValueTag fromString(String tag) {
            try {
                return valueOf(tag);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unsupported AnyValue tag: " + tag, e);
            }
        }
    }
}
