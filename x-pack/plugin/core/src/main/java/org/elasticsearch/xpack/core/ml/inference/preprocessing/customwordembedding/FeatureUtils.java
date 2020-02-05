/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.core.ml.inference.preprocessing.customwordembedding;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * A collection of messy feature extractors
 */
public final class FeatureUtils {

    private FeatureUtils() {}

    /**
     * Truncates a string to the number of characters that fit in X bytes avoiding multi byte characters being cut in
     * half at the cut off point. Also handles surrogate pairs where 2 characters in the string is actually one literal
     * character.
     *
     * Based on: https://stackoverflow.com/a/35148974/1818849
     *
     */
    public static String truncateToNumValidBytes(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        byte[] sba = text.getBytes(StandardCharsets.UTF_8);
        if (sba.length <= maxLength) {
            return text;
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        // Ensure truncation by having byte buffer = maxBytes
        ByteBuffer bb = ByteBuffer.wrap(sba, 0, maxLength);
        CharBuffer cb = CharBuffer.allocate(maxLength);
        // Ignore an incomplete character
        decoder.onMalformedInput(CodingErrorAction.IGNORE);
        decoder.decode(bb, cb, true);
        decoder.flush(cb);
        return new String(cb.array(), 0, cb.position());
    }

    /**
     * Cleanup text and lower-case it
     * NOTE: This does not do any string compression by removing duplicate tokens
     */
    public static String cleanAndLowerText(String text) {
        // 1. Start with ' ', only if the string already does not start with a space
        String newText = text.startsWith(" ") ? "" : " ";

        // 2. Replace punctuation and whitespace with ' '
        // NOTE: we capture unicode letters AND marks as Nepalese and other languages
        newText += text.replaceAll("[^\\p{L}|\\p{M}|\\s]|\\|", " ");

        // 2.1. Replace spacing modifier characters
        newText = newText.replaceAll("\\p{InSpacing_Modifier_Letters}", " ");

        // 3. Add space at end
        newText += text.endsWith(" ") ? "" : " ";

        // 4. Remove multiple spaces (2 or more) with a single space
        newText = newText.replaceAll("\\p{IsWhite_Space}+", " ");

        // 5. Replace Turkish İ with I (TODO - check this out better...)
        newText = newText.replaceAll("\\u0130", "I");

        return newText.toLowerCase(Locale.ROOT);
    }
}
