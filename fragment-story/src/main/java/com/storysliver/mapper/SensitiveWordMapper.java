package com.storysliver.mapper;

import com.storysliver.pojo.SensitiveWord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 敏感词 Mapper（v1.2）：操作 sensitive_word 表。
 * 干什么用：注册/改昵称校验取全部词；站长管理后台增删。
 */
@Mapper
public interface SensitiveWordMapper {

    /** 取全部敏感词：校验时用 contains 匹配（词量小，直接全量加载即可） */
    @Select("select word from sensitive_word")
    List<String> selectAllWords();

    /** 取全部记录（管理后台列表，倒序：新加的在前） */
    @Select("select id, word, created_at as createdAt from sensitive_word order by id desc")
    List<SensitiveWord> selectAll();

    /** 新增敏感词（word 唯一，重复会抛数据库唯一键异常，Service 里先查重） */
    @Insert("insert into sensitive_word (word) values (#{word})")
    int insert(String word);

    /** 删除敏感词 */
    @Delete("delete from sensitive_word where id = #{id}")
    int deleteById(Long id);
}
