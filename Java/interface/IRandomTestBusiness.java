package com.viewsources.chsm.host.business;

/**
 * <p>
 * IRandomTestBusiness
 * </p >
 *
 * @author yuhui.fan
 * @since 2023/4/10 17:28
 */
public interface IRandomTestBusiness {

    /**
     * 出厂检测 （5min）
     *
     * @param deviceIp 加密设备
     * @return 检测结果
     */
    boolean factoryInspection(String deviceIp);

    /**
     * 上电检测 （1min30s）
     *
     * @param deviceIp 加密设备
     * @return 检测结果
     */
    boolean powerOnDetection(String deviceIp);

    /**
     * 使用检测-周期检测  （3s）
     * 检测项目:对采集随机数按照GB/T 32915中除离散傅立叶检测、线性复杂度检测、通用统计检测外的12项项目检测。
     *
     * @param deviceIp 加密设备
     * @return 检测结果
     */
    boolean cycleDetection(String deviceIp);

    /**
     * 使用检测-单次检测
     * 检测项目:扑克检测，参数m=2。
     *
     * @param deviceIp 加密设备
     * @return 检测结果
     */
    boolean singleTest(String deviceIp);
}
