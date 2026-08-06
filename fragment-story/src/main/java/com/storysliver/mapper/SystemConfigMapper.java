package com.storysliver.mapper;

import com.storysliver.pojo.SystemConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统配置 Mapper：操作 system_config 表。
 */
@Mapper
public interface SystemConfigMapper {

    /** 按配置键读取：键不存在返回 null（调用方要判空） */
    @Select("select config_key as configKey, config_value as configValue, updated_at as updatedAt " +
            "from system_config where config_key = #{configKey}")
    SystemConfig selectByKey(String configKey);

    /** 插入或更新：MySQL 的 ON DUPLICATE KEY UPDATE，站长改配置用 */
    @Insert("insert into system_config (config_key, config_value) values (#{configKey}, #{configValue}) " +
            "on duplicate key update config_value = #{configValue}")
    int upsert(@Param("configKey") String configKey, @Param("configValue") String configValue);
}
