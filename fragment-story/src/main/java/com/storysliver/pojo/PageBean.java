package com.storysliver.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果包装：配合 PageHelper 使用。
 * 干什么用：total 给前端判断「还有没有下一页」，list 是本页数据。
 * 为什么这样设计：照抄 sims 项目的 PageBean，格式统一。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageBean {
    private Long total;//总记录数
    private List list;//本页数据列表
}
