package jp.kota.bcasim.experiment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Offline report: per-run observations and pointwise Student-t intervals across seeds. */
final class ExperimentReport {
    private ExperimentReport() {}
    static void write(Path directory, List<ExperimentRunner.Outcome> outcomes, String fingerprint) throws IOException {
        Set<String> metrics = new TreeSet<>();
        Map<String, Group> groups = new LinkedHashMap<>();
        int completed = 0;
        for (ExperimentRunner.Outcome outcome : outcomes) {
            Group group = groups.computeIfAbsent(outcome.run.getCondition(), ignored -> new Group(outcome.run));
            group.requested++;
            if (outcome.failure != null) continue;
            completed++;
            group.completed++;
            for (String key : outcome.values.stringPropertyNames()) {
                if (!key.startsWith("metric.")) continue;
                String metric = key.substring(7);
                metrics.add(metric);
                String raw = outcome.values.getProperty(key);
                if (raw.isEmpty()) continue;
                double value = Double.parseDouble(raw);
                if (!Double.isFinite(value)) continue;
                group.stats.computeIfAbsent(metric, ignored -> new Statistics()).add(value);
            }
        }
        StringBuilder summary = new StringBuilder();
        List<String> header = new ArrayList<>(Arrays.asList("runId", "status", "strategy", "attackerShare", "blockDelay", "seed", "processedEvents", "finalTime"));
        header.addAll(metrics); header.add("error");
        csv(summary, header);
        for (ExperimentRunner.Outcome outcome : outcomes) {
            ExperimentPlan.Run run = outcome.run;
            List<String> row = new ArrayList<>(Arrays.asList(run.getId(), outcome.failure == null ? "completed" : "failed",
                run.getStrategy(), Double.toString(run.getAttackerShare()), Double.toString(run.getBlockDelay()),
                Long.toString(run.getConfig().getSeed()), outcome.values.getProperty("processedEvents", ""), outcome.values.getProperty("finalTime", "")));
            for (String metric : metrics) row.add(outcome.values.getProperty("metric." + metric, ""));
            row.add(outcome.failure == null ? "" : outcome.failure);
            csv(summary, row);
        }
        ExperimentFiles.write(directory.resolve("summary.csv"), summary.toString());
        StringBuilder aggregate = new StringBuilder();
        csv(aggregate, Arrays.asList("condition", "strategy", "attackerShare", "blockDelay", "requested", "completed", "metric", "n", "mean", "standardDeviation", "ci95Low", "ci95High"));
        for (Group group : groups.values()) for (String metric : metrics) {
            Statistics s = group.stats.getOrDefault(metric, new Statistics());
            csv(aggregate, Arrays.asList(group.run.getCondition(), group.run.getStrategy(), Double.toString(group.run.getAttackerShare()),
                Double.toString(group.run.getBlockDelay()), Integer.toString(group.requested), Integer.toString(group.completed), metric,
                Integer.toString(s.n), s.n == 0 ? "" : Double.toString(s.mean), s.n < 2 ? "" : Double.toString(s.sd()),
                s.n < 2 ? "" : Double.toString(s.mean - s.margin()), s.n < 2 ? "" : Double.toString(s.mean + s.margin())));
        }
        ExperimentFiles.write(directory.resolve("aggregates.csv"), aggregate.toString());
        ExperimentFiles.write(directory.resolve("report.html"), html(outcomes, new ArrayList<>(groups.values()), metrics, completed, fingerprint));
    }

