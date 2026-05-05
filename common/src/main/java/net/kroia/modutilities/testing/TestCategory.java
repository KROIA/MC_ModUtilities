package net.kroia.modutilities.testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestCategory {

    public enum ServerType {
        MASTER_ONLY,
        SLAVE_ONLY,
        BOTH
    }

    private static final List<TestCategory> ALL_CATEGORIES = new ArrayList<>();

    private final String name;
    private final String description;
    private final ServerType serverType;
    private final boolean needsMinecraftContext;

    public TestCategory(String name, String description, ServerType serverType,
                        boolean needsMinecraftContext) {
        this.name = name;
        this.description = description;
        this.serverType = serverType;
        this.needsMinecraftContext = needsMinecraftContext;
        ALL_CATEGORIES.add(this);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ServerType getServerType() {
        return serverType;
    }

    public boolean needsMinecraftContext() {
        return needsMinecraftContext;
    }

    public boolean canRunOn(boolean isSlave) {
        if (serverType == ServerType.BOTH) return true;
        if (isSlave) return serverType == ServerType.SLAVE_ONLY;
        return serverType == ServerType.MASTER_ONLY;
    }

    public static TestCategory fromName(String name) {
        for (TestCategory category : ALL_CATEGORIES) {
            if (category.name.equalsIgnoreCase(name)) {
                return category;
            }
        }
        return null;
    }

    public static List<TestCategory> getAllCategories() {
        return Collections.unmodifiableList(ALL_CATEGORIES);
    }
}
