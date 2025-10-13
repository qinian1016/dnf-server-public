package com.aiyi.game.dnfserver.pvf;

import com.xiaoyouma.dnf.parser.pvf.coder.PvfCoder;
import com.xiaoyouma.dnf.parser.pvf.model.Pvf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.Charset;

/**
 * @author xiatian
 */
@Component
public class PvfManager {

    Logger logger = LoggerFactory.getLogger(PvfManager.class);

    @PostConstruct
    public void init(){
        File file = new File("Script.pvf");
        if (!file.exists()){
            logger.warn("Script.pvf not found!");
        }
        PvfCoder.initialize(file.getAbsolutePath(), Charset.forName("Big5"));
    }

    public Pvf getPvf(){
        return PvfCoder.getPvf();
    }
}
