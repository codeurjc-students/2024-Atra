package codeurjc_students.ATRA.model.auxiliary;

public enum VisibilityType {
    PRIVATE(0), //only you can see it
    MURAL_SPECIFIC(1), //only specified murals can see it
    MURAL_PUBLIC(2), //all murals can see it
    PUBLIC(2); //anyone can access it ( /api/activities/{activityId} will return the activity)

    private final int intValue;

    VisibilityType(int value) {
        this.intValue = value;
    }

    public int getIntValue() {
        return intValue;
    }

    public boolean isSameScopeAs(VisibilityType other) {
        return this.intValue == other.intValue;
    }

    public boolean isMorePublicThan(VisibilityType other) {
        return this.intValue > other.intValue;
    }

    public boolean isLessPublicThan(VisibilityType other) {
        return this.intValue < other.intValue;
    }

    public boolean isMorePrivateThan(VisibilityType other) {
        return this.intValue < other.intValue;
    }

    public boolean isLessPrivateThan(VisibilityType other) {
        return this.intValue > other.intValue;
    }
}
