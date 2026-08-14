package com.storysliver.mapper;

import com.storysliver.pojo.CommentReport;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 评论举报 Mapper（v2.0 Task 21）：操作 comment_report 表。
 * 干什么用：举报入库、防重复查询、管理端待处理列表、标记已处理。
 */
@Mapper
public interface CommentReportMapper {

    /** 新增举报记录：status 默认 0（待处理） */
    @Insert("insert into comment_report (comment_id, reporter_id, reason) values (#{commentId}, #{reporterId}, #{reason})")
    int insert(CommentReport report);

    /** 查某人对某条评论的举报记录：防重复举报（同一组合唯一键） */
    @Select("select id, comment_id as commentId, reporter_id as reporterId, reason, status, handled_at as handledAt " +
            "from comment_report where comment_id = #{commentId} and reporter_id = #{reporterId}")
    CommentReport selectByCommentAndReporter(@Param("commentId") Long commentId, @Param("reporterId") Long reporterId);

    /** 待处理举报列表：join 评论表和用户表，一次带出展示/操作所需冗余字段；只查待处理，最早的在最前 */
    @Select("select r.id, r.comment_id as commentId, r.reporter_id as reporterId, r.reason, r.status, r.handled_at as handledAt, " +
            "c.content as commentContent, c.fragment_id as fragmentId, c.user_id as commenterId, u.nickname as commenterName " +
            "from comment_report r " +
            "left join comment c on r.comment_id = c.id " +
            "left join `user` u on c.user_id = u.id " +
            "where r.status = 0 order by r.id asc")
    List<CommentReport> selectPending();

    /** 按主键查举报：处理前取详情（评论者 id 等） */
    @Select("select id, comment_id as commentId, reporter_id as reporterId, reason, status, handled_at as handledAt " +
            "from comment_report where id = #{id}")
    CommentReport selectById(Long id);

    /** 标记举报已处理：写入状态与处理时间 */
    @Update("update comment_report set status = 1, handled_at = now() where id = #{id}")
    int markHandled(Long id);
}
