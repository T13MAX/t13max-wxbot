package com.t13max.wxbot.utils;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

/**
 * @author t13max
 * @since 14:59 2024/12/16
 */
@UtilityClass
public class MatcherUtils {

    public final static Pattern WINDOWS_CODE = Pattern.compile("window.code=(\\d+)");
    public final static Pattern WINDOW_REDIRECT_URI = Pattern.compile("window.redirect_uri=\"(\\S+)\";");


}
