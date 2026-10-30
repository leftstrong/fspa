package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.barter.EbBarterMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 换物站内消息 Mapper
 */
@Mapper
public interface BarterMessageDao extends BaseMapper<EbBarterMessage> {

    /** 按分组统计未读数（返回 group_type -> count） */
    @Select("SELECT group_type, COUNT(1) FROM eb_barter_message WHERE uid = #{uid} AND is_read = 0 GROUP BY group_type")
    java.util.List<java.util.Map<String, Object>> countUnreadGroup(@Param("uid") Integer uid);

    /** 整组标记已读 */
    @Update("UPDATE eb_barter_message SET is_read = 1, read_time = NOW() WHERE uid = #{uid} AND is_read = 0 AND group_type = #{group}")
    int markGroupRead(@Param("uid") Integer uid, @Param("group") String group);

    /** 全部标记已读 */
    @Update("UPDATE eb_barter_message SET is_read = 1, read_time = NOW() WHERE uid = #{uid} AND is_read = 0")
    int markAllRead(@Param("uid") Integer uid);
}
