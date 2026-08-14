package com.storysliver.service;

import com.storysliver.pojo.SensitiveWord;
import java.util.List;

/**
 * 敏感词服务接口（v1.2）。
 * 干什么用：注册/改昵称时校验文本是否含敏感词；站长管理后台维护词库。
 */
public interface SensitiveWordService {

    /** 判断文本是否包含任意敏感词（包含匹配） */
    boolean containsSensitive(String text);

    /** 全部敏感词（管理后台列表） */
    List<SensitiveWord> listAll();

    /** 新增敏感词（去空格、查重、刷新缓存） */
    void add(String word);

    /** 删除敏感词（刷新缓存） */
    void delete(Long id);
}
