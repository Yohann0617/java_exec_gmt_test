package com.viewsources.chsm.common.utils;

import lombok.extern.slf4j.Slf4j;
import com.sun.jna.Library;
import com.sun.jna.Native;

import java.util.function.Function;



/**
 * <p>
 * 扑克检测算法-C语言实现，Java调用 （m=2）
 * </p >
 *
 * @author Yohann
 * @since 2022/9/14 11:11
 */
@Slf4j
public class PokerUtils {

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    // 判断系统
    public static String JudgeSystem() {
        if (isLinux()) {
            return "linux";
        } else if (isWindows()) {
            return "windows";
        } else {
            return "other system";
        }
    }

    /**
     * 加载动态库，定义动态库中的方法
     **/
    public interface CLibrary extends Library {
        CLibrary INSTANCE = Native.load("windows".equals(JudgeSystem()) ? "libpoker_utils.dll" : "libpoker.so", CLibrary.class);

        // 扑克检测
        int PokerDetect(int M, int n, byte[] epsilon);
    }

    /**
     * 使用检测-单次检测
     *
     * @param randomFunc Integer是字节数     返回值是随机数字节数组
     * @return 是否通过
     */
    public static boolean powerOnPokerDetect(Function<Integer, byte[]> randomFunc) {
        CLibrary instance = CLibrary.INSTANCE;
        int errorTimes = 0;
        for (int i = 0; i < 20; i++) {
            byte[] apply = randomFunc.apply(10000/8);
            // 扑克检测
            int ret = instance.PokerDetect(2, 10000, apply);

            if (ret != 1) {
                ++errorTimes;
            }
        }
        log.info("【使用检测-单次检测】第一次未通过次数：" + errorTimes);

        // 允许重复1次检测
        if (errorTimes >= 2) {
            int errorTimes2 = 0;
            for (int i = 0; i < 20; i++) {
                byte[] apply = randomFunc.apply(10000/8);
                // 扑克检测
                int ret = instance.PokerDetect(2, 10000, apply);

                if (ret != 1) {
                    ++errorTimes2;
                }
            }
            log.info("【使用检测-单次检测】第二次未通过次数：" + errorTimes2);
            return errorTimes2 < 2;
        }
        return true;
    }

    /**
     * 周期检测
     *
     * @param randomFunc Integer是字节数     返回值是随机数字节数组
     * @return 是否通过
     */
    public static boolean cyclePokerDetect(Function<Integer, byte[]> randomFunc) {
        CLibrary instance = CLibrary.INSTANCE;
        int errorTimes = 0;
        for (int i = 0; i < 5; i++) {
            byte[] apply = randomFunc.apply(10000/8);
            // 扑克检测
            int ret = instance.PokerDetect(2, 10000, apply);
//            System.out.println(Arrays.toString(apply));

            if (ret != 1) {
                ++errorTimes;
            }
        }
        log.info("【周期检测】第一次errorTimes:" + errorTimes);

        // 允许重复1次检测
        if (errorTimes >= 1) {
            int errorTimes2 = 0;
            for (int i = 0; i < 5; i++) {
                byte[] apply = randomFunc.apply(10000/8);
                // 扑克检测
                int ret = instance.PokerDetect(2, 10000, apply);

                if (ret != 1) {
                    ++errorTimes2;
                }
            }
            log.info("【周期检测】第二次errorTimes:" + errorTimes2);
            return errorTimes2 < 1;
        }

        return true;
    }

}
