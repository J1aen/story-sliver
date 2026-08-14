package com.storysliver.mapper;

import com.storysliver.pojo.Comment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 评论 Mapper（v2.0 Task 21）：操作 comment 表。
 * 干什么用：发评论落库、按碎片查评论、按 id 查单条、硬删除。
 * 为什么 SQL 全用注解：都是简单单表 SQL，不需要 XML。
 */
@Mapper
public interface CommentMapper {

    /** 新增评论：status 默认 0（免审核，发布即显示）；@Options 回填自增 id */
    @Insert("insert into comment (fragment_id, user_id, content) values (#{fragmentId}, #{userId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Comment comment);

    /** 按碎片查评论：只查正常（status=0），按 id 正序 = 时间正序（Q5）；分页由 PageHelper 注入 */
    @Select("select id, fragment_id as fragmentId, user_id as userId, content, status, created_at as createdAt " +
            "from comment where fragment_id = #{fragmentId} and status = 0 order by id asc")
    List<Comment> selectByFragment(Long fragmentId);

    /** 按主键查单条：删除前校验归属、举报前校验存在 */
    @Select("select id, fragment_id as fragmentId, user_id as userId, content, status, created_at as createdAt " +
            "from comment where id = #{id}")
    Comment selectById(Long id);

    /** 硬删除：作者本人删除 / 管理端下架共用（前端已弹窗确认不可恢复） */
    @Delete("delete from comment where id = #{id}")
    int deleteById(Long id);
}
