package com.storysliver.mapper;

import com.storysliver.pojo.FragmentLike;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 点赞 Mapper：操作 fragment_like 表。
 */
@Mapper
public interface FragmentLikeMapper {

    /** 插入一条点赞记录：@Options 回填自增 id */
    @Insert("insert into fragment_like (fragment_id, user_id) values (#{fragmentId}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FragmentLike like);

    /** 删除点赞：userId 传 null 时删除该碎片全部点赞（硬删除清理用），动态 SQL 在 XML */
    int deleteByFragmentAndUser(@Param("fragmentId") Long fragmentId, @Param("userId") Long userId);

    /** 判断某用户是否赞过某碎片：点赞幂等校验 */
    @Select("select count(*) > 0 from fragment_like where fragment_id = #{fragmentId} and user_id = #{userId}")
    boolean exists(@Param("fragmentId") Long fragmentId, @Param("userId") Long userId);

    /** 查询某用户赞过的全部碎片 id：首页列表用于标记「已赞」状态 */
    @Select("select fragment_id from fragment_like where user_id = #{userId}")
    List<Long> selectLikedFragmentIds(Long userId);
}
