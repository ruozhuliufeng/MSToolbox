package com.msop.core.oss.template;

import com.msop.core.oss.model.OssFile;
import com.msop.core.oss.propertis.OssProperties;
import com.msop.core.oss.rule.OssRule;
import com.msop.core.tool.constant.StringConstant;
import com.msop.core.tool.utils.CollectionUtil;
import com.msop.core.tool.utils.Func;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * 七牛云图片操作
 *
 * @author ruozhuliufeng
 */
@Slf4j
public class QiniuTemplate {
    @Resource
    private Auth auth;
    @Resource
    private UploadManager uploadManager;
    @Resource
    private BucketManager bucketManager;
    @Resource
    private OssProperties ossProperties;
    @Resource
    private OssRule ossRule;


    @SneakyThrows
    public void makeBucket(String bucketName) {
        if (!CollectionUtil.contains(bucketManager.buckets(), getBucketName(bucketName))) {
            bucketManager.createBucket(getBucketName(bucketName), Zone.zone0().getRegion());
        }
    }

    @SneakyThrows
    public void removeBucket(String bucketName) {
        bucketManager.deleteBucketEvent(getBucketName(bucketName),null);
    }

    @SneakyThrows
    public boolean bucketExists(String bucketName) {
        return CollectionUtil.contains(bucketManager.buckets(), getBucketName(bucketName));
    }

    @SneakyThrows
    public void copyFile(String bucketName, String fileName, String destBucketName) {
        bucketManager.copy(getBucketName(bucketName), fileName, getBucketName(destBucketName), fileName);
    }

    @SneakyThrows
    public void copyFile(String bucketName, String fileName, String destBucketName, String destFileName) {
        bucketManager.copy(getBucketName(bucketName), fileName, getBucketName(destBucketName), destFileName);
    }

    @SneakyThrows
    public OssFile statFile(String fileName) {
        return statFile(ossProperties.getBucketName(), fileName);
    }


    @SneakyThrows
    public OssFile statFile(String bucketName, String fileName) {
        FileInfo stat = bucketManager.stat(getBucketName(bucketName), fileName);
        OssFile ossFile = new OssFile();
        ossFile.setName(Func.isEmpty(stat.key) ? fileName : stat.key);
        ossFile.setLink(fileLink(ossFile.getName()));
        ossFile.setHash(stat.hash);
        ossFile.setLength(stat.fsize);
        ossFile.setPutTime(new Date(stat.putTime/10000));
        ossFile.setContentType(stat.mimeType);
        return ossFile;
    }

    @SneakyThrows
    public String filePath(String fileName){
        return getBucketName().concat(StringConstant.SLASH).concat(fileName);
    }
    @SneakyThrows
    public String filePath(String bucketName,String fileName){
        return getBucketName(bucketName).concat(StringConstant.SLASH).concat(fileName);
    }


    @SneakyThrows
    public String fileLink(String fileName) {
        return ossProperties.getEndpoint().concat(StringConstant.SLASH).concat(fileName);
    }


    @SneakyThrows
    public String fileLink(String bucketName, String fileName) {
        return ossProperties.getEndpoint().concat(StringConstant.SLASH).concat(fileName);
    }


    /**
     * 获取文件公开链接
     *
     * @param fileName 文件名
     * @return 文件公开链接
     */
    public String publicFileLink(String fileName) {
        return String.format("%s/%s", ossProperties.getEndpoint(), fileName);
    }

    /**
     * 获取文件私有链接
     *
     * @param fileName   文件名
     * @param expireTime 超时时间
     * @return 私有文件链接
     */
    @SneakyThrows
    public String privateFileLink(String fileName, Long expireTime) {
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        String publicUrl = String.format("%s/%s", ossProperties.getEndpoint(), encodedFileName);
        return auth.privateDownloadUrl(publicUrl, expireTime);
    }


    @SneakyThrows
    public com.msop.core.oss.model.FileInfo putFile(MultipartFile file) {
        return putFile(ossProperties.getBucketName(), file.getOriginalFilename(), file);
    }


    @SneakyThrows
    public com.msop.core.oss.model.FileInfo putFile(String fileName, MultipartFile file) {
        return putFile(ossProperties.getBucketName(), fileName, file);
    }


    @SneakyThrows
    public com.msop.core.oss.model.FileInfo putFile(String bucketName, String fileName, MultipartFile file) {
        return putFile(bucketName, fileName, file);
    }


    @SneakyThrows
    public com.msop.core.oss.model.FileInfo putFile(String fileName, InputStream stream) {
        return putFile(ossProperties.getBucketName(), fileName, stream);
    }


    @SneakyThrows
    public com.msop.core.oss.model.FileInfo putFile(String bucketName, String fileName, InputStream stream) {
        return put(bucketName, stream, fileName, false);
    }

    @SneakyThrows
    public com.msop.core.oss.model.FileInfo put(String bucketName, InputStream stream, String key, boolean cover) {
        com.msop.core.oss.model.FileInfo file = new com.msop.core.oss.model.FileInfo();
        file.setOriginalName(key);
        makeBucket(bucketName);
        key = getFileName(key);
        // 覆盖上传
        if (cover) {
            uploadManager.put(stream, key, getUploadToken(bucketName, key), null, null);
        } else {
            Response response = uploadManager.put(stream, key, getUploadToken(bucketName), null, null);
            int retry = 0;
            int retryCount = 5;
            while (response.needRetry() && retry < retryCount) {
                response = uploadManager.put(stream, key, getUploadToken(bucketName), null, null);
                retry++;
            }
        }
        file.setName(key);
        file.setLink(fileLink(bucketName, key));
        return file;
    }


    @SneakyThrows
    public void removeFile(String fileName) {
        bucketManager.delete(getBucketName(), fileName);
    }


    @SneakyThrows
    public void removeFile(String bucketName, String fileName) {
        bucketManager.delete(getBucketName(bucketName), fileName);
    }


    @SneakyThrows
    public void removeFiles(List<String> fileNames) {
        fileNames.forEach(this::removeFile);
    }


    @SneakyThrows
    public void removeFiles(String bucketName, List<String> fileNames) {
        fileNames.forEach(fileName -> removeFile(getBucketName(bucketName), fileName));
    }

    /**
     * 根据规则生成存储桶名称规则
     *
     * @return String
     */
    private String getBucketName() {
        return getBucketName(ossProperties.getBucketName());
    }

    /**
     * 根据规则生成存储桶名称规则
     *
     * @param bucketName 存储桶名称
     * @return String
     */
    private String getBucketName(String bucketName) {
        return ossRule.bucketName(bucketName);
    }

    /**
     * 根据规则生成文件名称规则
     *
     * @param originalFilename 原始文件名
     * @return string
     */
    private String getFileName(String originalFilename) {
        return ossRule.fileName(originalFilename);
    }

    /**
     * 获取上传凭证，普通上传
     */
    public String getUploadToken(String bucketName) {
        return auth.uploadToken(getBucketName(bucketName));
    }

    /**
     * 获取上传凭证，覆盖上传
     */
    private String getUploadToken(String bucketName, String key) {
        return auth.uploadToken(getBucketName(bucketName), key);
    }

}
