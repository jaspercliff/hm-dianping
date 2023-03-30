package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.hmdp.config.SystemConstants;
import com.hmdp.dto.Result;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static com.hmdp.config.SystemConstants.*;

public class UploadUtil {

    public static String upload(MultipartFile file){
        String result = "";
        String originalFileName = file.getOriginalFilename();
        String newFileName = createNewFileName(originalFileName);
        OSS oss = new OSSClientBuilder().build(ENDPOINT, ACCESSKEYID, ACCESSKEYSECRET);
        try {
            PutObjectRequest request = new PutObjectRequest(BUCKETNAME, newFileName, new ByteArrayInputStream(file.getBytes()));
            request.setProcess("true");//设置返回response
            PutObjectResult putObjectResult = oss.putObject(request);
            System.out.println(putObjectResult.getResponse().getStatusCode());
            String filePath = "http://"+BUCKETNAME+"."+ENDPOINT+"/"+newFileName;
            result+=filePath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            oss.shutdown();
        }
        return result;
    }

    public static String createNewFileName(String originalFileName){
//        获取后缀
        String suffix = StrUtil.subAfter(originalFileName, '.', true);
//        生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash>>4)& 0xF;
////        判断目录是否存在
//        File dir = new File(SystemConstants.IMAGE_UPLOAD_DIR,StrUtil.format("/blogs/{}/{}",d1,d2));
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        file name
        return StrUtil.format("blogs/{}/{}/{}.{}",d1,d2,name,suffix);
    }
}
