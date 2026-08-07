package chire.idea.mindustry.run;

import com.intellij.AbstractBundle;

public class MindustryRunBundle extends AbstractBundle {
    public static final MindustryRunBundle BUNDLE = new MindustryRunBundle();

    protected MindustryRunBundle() {
        super("messages.MindustryRunBundle");
    }

    public static String bundle(String key, Object... params) {
        return BUNDLE.getMessage(key, params);
    }
}
