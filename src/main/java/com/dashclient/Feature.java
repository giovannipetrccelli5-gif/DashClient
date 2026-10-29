package com.dashclient;

/** An independent, user-configurable DashClient feature. */
public final class Feature {
    public final String id, name, category, description;
    public final boolean initial;
    public boolean enabled;
    public Feature(String id, String name, String category, String description, boolean initial) {
        this.id=id; this.name=name; this.category=category;
        this.description=description; this.initial=initial; this.enabled=initial;
    }
}