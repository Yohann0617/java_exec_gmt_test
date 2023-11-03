package impl;

import cn.hutool.core.util.RuntimeUtil;
import com.viewsources.chsm.host.business.ILinuxBusiness;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * LinuxBusinessImpl
 * </p >
 *
 * @author Yohann
 * @since 2023/3/23 18:50
 */
@Slf4j
@Service
public class LinuxBusinessImpl implements ILinuxBusiness {

    @Override
    public List<String> executeCommand(String command) {
        try {
            return RuntimeUtil.execForLines(command);
        } catch (Exception e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }


}
