package com.viewsources.chsm.host.business.impl;

import cn.hutool.core.io.FileUtil;
import com.viewsources.base.exception.BusinessException;
import com.viewsources.chsm.common.enums.host.SelfTestTypeEnum;
import com.viewsources.chsm.common.utils.CommonUtils;
import com.viewsources.chsm.common.utils.PokerUtils;
import com.viewsources.chsm.common.utils.selfTest.CipherExecutor;
import com.viewsources.chsm.host.business.ILinuxBusiness;
import com.viewsources.chsm.host.business.IRandomTestBusiness;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.viewsources.chsm.common.enums.host.SelfTestTypeEnum.*;
import static com.viewsources.chsm.common.exception.ChsmCommonErrorEnums.COMMON_ERROR;

/**
 * <p>
 * RandomTestBusinessImpl
 * </p >
 *
 * @author Yohann
 * @since 2023/4/10 17:28
 */
@Slf4j
@Service
public class RandomTestBusinessImpl implements IRandomTestBusiness {

    @Value("${fileManage.gmt-trng-dir}")
    private String gmtAlgorithmDir;
    @Value("${fileManage.gmt-trng-shell}")
    private String gmtAlgorithmShell;
    @Resource
    private ILinuxBusiness linuxBusiness;

    // 检测算法所需配置文件修改项
    private static final String NUM_OF_BITS_PER_STREAM = "num_of_bits_per_stream";
    private static final String NUM_OF_STREAMS = "num_of_streams";
    private static final String THRESHOLD = "threshold";
    private static final Map<String, Integer> TEST_TURN_MAP = new HashMap<String, Integer>(3) {
        {
            // 出厂检测，采集50*1000000比特随机数，分成50组，每组1000000比特。
            put(FACTORY_INSPECTION.getCode(), 196);
            // 上电检测，采集20*1000000比特随机数，分成20组，每组1000000比特。
            put(POWER_ON_DETECTION.getCode(), 79);
            // 使用检测-周期检测，采集4*100000比特随机数，分成20组，每组10000比特。
            put(CYCLE_DETECTION.getCode(), 2);
        }
    };
    private static final List<String> PYTHON_LIST = new ArrayList<String>() {
        {
            add("assess_monobit_frequency_test.py ");
            add("assess_frequency_test_within_a_block_test.py ");
            add("assess_poker_test.py ");
            add("assess_serial_test.py ");
            add("assess_runs_test.py ");
            add("assess_runs_distribution_test.py ");
            add("assess_longest_run_of_ones_in_a_block_test.py ");
            add("assess_binary_derivative_test.py ");
            add("assess_autocorrelation_test.py ");
            add("assess_binary_matrix_rank_test.py ");
            add("assess_cumulative_sums_test.py ");
            add("assess_approximate_entropy_test.py ");
            add("assess_linear_complexity_test.py ");
            add("assess_maurer_universal_statistical_test.py ");
            add("assess_discrete_fourier_transform_test.py ");
        }
    };

    @Override
    public synchronized boolean factoryInspection(String deviceIp) {
        String randomDataFile1 = gmtAlgorithmDir + "random1.bin";
        String randomDataFile2 = gmtAlgorithmDir + "random2.bin";
        // 生成2份随机数二进制文件，第2份异步生成
        generateRandomBin(deviceIp, FACTORY_INSPECTION, randomDataFile1);
        CompletableFuture.runAsync(() -> generateRandomBin(deviceIp, FACTORY_INSPECTION, randomDataFile2));

        boolean result = execTestByType(FACTORY_INSPECTION, randomDataFile1);
        if (!result) {
            log.info("随机数出厂检测第1次检测失败，正在进行第2次检测...");
            result = execTestByType(FACTORY_INSPECTION, randomDataFile2);
        }
        FileUtil.del(randomDataFile2);
        return result;
    }

    @Override
    public synchronized boolean powerOnDetection(String deviceIp) {
        String randomDataFile1 = gmtAlgorithmDir + "random1.bin";
        String randomDataFile2 = gmtAlgorithmDir + "random2.bin";
        // 生成2份随机数二进制文件，第2份异步生成
        generateRandomBin(deviceIp, POWER_ON_DETECTION, randomDataFile1);
        CompletableFuture.runAsync(() -> generateRandomBin(deviceIp, POWER_ON_DETECTION, randomDataFile2));

        boolean result = execTestByType(POWER_ON_DETECTION, randomDataFile1);
        if (!result) {
            log.info("随机数上电检测第1次检测失败，正在进行第2次检测...");
            result = execTestByType(POWER_ON_DETECTION, randomDataFile2);
        }
        FileUtil.del(randomDataFile2);
        return result;
    }

    @Override
    public synchronized boolean cycleDetection(String deviceIp) {
        String randomDataFile1 = gmtAlgorithmDir + "random1.bin";
        String randomDataFile2 = gmtAlgorithmDir + "random2.bin";
        // 生成2份随机数二进制文件，第2份异步生成
        generateRandomBin(deviceIp, CYCLE_DETECTION, randomDataFile1);
        CompletableFuture.runAsync(() -> generateRandomBin(deviceIp, CYCLE_DETECTION, randomDataFile2));

        boolean result = execTestByType(CYCLE_DETECTION, randomDataFile1);
        if (!result) {
            log.info("随机数周期检测第1次检测失败，正在进行第2次检测...");
            result = execTestByType(CYCLE_DETECTION, randomDataFile2);
        }
        FileUtil.del(randomDataFile2);
        return result;
    }

