package com.storysliver.service;

import com.storysliver.common.BusinessException;
import com.storysliver.mapper.AnnouncementMapper;
import com.storysliver.pojo.Announcement;
import com.storysliver.service.impl.AnnouncementServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 公告服务单元测试：删除 = 硬删除（数据库行 + 本地图片文件一起删）；下架不影响。
 */
class AnnouncementServiceTest {

    private AnnouncementMapper mapper;
    private AnnouncementServiceImpl service;
    private Path tmpDir;

    @BeforeEach
    void setUp() throws IOException {
        mapper = mock(AnnouncementMapper.class);
        service = new AnnouncementServiceImpl();
        ReflectionTestUtils.setField(service, "announcementMapper", mapper);
        tmpDir = Files.createTempDirectory("ann-test");
        ReflectionTestUtils.setField(service, "uploadDir", tmpDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.walk(tmpDir).sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
    }

    /** 删除公告：数据库行硬删除，且本地图片文件也要一起删（不留孤儿文件） */
    @Test
    void deleteRemovesRowAndImageFile() throws IOException {
        File img = tmpDir.resolve("ann_1.png").toFile();
        Files.write(img.toPath(), new byte[]{1, 2, 3});
        Announcement a = new Announcement();
        a.setId(1L);
        a.setImageUrl("/uploads/ann_1.png");
        when(mapper.selectById(1L)).thenReturn(a);

        service.delete(1L);

        verify(mapper).deleteById(1L);// 行硬删除
        assertFalse(img.exists(), "公告图片文件应被硬删除");
    }

    /** 没图的公告：照样硬删行，不报错 */
    @Test
    void deleteWithoutImageStillRemovesRow() {
        Announcement a = new Announcement();
        a.setId(1L);
        when(mapper.selectById(1L)).thenReturn(a);

        service.delete(1L);

        verify(mapper).deleteById(1L);
    }

    /** 删除不存在的公告：抛 404，不删任何东西 */
    @Test
    void deleteMissingThrowsNotFound() {
        when(mapper.selectById(1L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.delete(1L));
        verify(mapper, never()).deleteById(any());
    }
}
