package com.lab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lab.entity.DatabaseBackup;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据库备份 Mapper
 */
@Mapper
public interface DatabaseBackupMapper extends BaseMapper<DatabaseBackup> {
}