    private static String html(List<ExperimentRunner.Outcome> outcomes, List<Group> groups, Set<String> metrics,
                               int completed, String fingerprint) {
        StringBuilder h = new StringBuilder("<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>BCASim experiment comparison</title><style>");
        h.append("*{box-sizing:border-box}body{margin:0;background:#f4f6fa;color:#162238;font:15px/1.6 system-ui,sans-serif}main{max-width:1250px;margin:auto;padding:36px 24px}header{padding:24px 0}h1{font-size:34px;line-height:1.2;margin:8px 0}h2{font-size:21px}p{max-width:1000px}.kicker{color:#355caf;font-weight:700;letter-spacing:.1em}.cards{display:flex;gap:16px;flex-wrap:wrap}.card{background:#fff;padding:18px 24px;border:1px solid #dce3ed;border-radius:12px;min-width:170px}.card strong{display:block;font-size:30px}.section{background:#fff;border:1px solid #dce3ed;border-radius:14px;padding:24px;margin-top:24px}.scroll{overflow:auto}table{border-collapse:collapse;width:100%;font-size:13px}th,td{text-align:left;padding:9px 12px;border-bottom:1px solid #e4e9f0;white-space:nowrap}th{background:#f3f6fb}a{color:#2453ad}code{font-size:12px;overflow-wrap:anywhere}.muted{color:#59677d}.failed{color:#a92d36}svg{display:block;min-width:760px;width:100%;height:auto}details{margin:16px 0}summary{cursor:pointer;font-weight:600}</style><main><header><div class=\"kicker\">BCASIM / EXPERIMENTS</div><h1>Compare conditions across seeds</h1>");
        h.append("<p class=\"muted\">Each observation is a complete simulation. Conditions use the same seed list; intervals describe each condition separately and do not test differences between conditions.</p></header><div class=\"cards\">");
        card(h, "Completed runs", Integer.toString(completed));
        card(h, "Conditions", Integer.toString(groups.size()));
        card(h, "Failed runs", Integer.toString(outcomes.size() - completed));
        h.append("</div><p><a href=\"summary.csv\">Per-run CSV</a> · <a href=\"aggregates.csv\">Aggregate CSV</a> · <a href=\"batch.properties\">Batch manifest</a></p>");
        h.append("<div class=\"section\"><h2>Reading the comparison</h2><p>Points show arithmetic means across independent seeds within each condition. Whiskers show pointwise 95% Student-t confidence intervals for the mean (sample standard deviation / √n). One sample has no interval. Undefined values and failed runs are excluded; n is reported for every metric. Small samples and rare events can yield unreliable intervals. Ratio intervals are not clipped to [0, 1].</p><p>Attacker share and strategy refer to node 0, including the honest baseline. Chain metrics use the configured observer node and exclude genesis. Attack success is the simulator's attack-state outcome, not proof of a reversed payment.</p></div>");
        for (String metric : Arrays.asList("attackerRevenueShare", "staleFraction", "attackSuccessRate", "meanPropagationDelay", "transactionsConfirmed")) {
            if (!metrics.contains(metric)) continue;
            h.append("<section class=\"section\"><h2>").append(escape(label(metric))).append("</h2><div class=\"scroll\">");
            chart(h, groups, metric);
            h.append("</div></section>");
        }
        h.append("<section class=\"section\"><h2>All measurements</h2>");
        for (String metric : metrics) {
            h.append("<details><summary>").append(escape(label(metric))).append("</summary><div class=\"scroll\"><table><thead><tr><th>Strategy</th><th>Node 0 share</th><th>Delay (s)</th><th>Completed / planned</th><th>n</th><th>Mean</th><th>95% CI</th></tr></thead><tbody>");
            for (Group group : groups) {
                Statistics s = group.stats.getOrDefault(metric, new Statistics());
                h.append("<tr><td>").append(escape(group.run.getStrategy())).append("</td><td>").append(format(group.run.getAttackerShare()))
                    .append("</td><td>").append(format(group.run.getBlockDelay())).append("</td><td>").append(group.completed).append(" / ").append(group.requested)
                    .append("</td><td>").append(s.n).append("</td><td>").append(s.n == 0 ? "—" : format(s.mean)).append("</td><td>")
                    .append(s.n < 2 ? "—" : format(s.mean - s.margin()) + " to " + format(s.mean + s.margin())).append("</td></tr>");
            }
            h.append("</tbody></table></div></details>");
        }
        h.append("</section><section class=\"section\"><h2>Run files</h2><div class=\"scroll\"><table><thead><tr><th>Run</th><th>Seed</th><th>Strategy</th><th>Share</th><th>Delay</th><th>Status</th></tr></thead><tbody>");
        for (ExperimentRunner.Outcome outcome : outcomes) {
            ExperimentPlan.Run run = outcome.run;
            h.append("<tr><td><a href=\"runs/").append(run.getId()).append("/configuration.properties\">").append(run.getId()).append("</a></td><td>")
                .append(run.getConfig().getSeed()).append("</td><td>").append(escape(run.getStrategy())).append("</td><td>").append(format(run.getAttackerShare()))
                .append("</td><td>").append(format(run.getBlockDelay())).append("</td><td>").append(outcome.failure == null ? "Completed" : "<span class=\"failed\">" + escape(outcome.failure) + "</span>").append("</td></tr>");
        }
        h.append("</tbody></table></div></section><p class=\"muted\">Plan SHA-256: <code>").append(fingerprint).append("</code></p></main></html>");
        return h.toString();
    }

