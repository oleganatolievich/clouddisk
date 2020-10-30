package common;

public enum SignalByte {

    AUTHORIZATION(1),
    FILE(2);

    private final int value;

    SignalByte(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}