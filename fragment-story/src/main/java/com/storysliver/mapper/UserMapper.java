package com.storysliver.mapper;

import com.storysliver.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户 Mapper：操作 user 表。
 * 全部 SQL 简单，直接用注解，不需要 XML。
 */
@Mapper
public interface UserMapper {

    /** 新增用户：@Options 把自增 id 回填到 user.id（注册后签发 token 需要） */
    @Insert("insert into `user` (username, nickname, password, email, role, status) " +
            "values (#{username}, #{nickname}, #{password}, #{email}, #{role}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    /** 按用户名精确查询：登录、注册唯一性校验用 */
    @Select("select id, username, nickname, password, email, role, status, created_at, updated_at " +
            "from `user` where username = #{username}")
    User selectByUsername(String username);

    /** 按主键查询：我的信息、管理端查看发布者 */
    @Select("select id, username, nickname, password, email, role, status, created_at, updated_at " +
            "from `user` where id = #{id}")
    User selectById(Long id);

    /** 分页查询用户（管理端用户列表，分页由 PageHelper 注入） */
    @Select("select id, username, nickname, password, email, role, status, created_at, updated_at " +
            "from `user` order by id")
    List<User> selectPage();

    /** 用户总数：分页 total，以及「第一个注册管理员 = 站长」的判断 */
    @Select("select count(*) from `user`")
    long countAll();

    /** 管理员及以上（role >= 1）的人数：判断「第一个管理员 = 站长」。
     * 为什么不用 countAll：普通用户先注册不影响站长位，
     * 否则「第一个用户恰好是普通用户」会导致站长位永远空着。 */
    @Select("select count(*) from `user` where role >= 1")
    long countAdmin();

    /** 修改角色：站长指定/撤销管理员时使用 */
    @Update("update `user` set role = #{role} where id = #{id}")
    int updateRole(@Param("id") Long id, @Param("role") Integer role);
}