    @Override
    public synchronized boolean singleTest(String deviceIp) {
        CipherExecutor cipherExecutor = CipherExecutor.getInstance(deviceIp);
        boolean isTestSuccess = PokerUtils.powerOnPokerDetect(cipherExecutor::genRandom);
        if (!isTestSuccess) {
            log.error("×××××××××××××××××××× 随机数使用检测-单次检测未通过 ×××××××××××××××××××××");
            return false;
        }
        log.info("====================随机数使用检测-单次检测通过====================");
        return true;
    }

    /**
     * 根据检测类型执行检测
     *
     * @param typeEnum 检测类型
     * @param dataFile 随机数文件
     * @return 结果
     */
    private boolean execTestByType(SelfTestTypeEnum typeEnum, String dataFile) {
        if (!new File(gmtAlgorithmDir).exists()) {
            throw new BusinessException(COMMON_ERROR, "随机数质量检测算法文件存放目录不存在");
        }

        // 根据类型修改json配置文件
        Map<String, Integer> map = getUpdateJsonMap(typeEnum);
        CommonUtils.updateJsonFile(gmtAlgorithmDir + "config.json", map);

        // 15个线程异步执行检测
        boolean result = multiThreadExecTest(typeEnum, dataFile);

        // 删除临时二进制文件
        FileUtil.del(dataFile);

        return result;
    }

    private boolean exec(SelfTestTypeEnum typeEnum, String pyFile, String randomDataFile, AtomicBoolean flag) {
        // 执行任务
        List<String> resList = linuxBusiness.executeCommand("sh " + gmtAlgorithmShell + " "
                + pyFile + gmtAlgorithmDir + " " + randomDataFile);
        // 周期检测只需检测除离散傅立叶检测、线性复杂度检测、通用统计检测外的12项项目检测。
        if (typeEnum.equals(CYCLE_DETECTION)) {
            resList.removeIf(x -> x.contains("discrete_fourier_transform_test"));
            resList.removeIf(x -> x.contains("linear_complexity_test"));
            resList.removeIf(x -> x.contains("maurer_universal_statistical_test"));
        }
//        // 逐行打印结果
//        System.out.println(typeEnum.getDesc() + "结果如下：");
//        resList.forEach(System.out::println);
        // 结果存在false则直接返回
        for (String resLine : resList) {
            if (resLine.contains("False")) {
                flag.set(false);
                return false;
            }
        }
        return true;
    }

    /**
     * 15个线程异步执行检测，有一个失败则直接返回false
     *
     * @param typeEnum       检测类型
     * @param randomDataFile 随机数文件
     * @return 结果
     */
    private boolean multiThreadExecTest(SelfTestTypeEnum typeEnum, String randomDataFile) {
        AtomicBoolean flag = new AtomicBoolean(true);
        ExecutorService executor = new ThreadPoolExecutor(15, 20, 2L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(15),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            int index = i;
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() ->
                    exec(typeEnum, PYTHON_LIST.get(index), randomDataFile, flag), executor);
            futures.add(future);
        }
        CompletableFuture.anyOf(futures.toArray(new CompletableFuture[0]));
        // 遍历所有任务的结果，并判断是否为false
        for (CompletableFuture<Boolean> task : futures) {
            try {
                if (!task.get()) {
                    futures.forEach(x -> {
                        x.cancel(true);
                    });
                    executor.shutdownNow();
                    linuxBusiness.executeCommand("killall -9 python3");
                    return flag.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new BusinessException(COMMON_ERROR, "多线程异步执行检测失败：" + e.getLocalizedMessage());
            }
        }
        executor.shutdownNow();
        return flag.get();
    }

    /**
     * 根据检测类型修改Json文件属性值
     *
     * @param typeEnum 检测类型
     * @return Json文件属性及值
     */
    private Map<String, Integer> getUpdateJsonMap(SelfTestTypeEnum typeEnum) {
        return new HashMap<String, Integer>(3) {
            {
                put(NUM_OF_BITS_PER_STREAM, typeEnum.getCode().equals(CYCLE_DETECTION.getCode()) ? 20000 : 1000000);
                put(NUM_OF_STREAMS, typeEnum.getCode().equals(FACTORY_INSPECTION.getCode()) ? 50 : 20);
                put(THRESHOLD, typeEnum.getCode().equals(FACTORY_INSPECTION.getCode()) ? 47 : 18);
            }
        };
    }

    /**
     * 生成随机数二进制文件
     *
     * @param deviceIp     加密设备地址
     * @param typeEnum     检测类型 每轮生成32000字节
     * @param dataFilePath 随机数文件
     */
    public static void generateRandomBin(String deviceIp, SelfTestTypeEnum typeEnum, String dataFilePath) {
        CipherExecutor cipherExecutor = CipherExecutor.getInstance(deviceIp);

        try (FileOutputStream fos = new FileOutputStream(dataFilePath, true)) {
            for (int i = 0; i < TEST_TURN_MAP.get(typeEnum.getCode()); i++) {
                byte[] bytes = cipherExecutor.genRandom(32000);
                fos.write(bytes);
            }
        } catch (IOException e) {
            throw new BusinessException(COMMON_ERROR, "生成随机数二进制文件失败：" + e.getLocalizedMessage());
        }
    }
}
