package kz.spt.lib.enums;

public enum SyslogTypeEnum {
    PAYMENT_REGISTRY ("");

    private final String type;

    SyslogTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
