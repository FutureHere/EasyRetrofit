package com.hly.easyretrofit.util;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * Created by hly on 2016/10/29.
 * email hly910206@gmail.com
 */

public class CommonUtils {
    /**
     * 获取CPU核心数
     *
     * @return
     */
    public static int getNumCores() {

        class CpuFilter implements FileFilter {
            @Override
            public boolean accept(File pathname) {
                // Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {
            // Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            // Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());
            // Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {
            e.printStackTrace();
            // Default to return 1 core
            return 1;
        }
    }

}
