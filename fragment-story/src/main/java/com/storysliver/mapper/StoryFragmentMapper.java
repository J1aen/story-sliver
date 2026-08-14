package com.storysliver.mapper;

import com.storysliver.pojo.StoryFragment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 碎片 Mapper：操作 story_fragment 表。
 * 简单 SQL 用注解，动态 SQL（按状态筛选）在 resources/com/storysliver/mapper/StoryFragmentMapper.xml。
 * 为什么用 PageHelper：分页参数由 PageHelper 注入，方法签名不用写 offset/limit。
 */
@Mapper
public interface StoryFragmentMapper {

    /** 新增碎片：status 由 Service 置为 0（待审核）；@Options 把自增 id 回填到 fragment.id */
    @Insert("insert into story_fragment (user_id, content, like_count, is_anonymous, status) " +
            "values (#{userId}, #{content}, #{likeCount}, #{isAnonymous}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(StoryFragment fragment);

    /** 按主键查询：点赞/隐藏/删除前的状态与归属校验 */
    @Select("select id, user_id, content, like_count, is_anonymous, status, created_at, updated_at " +
            "from story_fragment where id = #{id}")
    StoryFragment selectById(Long id);

    /** 首页分页列表：只查已发布，时间倒序；分页由 PageHelper 注入 */
    @Select("select id, user_id, content, like_count, is_anonymous, status, created_at, updated_at, " +
            "(select count(*) from comment c where c.fragment_id = story_fragment.id and c.status = 0) as commentCount " +
            "from story_fragment where status = 1 order by created_at desc, id desc")
    List<StoryFragment> selectPublishedPage();

    /** 我的碎片：按用户查全部状态（待审核/已发布/已隐藏） */
    @Select("select id, user_id, content, like_count, is_anonymous, status, created_at, updated_at, " +
            "(select count(*) from comment c where c.fragment_id = story_fragment.id and c.status = 0) as commentCount " +
            "from story_fragment where user_id = #{userId} order by created_at desc, id desc")
    List<StoryFragment> selectByUser(Long userId);

    /** 管理列表：status 传 null 查全部，传 0/1 按状态筛选（动态 SQL 在 XML） */
    List<StoryFragment> selectManagePage(Integer status);

    /** 状态流转：审核通过（0→1）、隐藏（1→2）、取消隐藏（2→1） */
    @Update("update story_fragment set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 硬删除：物理删行（前端已弹窗确认不可恢复） */
    @Delete("delete from story_fragment where id = #{id}")
    int deleteById(Long id);

    /** 点赞数 +1 */
    @Update("update story_fragment set like_count = like_count + 1 where id = #{id}")
    int increaseLikeCount(Long id);

    /** 点赞数 -1：GREATEST 保证不小于 0 */
    @Update("update story_fragment set like_count = greatest(like_count - 1, 0) where id = #{id}")
    int decreaseLikeCount(Long id);


}
