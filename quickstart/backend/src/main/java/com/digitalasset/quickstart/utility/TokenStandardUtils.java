package com.digitalasset.quickstart.utility;

import java.util.Map;
import splice_api_token_metadata_v1.splice.api.token.metadatav1.Metadata;

public class TokenStandardUtils {

    private TokenStandardUtils() {}

    public static Metadata toTokenStandardMetadata(Map<String, String> meta) {
        return new Metadata(meta);
    }

}
