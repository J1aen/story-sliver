package com.storysliver.mapper;

import com.storysliver.pojo.Announcement;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 公告 Mapper（v1.2）：操作 announcement 表。
 * 干什么用：公开接口取「最新一条上架公告」；站长管理后台做增删改和上下架。
 */
@Mapper
public interface AnnouncementMapper {

    /** 新增公告：@Options 回填自增 id */
    @Insert("insert into announcement (title, content, image_url, status) values (#{title}, #{content}, #{imageUrl}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Announcement announcement);

    /** 编辑公告：只改标题/正文/图片，不改上下架状态 */
    @Update("update announcement set title = #{title}, content = #{content}, image_url = #{imageUrl} where id = #{id}")
    int update(Announcement announcement);

    /** 上架/下架：status 0下架 1上架 */
    @Update("update announcement set status = #{status} where id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 删除公告（站长可选操作） */
    @Delete("delete from announcement where id = #{id}")
    int deleteById(Long id);

    /** 查询最新一条上架公告：进站弹窗用；id 倒序保证「最新」 */
    @Select("select id, title, content, image_url as imageUrl, status, created_at as createdAt, updated_at as updatedAt " +
            "from announcement where status = 1 order by id desc limit 1")
    Announcement selectActiveLatest();

    /** 全部公告（管理后台列表，倒序） */
    @Select("select id, title, content, image_url as imageUrl, status, created_at as createdAt, updated_at as updatedAt " +
            "from announcement order by id desc")
    List<Announcement> selectAll();

    /** 按 id 查询（编辑/上下架前校验存在） */
    @Select("select id, title, content, image_url as imageUrl, status, created_at as createdAt, updated_at as updatedAt " +
            "from announcement where id = #{id}")
    Announcement selectById(Long id);
}
