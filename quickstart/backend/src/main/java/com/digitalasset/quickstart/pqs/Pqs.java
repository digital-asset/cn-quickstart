// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

package com.digitalasset.quickstart.pqs;

import com.digitalasset.quickstart.utility.LoggingSpanHelper;
import com.digitalasset.transcode.Converter;
import com.digitalasset.transcode.codec.json.JsonStringCodec;
import com.digitalasset.transcode.java.ContractId;
import com.digitalasset.transcode.java.Template;
import com.digitalasset.transcode.java.Utils;
import com.digitalasset.transcode.schema.Dictionary;
import com.digitalasset.transcode.schema.Identifier;
import daml.Daml;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * PQS adapter that provides access to Daml contracts from a Postgres database.
 */
@Component
public class Pqs {

    private static final Logger logger = LoggerFactory.getLogger(Pqs.class);

    private final JdbcTemplate jdbcTemplate;
    private final Dictionary<Converter<String, Object>> json2Dto;

    @Autowired
    public Pqs(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.json2Dto = Utils.getConverters(new JsonStringCodec(true, true), Daml.ENTITIES);
    }

    /**
     * Retrieves all active contracts of a specific template type.
     */
    @WithSpan
    public <T extends Template> CompletableFuture<List<Contract<T>>> active(Class<T> clazz) {
        Identifier identifier = Utils.getTemplateIdByClass(clazz);
        Span span = Span.current();
        Map<String, Object> baseAttrs = Map.of("templateId", identifier.qualifiedName());
        LoggingSpanHelper.setSpanAttributes(span, baseAttrs);
        LoggingSpanHelper.logInfo(logger, "Fetching active contracts", baseAttrs);

        return CompletableFuture
                .supplyAsync(() -> {
                    String sql = "select contract_id, payload from active(?)";
                    List<Contract<T>> results = jdbcTemplate.query(
                            sql,
                            new PqsContractRowMapper<>(identifier),
                            identifier.qualifiedName()
                    );
                    LoggingSpanHelper.setSpanAttributes(
                            span,
                            Map.of("backend.get.active.result.count", results.size())
                    );
                    return results;
                })
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LoggingSpanHelper.logError(logger, "Failed to fetch active contracts", baseAttrs, ex);
                        LoggingSpanHelper.recordException(span, ex);
                    } else {
                        Map<String, Object> successAttrs = Map.of(
                                "templateId", identifier.qualifiedName(),
                                "resultCount", res.size()
                        );
                        LoggingSpanHelper.logInfo(logger, "Fetched active contracts", successAttrs);
                    }
                });
    }

    /**
     * Retrieves all active contracts of a specific template type with a custom WHERE clause.
     */
    @WithSpan
    public <T extends Template> CompletableFuture<List<Contract<T>>> activeWhere(
            Class<T> clazz,
            String whereClause,
            Object... params
    ) {
        Identifier identifier = Utils.getTemplateIdByClass(clazz);
        Span span = Span.current();
        Map<String, Object> baseAttrs = Map.of(
                "templateId", identifier.qualifiedName(),
                "whereClause", whereClause
        );
        LoggingSpanHelper.setSpanAttributes(span, baseAttrs);
        LoggingSpanHelper.logInfo(logger, "Fetching multiple active contracts with custom whereClause", baseAttrs);

        return CompletableFuture
                .supplyAsync(() -> {
                    String sql = "select contract_id, payload from active(?) where " + whereClause;
                    List<Contract<T>> results = jdbcTemplate.query(
                            sql,
                            new PqsContractRowMapper<>(identifier),
                            combineParams(identifier.qualifiedName(), params)
                    );
                    return results;
                })
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LoggingSpanHelper.logError(
                                logger,
                                "Failed to fetch contracts with custom whereClause",
                                baseAttrs,
                                ex
                        );
                        LoggingSpanHelper.recordException(span, ex);
                    } else {
                        LoggingSpanHelper.logInfo(
                                logger,
                                "Fetched active contracts with custom whereClause",
                                baseAttrs
                        );
                    }
                });
    }

    /**
     * Retrieves a single active contract of a specific template type matching a custom WHERE clause.
     */
    @WithSpan
    public <T extends Template> CompletableFuture<Optional<Contract<T>>> singleActiveWhere(
            Class<T> clazz,
            String whereClause,
            Object... params
    ) {
        Identifier identifier = Utils.getTemplateIdByClass(clazz);
        Span span = Span.current();
        Map<String, Object> baseAttrs = Map.of(
                "templateId", identifier.qualifiedName(),
                "whereClause", whereClause
        );
        LoggingSpanHelper.setSpanAttributes(span, baseAttrs);
        LoggingSpanHelper.logInfo(logger, "Fetching single active contract with custom whereClause", baseAttrs);

        return CompletableFuture
                .<Optional<Contract<T>>>supplyAsync(() -> {
                    String sql = "select contract_id, payload from active(?) where " + whereClause;
                    List<Contract<T>> results = jdbcTemplate.query(
                            sql,
                            new PqsContractRowMapper<>(identifier),
                            combineParams(identifier.qualifiedName(), params)
                    );
                    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
                })
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LoggingSpanHelper.logError(
                                logger,
                                "Failed to fetch contract with custom whereClause",
                                baseAttrs,
                                ex
                        );
                        LoggingSpanHelper.recordException(span, ex);
                    } else {
                        LoggingSpanHelper.logInfo(
                                logger,
                                "Fetched single active contract (or none) with custom whereClause",
                                baseAttrs
                        );
                    }
                });
    }

    /**
     * Retrieves a contract by its contract ID from the underlying store.
     */
    @WithSpan
    public <T extends Template> CompletableFuture<Contract<T>> byContractId(
            Class<T> clazz,
            @SpanAttribute("backend.get.contract.id") String id
    ) {
        Identifier identifier = Utils.getTemplateIdByClass(clazz);
        Span span = Span.current();
        Map<String, Object> baseAttrs = Map.of(
                "templateId", identifier.qualifiedName(),
                "contractId", id
        );
        LoggingSpanHelper.setSpanAttributes(span, baseAttrs);
        LoggingSpanHelper.logInfo(logger, "Fetching contract by ID", baseAttrs);

        return CompletableFuture
                .<Contract<T>>supplyAsync(() -> {
                    String sql = "select contract_id, payload from lookup_contract(?)";
                    return jdbcTemplate.queryForObject(
                            sql,
                            new PqsContractRowMapper<>(identifier),
                            id
                    );
                })
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        LoggingSpanHelper.logError(logger, "Failed to fetch contract by ID", baseAttrs, ex);
                        LoggingSpanHelper.recordException(span, ex);
                    } else {
                        LoggingSpanHelper.logInfo(logger, "Fetched contract by ID", baseAttrs);
                    }
                });
    }

    private Object[] combineParams(String qname, Object... params) {
        Object[] combined = new Object[params.length + 1];
        combined[0] = qname;
        System.arraycopy(params, 0, combined, 1, params.length);
        return combined;
    }

    private class PqsContractRowMapper<T extends Template> implements RowMapper<Contract<T>> {
        private final Identifier templateId;

        public PqsContractRowMapper(Identifier templateId) {
            this.templateId = templateId;
        }

        @WithSpan
        @Override
        public Contract<T> mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Contract<>(
                    new ContractId<>(rs.getString("contract_id")),
                    (T) json2Dto.template(templateId).convert(rs.getString("payload"))
            );
        }
    }
}
