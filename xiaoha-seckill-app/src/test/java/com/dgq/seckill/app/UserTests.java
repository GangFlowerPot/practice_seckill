package com.dgq.seckill.app;

import com.dgq.seckill.common.domain.dataobject.UserDO;
import com.dgq.seckill.common.domain.mapper.UserDOMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

@SpringBootTest
public class UserTests {

    @Resource
    private UserDOMapper userDOMapper;

    @Test
    void testInsertUser() {
        userDOMapper.insert(UserDO.builder()
                .nickname("test1")
                .password("123456")
                .mobile("13800000000")
                .status(1)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build()
        );
    }
}
