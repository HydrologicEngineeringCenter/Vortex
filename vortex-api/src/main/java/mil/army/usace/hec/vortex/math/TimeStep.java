package mil.army.usace.hec.vortex.math;

public enum TimeStep {
    MIN_1(1),
    MIN_2(2),
    MIN_3(3),
    MIN_4(4),
    MIN_5(5),
    MIN_6(6),
    MIN_10(10),
    MIN_15(15),
    MIN_20(20),
    MIN_30(30),
    HR_1(60),
    HR_2(120),
    HR_3(180),
    HR_4(240),
    HR_6(360),
    HR_8(480),
    HR_12(720),
    HR_24(1440),
    WEEK(10080),
    MONTH(43200),
    YEAR(525600);

    private final int intervalInMinutes;

    TimeStep(int intervalInMinutes) {
        this.intervalInMinutes = intervalInMinutes;
    }

    public int intervalInMinutes() {
        return intervalInMinutes;
    }
}
