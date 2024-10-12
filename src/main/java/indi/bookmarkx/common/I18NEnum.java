package indi.bookmarkx.common;

import java.util.Locale;

/**
 * @author Nonoas
 * @date 2024/10/11
 * @since
 */
public enum I18NEnum {
    CHINESE("简体中文", Locale.CHINA),
    ENGLISH("English", Locale.ENGLISH),
    FRENCH("En français", Locale.FRENCH),
    ;

    private String label;
    private Locale locale;

    I18NEnum(String label, Locale locale) {
        this.label = label;
        this.locale = locale;
    }

    public static I18NEnum getDefault() {
        for (I18NEnum value : values()) {
            if (value.locale.equals(Locale.getDefault())) {
                return value;
            }
        }
        return ENGLISH;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }


    @Override
    public String toString() {
        return label;
    }
}