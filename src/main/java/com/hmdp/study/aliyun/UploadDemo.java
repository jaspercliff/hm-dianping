package com.hmdp.study.aliyun;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;

import java.io.ByteArrayInputStream;

import static com.hmdp.config.SystemConstants.*;

public class UploadDemo {
    public static void main(String[] args) {
        String objectName = "hmdp/example1.txt";
        OSS oss = new OSSClientBuilder().build(ENDPOINT, ACCESSKEYID, ACCESSKEYSECRET);
        try {
            String content = "jasper is handsome";
            oss.putObject(BUCKETNAME,objectName,new ByteArrayInputStream(content.getBytes()));
        } catch (OSSException e) {
            System.out.println(e.getMessage());
        } catch (ClientException e) {
            System.out.println(e.getMessage());
        } finally {
            if(oss != null){
                oss.shutdown();
            }
        }

    }
}