    private static void chart(StringBuilder h, List<Group> groups, String metric) {
        double low = 0, high = 0;
        boolean hasValues = false;
        for (Group group : groups) {
            Statistics s = group.stats.get(metric);
            if (s == null || s.n == 0) continue;
            hasValues = true;
            double margin = s.n < 2 ? 0 : s.margin();
            low = Math.min(low, s.mean - margin); high = Math.max(high, s.mean + margin);
        }
        if (!hasValues) { h.append("<p class=\"muted\">No defined observations.</p>"); return; }
        if (high == low) high = low + 1;
        double span = high - low;
        int height = groups.size() * 38 + 64;
        h.append("<svg xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" aria-label=\"").append(escape(label(metric))).append(" means and 95% confidence intervals\" viewBox=\"0 0 1080 ").append(height).append("\"><title>").append(escape(label(metric))).append(" by condition</title>");
        for (int i = 0; i <= 4; i++) {
            double x = 350 + 560.0 * i / 4;
            h.append("<line x1=\"").append(x).append("\" x2=\"").append(x).append("\" y1=\"12\" y2=\"").append(height - 32).append("\" stroke=\"#e3e9f1\"/>")
                .append("<text x=\"").append(x).append("\" y=\"").append(height - 9).append("\" text-anchor=\"middle\" fill=\"#59677d\" font-size=\"12\">").append(format(low + span * i / 4)).append("</text>");
        }
        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i); Statistics s = group.stats.get(metric); int y = i * 38 + 28;
            String caption = group.run.getStrategy() + " · share " + format(group.run.getAttackerShare()) + " · delay " + format(group.run.getBlockDelay()) + "s";
            h.append("<text x=\"8\" y=\"").append(y + 4).append("\" font-size=\"13\" fill=\"#162238\">").append(escape(caption)).append("</text>");
            if (s == null || s.n == 0) {
                h.append("<text x=\"938\" y=\"").append(y + 4).append("\" font-size=\"12\" fill=\"#59677d\">n=0</text>"); continue;
            }
            double center = 350 + (s.mean - low) / span * 560;
            if (s.n >= 2) {
                double left = 350 + (s.mean - s.margin() - low) / span * 560;
                double right = 350 + (s.mean + s.margin() - low) / span * 560;
                h.append("<path d=\"M").append(left).append(' ').append(y).append("H").append(right).append(" M").append(left).append(' ').append(y - 5).append("V").append(y + 5).append(" M").append(right).append(' ').append(y - 5).append("V").append(y + 5).append("\" stroke=\"#426cc0\" fill=\"none\" stroke-width=\"2\"/>");
            }
            h.append("<circle cx=\"").append(center).append("\" cy=\"").append(y).append("\" r=\"5\" fill=\"#2453ad\"><title>").append(escape(caption)).append(": mean ").append(s.mean).append("; n=").append(s.n).append("</title></circle><text x=\"938\" y=\"").append(y + 4).append("\" font-size=\"12\" fill=\"#59677d\">").append(format(s.mean)).append(" · n=").append(s.n).append("</text>");
        }
        h.append("</svg>");
    }

    private static void card(StringBuilder h, String caption, String value) { h.append("<div class=\"card\"><span class=\"muted\">").append(caption).append("</span><strong>").append(value).append("</strong></div>"); }
    private static String label(String metric) { return metric.replaceAll("([a-z])([A-Z])", "$1 $2"); }
    private static String format(double value) { return String.format(Locale.ROOT, "%.4g", value); }
    private static String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    private static void csv(StringBuilder result, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) result.append(',');
            result.append('"').append(values.get(i).replace("\"", "\"\"")).append('"');
        }
        result.append('\n');
    }

    private static final class Group {
        final ExperimentPlan.Run run;
        final Map<String, Statistics> stats = new LinkedHashMap<>();
        int requested, completed;
        Group(ExperimentPlan.Run run) { this.run = run; }
    }

    static final class Statistics {
        int n;
        double mean, m2;
        void add(double value) { n++; double delta = value - mean; mean += delta / n; m2 += delta * (value - mean); }
        double sd() { return Math.sqrt(Math.max(0, m2 / (n - 1))); }
        double margin() { return t975(n - 1) * sd() / Math.sqrt(n); }
        static double t975(int df) {
            double[] table = {Double.NaN, 12.706204736, 4.302652730, 3.182446305, 2.776445105,
                2.570581836, 2.446911851, 2.364624252, 2.306004135, 2.262157163, 2.228138852,
                2.200985160, 2.178812830, 2.160368656, 2.144786688, 2.131449546, 2.119905299,
                2.109815578, 2.100922040, 2.093024054, 2.085963447, 2.079613845, 2.073873068,
                2.068657610, 2.063898562, 2.059538553, 2.055529439, 2.051830516, 2.048407142,
                2.045229642, 2.042272456};
            if (df < table.length) return table[df];
            double z = 1.959963984540054, z2 = z * z;
            // Cornish-Fisher expansion, accurate to < 0.00001 for df > 30.
            return z + z * (z2 + 1) / (4.0 * df) + z * (5 * z2 * z2 + 16 * z2 + 3) / (96.0 * df * df)
                + z * (3 * z2 * z2 * z2 + 19 * z2 * z2 + 17 * z2 - 15) / (384.0 * df * df * df);
        }
    }
}
