package com.dgq.seckill.common.domain.mapper;

import com.dgq.seckill.common.domain.dataobject.UserDO;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

public interface UserDOMapper {
    int deleteByPrimaryKey(Long id);

    int insert(UserDO record);

    int insertSelective(UserDO record);

    UserDO selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(UserDO record);

    int updateByPrimaryKey(UserDO record);

    Long selectIdByMobile(String mobile);

    UserDO selectByMobile(String mobile);

    List<UserDO> selectByNicknamePrefix(@Param("prefix") String prefix);
}