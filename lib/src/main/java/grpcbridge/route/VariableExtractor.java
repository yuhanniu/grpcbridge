package grpcbridge.route;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts variables from the path and body definitions as described by
 * {@link com.google.api.HttpRule}. The vars are specified as
 * {path.to.protobuf.field}. The var can only point to a primitive field
 * types.
 */
final class VariableExtractor {
    private static final String VAR_SEGMENT = "([^/]+)";
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{([\\w.]+)\\}");

    private final Pattern pattern;
    private final List<String> pathVars;
    private final UrlQueryParams queryVars;

    /**
     * Creates new var extractor.
     *
     * @param pattern a pattern to extract the pathVars from
     */
    public VariableExtractor(String pattern) {
        UrlPathAndQuery pathAndQuery = UrlPathAndQuery.parse(pattern);

        this.pathVars = new ArrayList<>();

        StringBuffer pathPatternBuilder = new StringBuffer();
        Matcher matcher = VAR_PATTERN.matcher(pathAndQuery.path());
        int lastMatched = 0;

        while (matcher.find()) {
            String varName = matcher.group().substring(1, matcher.group().length() - 1);
            pathVars.add(varName);
            matcher.appendReplacement(pathPatternBuilder, VAR_SEGMENT);
            lastMatched = matcher.end();
        }

        pathPatternBuilder.append(pathAndQuery.path().substring(lastMatched));
        this.pattern = Pattern.compile(pathPatternBuilder.toString());
        this.queryVars = new UrlQueryParams(pathAndQuery.query());
    }

    /**
     * Checks if the input containsAll the variable extractor pattern.
     *
     * @param input input to parse
     * @return true if the variable extractor containsAll the input
     */
    public boolean matches(String input) {
        UrlPathAndQuery pathAndQuery = UrlPathAndQuery.parse(input);

        if (!pattern.matcher(pathAndQuery.path()).matches()) {
            return false;
        }

        if (!queryVars.containsAll(pathAndQuery.query())) {
            return false;
        }

        return true;
    }

    /**
     * Extracts the variables from the specified input.
     *
     * @param input input to parse
     * @return list of extracted variables
     */
    public List<Variable> extract(String input) {
        UrlPathAndQuery pathAndQuery = UrlPathAndQuery.parse(input);

        List<Variable> result = new ArrayList<>();

        Matcher pathMatcher = pattern.matcher(pathAndQuery.path());
        if (pathMatcher.matches()) {
            for (int i = 0; i < pathVars.size(); i++) {
                result.add(new Variable(pathVars.get(i), pathMatcher.group(1 + i)));
            }
        }

        result.addAll(queryVars.extractVars(pathAndQuery.query()));

        return result;
    }

    @Override public String toString() {
        return pattern.toString();
    }
}
