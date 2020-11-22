/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package arina.utils;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.NoSuchLanguageException;
import org.apache.camel.builder.ExpressionBuilder;
import org.apache.camel.spi.Language;
import org.apache.camel.util.StringHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
    Part of this code copied from Apache-Camel source code (ToDynamicDefinition.java)
 */

public class LanguageUtils
{
    private static final Pattern RAW_PATTERN = Pattern.compile("RAW\\([^\\)]+\\)");

    private static class Pair {
        int left;
        int right;
        Pair(int left, int right) {
            this.left = left;
            this.right = right;
        }
    }

    private static List<Pair> checkRAW(String s) {
        Matcher matcher = RAW_PATTERN.matcher(s);
        List<Pair> answer = new ArrayList<>();
        // Check all occurrences
        while (matcher.find()) {
            answer.add(new Pair(matcher.start(), matcher.end() - 1));
        }
        return answer;
    }

    private static boolean isRaw(int index, List<Pair>pairs) {
        for (Pair pair : pairs) {
            if (index < pair.left) {
                return false;
            } else {
                    if (index <= pair.right) {
                        return true;
                    }
            }
        }
        return false;
    }

    /**
     * We need to split the string safely for each + sign, but avoid splitting within RAW(...).
     */
    private static String[] safeSplitRaw(String s) {
        List<String> list = new ArrayList<>();

        if (!s.contains("+")) {
            // no plus sign so there is only one part, so no need to split
            list.add(s);
        } else {
            // there is a plus sign so we need to split in a safe manner
            List<Pair> rawPairs = checkRAW(s);
            StringBuilder sb = new StringBuilder();
            char chars[] = s.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char ch = chars[i];
                if (ch != '+' || isRaw(i, rawPairs)) {
                    sb.append(ch);
                } else {
                    list.add(sb.toString());
                    sb.setLength(0);
                }
            }
            // any leftover?
            if (sb.length() > 0) {
                list.add(sb.toString());
                sb.setLength(0);
            }
        }

        return list.toArray(new String[0]);
    }

    public static <T> T evaluate(Exchange exchange, String exprValue, Class<T> tClass)
    {
        if(exprValue == null)
            return null;

        List<Expression> list = new ArrayList<>();
        String[] parts = safeSplitRaw(exprValue);

        for (String part : parts) {
            // the part may have optional language to use, so you can mix languages
            String value = StringHelper.after(part, "language:");
            if (value != null) {
                String before = StringHelper.before(value, ":");
                String after = StringHelper.after(value, ":");
                if (before != null && after != null) {
                    // maybe its a language, must have language: as prefix
                    try {
                        Language partLanguage = exchange.getContext().resolveLanguage(before);
                        if (partLanguage != null) {
                            Expression exp = partLanguage.createExpression(after);
                            list.add(exp);
                            continue;
                        }
                    } catch (NoSuchLanguageException e) {
                        // ignore
                    }
                }
            }
            // fallback and use simple language
            Language lan = exchange.getContext().resolveLanguage("simple");
            Expression exp = lan.createExpression(part);
            list.add(exp);
        }

        Expression expr;
        if (list.size() == 1) {
            expr = list.get(0);
        } else {
            expr = ExpressionBuilder.concatExpression(list);
        }

        //Expression expr = SimpleLanguage.expression(exprValue);
        return expr.evaluate(exchange, tClass);
    }
}
