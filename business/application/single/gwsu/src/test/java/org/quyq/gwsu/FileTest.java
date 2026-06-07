package org.quyq.gwsu;


import org.junit.jupiter.api.Test;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

/**
 * @author Quyq
 * @date 2026/6/7
 * @description
 */
@SpringBootTest
public class FileTest {


    @Test
    public void test() {

        KitFileInfoVO upload = FileUtils.upload(new File("/Users/quyq/Downloads/dataquery.tar.gz"));
        System.out.println(upload);

    }

    @Test
    public void test2() {
        FileUtils.delete("2063529660421996544");
    }

}
