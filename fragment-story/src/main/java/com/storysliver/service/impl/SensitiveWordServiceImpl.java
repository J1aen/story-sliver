package com.storysliver.service.impl;

import com.storysliver.common.BusinessException;
import com.storysliver.common.ResultCode;
import com.storysliver.mapper.SensitiveWordMapper;
import com.storysliver.pojo.SensitiveWord;
import com.storysliver.service.SensitiveWordService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 敏感词服务实现（v1.2）。
 * 为什么用内存缓存：词库量小（几十到几百），每次校验全查库没必要；
 * volatile 保证多线程可见性，增删后 refresh 重新加载。
 */
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;//敏感词表操作

    /** 词库内存缓存：启动时加载，增删后刷新；volatile 保证其他线程立即可见 */
    private volatile List<String> cache = new ArrayList<>();

    /** 启动时加载一次词库 */
    @PostConstruct
    public void init() {
        refresh();
    }

    /** 从数据库重新加载词库缓存 */
    private void refresh() {
        cache = sensitiveWordMapper.selectAllWords();
    }

    @Override
    public boolean containsSensitive(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // 包含匹配：昵称里出现任意敏感词即命中
        for (String word : cache) {
            if (!word.isEmpty() && text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<SensitiveWord> listAll() {
        return sensitiveWordMapper.selectAll();
    }

    @Override
    public void add(String word) {
        word = word == null ? "" : word.trim();
        if (word.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "敏感词不能为空");
        }
        if (word.length() > 50) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "敏感词最长 50 字");
        }
        // 先查重再插入，避免数据库唯一键异常
        if (cache.contains(word)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "敏感词已存在");
        }
        sensitiveWordMapper.insert(word);
        refresh();// 新词立即生效，不用重启
    }

    @Override
    public void delete(Long id) {
        sensitiveWordMapper.deleteById(id);
        refresh();// 删除后立即生效
    }
}
