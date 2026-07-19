package chire.idea.mindustry.generators;

public class MindustryProjectModel{
    public PluginCoordinates pluginCoordinates = new PluginCoordinates();

    public String mainClassName = "example";
    public String packageClassName = "ExampleJavaMod";

    public static class PluginCoordinates{
        public String displayName = "Java Mod Template";
        public String name = "example-java-mod";
        public String author = "You";
        public String main = "example.ExampleJavaMod";
        public String description = "A Mindustry Java mod template.";
        public String version = "1.0";
        public String minGameVersion = "158";
    }
}
