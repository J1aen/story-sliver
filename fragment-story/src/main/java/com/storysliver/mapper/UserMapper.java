package com.storysliver.mapper;

import com.storysliver.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
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
    @Select("select id, username, nickname, password, email, role, status, avatar, avatar_pending, avatar_reject_reason, ban_expires_at, ban_reason, created_at, updated_at " +
            "from `user` where username = #{username}")
    User selectByUsername(String username);

    /** 按昵称精确查询（v1.2）：注册/修改昵称时的唯一性校验 */
    @Select("select id, username, nickname, password, email, role, status, avatar, avatar_pending, avatar_reject_reason, ban_expires_at, ban_reason, created_at, updated_at " +
            "from `user` where nickname = #{nickname}")
    User selectByNickname(String nickname);

    /** 按主键查询：我的信息、管理端查看发布者 */
    @Select("select id, username, nickname, password, email, role, status, avatar, avatar_pending, avatar_reject_reason, ban_expires_at, ban_reason, created_at, updated_at " +
            "from `user` where id = #{id}")
    User selectById(Long id);

    /** 公开用户信息（v2.0 Task 20）：只取展示需要的字段，绝不带 password/email */
    @Select("select id, nickname, avatar, role from `user` where id = #{id}")
    User selectPublicById(Long id);

    /** 分页查询用户（管理端用户列表，分页由 PageHelper 注入） */
    @Select("select id, username, nickname, password, email, role, status, avatar, avatar_pending, avatar_reject_reason, ban_expires_at, ban_reason, created_at, updated_at " +
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

    /** 修改昵称（v1.2）：设置下拉里「修改昵称」使用 */
    @Update("update `user` set nickname = #{nickname} where id = #{id}")
    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    /** 修改密码（v1.2）：只存 BCrypt 哈希，旧密码校验通过后调用 */
    @Update("update `user` set password = #{password} where id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    /** 封禁：status=1，记录到期时间（空=永久）与理由（必填） */
    @Update("update `user` set status = 1, ban_expires_at = #{banExpiresAt}, ban_reason = #{banReason} where id = #{id}")
    int ban(@Param("id") Long id, @Param("banExpiresAt") LocalDateTime banExpiresAt, @Param("banReason") String banReason);

    /** 解封：status=0，清空到期时间与理由 */
    @Update("update `user` set status = 0, ban_expires_at = null, ban_reason = null where id = #{id}")
    int unban(@Param("id") Long id);

    /** 设置待审核头像（用户上传头像后调用；同时清掉上次的拒绝原因） */
    @Update("update `user` set avatar_pending = #{avatarPending}, avatar_reject_reason = null where id = #{id}")
    int updateAvatarPending(@Param("id") Long id, @Param("avatarPending") String avatarPending);

    /** 头像审核通过：待审核头像转正（avatar = avatar_pending，并清空待审核与拒绝原因） */
    @Update("update `user` set avatar = avatar_pending, avatar_pending = null, avatar_reject_reason = null where id = #{id} and avatar_pending is not null")
    int approveAvatar(@Param("id") Long id);

    /** 头像审核拒绝：清空待审核头像（保留旧头像），并记录拒绝原因供用户查看 */
    @Update("update `user` set avatar_pending = null, avatar_reject_reason = #{reason} where id = #{id}")
    int rejectAvatar(@Param("id") Long id, @Param("reason") String reason);

    /** 查询有「待审核头像」的用户（管理端审核队列，分页由 PageHelper 注入） */
    @Select("select id, username, nickname, avatar, avatar_pending, avatar_reject_reason, ban_expires_at, ban_reason, role, status, created_at, updated_at " +
            "from `user` where avatar_pending is not null order by id")
    List<User> selectPendingAvatarUsers();
}
