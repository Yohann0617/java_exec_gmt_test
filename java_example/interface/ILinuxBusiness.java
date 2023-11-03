package com.viewsources.chsm.host.business;

import com.viewsources.chsm.common.bean.response.syscfg.ResDnsConfigInfo;
import com.viewsources.chsm.common.bean.response.syscfg.ResNpt;

import java.util.List;

/**
 * <p>
 * ILinuxBusiness
 * </p >
 *
 * @author Yohann
 * @since 2023/3/23 18:50
 */
public interface ILinuxBusiness {

    /**
     * 执行命令行
     *
     * @param command 命令行
     * @return 结果的所有行
     */
    List<String> executeCommand(String command);
}
