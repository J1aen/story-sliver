package com.storysliver.service.impl;

import com.storysliver.common.BusinessException;
import com.storysliver.common.ResultCode;
import com.storysliver.mapper.AnnouncementMapper;
import com.storysliver.pojo.Admin.AnnouncementRequest;
import com.storysliver.pojo.Announcement;
import com.storysliver.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 公告服务实现（v1.2）。
 * 为什么公告图片和头像共用一个上传目录：/uploads/** 静态映射已配好，直接复用省配置。
 */
@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;//公告表操作

    /** 上传目录（与头像同一个，来自 application.properties） */
    @Value("${app.upload.avatar-dir}")
    private String uploadDir;

    /** 公告图片大小上限：5MB */
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024L;

    @Override
    public Announcement getActive() {
        return announcementMapper.selectActiveLatest();
    }

    @Override
    public List<Announcement> listAll() {
        return announcementMapper.selectAll();
    }

    @Override
    public void create(AnnouncementRequest request) {
        Announcement a = new Announcement();
        a.setTitle(request.getTitle().trim());
        a.setContent(request.getContent().trim());
        a.setImageUrl(request.getImageUrl());
        a.setStatus(0);// 默认下架：站长确认后再手动上架，避免误发
        announcementMapper.insert(a);
    }

    @Override
    public void update(Long id, AnnouncementRequest request) {
        requireExists(id);
        Announcement a = new Announcement();
        a.setId(id);
        a.setTitle(request.getTitle().trim());
        a.setContent(request.getContent().trim());
        a.setImageUrl(request.getImageUrl());
        announcementMapper.update(a);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        requireExists(id);
        // 只允许 0/1，防止非法值
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能为 0 或 1");
        }
        announcementMapper.updateStatus(id, status);
    }

    @Override
    public void delete(Long id) {
        requireExists(id);
        announcementMapper.deleteById(id);
    }

    @Override
    public String uploadImage(MultipartFile file) {
        // 类型校验：只允许常见图片格式
        String contentType = file == null ? null : file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/webp") || contentType.equals("image/gif"))) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "公告图片只支持 jpg/png/webp/gif 格式");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "公告图片不能超过 5MB");
        }
        // 生成唯一文件名并保存（ann_ 前缀区分公告图片）
        String fileName = "ann_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 10000) + ".png";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File target = new File(dir, fileName);
        try {
            BufferedImage src = ImageIO.read(file.getInputStream());
            if (src == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "无法解析图片");
            }
            ImageIO.write(src, "png", target);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "公告图片保存失败");
        }
        return "/uploads/" + fileName;
    }

    /** 操作前确认公告存在，不存在抛 404 */
    private void requireExists(Long id) {
        if (announcementMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.ANNOUNCEMENT_NOT_FOUND);
        }
    }
}
